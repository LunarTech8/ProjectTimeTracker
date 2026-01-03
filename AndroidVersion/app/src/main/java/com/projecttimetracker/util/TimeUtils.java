package com.projecttimetracker.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for time-related operations.
 */
public class TimeUtils {

    public static final SimpleDateFormat DISPLAY_FORMAT =
            new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault());

    public static final SimpleDateFormat SAVE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    /**
     * Formats duration in seconds to a human-readable string.
     * Format: "H:MM:SS" or "M:SS" if less than an hour.
     */
    public static String formatDuration(long totalSeconds) {
        long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        }
    }

    /**
     * Formats a Date object for display.
     */
    public static String formatDateTimeForDisplay(Date date) {
        if (date == null) {
            return "-";
        }
        return DISPLAY_FORMAT.format(date);
    }

    /**
     * Formats a Date object for saving.
     */
    public static String formatDateTimeForSave(Date date) {
        if (date == null) {
            return "";
        }
        return SAVE_FORMAT.format(date);
    }

    /**
     * Parses a date string from saved format.
     */
    public static Date parseDateTimeFromSave(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return SAVE_FORMAT.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Calculates the number of days between two dates (inclusive).
     */
    public static int daysBetween(Date start, Date end) {
        if (start == null || end == null) {
            return 0;
        }
        long diffMillis = end.getTime() - start.getTime();
        return (int) TimeUnit.MILLISECONDS.toDays(diffMillis) + 1;
    }

    /**
     * Checks if two dates are on the same day.
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(date1).equals(sdf.format(date2));
    }
}
