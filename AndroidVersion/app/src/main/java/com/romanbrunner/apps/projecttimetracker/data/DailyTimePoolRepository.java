package com.romanbrunner.apps.projecttimetracker.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.romanbrunner.apps.projecttimetracker.model.DailyTimePool;
import com.romanbrunner.apps.projecttimetracker.util.PreferencesManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Repository for managing daily time pool data persistence.
 */
public class DailyTimePoolRepository {
    private static final String FIELD_SEPARATOR = " --- ";

    private final PreferencesManager preferencesManager;
    private final Gson gson;
    private Map<String, Integer> pools; // category -> daily minutes

    public DailyTimePoolRepository(Context context) {
        preferencesManager = new PreferencesManager(context);
        gson = new Gson();
        loadPools();
    }

    private void loadPools() {
        String json = preferencesManager.getTimePoolsJson();
        if (json != null) {
            Type type = new TypeToken<HashMap<String, Integer>>(){}.getType();
            pools = gson.fromJson(json, type);
            if (pools == null) {
                pools = new HashMap<>();
            }
        } else {
            pools = new HashMap<>();
        }
    }

    private void savePools() {
        String json = gson.toJson(pools);
        preferencesManager.setTimePoolsJson(json);
    }

    public Set<String> getCategories() {
        return pools.keySet();
    }

    public List<String> getAllCategories() {
        return new ArrayList<>(pools.keySet());
    }

    public int getDailyMinutes(String category) {
        return pools.getOrDefault(category, 0);
    }

    public void setDailyMinutes(String category, int minutes) {
        pools.put(category, Math.max(0, minutes));
        savePools();
    }

    public void removeCategory(String category) {
        pools.remove(category);
        savePools();
    }

    public List<DailyTimePool> getAllPools() {
        List<DailyTimePool> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pools.entrySet()) {
            list.add(new DailyTimePool(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    public void addOrUpdatePool(DailyTimePool pool) {
        pools.put(pool.getCategory(), pool.getDailyMinutes());
        savePools();
    }

    /**
     * Exports pools to a text file in Python format.
     * Format: CATEGORY --- DAILY_MINUTES
     */
    public void exportToTextFile(OutputStream outputStream) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

        try {
            for (Map.Entry<String, Integer> entry : pools.entrySet()) {
                String line = entry.getKey() + FIELD_SEPARATOR + entry.getValue();
                writer.write(line);
                writer.newLine();
            }
        } finally {
            writer.close();
        }
    }

    /**
     * Imports pools from a text file in Python format.
     * Format: CATEGORY --- DAILY_MINUTES
     */
    public void importFromTextFile(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        Map<String, Integer> importedPools = new HashMap<>();

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split(FIELD_SEPARATOR);
                if (parts.length >= 2) {
                    String category = parts[0].trim();
                    int dailyMinutes = Integer.parseInt(parts[1].trim());
                    importedPools.put(category, dailyMinutes);
                }
            }
        } finally {
            reader.close();
        }

        // Replace current pools with imported ones
        pools.clear();
        pools.putAll(importedPools);
        savePools();
    }
}
