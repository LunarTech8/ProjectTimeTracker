package com.romanbrunner.apps.projecttimetracker;

import android.content.Context;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.romanbrunner.apps.projecttimetracker.data.DailyTimePoolRepository;
import com.romanbrunner.apps.projecttimetracker.data.TimeEntryRepository;
import com.romanbrunner.apps.projecttimetracker.util.TimeUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manager class for time pool functionality.
 */
public class TimePoolsManager
{
    // Constants:
    private static final int SECONDS_PER_MINUTE = 60;

    private final Context context;
    private final RecyclerView rvPools;
    private final DailyTimePoolRepository dailyTimePoolRepository;
    private final TimeEntryRepository timeEntryRepository;
    private PoolAdapter poolAdapter;

    public TimePoolsManager(Context context, RecyclerView rvPools, DailyTimePoolRepository dailyTimePoolRepository, TimeEntryRepository timeEntryRepository)
    {
        this.context = context;
        this.rvPools = rvPools;
        this.dailyTimePoolRepository = dailyTimePoolRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    public void setupRecyclerView()
    {
        poolAdapter = new PoolAdapter(getPoolData());
        rvPools.setLayoutManager(new LinearLayoutManager(context));
        rvPools.setAdapter(poolAdapter);
    }

    public void refreshPoolsData()
    {
        if (poolAdapter != null)
        {
            poolAdapter.updateData(getPoolData());
        }
    }

    public void loadPoolsFromFile(Uri uri)
    {
        try
        {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null)
            {
                dailyTimePoolRepository.importFromTextFile(inputStream);
                inputStream.close();
                refreshPoolsData();
                Toast.makeText(context, "Pools loaded successfully", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e)
        {
            Toast.makeText(context, "Error loading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public void savePoolsToFile(Uri uri)
    {
        try
        {
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream != null)
            {
                dailyTimePoolRepository.exportToTextFile(outputStream);
                outputStream.close();
                Toast.makeText(context, "Pools saved successfully", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e)
        {
            Toast.makeText(context, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public void showRemoveCategoryDialog()
    {
        List<String> categories = new ArrayList<>(dailyTimePoolRepository.getCategories());
        if (categories.isEmpty())
        {
            Toast.makeText(context, "No categories to remove", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] categoryArray = categories.toArray(new String[0]);
        new AlertDialog.Builder(context)
                .setTitle(R.string.remove_category)
                .setItems(categoryArray, (dialog, which) ->
                {
                    String categoryToRemove = categoryArray[which];
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.confirm_delete)
                            .setMessage("Remove category \"" + categoryToRemove + "\"?")
                            .setPositiveButton(R.string.delete, (d, w) ->
                            {
                                timeEntryRepository.removeEntriesByCategory(categoryToRemove);
                                dailyTimePoolRepository.removeCategory(categoryToRemove);
                                refreshPoolsData();
                                Toast.makeText(context, "Category and its entries removed", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public void showAddCategoryDialog()
    {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_category, null);
        TextInputEditText etCategory = dialogView.findViewById(R.id.et_category_name);
        TextInputEditText etDailyMinutes = dialogView.findViewById(R.id.et_daily_minutes);
        new AlertDialog.Builder(context)
                .setTitle(R.string.add_category)
                .setView(dialogView)
                .setPositiveButton(R.string.add, (dialog, which) ->
                {
                    String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
                    String minutesStr = etDailyMinutes.getText() != null ? etDailyMinutes.getText().toString() : "0";
                    int minutes = 0;
                    try
                    {
                        minutes = Integer.parseInt(minutesStr);
                    }
                    catch (NumberFormatException e)
                    {
                    }
                    if (!category.isEmpty())
                    {
                        dailyTimePoolRepository.setDailyMinutes(category, minutes);
                        refreshPoolsData();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private List<CategoryPoolData> getPoolData()
    {
        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(dailyTimePoolRepository.getCategories());
        allCategories.addAll(timeEntryRepository.getAllCategories());
        List<CategoryPoolData> data = new ArrayList<>();
        for (String category : allCategories)
        {
            int dailyMinutes = dailyTimePoolRepository.getDailyMinutes(category);
            long totalSeconds = timeEntryRepository.getTotalDurationForCategory(category);
            long poolSeconds = calculatePoolTime(category, dailyMinutes);
            data.add(new CategoryPoolData(category, dailyMinutes, poolSeconds, totalSeconds));
        }
        Collections.sort(data, (a, b) -> a.category.compareToIgnoreCase(b.category));
        return data;
    }

    private long calculatePoolTime(String category, int dailyMinutes)
    {
        if (dailyMinutes <= 0)
        {
            return 0;
        }
        Date earliestDate = timeEntryRepository.getEarliestStartDateForCategory(category);
        int days = TimeUtils.daysBetween(earliestDate, new Date());
        long poolSeconds = (long)dailyMinutes * SECONDS_PER_MINUTE * days;
        long usedSeconds = timeEntryRepository.getTotalDurationForCategory(category);
        return poolSeconds - usedSeconds;
    }

    private static class CategoryPoolData
    {
        String category;
        int dailyMinutes;
        long poolSeconds;
        long totalSeconds;

        CategoryPoolData(String category, int dailyMinutes, long poolSeconds, long totalSeconds)
        {
            this.category = category;
            this.dailyMinutes = dailyMinutes;
            this.poolSeconds = poolSeconds;
            this.totalSeconds = totalSeconds;
        }
    }

    private class PoolAdapter extends RecyclerView.Adapter<PoolAdapter.ViewHolder>
    {
        private List<CategoryPoolData> data;

        PoolAdapter(List<CategoryPoolData> data)
        {
            this.data = new ArrayList<>(data);
        }

        void updateData(List<CategoryPoolData> newData)
        {
            this.data = new ArrayList<>(newData);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_pool, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            CategoryPoolData item = data.get(position);
            holder.tvCategory.setText(item.category);
            holder.etDailyMinutes.removeTextChangedListener(holder.textWatcher);
            holder.etDailyMinutes.setText(String.valueOf(item.dailyMinutes));
            holder.tvPoolTime.setText(TimeUtils.formatDuration(Math.abs(item.poolSeconds)));
            if (item.poolSeconds >= 0)
            {
                holder.tvPoolTime.setTextColor(context.getResources().getColor(R.color.pool_positive, null));
            }
            else
            {
                holder.tvPoolTime.setTextColor(context.getResources().getColor(R.color.pool_negative, null));
            }
            holder.tvTotalTime.setText(TimeUtils.formatDuration(item.totalSeconds));
            holder.textWatcher = new TextWatcher()
            {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s)
                {
                    int minutes = 0;
                    try
                    {
                        minutes = Integer.parseInt(s.toString());
                    }
                    catch (NumberFormatException e)
                    {
                    }
                    dailyTimePoolRepository.setDailyMinutes(item.category, minutes);
                    long newPoolSeconds = calculatePoolTime(item.category, minutes);
                    holder.tvPoolTime.setText(TimeUtils.formatDuration(Math.abs(newPoolSeconds)));
                    if (newPoolSeconds >= 0)
                    {
                        holder.tvPoolTime.setTextColor(context.getResources().getColor(R.color.pool_positive, null));
                    }
                    else
                    {
                        holder.tvPoolTime.setTextColor(context.getResources().getColor(R.color.pool_negative, null));
                    }
                }
            };
            holder.etDailyMinutes.addTextChangedListener(holder.textWatcher);
        }

        @Override
        public int getItemCount()
        {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder
        {
            TextView tvCategory, tvPoolTime, tvTotalTime;
            EditText etDailyMinutes;
            TextWatcher textWatcher;

            ViewHolder(View itemView)
            {
                super(itemView);
                tvCategory = itemView.findViewById(R.id.tv_pool_category);
                etDailyMinutes = itemView.findViewById(R.id.et_pool_daily_minutes);
                tvPoolTime = itemView.findViewById(R.id.tv_pool_remaining);
                tvTotalTime = itemView.findViewById(R.id.tv_pool_total);
            }
        }
    }
}
