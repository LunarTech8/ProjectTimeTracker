package com.romanbrunner.apps.projecttimetracker;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.romanbrunner.apps.projecttimetracker.data.DailyTimePoolRepository;
import com.romanbrunner.apps.projecttimetracker.data.TimeEntryRepository;
import com.romanbrunner.apps.projecttimetracker.model.TimeEntry;
import com.romanbrunner.apps.projecttimetracker.util.TimeUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Configuration constants (matching Python version)
    private static final int[] REMINDER_INTERVAL_CHOICES = {0, 15, 30, 60, 120}; // minutes
    private static final int UPDATE_INTERVAL = 1000; // milliseconds
    private static final int REMINDER_ALERT_DURATION = 3000; // milliseconds
    private static final int REMINDER_BEEP_INTERVAL = 250; // milliseconds
    private static final int REMINDER_BEEP_FREQUENCY = 3000; // Hz

    // UI Components
    private Button btnStartStop;
    private Button btnEnd;
    private MaterialAutoCompleteTextView spinnerReminder;
    private MaterialAutoCompleteTextView spinnerProject;
    private MaterialAutoCompleteTextView spinnerCategory;
    private TextView tvCurrentDuration;
    private TextView tvTotalProjectDuration;
    private TextView tvTotalCategoryDuration;
    private TextView tvPoolTime;
    private TextView tvStartDate;
    private RecyclerView rvEntries;
    private RecyclerView rvPools;
    private RadioGroup radioGroupSections;
    private MaterialCardView cardControlPanel;
    private MaterialCardView cardEntries;
    private MaterialCardView cardPools;
    private Button btnLoadEntries;
    private Button btnSaveEntries;
    private Button btnLoadPools;
    private Button btnSavePools;

    // File pickers
    private ActivityResultLauncher<String[]> loadEntriesFileLauncher;
    private ActivityResultLauncher<String> saveEntriesFileLauncher;
    private ActivityResultLauncher<String[]> loadPoolsFileLauncher;
    private ActivityResultLauncher<String> savePoolsFileLauncher;

    // Data
    private TimeEntryRepository timeEntryRepository;
    private DailyTimePoolRepository dailyTimePoolRepository;
    private TimeEntryAdapter adapter;
    private PoolAdapter poolAdapter;

    // State
    private Date firstStartDatetime = null;
    private Date currentStartDatetime = null;
    private long accumulatedDurationSeconds = 0;
    private int reminderIntervalSeconds = 0;
    private int nextReminderSeconds = 0;
    private Date alertUntilDatetime = null;
    private boolean isRunning = false;
    private boolean isPaused = false;

    // Handler for periodic updates
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning && !isPaused) {
                updateCurrentDuration();
            }
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    private final Runnable alertRunnable = new Runnable() {
        @Override
        public void run() {
            if (alertUntilDatetime != null && new Date().before(alertUntilDatetime)) {
                flashAndBeep();
                handler.postDelayed(this, REMINDER_BEEP_INTERVAL);
            } else {
                alertUntilDatetime = null;
                tvCurrentDuration.setTextColor(getResources().getColor(R.color.text_primary, null));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize file pickers
        initializeFilePickers();

        // Initialize repositories
        timeEntryRepository = new TimeEntryRepository(this);
        dailyTimePoolRepository = new DailyTimePoolRepository(this);

        // Initialize UI
        initializeViews();
        setupSpinners();
        setupRecyclerView();
        setupPoolsRecyclerView();
        updateTotalDurations();
        updatePoolTime();

        // Start periodic updates
        handler.post(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
        handler.removeCallbacks(alertRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from CategoryPoolsActivity
        updatePoolTime();
        updateSpinnerData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_category_pools) {
            startActivity(new Intent(this, CategoryPoolsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        btnStartStop = findViewById(R.id.btn_start_stop);
        btnEnd = findViewById(R.id.btn_end);
        spinnerReminder = findViewById(R.id.spinner_reminder);
        spinnerProject = findViewById(R.id.spinner_project);
        spinnerCategory = findViewById(R.id.spinner_category);
        tvCurrentDuration = findViewById(R.id.tv_current_duration);
        tvTotalProjectDuration = findViewById(R.id.tv_total_project_duration);
        tvTotalCategoryDuration = findViewById(R.id.tv_total_category_duration);
        tvPoolTime = findViewById(R.id.tv_pool_time);
        tvStartDate = findViewById(R.id.tv_start_date);
        rvEntries = findViewById(R.id.rv_entries);
        rvPools = findViewById(R.id.rv_pools);
        btnLoadEntries = findViewById(R.id.btn_load_entries);
        btnSaveEntries = findViewById(R.id.btn_save_entries);
        btnLoadPools = findViewById(R.id.btn_load_pools);
        btnSavePools = findViewById(R.id.btn_save_pools);

        radioGroupSections = findViewById(R.id.radio_group_sections);
        cardControlPanel = findViewById(R.id.card_control_panel);
        cardEntries = findViewById(R.id.card_entries);
        cardPools = findViewById(R.id.card_pools);

        btnStartStop.setOnClickListener(v -> onStartStopClicked());
        btnEnd.setOnClickListener(v -> onEndClicked());
        btnLoadEntries.setOnClickListener(v -> loadEntriesFileLauncher.launch(new String[]{"text/plain"}));
        btnSaveEntries.setOnClickListener(v -> saveEntriesFileLauncher.launch("MetaDataProjectTime.txt"));
        btnLoadPools.setOnClickListener(v -> loadPoolsFileLauncher.launch(new String[]{"text/plain"}));
        btnSavePools.setOnClickListener(v -> savePoolsFileLauncher.launch("MetaDataDailyTimePools.txt"));

        // Setup radio group listener
        radioGroupSections.setOnCheckedChangeListener((group, checkedId) -> {
            cardControlPanel.setVisibility(checkedId == R.id.radio_control ? View.VISIBLE : View.GONE);
            cardEntries.setVisibility(checkedId == R.id.radio_entries ? View.VISIBLE : View.GONE);
            cardPools.setVisibility(checkedId == R.id.radio_pools ? View.VISIBLE : View.GONE);
        });
    }

    private void setupSpinners() {
        // Reminder interval spinner
        String[] reminderChoices = new String[REMINDER_INTERVAL_CHOICES.length];
        for (int i = 0; i < REMINDER_INTERVAL_CHOICES.length; i++) {
            reminderChoices[i] = String.valueOf(REMINDER_INTERVAL_CHOICES[i]);
        }
        ArrayAdapter<String> reminderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, reminderChoices);
        spinnerReminder.setAdapter(reminderAdapter);
        spinnerReminder.setText(reminderChoices[0], false);
        spinnerReminder.setOnItemClickListener((parent, view, position, id) -> {
            reminderIntervalSeconds = REMINDER_INTERVAL_CHOICES[position] * 60;
            updateNextReminder();
        });

        // Project and category spinners
        updateSpinnerData();

        // Listen for changes
        spinnerProject.setOnItemClickListener((parent, view, position, id) -> {
            updateTotalDurations();
            updatePoolTime();
        });

        spinnerCategory.setOnItemClickListener((parent, view, position, id) -> {
            updateProjectsForCategory();
            updateTotalDurations();
            updatePoolTime();
        });
    }

    private void updateSpinnerData() {
        // Categories (set up first)
        List<String> categories = new ArrayList<>(timeEntryRepository.getAllCategories());
        // Add categories from pools
        categories.addAll(dailyTimePoolRepository.getCategories());
        List<String> uniqueCategories = new ArrayList<>(new java.util.HashSet<>(categories));
        Collections.sort(uniqueCategories);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, uniqueCategories);
        spinnerCategory.setAdapter(categoryAdapter);
        if (spinnerCategory.getText().toString().isEmpty() && !uniqueCategories.isEmpty()) {
            spinnerCategory.setText(uniqueCategories.get(0), false);
        }

        // Projects (filter by selected category)
        updateProjectsForCategory();
    }

    private void updateProjectsForCategory() {
        String category = spinnerCategory.getText().toString();
        List<String> projects = new ArrayList<>(timeEntryRepository.getProjectsForCategory(category));
        Collections.sort(projects);
        ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, projects);
        spinnerProject.setAdapter(projectAdapter);
        // Reset to first project in the filtered list
        if (!projects.isEmpty()) {
            spinnerProject.setText(projects.get(0), false);
        }
    }

    private void setupRecyclerView() {
        adapter = new TimeEntryAdapter(timeEntryRepository.getAllEntries());
        rvEntries.setLayoutManager(new LinearLayoutManager(this));
        rvEntries.setAdapter(adapter);
    }

    private void setupPoolsRecyclerView() {
        poolAdapter = new PoolAdapter(getPoolData());
        rvPools.setLayoutManager(new LinearLayoutManager(this));
        rvPools.setAdapter(poolAdapter);
    }

    private List<CategoryPoolData> getPoolData() {
        // Combine categories from pools and entries
        java.util.Set<String> allCategories = new java.util.HashSet<>();
        allCategories.addAll(dailyTimePoolRepository.getCategories());
        allCategories.addAll(timeEntryRepository.getAllCategories());

        List<CategoryPoolData> data = new ArrayList<>();
        for (String category : allCategories) {
            int dailyMinutes = dailyTimePoolRepository.getDailyMinutes(category);
            long totalSeconds = timeEntryRepository.getTotalDurationForCategory(category);
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

        Date earliestDate = timeEntryRepository.getEarliestStartDateForCategory(category);
        if (firstStartDatetime != null && firstStartDatetime.before(earliestDate)) {
            earliestDate = firstStartDatetime;
        }

        int days = TimeUtils.daysBetween(earliestDate, new Date());
        long poolSeconds = (long) dailyMinutes * 60 * days;
        long usedSeconds = timeEntryRepository.getTotalDurationForCategory(category);

        // Add current session if running and same category
        if (isRunning && category.equals(spinnerCategory.getText().toString())) {
            usedSeconds += getTotalCurrentDurationSeconds();
        }

        return poolSeconds - usedSeconds;
    }

    private void onStartStopClicked() {
        if (!isRunning) {
            // Start
            isRunning = true;
            isPaused = false;
            currentStartDatetime = new Date();
            if (firstStartDatetime == null) {
                firstStartDatetime = currentStartDatetime;
                tvStartDate.setText(TimeUtils.formatDateTimeForDisplay(firstStartDatetime));
            }
            btnStartStop.setText(R.string.pause);
            updateNextReminder();
        } else if (!isPaused) {
            // Pause
            isPaused = true;
            accumulatedDurationSeconds += getCurrentSessionSeconds();
            currentStartDatetime = null;
            btnStartStop.setText(R.string.resume);
        } else {
            // Resume
            isPaused = false;
            currentStartDatetime = new Date();
            btnStartStop.setText(R.string.pause);
            updateNextReminder();
        }
    }

    private void onEndClicked() {
        if (firstStartDatetime == null) {
            return;
        }

        // Calculate final duration
        if (currentStartDatetime != null) {
            accumulatedDurationSeconds += getCurrentSessionSeconds();
        }

        // Save entry
        TimeEntry entry = new TimeEntry(
                spinnerProject.getText().toString(),
                spinnerCategory.getText().toString(),
                accumulatedDurationSeconds,
                firstStartDatetime
        );
        timeEntryRepository.addEntry(entry);

        // Reset state
        resetState();

        // Update UI
        updateSpinnerData();
        updateTotalDurations();
        updatePoolTime();
        refreshEntryList();
    }

    private void resetState() {
        firstStartDatetime = null;
        currentStartDatetime = null;
        accumulatedDurationSeconds = 0;
        nextReminderSeconds = 0;
        isRunning = false;
        isPaused = false;
        btnStartStop.setText(R.string.start);
        tvCurrentDuration.setText(TimeUtils.formatDuration(0));
        tvStartDate.setText("-");
    }

    private long getCurrentSessionSeconds() {
        if (currentStartDatetime == null) {
            return 0;
        }
        return (new Date().getTime() - currentStartDatetime.getTime()) / 1000;
    }

    private long getTotalCurrentDurationSeconds() {
        return accumulatedDurationSeconds + getCurrentSessionSeconds();
    }

    private void updateCurrentDuration() {
        long totalSeconds = getTotalCurrentDurationSeconds();
        tvCurrentDuration.setText(TimeUtils.formatDuration(totalSeconds));
        updateTotalDurations();
        updatePoolTime();

        // Check for reminder
        if (nextReminderSeconds > 0 && reminderIntervalSeconds > 0 && totalSeconds >= nextReminderSeconds) {
            triggerReminder();
            nextReminderSeconds += reminderIntervalSeconds;
        }
    }

    private void updateTotalDurations() {
        String project = spinnerProject.getText().toString();
        String category = spinnerCategory.getText().toString();

        long totalProject = timeEntryRepository.getTotalDurationForProject(project) +
                (project.equals(spinnerProject.getText().toString()) ? getTotalCurrentDurationSeconds() : 0);
        long totalCategory = timeEntryRepository.getTotalDurationForCategory(category) +
                (category.equals(spinnerCategory.getText().toString()) ? getTotalCurrentDurationSeconds() : 0);

        // Only add current session if it matches the selected project/category
        if (isRunning) {
            totalProject = timeEntryRepository.getTotalDurationForProject(project) + getTotalCurrentDurationSeconds();
            totalCategory = timeEntryRepository.getTotalDurationForCategory(category) + getTotalCurrentDurationSeconds();
        }

        tvTotalProjectDuration.setText(TimeUtils.formatDuration(totalProject));
        tvTotalCategoryDuration.setText(TimeUtils.formatDuration(totalCategory));
    }

    private void updatePoolTime() {
        String category = spinnerCategory.getText().toString();
        int dailyMinutes = dailyTimePoolRepository.getDailyMinutes(category);

        if (dailyMinutes <= 0) {
            tvPoolTime.setText("-");
            tvPoolTime.setTextColor(getResources().getColor(R.color.text_primary, null));
            return;
        }

        // Calculate remaining pool time
        Date earliestDate = timeEntryRepository.getEarliestStartDateForCategory(category);
        if (firstStartDatetime != null && firstStartDatetime.before(earliestDate)) {
            earliestDate = firstStartDatetime;
        }

        int days = TimeUtils.daysBetween(earliestDate, new Date());
        long poolSeconds = (long) dailyMinutes * 60 * days;
        long usedSeconds = timeEntryRepository.getTotalDurationForCategory(category);

        // Add current session if running and same category
        if (isRunning && category.equals(spinnerCategory.getText().toString())) {
            usedSeconds += getTotalCurrentDurationSeconds();
        }

        long remainingSeconds = poolSeconds - usedSeconds;
        String timeStr = TimeUtils.formatDuration(Math.abs(remainingSeconds));
        tvPoolTime.setText(remainingSeconds < 0 ? "-" + timeStr : timeStr);

        if (remainingSeconds >= 0) {
            tvPoolTime.setTextColor(getResources().getColor(R.color.pool_positive, null));
        } else {
            tvPoolTime.setTextColor(getResources().getColor(R.color.pool_negative, null));
        }
    }

    private void updateNextReminder() {
        if (reminderIntervalSeconds > 0 && isRunning && !isPaused) {
            long currentSeconds = getTotalCurrentDurationSeconds();
            nextReminderSeconds = (int) ((currentSeconds / reminderIntervalSeconds) + 1) * reminderIntervalSeconds;
        } else {
            nextReminderSeconds = 0;
        }
    }

    private void triggerReminder() {
        alertUntilDatetime = new Date(System.currentTimeMillis() + REMINDER_ALERT_DURATION);
        handler.post(alertRunnable);
    }

    private void flashAndBeep() {
        // Flash color
        int currentColor = tvCurrentDuration.getCurrentTextColor();
        int flashColor = getResources().getColor(R.color.reminder_flash, null);
        int normalColor = getResources().getColor(R.color.text_primary, null);
        tvCurrentDuration.setTextColor(currentColor == flashColor ? normalColor : flashColor);

        // Beep
        try {
            ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
        } catch (Exception e) {
            // Fallback to vibration
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private void refreshEntryList() {
        adapter.updateEntries(timeEntryRepository.getAllEntries());
        poolAdapter.updateData(getPoolData());
    }

    // Data class for Category Pool
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

    // RecyclerView Adapter for Pools
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

            String poolTimeStr = TimeUtils.formatDuration(Math.abs(item.poolSeconds));
            holder.tvPoolTime.setText(item.poolSeconds < 0 ? "-" + poolTimeStr : poolTimeStr);
            if (item.poolSeconds >= 0) {
                holder.tvPoolTime.setTextColor(getResources().getColor(R.color.pool_positive, null));
            } else {
                holder.tvPoolTime.setTextColor(getResources().getColor(R.color.pool_negative, null));
            }

            holder.tvTotalTime.setText(TimeUtils.formatDuration(item.totalSeconds));

            // Set up text watcher for daily minutes
            holder.textWatcher = new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    int minutes = 0;
                    try {
                        minutes = Integer.parseInt(s.toString());
                    } catch (NumberFormatException e) {
                        // Keep default 0
                    }
                    dailyTimePoolRepository.setDailyMinutes(item.category, minutes);

                    // Update pool time display
                    long newPoolSeconds = calculatePoolTime(item.category, minutes);
                    String poolStr = TimeUtils.formatDuration(Math.abs(newPoolSeconds));
                    holder.tvPoolTime.setText(newPoolSeconds < 0 ? "-" + poolStr : poolStr);
                    if (newPoolSeconds >= 0) {
                        holder.tvPoolTime.setTextColor(getResources().getColor(R.color.pool_positive, null));
                    } else {
                        holder.tvPoolTime.setTextColor(getResources().getColor(R.color.pool_negative, null));
                    }

                    // Update main pool time if this is the selected category
                    if (item.category.equals(spinnerCategory.getText().toString())) {
                        updatePoolTime();
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
            android.widget.EditText etDailyMinutes;
            android.text.TextWatcher textWatcher;

            ViewHolder(View itemView) {
                super(itemView);
                tvCategory = itemView.findViewById(R.id.tv_pool_category);
                etDailyMinutes = itemView.findViewById(R.id.et_pool_daily_minutes);
                tvPoolTime = itemView.findViewById(R.id.tv_pool_remaining);
                tvTotalTime = itemView.findViewById(R.id.tv_pool_total);
            }
        }
    }

    // RecyclerView Adapter for Time Entries
    private class TimeEntryAdapter extends RecyclerView.Adapter<TimeEntryAdapter.ViewHolder> {
        private List<TimeEntry> entries;

        TimeEntryAdapter(List<TimeEntry> entries) {
            this.entries = new ArrayList<>(entries);
            // Reverse to show newest first
            Collections.reverse(this.entries);
        }

        void updateEntries(List<TimeEntry> newEntries) {
            this.entries = new ArrayList<>(newEntries);
            Collections.reverse(this.entries);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_time_entry, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TimeEntry entry = entries.get(position);
            holder.tvProject.setText(entry.getProject());
            holder.tvCategory.setText(entry.getCategory());
            holder.tvDuration.setText(TimeUtils.formatDuration(entry.getDurationSeconds()));
            holder.tvStartTime.setText(TimeUtils.formatDateTimeForDisplay(entry.getStartTime()));

            holder.btnRemove.setOnClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.confirm_delete)
                        .setMessage(R.string.confirm_delete_message)
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            // Find actual index in repository (entries are reversed)
                            int actualIndex = timeEntryRepository.getEntryCount() - 1 - position;
                            timeEntryRepository.removeEntry(actualIndex);
                            updateSpinnerData();
                            updateTotalDurations();
                            updatePoolTime();
                            refreshEntryList();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvProject, tvCategory, tvDuration, tvStartTime;
            ImageButton btnRemove;

            ViewHolder(View itemView) {
                super(itemView);
                tvProject = itemView.findViewById(R.id.tv_entry_project);
                tvCategory = itemView.findViewById(R.id.tv_entry_category);
                tvDuration = itemView.findViewById(R.id.tv_entry_duration);
                tvStartTime = itemView.findViewById(R.id.tv_entry_start_time);
                btnRemove = itemView.findViewById(R.id.btn_remove_entry);
            }
        }
    }

    /**
     * Initialize file picker launchers for Load/Save functionality.
     */
    private void initializeFilePickers() {
        // Load entries file picker
        loadEntriesFileLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        loadEntriesFromFile(uri);
                    }
                }
        );

        // Save entries file picker
        saveEntriesFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/plain"),
                uri -> {
                    if (uri != null) {
                        saveEntriesToFile(uri);
                    }
                }
        );

        // Load pools file picker
        loadPoolsFileLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        loadPoolsFromFile(uri);
                    }
                }
        );

        // Save pools file picker
        savePoolsFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/plain"),
                uri -> {
                    if (uri != null) {
                        savePoolsToFile(uri);
                    }
                }
        );
    }

    /**
     * Load time entries from a text file in Python format.
     */
    private void loadEntriesFromFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                timeEntryRepository.importFromTextFile(inputStream);
                inputStream.close();

                // Refresh UI
                updateSpinnerData();
                updateTotalDurations();
                updatePoolTime();
                refreshEntryList();

                Toast.makeText(this, "Entries loaded successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Save time entries to a text file in Python format.
     */
    private void saveEntriesToFile(Uri uri) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                timeEntryRepository.exportToTextFile(outputStream);
                outputStream.close();

                Toast.makeText(this, "Entries saved successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Load pools from a text file in Python format.
     */
    private void loadPoolsFromFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                dailyTimePoolRepository.importFromTextFile(inputStream);
                inputStream.close();

                // Refresh UI
                updatePoolTime();
                poolAdapter.notifyDataSetChanged();

                Toast.makeText(this, "Pools loaded successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Save pools to a text file in Python format.
     */
    private void savePoolsToFile(Uri uri) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                dailyTimePoolRepository.exportToTextFile(outputStream);
                outputStream.close();

                Toast.makeText(this, "Pools saved successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
