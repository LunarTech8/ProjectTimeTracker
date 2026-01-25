package com.romanbrunner.apps.projecttimetracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.romanbrunner.apps.projecttimetracker.data.DailyTimePoolRepository;
import com.romanbrunner.apps.projecttimetracker.data.TimeEntryRepository;
import com.romanbrunner.apps.projecttimetracker.model.TimeEntry;
import com.romanbrunner.apps.projecttimetracker.util.PreferencesManager;
import com.romanbrunner.apps.projecttimetracker.util.TimeUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Main activity for time tracking.
 */
public class MainActivity extends AppCompatActivity {
    // Constants:
    private static final int[] REMINDER_INTERVAL_CHOICES = {0, 15, 30, 45, 60, 90, 120};
    private static final int UPDATE_INTERVAL = 1000;
    private static final int REMINDER_REQUEST_CODE = 1000;
    private static final int FLASH_DURATION = 3000;
    private static final int FLASH_INTERVAL = 250;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;

    // Static reference for callbacks from BroadcastReceiver
    private static MainActivity currentInstance;

    // UI Components
    private Button btnStartStop;
    private Button btnReset;
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
    private RecyclerView rvSectionSelector;
    private MaterialCardView cardControlPanel;
    private MaterialCardView cardEntries;
    private MaterialCardView cardOverview;
    private MaterialCardView cardPools;
    private SectionSelectorAdapter sectionSelectorAdapter;
    private int selectedSectionIndex = 0;
    private Button btnLoadEntries;
    private Button btnSaveEntries;
    private TimeOverviewChartManager chartManager;
    private CategoryPoolsManager poolsManager;
    private LineChart chartMain;
    private RecyclerView rvPoolsMain;
    private Button btnTimeRangeWeekMain;
    private Button btnTimeRangeMonthMain;
    private Button btnTimeRangeYearMain;
    private Button btnTimeRangeFullMain;
    private Button btnLoadPoolsMain;
    private Button btnSavePoolsMain;
    private Button btnRemoveCategoryMain;
    private Button btnAddPoolMain;
    private ActivityResultLauncher<String[]> loadPoolsFileLauncher;
    private ActivityResultLauncher<String> savePoolsFileLauncher;

    // File pickers
    private ActivityResultLauncher<String[]> loadEntriesFileLauncher;
    private ActivityResultLauncher<String> saveEntriesFileLauncher;

    // Data
    private TimeEntryRepository timeEntryRepository;
    private DailyTimePoolRepository dailyTimePoolRepository;
    private PreferencesManager preferencesManager;
    private TimeEntryAdapter adapter;
    private AlarmManager alarmManager;

    // State
    private Date firstStartDatetime = null;
    private Date currentStartDatetime = null;
    private long accumulatedDurationSeconds = 0;
    private int reminderIntervalSeconds = 0;
    private int nextReminderSeconds = 0;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private Date flashUntilDatetime = null;
    private boolean isInitialSetup = true; // Flag to track initial app setup

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

    private final Runnable flashRunnable = new Runnable() {
        @Override
        public void run() {
            if (flashUntilDatetime != null && new Date().before(flashUntilDatetime)) {
                toggleFlash();
                handler.postDelayed(this, FLASH_INTERVAL);
            } else {
                flashUntilDatetime = null;
                // Reset to default text color
                if (tvCurrentDuration != null) {
                    tvCurrentDuration.setTextColor(tvStartDate.getCurrentTextColor());
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeFilePickers();
        timeEntryRepository = new TimeEntryRepository(this);
        dailyTimePoolRepository = new DailyTimePoolRepository(this);
        preferencesManager = new PreferencesManager(this);
        alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        initializeViews();
        setupSpinners();
        setupRecyclerView();
        updateTotalDurations();
        updatePoolTime();
        restoreReminderInterval();
        isInitialSetup = false;
        handler.post(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
        handler.removeCallbacks(flashRunnable);
        cancelReminderAlarm();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentInstance = this;
        updatePoolTime();
        updateSpinnerData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        currentInstance = null;
    }

    private void initializeViews() {
        btnStartStop = findViewById(R.id.btn_start_stop);
        btnReset = findViewById(R.id.btn_reset);
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
        btnLoadEntries = findViewById(R.id.btn_load_entries);
        btnSaveEntries = findViewById(R.id.btn_save_entries);
        rvSectionSelector = findViewById(R.id.rv_section_selector);
        cardControlPanel = findViewById(R.id.card_control_panel);
        cardEntries = findViewById(R.id.card_entries);
        cardOverview = findViewById(R.id.card_overview);
        cardPools = findViewById(R.id.card_pools);
        chartMain = findViewById(R.id.chart_main);
        rvPoolsMain = findViewById(R.id.rv_pools_main);
        btnTimeRangeWeekMain = findViewById(R.id.btn_time_range_week_main);
        btnTimeRangeMonthMain = findViewById(R.id.btn_time_range_month_main);
        btnTimeRangeYearMain = findViewById(R.id.btn_time_range_year_main);
        btnTimeRangeFullMain = findViewById(R.id.btn_time_range_full_main);
        ImageButton btnTimePrevMain = findViewById(R.id.btn_time_prev_main);
        ImageButton btnTimeNextMain = findViewById(R.id.btn_time_next_main);
        TextView tvTimeRangeLabelMain = findViewById(R.id.tv_time_range_label_main);
        btnLoadPoolsMain = findViewById(R.id.btn_load_pools_main);
        btnSavePoolsMain = findViewById(R.id.btn_save_pools_main);
        btnRemoveCategoryMain = findViewById(R.id.btn_remove_category_main);
        btnAddPoolMain = findViewById(R.id.btn_add_pool_main);
        btnStartStop.setOnClickListener(v -> onStartStopClicked());
        btnReset.setOnClickListener(v -> onResetClicked());
        btnEnd.setOnClickListener(v -> onEndClicked());
        btnLoadEntries.setOnClickListener(v -> loadEntriesFileLauncher.launch(new String[]{"text/plain"}));
        btnSaveEntries.setOnClickListener(v -> saveEntriesFileLauncher.launch("MetaDataProjectTime.txt"));
        chartManager = new TimeOverviewChartManager(chartMain, timeEntryRepository, btnTimePrevMain, btnTimeNextMain, tvTimeRangeLabelMain);
        poolsManager = new CategoryPoolsManager(this, rvPoolsMain, dailyTimePoolRepository, timeEntryRepository);
        btnTimeRangeWeekMain.setOnClickListener(v -> setTimeRangeMode(TimeOverviewChartManager.TimeRangeMode.WEEK));
        btnTimeRangeMonthMain.setOnClickListener(v -> setTimeRangeMode(TimeOverviewChartManager.TimeRangeMode.MONTH));
        btnTimeRangeYearMain.setOnClickListener(v -> setTimeRangeMode(TimeOverviewChartManager.TimeRangeMode.YEAR));
        btnTimeRangeFullMain.setOnClickListener(v -> setTimeRangeMode(TimeOverviewChartManager.TimeRangeMode.FULL));
        btnTimePrevMain.setOnClickListener(v -> chartManager.navigateTimePrevious());
        btnTimeNextMain.setOnClickListener(v -> chartManager.navigateTimeNext());
        btnLoadPoolsMain.setOnClickListener(v -> loadPoolsFileLauncher.launch(new String[]{"text/plain"}));
        btnSavePoolsMain.setOnClickListener(v -> savePoolsFileLauncher.launch("MetaDataDailyTimePools.txt"));
        btnRemoveCategoryMain.setOnClickListener(v -> poolsManager.showRemoveCategoryDialog());
        btnAddPoolMain.setOnClickListener(v -> poolsManager.showAddCategoryDialog());
        setupSectionSelector();
        chartManager.setupChart();
        poolsManager.setupRecyclerView();
        initializePoolsFilePickers();
    }

    private void setupSpinners() {
        // Reminder interval spinner:
        String[] reminderChoices = new String[REMINDER_INTERVAL_CHOICES.length];
        for (int i = 0; i < REMINDER_INTERVAL_CHOICES.length; i++) {
            reminderChoices[i] = String.valueOf(REMINDER_INTERVAL_CHOICES[i]);
        }
        ArrayAdapter<String> reminderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, reminderChoices);
        spinnerReminder.setAdapter(reminderAdapter);
        spinnerReminder.setText(reminderChoices[0], false);
        spinnerReminder.setOnItemClickListener((parent, view, position, id) -> {
            updateReminderInterval();
        });
        // Handle custom input when focus is lost:
        spinnerReminder.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateReminderInterval();
            }
        });
        // Project and category spinners:
        updateSpinnerData();
        // Listen for changes:
        spinnerProject.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProject = spinnerProject.getText().toString();
            // Save the selected project:
            if (!isInitialSetup) {
                preferencesManager.setLastProject(selectedProject);
            }
            updateTotalDurations();
            updatePoolTime();
        });
        spinnerCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCategory = spinnerCategory.getText().toString();
            // Save the selected category:
            if (!isInitialSetup) {
                preferencesManager.setLastCategory(selectedCategory);
            }
            updateProjectsForCategory();
            updateTotalDurations();
            updatePoolTime();
        });
    }

    private void updateReminderInterval() {
        try {
            String text = spinnerReminder.getText().toString().trim();
            if (!text.isEmpty()) {
                int minutes = Integer.parseInt(text);
                reminderIntervalSeconds = minutes * 60;
                // Save the reminder interval:
                preferencesManager.setLastReminder(minutes);
                updateNextReminder();
            }
        } catch (NumberFormatException e) {
            // Invalid input, keep current value:
            Toast.makeText(this, "Invalid reminder interval", Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreReminderInterval() {
        int lastReminder = preferencesManager.getLastReminder();
        if (lastReminder > 0) {
            spinnerReminder.setText(String.valueOf(lastReminder), false);
            reminderIntervalSeconds = lastReminder * 60;
            updateNextReminder();
        }
    }

    private void updateSpinnerData() {
        // Categories (set up first):
        List<String> categories = new ArrayList<>(timeEntryRepository.getAllCategories());
        // Add categories from pools:
        categories.addAll(dailyTimePoolRepository.getCategories());
        List<String> uniqueCategories = new ArrayList<>(new java.util.HashSet<>(categories));
        Collections.sort(uniqueCategories);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, uniqueCategories);
        spinnerCategory.setAdapter(categoryAdapter);
        if (spinnerCategory.getText().toString().isEmpty() && !uniqueCategories.isEmpty()) {
            // Try to restore last selected category:
            String lastCategory = preferencesManager.getLastCategory();
            if (!lastCategory.isEmpty() && uniqueCategories.contains(lastCategory)) {
                spinnerCategory.setText(lastCategory, false);
            } else {
                // Fall back to first category if no saved category or it doesn't exist:
                spinnerCategory.setText(uniqueCategories.get(0), false);
            }
        }
        // Projects (filter by selected category):
        updateProjectsForCategory();
    }

    private void updateProjectsForCategory() {
        String category = spinnerCategory.getText().toString();
        List<String> projects = new ArrayList<>(timeEntryRepository.getProjectsForCategory(category));
        Collections.sort(projects);
        ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, projects);
        // Save current project selection before updating adapter:
        String currentProject = spinnerProject.getText().toString();
        spinnerProject.setAdapter(projectAdapter);
        // Set project selection based on context:
        if (!projects.isEmpty()) {
            if (currentProject.isEmpty() || !projects.contains(currentProject)) {
                // During initial setup, try to restore last selected project:
                if (isInitialSetup) {
                    String lastProject = preferencesManager.getLastProject();
                    if (!lastProject.isEmpty() && projects.contains(lastProject)) {
                        spinnerProject.setText(lastProject, false);
                        return;
                    }
                }
                // During runtime category change, select project with most time:
                String projectWithMostTime = projects.get(0);
                long maxDuration = 0;
                for (String project : projects) {
                    long duration = timeEntryRepository.getTotalDurationForProject(project);
                    if (duration > maxDuration) {
                        maxDuration = duration;
                        projectWithMostTime = project;
                    }
                }
                spinnerProject.setText(projectWithMostTime, false);
            } else {
                // Restore the current project selection if it's still valid:
                spinnerProject.setText(currentProject, false);
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new TimeEntryAdapter(timeEntryRepository.getAllEntries());
        rvEntries.setLayoutManager(new LinearLayoutManager(this));
        rvEntries.setAdapter(adapter);
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
        long poolSeconds = (long)dailyMinutes * 60 * days;
        long usedSeconds = timeEntryRepository.getTotalDurationForCategory(category);
        // Add current session if running and same category:
        if (isRunning && category.equals(spinnerCategory.getText().toString())) {
            usedSeconds += getTotalCurrentDurationSeconds();
        }
        return poolSeconds - usedSeconds;
    }

    private void onStartStopClicked() {
        if (!isRunning) {
            // Start:
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
            // Pause:
            isPaused = true;
            accumulatedDurationSeconds += getCurrentSessionSeconds();
            currentStartDatetime = null;
            cancelReminderAlarm();
            btnStartStop.setText(R.string.resume);
        } else {
            // Resume:
            isPaused = false;
            currentStartDatetime = new Date();
            btnStartStop.setText(R.string.pause);
            updateNextReminder();
        }
        updateButtonVisibility();
    }

    private void onResetClicked() {
        if (firstStartDatetime == null) {
            return;
        }
        // Reset state without saving:
        resetState();
        // Update UI:
        updateTotalDurations();
        updatePoolTime();
    }

    private void onEndClicked() {
        if (firstStartDatetime == null) {
            return;
        }
        // Calculate final duration:
        if (currentStartDatetime != null) {
            accumulatedDurationSeconds += getCurrentSessionSeconds();
        }
        // Save entry:
        TimeEntry entry = new TimeEntry(
                spinnerProject.getText().toString(),
                spinnerCategory.getText().toString(),
                accumulatedDurationSeconds,
                firstStartDatetime
        );
        timeEntryRepository.addEntry(entry);
        // Reset state:
        resetState();
        // Update UI:
        updateSpinnerData();
        updateTotalDurations();
        updatePoolTime();
        refreshEntryList();
    }

    private void resetState() {
        cancelReminderAlarm();
        firstStartDatetime = null;
        currentStartDatetime = null;
        accumulatedDurationSeconds = 0;
        nextReminderSeconds = 0;
        isRunning = false;
        isPaused = false;
        btnStartStop.setText(R.string.start);
        tvCurrentDuration.setText(TimeUtils.formatDuration(0));
        tvStartDate.setText("-");
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        boolean timerActive = isRunning || isPaused;
        btnReset.setVisibility(timerActive ? View.VISIBLE : View.GONE);
        btnEnd.setVisibility(timerActive ? View.VISIBLE : View.GONE);
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
        // Check for reminder - AlarmManager handles the actual triggering:
        // This is just a fallback check in case we missed a scheduled alarm:
        if (nextReminderSeconds > 0 && reminderIntervalSeconds > 0 && totalSeconds >= nextReminderSeconds) {
            // Time has passed the reminder point, reschedule for next interval:
            nextReminderSeconds += reminderIntervalSeconds;
            long delayMillis = (nextReminderSeconds - totalSeconds) * 1000;
            if (delayMillis > 0) {
                scheduleReminderAlarm(delayMillis);
            }
        }
    }

    private void updateTotalDurations() {
        String project = spinnerProject.getText().toString();
        String category = spinnerCategory.getText().toString();
        long totalProject = timeEntryRepository.getTotalDurationForProject(project) +
                (project.equals(spinnerProject.getText().toString()) ? getTotalCurrentDurationSeconds() : 0);
        long totalCategory = timeEntryRepository.getTotalDurationForCategory(category) +
                (category.equals(spinnerCategory.getText().toString()) ? getTotalCurrentDurationSeconds() : 0);
        // Only add current session if it matches the selected project/category:
        if (isRunning) {
            totalProject = timeEntryRepository.getTotalDurationForProject(project) + getTotalCurrentDurationSeconds();
            totalCategory = timeEntryRepository.getTotalDurationForCategory(category) + getTotalCurrentDurationSeconds();
        }
        tvTotalProjectDuration.setText(TimeUtils.formatDuration(totalProject));
        tvTotalCategoryDuration.setText(TimeUtils.formatDuration(totalCategory));
    }

    private String formatPoolTimeDisplay(long poolSeconds) {
        if (poolSeconds == 0) {
            return "-";
        }
        String timeStr = TimeUtils.formatDuration(Math.abs(poolSeconds));
        return poolSeconds < 0 ? "-" + timeStr : timeStr;
    }

    private void setPoolTimeDisplay(TextView textView, long poolSeconds) {
        textView.setText(formatPoolTimeDisplay(poolSeconds));
        if (poolSeconds == 0) {
            // Use default text color for "-":
            textView.setTextColor(tvStartDate.getCurrentTextColor());
        } else if (poolSeconds >= 0) {
            textView.setTextColor(getResources().getColor(R.color.pool_positive, null));
        } else {
            textView.setTextColor(getResources().getColor(R.color.pool_negative, null));
        }
    }

    private void updatePoolTime() {
        String category = spinnerCategory.getText().toString();
        int dailyMinutes = dailyTimePoolRepository.getDailyMinutes(category);
        if (dailyMinutes <= 0) {
            setPoolTimeDisplay(tvPoolTime, 0);
            return;
        }
        // Calculate remaining pool time:
        Date earliestDate = timeEntryRepository.getEarliestStartDateForCategory(category);
        if (firstStartDatetime != null && firstStartDatetime.before(earliestDate)) {
            earliestDate = firstStartDatetime;
        }
        int days = TimeUtils.daysBetween(earliestDate, new Date());
        long poolSeconds = (long)dailyMinutes * 60 * days;
        long usedSeconds = timeEntryRepository.getTotalDurationForCategory(category);
        // Add current session if running and same category:
        if (isRunning && category.equals(spinnerCategory.getText().toString())) {
            usedSeconds += getTotalCurrentDurationSeconds();
        }
        long remainingSeconds = poolSeconds - usedSeconds;
        setPoolTimeDisplay(tvPoolTime, remainingSeconds);
    }

    private void updateNextReminder() {
        // Cancel any existing alarms:
        cancelReminderAlarm();
        if (reminderIntervalSeconds > 0 && isRunning && !isPaused) {
            long currentSeconds = getTotalCurrentDurationSeconds();
            nextReminderSeconds = (int)((currentSeconds / reminderIntervalSeconds) + 1) * reminderIntervalSeconds;
            // Schedule next alarm:
            long delayMillis = (nextReminderSeconds - currentSeconds) * 1000;
            if (delayMillis > 0) {
                scheduleReminderAlarm(delayMillis);
            }
        } else {
            nextReminderSeconds = 0;
        }
    }

    private void scheduleReminderAlarm(long delayMillis) {
        if (alarmManager == null || reminderIntervalSeconds <= 0) {
            return;
        }
        // Check if we have permission to schedule exact alarms (Android 12+):
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Exact alarm permission needed for reminders", Toast.LENGTH_LONG).show();
                return;
            }
        }
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        long triggerTime = System.currentTimeMillis() + delayMillis;
        // Use setExactAndAllowWhileIdle for precise timing even in Doze mode:
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        );
    }

    private void cancelReminderAlarm() {
        if (alarmManager == null) {
            return;
        }
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    public static void triggerFlashIfActive() {
        if (currentInstance != null) {
            currentInstance.runOnUiThread(() -> currentInstance.startFlashing());
        }
    }

    private void startFlashing() {
        flashUntilDatetime = new Date(System.currentTimeMillis() + FLASH_DURATION);
        handler.post(flashRunnable);
    }

    private void toggleFlash() {
        if (tvCurrentDuration == null || tvStartDate == null) {
            return;
        }
        int currentColor = tvCurrentDuration.getCurrentTextColor();
        int flashColor = getResources().getColor(R.color.colorAccent, null);
        int normalColor = tvStartDate.getCurrentTextColor();
        tvCurrentDuration.setTextColor(currentColor == flashColor ? normalColor : flashColor);
    }

    private void refreshEntryList() {
        adapter.updateEntries(timeEntryRepository.getAllEntries());
    }

    // RecyclerView Adapter for Time Entries:
    private class TimeEntryAdapter extends RecyclerView.Adapter<TimeEntryAdapter.ViewHolder> {
        private List<TimeEntry> entries;

        TimeEntryAdapter(List<TimeEntry> entries) {
            this.entries = new ArrayList<>(entries);
            // Reverse to show newest first:
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
                            // Find actual index in repository (entries are reversed):
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
    }

    private void loadEntriesFromFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                timeEntryRepository.importFromTextFile(inputStream);
                inputStream.close();
                // Refresh UI:
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

    private void setupSectionSelector() {
        String[] sections = {
            getString(R.string.control_header),
            getString(R.string.entries_header),
            getString(R.string.pools_header),
            getString(R.string.overview_header)
        };
        // Calculate item width to show 3 items at a time:
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int paddingPx = (int)(24 * displayMetrics.density);
        int marginPerItemPx = (int)(8 * displayMetrics.density);
        int availableWidth = screenWidth - paddingPx;
        int itemWidth = (availableWidth - (marginPerItemPx * 3)) / 3;
        sectionSelectorAdapter = new SectionSelectorAdapter(sections, itemWidth);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvSectionSelector.setLayoutManager(layoutManager);
        rvSectionSelector.setAdapter(sectionSelectorAdapter);
    }

    private void showSection(int index) {
        selectedSectionIndex = index;
        sectionSelectorAdapter.notifyDataSetChanged();
        cardControlPanel.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        cardEntries.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        cardPools.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        cardOverview.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        if (index == 2) {
            poolsManager.refreshPoolsData();
        }
        if (index == 3) {
            chartManager.loadChartData(true); // Auto-navigate to nearest data on first load
            updateTimeRangeButtonStates();
        }
    }

    private class SectionSelectorAdapter extends RecyclerView.Adapter<SectionSelectorAdapter.ViewHolder> {
        private final String[] sections;
        private final int itemWidth;

        SectionSelectorAdapter(String[] sections, int itemWidth) {
            this.sections = sections;
            this.itemWidth = itemWidth;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_section_selector, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String section = sections[position];
            holder.textView.setText(section);
            // Set width to show 3 items at a time:
            ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
            layoutParams.width = itemWidth;
            holder.itemView.setLayoutParams(layoutParams);
            // Update appearance based on selection:
            boolean isSelected = position == selectedSectionIndex;
            int backgroundColor = isSelected ?
                getResources().getColor(R.color.section_selected, null) :
                getResources().getColor(R.color.section_unselected, null);
            holder.card.setCardBackgroundColor(backgroundColor);
            holder.itemView.setOnClickListener(v -> showSection(position));
        }

        @Override
        public int getItemCount() {
            return sections.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.section_card);
                textView = itemView.findViewById(R.id.section_text);
            }
        }
    }

    private void setTimeRangeMode(TimeOverviewChartManager.TimeRangeMode mode) {
        chartManager.setTimeRangeMode(mode);
        updateTimeRangeButtonStates();
    }

    private void updateTimeRangeButtonStates() {
        int selectedColor = getResources().getColor(R.color.section_selected, null);
        int unselectedColor = getResources().getColor(R.color.section_unselected, null);
        btnTimeRangeWeekMain.setBackgroundColor(chartManager.getTimeRangeMode() == TimeOverviewChartManager.TimeRangeMode.WEEK ? selectedColor : unselectedColor);
        btnTimeRangeMonthMain.setBackgroundColor(chartManager.getTimeRangeMode() == TimeOverviewChartManager.TimeRangeMode.MONTH ? selectedColor : unselectedColor);
        btnTimeRangeYearMain.setBackgroundColor(chartManager.getTimeRangeMode() == TimeOverviewChartManager.TimeRangeMode.YEAR ? selectedColor : unselectedColor);
        btnTimeRangeFullMain.setBackgroundColor(chartManager.getTimeRangeMode() == TimeOverviewChartManager.TimeRangeMode.FULL ? selectedColor : unselectedColor);
    }

    private void initializePoolsFilePickers() {
        loadPoolsFileLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        poolsManager.loadPoolsFromFile(uri);
                    }
                }
        );
        savePoolsFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/plain"),
                uri -> {
                    if (uri != null) {
                        poolsManager.savePoolsToFile(uri);
                    }
                }
        );
    }
}
