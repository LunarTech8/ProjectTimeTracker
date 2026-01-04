package com.romanbrunner.apps.projecttimetracker.model;

import java.io.Serializable;

/**
 * Represents a daily time pool configuration for a category.
 */
public class DailyTimePool implements Serializable {
    private String category;
    private int dailyMinutes;

    public DailyTimePool() {
    }

    public DailyTimePool(String category, int dailyMinutes) {
        this.category = category;
        this.dailyMinutes = dailyMinutes;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getDailyMinutes() {
        return dailyMinutes;
    }

    public void setDailyMinutes(int dailyMinutes) {
        this.dailyMinutes = Math.max(0, dailyMinutes);
    }

    @Override
    public String toString() {
        return "DailyTimePool{" +
                "category='" + category + '\'' +
                ", dailyMinutes=" + dailyMinutes +
                '}';
    }
}
