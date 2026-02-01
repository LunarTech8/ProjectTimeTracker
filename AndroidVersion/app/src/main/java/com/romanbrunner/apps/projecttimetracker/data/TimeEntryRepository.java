package com.romanbrunner.apps.projecttimetracker.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.romanbrunner.apps.projecttimetracker.model.TimeEntry;
import com.romanbrunner.apps.projecttimetracker.util.PreferencesManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Repository for time entry data persistence.
 */
public class TimeEntryRepository
{
    // Constants:
    private static final String FIELD_SEPARATOR = " --- ";
    private static final String PYTHON_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private final PreferencesManager preferencesManager;
    private final Gson gson;
    private List<TimeEntry> entries;

    public TimeEntryRepository(Context context)
    {
        preferencesManager = new PreferencesManager(context);
        gson = new Gson();
        loadEntries();
    }

    private void loadEntries()
    {
        String json = preferencesManager.getTimeEntriesJson();
        if (json != null)
        {
            Type type = new TypeToken<ArrayList<TimeEntry>>(){}.getType();
            entries = gson.fromJson(json, type);
            if (entries == null)
            {
                entries = new ArrayList<>();
            }
        }
        else
        {
            entries = new ArrayList<>();
        }
    }

    private void saveEntries()
    {
        String json = gson.toJson(entries);
        preferencesManager.setTimeEntriesJson(json);
    }

    public List<TimeEntry> getAllEntries()
    {
        return new ArrayList<>(entries);
    }

    public void addEntry(TimeEntry entry)
    {
        entries.add(entry);
        saveEntries();
    }

    public void removeEntry(String entryId)
    {
        entries.removeIf(e -> e.getId().equals(entryId));
        saveEntries();
    }

    public void removeEntry(int index)
    {
        if (index >= 0 && index < entries.size())
        {
            entries.remove(index);
            saveEntries();
        }
    }

    public void removeEntriesByCategory(String category)
    {
        entries.removeIf(e -> category.equals(e.getCategory()));
        saveEntries();
    }

    public int getEntryCount()
    {
        return entries.size();
    }

    public TimeEntry getEntry(int index)
    {
        if (index >= 0 && index < entries.size())
        {
            return entries.get(index);
        }
        return null;
    }

    /**
     * Gets all unique projects from entries.
     */
    public Set<String> getAllProjects()
    {
        Set<String> projects = new HashSet<>();
        projects.add("ProjectTimeTracker");
        for (TimeEntry entry : entries)
        {
            if (entry.getProject() != null && !entry.getProject().isEmpty())
            {
                projects.add(entry.getProject());
            }
        }
        return projects;
    }

    /**
     * Gets all unique categories from entries.
     */
    public Set<String> getAllCategories()
    {
        Set<String> categories = new HashSet<>();
        categories.add("Programming");
        for (TimeEntry entry : entries)
        {
            if (entry.getCategory() != null && !entry.getCategory().isEmpty())
            {
                categories.add(entry.getCategory());
            }
        }
        return categories;
    }

    /**
     * Gets projects filtered by category.
     */
    public Set<String> getProjectsForCategory(String category)
    {
        Set<String> projects = new HashSet<>();
        for (TimeEntry entry : entries)
        {
            if (category.equals(entry.getCategory()))
            {
                projects.add(entry.getProject());
            }
        }
        // Only add default if no projects found for this category:
        if (projects.isEmpty())
        {
            projects.add("ProjectTimeTracker");
        }
        return projects;
    }

    /**
     * Calculates total duration for a specific project.
     */
    public long getTotalDurationForProject(String project)
    {
        long total = 0;
        for (TimeEntry entry : entries)
        {
            if (project.equals(entry.getProject()))
            {
                total += entry.getDurationSeconds();
            }
        }
        return total;
    }

    /**
     * Calculates total duration for a specific category.
     */
    public long getTotalDurationForCategory(String category)
    {
        long total = 0;
        for (TimeEntry entry : entries)
        {
            if (category.equals(entry.getCategory()))
            {
                total += entry.getDurationSeconds();
            }
        }
        return total;
    }

    /**
     * Gets the earliest start date for entries in a category.
     */
    public Date getEarliestStartDateForCategory(String category)
    {
        Date earliest = new Date();
        for (TimeEntry entry : entries)
        {
            if (category.equals(entry.getCategory()) && entry.getStartTime() != null)
            {
                if (entry.getStartTime().before(earliest))
                {
                    earliest = entry.getStartTime();
                }
            }
        }
        return earliest;
    }

    /**
     * Exports entries to a text file in Python format.
     */
    public void exportToTextFile(OutputStream outputStream) throws IOException
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat(PYTHON_DATE_FORMAT, Locale.US);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        try
        {
            for (TimeEntry entry : entries)
            {
                String line = entry.getProject() + FIELD_SEPARATOR +
                             entry.getCategory() + FIELD_SEPARATOR +
                             entry.getDurationSeconds() + FIELD_SEPARATOR +
                             dateFormat.format(entry.getStartTime());
                writer.write(line);
                writer.newLine();
            }
        }
        finally
        {
            writer.close();
        }
    }

    /**
     * Imports entries from a text file in Python format.
     */
    public void importFromTextFile(InputStream inputStream) throws IOException, ParseException
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat(PYTHON_DATE_FORMAT, Locale.US);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<TimeEntry> importedEntries = new ArrayList<>();
        try
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (line.isEmpty())
                {
                    continue;
                }
                String[] parts = line.split(FIELD_SEPARATOR);
                if (parts.length >= 4)
                {
                    String project = parts[0].trim();
                    String category = parts[1].trim();
                    // Parse as double first to handle decimal seconds, then convert to long:
                    double durationDouble = Double.parseDouble(parts[2].trim());
                    long duration = (long)durationDouble;
                    Date startTime = dateFormat.parse(parts[3].trim());
                    TimeEntry entry = new TimeEntry(project, category, duration, startTime);
                    importedEntries.add(entry);
                }
            }
        }
        finally
        {
            reader.close();
        }
        // Replace current entries with imported ones:
        entries.clear();
        entries.addAll(importedEntries);
        saveEntries();
    }
}
