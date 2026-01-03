package com.projecttimetracker.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.projecttimetracker.model.TimeEntry;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Repository for managing time entry data persistence.
 */
public class TimeEntryRepository {
    private static final String PREFS_NAME = "time_entries_prefs";
    private static final String KEY_ENTRIES = "time_entries";

    private final SharedPreferences prefs;
    private final Gson gson;
    private List<TimeEntry> entries;

    public TimeEntryRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadEntries();
    }

    private void loadEntries() {
        String json = prefs.getString(KEY_ENTRIES, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<TimeEntry>>(){}.getType();
            entries = gson.fromJson(json, type);
            if (entries == null) {
                entries = new ArrayList<>();
            }
        } else {
            entries = new ArrayList<>();
        }
    }

    private void saveEntries() {
        String json = gson.toJson(entries);
        prefs.edit().putString(KEY_ENTRIES, json).apply();
    }

    public List<TimeEntry> getAllEntries() {
        return new ArrayList<>(entries);
    }

    public void addEntry(TimeEntry entry) {
        entries.add(entry);
        saveEntries();
    }

    public void removeEntry(String entryId) {
        entries.removeIf(e -> e.getId().equals(entryId));
        saveEntries();
    }

    public void removeEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
            saveEntries();
        }
    }

    public int getEntryCount() {
        return entries.size();
    }

    public TimeEntry getEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            return entries.get(index);
        }
        return null;
    }

    /**
     * Gets all unique projects from entries.
     */
    public Set<String> getAllProjects() {
        Set<String> projects = new HashSet<>();
        projects.add("ProjectTimeTracker"); // Default project
        for (TimeEntry entry : entries) {
            if (entry.getProject() != null && !entry.getProject().isEmpty()) {
                projects.add(entry.getProject());
            }
        }
        return projects;
    }

    /**
     * Gets all unique categories from entries.
     */
    public Set<String> getAllCategories() {
        Set<String> categories = new HashSet<>();
        categories.add("Programming"); // Default category
        for (TimeEntry entry : entries) {
            if (entry.getCategory() != null && !entry.getCategory().isEmpty()) {
                categories.add(entry.getCategory());
            }
        }
        return categories;
    }

    /**
     * Gets projects filtered by category.
     */
    public Set<String> getProjectsForCategory(String category) {
        Set<String> projects = new HashSet<>();
        projects.add("ProjectTimeTracker"); // Default project
        for (TimeEntry entry : entries) {
            if (category.equals(entry.getCategory())) {
                projects.add(entry.getProject());
            }
        }
        return projects;
    }

    /**
     * Calculates total duration for a specific project.
     */
    public long getTotalDurationForProject(String project) {
        long total = 0;
        for (TimeEntry entry : entries) {
            if (project.equals(entry.getProject())) {
                total += entry.getDurationSeconds();
            }
        }
        return total;
    }

    /**
     * Calculates total duration for a specific category.
     */
    public long getTotalDurationForCategory(String category) {
        long total = 0;
        for (TimeEntry entry : entries) {
            if (category.equals(entry.getCategory())) {
                total += entry.getDurationSeconds();
            }
        }
        return total;
    }

    /**
     * Gets the earliest start date for entries in a category.
     */
    public Date getEarliestStartDateForCategory(String category) {
        Date earliest = new Date();
        for (TimeEntry entry : entries) {
            if (category.equals(entry.getCategory()) && entry.getStartTime() != null) {
                if (entry.getStartTime().before(earliest)) {
                    earliest = entry.getStartTime();
                }
            }
        }
        return earliest;
    }
}
