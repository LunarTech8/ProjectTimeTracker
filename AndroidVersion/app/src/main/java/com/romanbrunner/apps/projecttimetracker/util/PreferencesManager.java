package com.romanbrunner.apps.projecttimetracker.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Centralized manager for SharedPreferences access across the application.
 * Provides consistent naming and access patterns for all preference operations.
 */
public class PreferencesManager {
    // Preference file names
    private static final String PREFS_APP_STATE = "appStatePrefs";
    private static final String PREFS_TIME_ENTRIES = "timeEntriesPrefs";
    private static final String PREFS_TIME_POOLS = "timePoolsPrefs";

    // App state preference keys
    private static final String KEY_LAST_CATEGORY = "lastCategory";
    private static final String KEY_LAST_PROJECT = "lastProject";
    private static final String KEY_LAST_REMINDER = "lastReminder";

    // Repository preference keys
    private static final String KEY_ENTRIES = "timeEntries";
    private static final String KEY_POOLS = "timePools";

    private final SharedPreferences appStatePrefs;
    private final SharedPreferences timeEntriesPrefs;
    private final SharedPreferences timePoolsPrefs;

    public PreferencesManager(Context context) {
        appStatePrefs = context.getSharedPreferences(PREFS_APP_STATE, Context.MODE_PRIVATE);
        timeEntriesPrefs = context.getSharedPreferences(PREFS_TIME_ENTRIES, Context.MODE_PRIVATE);
        timePoolsPrefs = context.getSharedPreferences(PREFS_TIME_POOLS, Context.MODE_PRIVATE);
    }

    // App State Preferences
    public String getLastCategory() {
        return appStatePrefs.getString(KEY_LAST_CATEGORY, "");
    }

    public void setLastCategory(String category) {
        appStatePrefs.edit().putString(KEY_LAST_CATEGORY, category).apply();
    }

    public String getLastProject() {
        return appStatePrefs.getString(KEY_LAST_PROJECT, "");
    }

    public void setLastProject(String project) {
        appStatePrefs.edit().putString(KEY_LAST_PROJECT, project).apply();
    }

    public int getLastReminder() {
        return appStatePrefs.getInt(KEY_LAST_REMINDER, 0);
    }

    public void setLastReminder(int reminderMinutes) {
        appStatePrefs.edit().putInt(KEY_LAST_REMINDER, reminderMinutes).apply();
    }

    // Time Entries Repository Preferences
    public SharedPreferences getTimeEntriesPrefs() {
        return timeEntriesPrefs;
    }

    public String getTimeEntriesJson() {
        return timeEntriesPrefs.getString(KEY_ENTRIES, null);
    }

    public void setTimeEntriesJson(String json) {
        timeEntriesPrefs.edit().putString(KEY_ENTRIES, json).apply();
    }

    // Time Pools Repository Preferences
    public SharedPreferences getTimePoolsPrefs() {
        return timePoolsPrefs;
    }

    public String getTimePoolsJson() {
        return timePoolsPrefs.getString(KEY_POOLS, null);
    }

    public void setTimePoolsJson(String json) {
        timePoolsPrefs.edit().putString(KEY_POOLS, json).apply();
    }
}
