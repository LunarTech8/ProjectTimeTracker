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
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.romanbrunner.apps.projecttimetracker.data.DailyTimePoolRepository;
import com.romanbrunner.apps.projecttimetracker.data.TimeEntryRepository;
import com.romanbrunner.apps.projecttimetracker.util.PreferencesManager;
import com.romanbrunner.apps.projecttimetracker.util.TimeUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
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
    public enum PoolResetInterval
    {
        DAILY, WEEKLY, MONTHLY, YEARLY, NEVER
    }

    private final Context context;
    private final RecyclerView rvPools;
    private final DailyTimePoolRepository dailyTimePoolRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final PreferencesManager preferencesManager;
    private final TextView tvPoolResetInterval;
    private PoolAdapter poolAdapter;
    private PoolResetInterval poolResetInterval = PoolResetInterval.NEVER;

    public TimePoolsManager(Context context, RecyclerView rvPools, DailyTimePoolRepository dailyTimePoolRepository, TimeEntryRepository timeEntryRepository, PreferencesManager preferencesManager, TextView tvPoolResetInterval)
    {
        this.context = context;
        this.rvPools = rvPools;
        this.dailyTimePoolRepository = dailyTimePoolRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.preferencesManager = preferencesManager;
        this.tvPoolResetInterval = tvPoolResetInterval;
        restorePoolResetInterval();
        setupResetIntervalDropdown();
    }

    public void setupRecyclerView()
    {
        poolAdapter = new PoolAdapter(getPoolData());
        rvPools.setLayoutManager(new LinearLayoutManager(context));
        rvPools.setAdapter(poolAdapter);
    }

    private void setupResetIntervalDropdown()
    {
        tvPoolResetInterval.setOnClickListener(v -> showResetIntervalPopup());
        updateResetIntervalLabel();
    }

    private void showResetIntervalPopup()
    {
        PopupMenu popup = new PopupMenu(context, tvPoolResetInterval);
        for (PoolResetInterval interval : PoolResetInterval.values())
        {
            int id = interval.ordinal();
            int titleRes = getIntervalStringResource(interval);
            popup.getMenu().add(0, id, id, titleRes);
        }

        popup.setOnMenuItemClickListener(item ->
        {
            int id = item.getItemId();
            if (id >= 0 && id < PoolResetInterval.values().length)
            {
                PoolResetInterval selected = PoolResetInterval.values()[id];
                setPoolResetInterval(selected);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void setPoolResetInterval(PoolResetInterval interval)
    {
        poolResetInterval = interval;
        preferencesManager.setPoolResetInterval(interval.name());
        updateResetIntervalLabel();
        refreshPoolsData();
    }

    private void restorePoolResetInterval()
    {
        try
        {
            poolResetInterval = PoolResetInterval.valueOf(preferencesManager.getPoolResetInterval());
        }
        catch (IllegalArgumentException e)
        {
            poolResetInterval = PoolResetInterval.NEVER;
        }
    }

    private void updateResetIntervalLabel()
    {
        int intervalRes = getIntervalStringResource(poolResetInterval);
        String intervalName = context.getString(intervalRes);
        String label = context.getString(R.string.ctp_reset_interval_format, intervalName);
        tvPoolResetInterval.setText(label);
    }

    public PoolResetInterval getPoolResetInterval()
    {
        return poolResetInterval;
    }

    /**
     * Helper to get the string resource ID for a given interval.
     */
    private static int getIntervalStringResource(PoolResetInterval interval)
    {
        switch (interval)
        {
            case DAILY:
                return R.string.pool_reset_daily;
            case WEEKLY:
                return R.string.pool_reset_weekly;
            case MONTHLY:
                return R.string.pool_reset_monthly;
            case YEARLY:
                return R.string.pool_reset_yearly;
            case NEVER:
            default:
                return R.string.pool_reset_never;
        }
    }

    /**
     * Helper to calculate period bounds and pool size for a given interval.
     * Returns {periodStart, periodEnd, poolSeconds}
     */
    public static class PeriodCalculation
    {
        public final Date periodStart;
        public final Date periodEnd;
        public final long poolSeconds;

        public PeriodCalculation(Date periodStart, Date periodEnd, long poolSeconds)
        {
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
            this.poolSeconds = poolSeconds;
        }
    }

    public static PeriodCalculation calculatePeriod(PoolResetInterval interval, int dailyMinutes)
    {
        Calendar cal = Calendar.getInstance();
        Date periodStart;
        Date periodEnd;
        long poolSeconds;

        switch (interval)
        {
            case DAILY:
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                periodStart = cal.getTime();
                cal.add(Calendar.DAY_OF_YEAR, 1);
                periodEnd = cal.getTime();
                poolSeconds = (long)dailyMinutes * SECONDS_PER_MINUTE;
                break;
            case WEEKLY:
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                int daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7;
                cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                periodStart = cal.getTime();
                cal.add(Calendar.DAY_OF_YEAR, 7);
                periodEnd = cal.getTime();
                poolSeconds = (long)dailyMinutes * SECONDS_PER_MINUTE * 7;
                break;
            case MONTHLY:
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                periodStart = cal.getTime();
                int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                cal.add(Calendar.MONTH, 1);
                periodEnd = cal.getTime();
                poolSeconds = (long)dailyMinutes * SECONDS_PER_MINUTE * daysInMonth;
                break;
            case YEARLY:
                cal.set(Calendar.DAY_OF_YEAR, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                periodStart = cal.getTime();
                int daysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
                cal.add(Calendar.YEAR, 1);
                periodEnd = cal.getTime();
                poolSeconds = (long)dailyMinutes * SECONDS_PER_MINUTE * daysInYear;
                break;
            case NEVER:
            default:
                // For NEVER, return null to indicate full history calculation
                return null;
        }

        return new PeriodCalculation(periodStart, periodEnd, poolSeconds);
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

        PeriodCalculation period = calculatePeriod(poolResetInterval, dailyMinutes);
        if (period != null)
        {
            long usedSeconds = timeEntryRepository.getTotalDurationForCategoryInRange(category, period.periodStart, period.periodEnd);
            return period.poolSeconds - usedSeconds;
        }
        else
        {
            // NEVER: calculate from earliest entry
            Date earliestDate = timeEntryRepository.getEarliestStartDateForCategory(category);
            int days = TimeUtils.daysBetween(earliestDate, new Date());
            long poolSeconds = (long)dailyMinutes * SECONDS_PER_MINUTE * days;
            long usedSeconds = timeEntryRepository.getTotalDurationForCategory(category);
            return poolSeconds - usedSeconds;
        }
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
