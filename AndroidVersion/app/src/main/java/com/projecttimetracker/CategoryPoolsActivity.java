package com.projecttimetracker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.projecttimetracker.data.DailyTimePoolRepository;
import com.projecttimetracker.data.TimeEntryRepository;
import com.projecttimetracker.model.DailyTimePool;
import com.projecttimetracker.model.TimeEntry;
import com.projecttimetracker.util.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryPoolsActivity extends AppCompatActivity {

    private RecyclerView rvPools;
    private FloatingActionButton fabAdd;
    private DailyTimePoolRepository poolRepository;
    private TimeEntryRepository entryRepository;
    private PoolAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_pools);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.category_pools_title);
        }

        poolRepository = new DailyTimePoolRepository(this);
        entryRepository = new TimeEntryRepository(this);

        rvPools = findViewById(R.id.rv_pools);
        fabAdd = findViewById(R.id.fab_add_pool);

        setupRecyclerView();

        fabAdd.setOnClickListener(v -> showAddCategoryDialog());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupRecyclerView() {
        adapter = new PoolAdapter(getPoolData());
        rvPools.setLayoutManager(new LinearLayoutManager(this));
        rvPools.setAdapter(adapter);
    }

    private List<CategoryPoolData> getPoolData() {
        // Combine categories from pools and entries
        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(poolRepository.getCategories());
        allCategories.addAll(entryRepository.getAllCategories());

        List<CategoryPoolData> data = new ArrayList<>();
        for (String category : allCategories) {
            int dailyMinutes = poolRepository.getDailyMinutes(category);
            long totalSeconds = entryRepository.getTotalDurationForCategory(category);
            long poolSeconds = calculatePoolTime(category, dailyMinutes);
            data.add(new CategoryPoolData(category, dailyMinutes, poolSeconds, totalSeconds));
        }

        Collections.sort(data, (a, b) -> a.category.compareToIgnoreCase(b.category));
        return data;
    }

    private long calculatePoolTime(String category, int dailyMinutes) {
        if (dailyMinutes <= 0) {
            return 0;
        }

        Date earliestDate = entryRepository.getEarliestStartDateForCategory(category);
        int days = TimeUtils.daysBetween(earliestDate, new Date());
        long poolSeconds = (long) dailyMinutes * 60 * days;
        long usedSeconds = entryRepository.getTotalDurationForCategory(category);

        return poolSeconds - usedSeconds;
    }

    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        TextInputEditText etCategory = dialogView.findViewById(R.id.et_category_name);
        TextInputEditText etDailyMinutes = dialogView.findViewById(R.id.et_daily_minutes);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_category)
                .setView(dialogView)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
                    String minutesStr = etDailyMinutes.getText() != null ? etDailyMinutes.getText().toString() : "0";
                    int minutes = 0;
                    try {
                        minutes = Integer.parseInt(minutesStr);
                    } catch (NumberFormatException e) {
                        // Keep default 0
                    }

                    if (!category.isEmpty()) {
                        poolRepository.setDailyMinutes(category, minutes);
                        adapter.updateData(getPoolData());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // Data class
    private static class CategoryPoolData {
        String category;
        int dailyMinutes;
        long poolSeconds;
        long totalSeconds;

        CategoryPoolData(String category, int dailyMinutes, long poolSeconds, long totalSeconds) {
            this.category = category;
            this.dailyMinutes = dailyMinutes;
            this.poolSeconds = poolSeconds;
            this.totalSeconds = totalSeconds;
        }
    }

    // Adapter
    private class PoolAdapter extends RecyclerView.Adapter<PoolAdapter.ViewHolder> {
        private List<CategoryPoolData> data;

        PoolAdapter(List<CategoryPoolData> data) {
            this.data = new ArrayList<>(data);
        }

        void updateData(List<CategoryPoolData> newData) {
            this.data = new ArrayList<>(newData);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_pool, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryPoolData item = data.get(position);

            holder.tvCategory.setText(item.category);
            holder.etDailyMinutes.removeTextChangedListener(holder.textWatcher);
            holder.etDailyMinutes.setText(String.valueOf(item.dailyMinutes));

            holder.tvPoolTime.setText(TimeUtils.formatDuration(Math.abs(item.poolSeconds)));
            if (item.poolSeconds >= 0) {
                holder.tvPoolTime.setTextColor(getResources().getColor(R.color.pool_positive, null));
            } else {
                holder.tvPoolTime.setTextColor(getResources().getColor(R.color.pool_negative, null));
            }

            holder.tvTotalTime.setText(TimeUtils.formatDuration(item.totalSeconds));

            // Set up text watcher for daily minutes
            holder.textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    int minutes = 0;
                    try {
                        minutes = Integer.parseInt(s.toString());
                    } catch (NumberFormatException e) {
                        // Keep default 0
                    }
                    poolRepository.setDailyMinutes(item.category, minutes);

                    // Update pool time display
                    long newPoolSeconds = calculatePoolTime(item.category, minutes);
                    holder.tvPoolTime.setText(TimeUtils.formatDuration(Math.abs(newPoolSeconds)));
                    if (newPoolSeconds >= 0) {
                        holder.tvPoolTime.setTextColor(getResources().getColor(R.color.pool_positive, null));
                    } else {
                        holder.tvPoolTime.setTextColor(getResources().getColor(R.color.pool_negative, null));
                    }
                }
            };
            holder.etDailyMinutes.addTextChangedListener(holder.textWatcher);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCategory, tvPoolTime, tvTotalTime;
            EditText etDailyMinutes;
            TextWatcher textWatcher;

            ViewHolder(View itemView) {
                super(itemView);
                tvCategory = itemView.findViewById(R.id.tv_pool_category);
                etDailyMinutes = itemView.findViewById(R.id.et_pool_daily_minutes);
                tvPoolTime = itemView.findViewById(R.id.tv_pool_remaining);
                tvTotalTime = itemView.findViewById(R.id.tv_pool_total);
            }
        }
    }
}
