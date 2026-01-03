package com.projecttimetracker.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.projecttimetracker.model.DailyTimePool;

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
    private static final String PREFS_NAME = "daily_time_pools_prefs";
    private static final String KEY_POOLS = "daily_time_pools";

    private final SharedPreferences prefs;
    private final Gson gson;
    private Map<String, Integer> pools; // category -> daily minutes

    public DailyTimePoolRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadPools();
    }

    private void loadPools() {
        String json = prefs.getString(KEY_POOLS, null);
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
        prefs.edit().putString(KEY_POOLS, json).apply();
    }

    public Set<String> getCategories() {
        return pools.keySet();
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
}
