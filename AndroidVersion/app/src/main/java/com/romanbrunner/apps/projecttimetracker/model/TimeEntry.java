package com.romanbrunner.apps.projecttimetracker.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Time entry for tracking project time.
 */
public class TimeEntry implements Serializable {
    private String project;
    private String category;
    private long durationSeconds;
    private Date startTime;
    private String id;

    public TimeEntry() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    public TimeEntry(String project, String category, long durationSeconds, Date startTime) {
        this();
        this.project = project;
        this.category = category;
        this.durationSeconds = durationSeconds;
        this.startTime = startTime;
    }

    // Getters and Setters:
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        return "TimeEntry{" +
                "project='" + project + '\'' +
                ", category='" + category + '\'' +
                ", durationSeconds=" + durationSeconds +
                ", startTime=" + startTime +
                '}';
    }
}
