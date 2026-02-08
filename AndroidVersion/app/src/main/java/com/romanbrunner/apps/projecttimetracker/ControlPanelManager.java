package com.romanbrunner.apps.projecttimetracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.romanbrunner.apps.projecttimetracker.data.DailyTimePoolRepository;
import com.romanbrunner.apps.projecttimetracker.data.TimeEntryRepository;
import com.romanbrunner.apps.projecttimetracker.model.TimeEntry;
import com.romanbrunner.apps.projecttimetracker.util.PreferencesManager;
import com.romanbrunner.apps.projecttimetracker.util.TimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Manager class for control panel functionality.
 */
public class ControlPanelManager
{
    // Static reference for callbacks from BroadcastReceiver:
    private static ControlPanelManager currentInstance;

    // Constants:
    private static final int[] REMINDER_INTERVAL_CHOICES = {0, 15, 30, 45, 60, 90, 120};
    private static final int UPDATE_INTERVAL = 1000;
    private static final int REMINDER_REQUEST_CODE = 1000;
    private static final int FLASH_DURATION = 3000;
    private static final int FLASH_INTERVAL = 250;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MILLIS_PER_SECOND = 1000;

    /**
     * Callback interface for control panel events.
     */
    public interface OnControlPanelEventListener
    {
        void onEntryEnded();
        void onTimerStateChanged();
    }

    private final Context context;
    private final TimeEntryRepository timeEntryRepository;
    private final DailyTimePoolRepository dailyTimePoolRepository;
    private final PreferencesManager preferencesManager;
    private final AlarmManager alarmManager;

    // UI Components:
    private final Button btnStartStop;
    private final Button btnReset;
    private final Button btnEnd;
    private final MaterialAutoCompleteTextView spinnerReminder;
    private final MaterialAutoCompleteTextView spinnerProject;
    private final MaterialAutoCompleteTextView spinnerCategory;
    private final TextView tvCurrentDuration;
    private final TextView tvTotalProjectDuration;
    private final TextView tvTotalCategoryDuration;
    private final TextView tvPoolTime;
    private final TextView tvStartDate;

    // State:
    private Date firstStartDatetime = null;
    private Date currentStartDatetime = null;
    private long accumulatedDurationSeconds = 0;
    private int reminderIntervalSeconds = 0;
    private int nextReminderSeconds = 0;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private Date flashUntilDatetime = null;
    private boolean isInitialSetup = true;
    private TimePoolsManager poolsManager = null;

    // Handler for periodic updates:
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (isRunning && !isPaused)
            {
                updateCurrentDuration();
            }
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    private final Runnable flashRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (flashUntilDatetime != null && new Date().before(flashUntilDatetime))
            {
                toggleFlash();
                handler.postDelayed(this, FLASH_INTERVAL);
            }
            else
            {
                flashUntilDatetime = null;
                // Reset to default text color:
                if (tvCurrentDuration != null)
                {
                    tvCurrentDuration.setTextColor(tvStartDate.getCurrentTextColor());
                }
            }
        }
    };

    private OnControlPanelEventListener listener;

    public static void triggerFlashIfActive()
    {
        if (currentInstance != null)
        {
            currentInstance.handler.post(() -> currentInstance.startFlashing());
        }
    }

    public ControlPanelManager(Context context,
                               TimeEntryRepository timeEntryRepository,
                               DailyTimePoolRepository dailyTimePoolRepository,
                               PreferencesManager preferencesManager,
                               AlarmManager alarmManager,
                               Button btnStartStop,
                               Button btnReset,
                               Button btnEnd,
                               MaterialAutoCompleteTextView spinnerReminder,
                               MaterialAutoCompleteTextView spinnerProject,
                               MaterialAutoCompleteTextView spinnerCategory,
                               TextView tvCurrentDuration,
                               TextView tvTotalProjectDuration,
                               TextView tvTotalCategoryDuration,
                               TextView tvPoolTime,
                               TextView tvStartDate)
    {
        this.context = context;
        this.timeEntryRepository = timeEntryRepository;
        this.dailyTimePoolRepository = dailyTimePoolRepository;
        this.preferencesManager = preferencesManager;
        this.alarmManager = alarmManager;
        this.btnStartStop = btnStartStop;
        this.btnReset = btnReset;
        this.btnEnd = btnEnd;
        this.spinnerReminder = spinnerReminder;
        this.spinnerProject = spinnerProject;
        this.spinnerCategory = spinnerCategory;
        this.tvCurrentDuration = tvCurrentDuration;
        this.tvTotalProjectDuration = tvTotalProjectDuration;
        this.tvTotalCategoryDuration = tvTotalCategoryDuration;
        this.tvPoolTime = tvPoolTime;
        this.tvStartDate = tvStartDate;
    }

    public void setOnControlPanelEventListener(OnControlPanelEventListener listener)
    {
        this.listener = listener;
    }

    public void setPoolsManager(TimePoolsManager poolsManager)
    {
        this.poolsManager = poolsManager;
    }

    public void initialize()
    {
        setupButtonListeners();
        setupSpinners();
        updateSpinnerData();
        updateTotalDurations();
        updatePoolTime();
        restoreReminderInterval();
        isInitialSetup = false;
        handler.post(updateRunnable);
    }

    public void onDestroy()
    {
        handler.removeCallbacks(updateRunnable);
        handler.removeCallbacks(flashRunnable);
        cancelReminderAlarm();
    }

    public void onResume()
    {
        currentInstance = this;
        // Preserve current spinner values across pause/resume:
        String currentCategory = spinnerCategory.getText().toString();
        String currentProject = spinnerProject.getText().toString();
        updatePoolTime();
        updateSpinnerData();
        // Restore the values that were entered before pause (even if not in dropdown list):
        if (!currentCategory.isEmpty())
        {
            spinnerCategory.setText(currentCategory, false);
        }
        if (!currentProject.isEmpty())
        {
            spinnerProject.setText(currentProject, false);
        }
    }

    public void onPause()
    {
        currentInstance = null;
    }

    private void setupButtonListeners()
    {
        btnStartStop.setOnClickListener(v -> onStartStopClicked());
        btnReset.setOnClickListener(v -> onResetClicked());
        btnEnd.setOnClickListener(v -> onEndClicked());
    }

    private void setupSpinners()
    {
        // Reminder interval spinner:
        String[] reminderChoices = new String[REMINDER_INTERVAL_CHOICES.length];
        for (int i = 0; i < REMINDER_INTERVAL_CHOICES.length; i++)
        {
            reminderChoices[i] = String.valueOf(REMINDER_INTERVAL_CHOICES[i]);
        }
        ArrayAdapter<String> reminderAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, reminderChoices);
        spinnerReminder.setAdapter(reminderAdapter);
        spinnerReminder.setText(reminderChoices[0], false);
        spinnerReminder.setOnItemClickListener((parent, view, position, id) -> updateReminderInterval());
        // Handle custom input when focus is lost:
        spinnerReminder.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) updateReminderInterval(); });
        // Project and category spinners:
        spinnerProject.setOnItemClickListener((parent, view, position, id) ->
        {
            String selectedProject = spinnerProject.getText().toString();
            if (!isInitialSetup)
            {
                preferencesManager.setLastProject(selectedProject);
            }
            updateTotalDurations();
            updatePoolTime();
        });
        spinnerCategory.setOnItemClickListener((parent, view, position, id) ->
        {
            String selectedCategory = spinnerCategory.getText().toString();
            if (!isInitialSetup)
            {
                preferencesManager.setLastCategory(selectedCategory);
            }
            updateProjectsForCategory();
            updateTotalDurations();
            updatePoolTime();
        });
    }

    private void updateReminderInterval()
    {
        try
        {
            String text = spinnerReminder.getText().toString().trim();
            if (!text.isEmpty())
            {
                int minutes = Integer.parseInt(text);
                reminderIntervalSeconds = minutes * SECONDS_PER_MINUTE;
                preferencesManager.setLastReminder(minutes);
                updateNextReminder();
            }
        }
        catch (NumberFormatException e)
        {
            Toast.makeText(context, "Invalid reminder interval", Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreReminderInterval()
    {
        int lastReminder = preferencesManager.getLastReminder();
        if (lastReminder > 0)
        {
            spinnerReminder.setText(String.valueOf(lastReminder), false);
            reminderIntervalSeconds = lastReminder * SECONDS_PER_MINUTE;
            updateNextReminder();
        }
    }

    public void updateSpinnerData()
    {
        // Categories (set up first):
        List<String> categories = new ArrayList<>(timeEntryRepository.getAllCategories());
        categories.addAll(dailyTimePoolRepository.getCategories());
        List<String> uniqueCategories = new ArrayList<>(new java.util.HashSet<>(categories));
        Collections.sort(uniqueCategories);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, uniqueCategories);
        spinnerCategory.setAdapter(categoryAdapter);
        if (spinnerCategory.getText().toString().isEmpty() && !uniqueCategories.isEmpty())
        {
            String lastCategory = preferencesManager.getLastCategory();
            if (!lastCategory.isEmpty() && uniqueCategories.contains(lastCategory))
            {
                spinnerCategory.setText(lastCategory, false);
            }
            else
            {
                spinnerCategory.setText(uniqueCategories.get(0), false);
            }
        }
        updateProjectsForCategory();
    }

    private void updateProjectsForCategory()
    {
        String category = spinnerCategory.getText().toString();
        List<String> projects = new ArrayList<>(timeEntryRepository.getProjectsForCategory(category));
        Collections.sort(projects);
        ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, projects);
        String currentProject = spinnerProject.getText().toString();
        spinnerProject.setAdapter(projectAdapter);
        if (!projects.isEmpty())
        {
            if (currentProject.isEmpty() || !projects.contains(currentProject))
            {
                if (isInitialSetup)
                {
                    String lastProject = preferencesManager.getLastProject();
                    if (!lastProject.isEmpty() && projects.contains(lastProject))
                    {
                        spinnerProject.setText(lastProject, false);
                        return;
                    }
                }
                String projectWithMostTime = projects.get(0);
                long maxDuration = 0;
                for (String project : projects)
                {
                    long duration = timeEntryRepository.getTotalDurationForProject(project);
                    if (duration > maxDuration)
                    {
                        maxDuration = duration;
                        projectWithMostTime = project;
                    }
                }
                spinnerProject.setText(projectWithMostTime, false);
            }
            else
            {
                spinnerProject.setText(currentProject, false);
            }
        }
    }

    private void onStartStopClicked()
    {
        if (!isRunning)
        {
            // Start:
            isRunning = true;
            isPaused = false;
            currentStartDatetime = new Date();
            if (firstStartDatetime == null)
            {
                firstStartDatetime = currentStartDatetime;
                tvStartDate.setText(TimeUtils.formatDateTimeForDisplay(firstStartDatetime));
            }
            btnStartStop.setText(R.string.pause);
            updateNextReminder();
        }
        else if (!isPaused)
        {
            // Pause:
            isPaused = true;
            accumulatedDurationSeconds += getCurrentSessionSeconds();
            currentStartDatetime = null;
            cancelReminderAlarm();
            btnStartStop.setText(R.string.resume);
        }
        else
        {
            // Resume:
            isPaused = false;
            currentStartDatetime = new Date();
            btnStartStop.setText(R.string.pause);
            updateNextReminder();
        }
        updateButtonVisibility();
        notifyTimerStateChanged();
    }

    private void onResetClicked()
    {
        if (firstStartDatetime == null)
        {
            return;
        }
        resetState();
        updateTotalDurations();
        updatePoolTime();
        notifyTimerStateChanged();
    }

    private void onEndClicked()
    {
        if (firstStartDatetime == null)
        {
            return;
        }
        if (currentStartDatetime != null)
        {
            accumulatedDurationSeconds += getCurrentSessionSeconds();
        }
        TimeEntry entry = new TimeEntry(
                spinnerProject.getText().toString(),
                spinnerCategory.getText().toString(),
                accumulatedDurationSeconds,
                firstStartDatetime
        );
        timeEntryRepository.addEntry(entry);
        resetState();
        updateSpinnerData();
        updateTotalDurations();
        updatePoolTime();
        notifyEntryEnded();
    }

    private void resetState()
    {
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

    private void updateButtonVisibility()
    {
        boolean timerActive = isRunning || isPaused;
        btnReset.setVisibility(timerActive ? View.VISIBLE : View.GONE);
        btnEnd.setVisibility(timerActive ? View.VISIBLE : View.GONE);
    }

    private long getCurrentSessionSeconds()
    {
        if (currentStartDatetime == null)
        {
            return 0;
        }
        return (new Date().getTime() - currentStartDatetime.getTime()) / MILLIS_PER_SECOND;
    }

    public long getTotalCurrentDurationSeconds()
    {
        return accumulatedDurationSeconds + getCurrentSessionSeconds();
    }

    public boolean isRunning()
    {
        return isRunning;
    }

    public String getSelectedCategory()
    {
        return spinnerCategory.getText().toString();
    }

    public String getSelectedProject()
    {
        return spinnerProject.getText().toString();
    }

    public Date getFirstStartDatetime()
    {
        return firstStartDatetime;
    }

    private void updateCurrentDuration()
    {
        long totalSeconds = getTotalCurrentDurationSeconds();
        tvCurrentDuration.setText(TimeUtils.formatDuration(totalSeconds));
        updateTotalDurations();
        updatePoolTime();
        if (nextReminderSeconds > 0 && reminderIntervalSeconds > 0 && totalSeconds >= nextReminderSeconds)
        {
            int intervalsPassed = (int)(totalSeconds / reminderIntervalSeconds);
            nextReminderSeconds = (intervalsPassed + 1) * reminderIntervalSeconds;
            long delayMillis = (nextReminderSeconds - totalSeconds) * MILLIS_PER_SECOND;
            if (delayMillis > 0)
            {
                scheduleReminderAlarm(delayMillis);
            }
        }
    }

    public void updateTotalDurations()
    {
        String project = spinnerProject.getText().toString();
        String category = spinnerCategory.getText().toString();
        long totalProject = timeEntryRepository.getTotalDurationForProject(project);
        long totalCategory = timeEntryRepository.getTotalDurationForCategory(category);
        if (isRunning)
        {
            totalProject += getTotalCurrentDurationSeconds();
            totalCategory += getTotalCurrentDurationSeconds();
        }
        tvTotalProjectDuration.setText(TimeUtils.formatDuration(totalProject));
        tvTotalCategoryDuration.setText(TimeUtils.formatDuration(totalCategory));
    }

    private String formatPoolTimeDisplay(long poolSeconds)
    {
        if (poolSeconds == 0)
        {
            return "-";
        }
        String timeStr = TimeUtils.formatDuration(Math.abs(poolSeconds));
        return poolSeconds < 0 ? "-" + timeStr : timeStr;
    }

    private void setPoolTimeDisplay(TextView textView, long poolSeconds)
    {
        textView.setText(formatPoolTimeDisplay(poolSeconds));
        if (poolSeconds == 0)
        {
            textView.setTextColor(tvStartDate.getCurrentTextColor());
        }
        else if (poolSeconds >= 0)
        {
            textView.setTextColor(context.getResources().getColor(R.color.pool_positive, null));
        }
        else
        {
            textView.setTextColor(context.getResources().getColor(R.color.pool_negative, null));
        }
    }

    public void updatePoolTime()
    {
        String category = spinnerCategory.getText().toString();
        int dailyMinutes = dailyTimePoolRepository.getDailyMinutes(category);
        if (dailyMinutes <= 0)
        {
            setPoolTimeDisplay(tvPoolTime, 0);
            return;
        }
        TimePoolsManager.PoolResetInterval interval = poolsManager != null ? poolsManager.getPoolResetInterval() : TimePoolsManager.PoolResetInterval.NEVER;
        long poolSeconds;
        long usedSeconds;

        TimePoolsManager.PeriodCalculation period = TimePoolsManager.calculatePeriod(interval, dailyMinutes);
        if (period != null)
        {
            poolSeconds = period.poolSeconds;
            usedSeconds = timeEntryRepository.getTotalDurationForCategoryInRange(category, period.periodStart, period.periodEnd);
        }
        else
        {
            Date earliestDate = timeEntryRepository.getEarliestStartDateForCategory(category);
            if (firstStartDatetime != null && firstStartDatetime.before(earliestDate))
            {
                earliestDate = firstStartDatetime;
            }
            int days = TimeUtils.daysBetween(earliestDate, new Date());
            poolSeconds = (long)dailyMinutes * SECONDS_PER_MINUTE * days;
            usedSeconds = timeEntryRepository.getTotalDurationForCategory(category);
        }

        if (isRunning && category.equals(spinnerCategory.getText().toString()))
        {
            usedSeconds += getTotalCurrentDurationSeconds();
        }
        long remainingSeconds = poolSeconds - usedSeconds;
        setPoolTimeDisplay(tvPoolTime, remainingSeconds);
    }

    private void updateNextReminder()
    {
        cancelReminderAlarm();
        if (reminderIntervalSeconds > 0 && isRunning && !isPaused)
        {
            long currentSeconds = getTotalCurrentDurationSeconds();
            int intervalsPassed = (int)(currentSeconds / reminderIntervalSeconds);
            nextReminderSeconds = (intervalsPassed + 1) * reminderIntervalSeconds;
            long delayMillis = (nextReminderSeconds - currentSeconds) * MILLIS_PER_SECOND;
            if (delayMillis > 0)
            {
                scheduleReminderAlarm(delayMillis);
            }
        }
        else
        {
            nextReminderSeconds = 0;
        }
    }

    private void scheduleReminderAlarm(long delayMillis)
    {
        if (alarmManager == null || reminderIntervalSeconds <= 0)
        {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            if (!alarmManager.canScheduleExactAlarms())
            {
                Toast.makeText(context, "Exact alarm permission needed for reminders", Toast.LENGTH_LONG).show();
                return;
            }
        }
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        long triggerTime = System.currentTimeMillis() + delayMillis;
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        );
    }

    private void cancelReminderAlarm()
    {
        if (alarmManager == null)
        {
            return;
        }
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    public void startFlashing()
    {
        flashUntilDatetime = new Date(System.currentTimeMillis() + FLASH_DURATION);
        handler.post(flashRunnable);
    }

    private void toggleFlash()
    {
        if (tvCurrentDuration == null || tvStartDate == null)
        {
            return;
        }
        int currentColor = tvCurrentDuration.getCurrentTextColor();
        int flashColor = context.getResources().getColor(R.color.colorAccent, null);
        int normalColor = tvStartDate.getCurrentTextColor();
        tvCurrentDuration.setTextColor(currentColor == flashColor ? normalColor : flashColor);
    }

    private void notifyEntryEnded()
    {
        if (listener != null)
        {
            listener.onEntryEnded();
        }
    }

    private void notifyTimerStateChanged()
    {
        if (listener != null)
        {
            listener.onTimerStateChanged();
        }
    }
}
