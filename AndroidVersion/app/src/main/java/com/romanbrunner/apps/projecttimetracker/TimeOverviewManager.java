package com.romanbrunner.apps.projecttimetracker;

import android.content.Context;
import android.graphics.Color;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.romanbrunner.apps.projecttimetracker.data.TimeEntryRepository;
import com.romanbrunner.apps.projecttimetracker.model.TimeEntry;
import com.romanbrunner.apps.projecttimetracker.util.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Manager class for time overview logic.
 */
public class TimeOverviewManager
{
    // Constants:
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int MONTHS_PER_YEAR = 12;
    private static final float LINE_WIDTH = 2f;
    private static final float CIRCLE_RADIUS = 3f;
    private static final int DAYS_PER_MONTH_PERIOD = 4;
    private static final int FULL_MODE_PERIODS = 10;
    private static final int WEEK_DAYS = 7;
    private static final int WEEK_MAX_INDEX = 6;
    private static final int YEAR_LABEL_SKIP = 2;
    private static final int FULL_LABEL_SKIP = 3;
    private static final int MAX_PERIOD_SEARCH = 52;

    public enum TimeRangeMode
    {
        WEEK, MONTH, YEAR, FULL
    }

    private final Context context;
    private final LineChart chart;
    private final TimeEntryRepository timeEntryRepository;
    private final ImageButton btnTimePrev;
    private final ImageButton btnTimeNext;
    private final TextView tvTimeRangeLabel;
    private TimeRangeMode timeRangeMode = TimeRangeMode.WEEK;
    private int currentTimeOffset = 0;

    public TimeOverviewManager(Context context, LineChart chart, TimeEntryRepository timeEntryRepository, ImageButton btnTimePrev, ImageButton btnTimeNext, TextView tvTimeRangeLabel)
    {
        this.context = context;
        this.chart = chart;
        this.timeEntryRepository = timeEntryRepository;
        this.btnTimePrev = btnTimePrev;
        this.btnTimeNext = btnTimeNext;
        this.tvTimeRangeLabel = tvTimeRangeLabel;
    }

    public void setupChart()
    {
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextColor(Color.WHITE);
        chart.getLegend().setWordWrapEnabled(true);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    public void setupClickListeners()
    {
        tvTimeRangeLabel.setOnClickListener(v -> showTimeRangeModePopup());
        btnTimePrev.setOnClickListener(v -> navigateTimePrevious());
        btnTimeNext.setOnClickListener(v -> navigateTimeNext());
    }

    public void showTimeRangeModePopup()
    {
        PopupMenu popup = new PopupMenu(context, tvTimeRangeLabel);
        popup.getMenu().add(0, 0, 0, R.string.time_range_week);
        popup.getMenu().add(0, 1, 1, R.string.time_range_month);
        popup.getMenu().add(0, 2, 2, R.string.time_range_year);
        popup.getMenu().add(0, 3, 3, R.string.time_range_full);
        popup.setOnMenuItemClickListener(item ->
        {
            switch (item.getItemId())
            {
                case 0:
                    setTimeRangeMode(TimeRangeMode.WEEK);
                    return true;
                case 1:
                    setTimeRangeMode(TimeRangeMode.MONTH);
                    return true;
                case 2:
                    setTimeRangeMode(TimeRangeMode.YEAR);
                    return true;
                case 3:
                    setTimeRangeMode(TimeRangeMode.FULL);
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    public void loadChartData()
    {
        loadChartData(false);
    }

    public void loadChartData(boolean autoNavigateToData)
    {
        List<TimeEntry> allEntries = timeEntryRepository.getAllEntries();
        if (allEntries.isEmpty())
        {
            chart.clear();
            chart.invalidate();
            tvTimeRangeLabel.setText("");
            return;
        }
        Set<String> categories = new HashSet<>();
        for (TimeEntry entry : allEntries)
        {
            categories.add(entry.getCategory());
        }
        Calendar calendar = Calendar.getInstance();
        Date rangeStart;
        Date rangeEnd;
        String xAxisFormat;
        String rangeLabel;
        switch (timeRangeMode)
        {
            case WEEK:
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                int daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7;
                calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
                calendar.add(Calendar.WEEK_OF_YEAR, currentTimeOffset);
                rangeStart = getDayStart(calendar.getTime());
                calendar.add(Calendar.DAY_OF_YEAR, 6);
                rangeEnd = getDayEnd(calendar.getTime());
                xAxisFormat = "EEE";
                SimpleDateFormat weekFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                Calendar labelCalendar = Calendar.getInstance();
                labelCalendar.setTime(rangeStart);
                String weekStart = weekFormat.format(labelCalendar.getTime());
                labelCalendar.add(Calendar.DAY_OF_YEAR, 6);
                String weekEnd = weekFormat.format(labelCalendar.getTime());
                rangeLabel = "Week: " + weekStart + " - " + weekEnd;
                break;
            case MONTH:
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.MONTH, currentTimeOffset);
                rangeStart = getDayStart(calendar.getTime());
                calendar.add(Calendar.MONTH, 1);
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                rangeEnd = getDayEnd(calendar.getTime());
                xAxisFormat = "d";
                SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                rangeLabel = "Month: " + monthFormat.format(rangeStart);
                break;
            case YEAR:
                calendar.set(Calendar.DAY_OF_YEAR, 1);
                calendar.add(Calendar.YEAR, currentTimeOffset);
                rangeStart = getDayStart(calendar.getTime());
                calendar.add(Calendar.YEAR, 1);
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                rangeEnd = getDayEnd(calendar.getTime());
                xAxisFormat = "MMM";
                SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
                rangeLabel = "Year: " + yearFormat.format(rangeStart);
                break;
            case FULL:
            default:
                Date earliestDate = null;
                Date latestDate = null;
                for (TimeEntry entry : allEntries)
                {
                    if (entry.getStartTime() != null)
                    {
                        if (earliestDate == null || entry.getStartTime().before(earliestDate))
                        {
                            earliestDate = entry.getStartTime();
                        }
                        if (latestDate == null || entry.getStartTime().after(latestDate))
                        {
                            latestDate = entry.getStartTime();
                        }
                    }
                }
                if (earliestDate == null || latestDate == null)
                {
                    chart.clear();
                    chart.invalidate();
                    return;
                }
                rangeStart = getDayStart(earliestDate);
                rangeEnd = getDayEnd(latestDate);
                xAxisFormat = "dd.MM.yy";
                SimpleDateFormat fullFormat = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());
                rangeLabel = "Full: " + fullFormat.format(rangeStart) + " - " + fullFormat.format(rangeEnd);
                btnTimePrev.setEnabled(false);
                btnTimeNext.setEnabled(false);
                break;
        }
        tvTimeRangeLabel.setText(rangeLabel + " ▼");
        if (timeRangeMode != TimeRangeMode.FULL)
        {
            btnTimePrev.setEnabled(true);
            btnTimeNext.setEnabled(true);
        }
        Map<String, TreeMap<Integer, Long>> categoryData = new HashMap<>();
        for (String category : categories)
        {
            categoryData.put(category, new TreeMap<>());
        }
        int maxIndex = 0;
        switch (timeRangeMode)
        {
            case WEEK:
                maxIndex = WEEK_MAX_INDEX;
                break;
            case MONTH:
                Calendar monthCalendar = Calendar.getInstance();
                monthCalendar.setTime(rangeStart);
                monthCalendar.add(Calendar.MONTH, 1);
                monthCalendar.add(Calendar.DAY_OF_MONTH, -1);
                maxIndex = TimeUtils.daysBetween(rangeStart, monthCalendar.getTime()) / DAYS_PER_MONTH_PERIOD;
                break;
            case YEAR:
                maxIndex = MONTHS_PER_YEAR - 1;
                break;
            case FULL:
            default:
                maxIndex = FULL_MODE_PERIODS - 1;
                break;
        }
        for (TimeEntry entry : allEntries)
        {
            if (entry.getStartTime() == null) continue;
            if (entry.getStartTime().before(rangeStart) || entry.getStartTime().after(rangeEnd))
            {
                continue;
            }
            String category = entry.getCategory();
            int index;
            switch (timeRangeMode)
            {
                case WEEK:
                    index = TimeUtils.daysBetween(rangeStart, entry.getStartTime()) - 1;
                    break;
                case MONTH:
                    index = TimeUtils.daysBetween(rangeStart, entry.getStartTime()) / DAYS_PER_MONTH_PERIOD;
                    break;
                case YEAR:
                    Calendar entryCalendar = Calendar.getInstance();
                    entryCalendar.setTime(entry.getStartTime());
                    Calendar startCalendar = Calendar.getInstance();
                    startCalendar.setTime(rangeStart);
                    index = (entryCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR)) * MONTHS_PER_YEAR +
                            (entryCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH));
                    break;
                case FULL:
                default:
                    int totalDays = TimeUtils.daysBetween(rangeStart, rangeEnd) + 1;
                    int daysSinceStart = TimeUtils.daysBetween(rangeStart, entry.getStartTime());
                    index = Math.min((daysSinceStart * FULL_MODE_PERIODS) / totalDays, FULL_MODE_PERIODS - 1);
                    break;
            }
            TreeMap<Integer, Long> dataMap = categoryData.get(category);
            long currentTotal = dataMap.getOrDefault(index, 0L);
            dataMap.put(index, currentTotal + entry.getDurationSeconds());
        }
        for (String category : categories)
        {
            TreeMap<Integer, Long> dataMap = categoryData.get(category);
            for (int i = 0; i <= maxIndex; i++)
            {
                if (!dataMap.containsKey(i))
                {
                    dataMap.put(i, 0L);
                }
            }
        }
        List<LineDataSet> dataSets = new ArrayList<>();
        int[] colors = {
            Color.rgb(255, 99, 71),
            Color.rgb(100, 149, 237),
            Color.rgb(144, 238, 144),
            Color.rgb(255, 215, 0),
            Color.rgb(218, 112, 214),
            Color.rgb(64, 224, 208),
            Color.rgb(255, 160, 122),
            Color.rgb(138, 43, 226),
        };
        int colorIndex = 0;
        for (String category : categories)
        {
            TreeMap<Integer, Long> dataMap = categoryData.get(category);
            // Skip categories with no hours in the current time range:
            long totalSeconds = 0;
            for (Long seconds : dataMap.values())
            {
                totalSeconds += seconds;
            }
            if (totalSeconds == 0)
            {
                continue;
            }
            List<Entry> entries = new ArrayList<>();
            for (Map.Entry<Integer, Long> dataEntry : dataMap.entrySet())
            {
                float hours;
                if (timeRangeMode == TimeRangeMode.WEEK)
                {
                    hours = dataEntry.getValue() / (float)SECONDS_PER_HOUR;
                }
                else if (timeRangeMode == TimeRangeMode.MONTH)
                {
                    hours = dataEntry.getValue() / (float)SECONDS_PER_HOUR / DAYS_PER_MONTH_PERIOD;
                }
                else if (timeRangeMode == TimeRangeMode.YEAR)
                {
                    Calendar monthStart = Calendar.getInstance();
                    monthStart.setTime(rangeStart);
                    monthStart.add(Calendar.MONTH, dataEntry.getKey());
                    int daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH);
                    hours = dataEntry.getValue() / (float)SECONDS_PER_HOUR / daysInMonth;
                }
                else
                {
                    int totalDays = TimeUtils.daysBetween(rangeStart, rangeEnd) + 1;
                    int daysPerPeriod = totalDays / FULL_MODE_PERIODS;
                    if (daysPerPeriod < 1) daysPerPeriod = 1;
                    hours = dataEntry.getValue() / (float)SECONDS_PER_HOUR / daysPerPeriod;
                }
                entries.add(new Entry(dataEntry.getKey(), hours));
            }
            if (!entries.isEmpty())
            {
                LineDataSet dataSet = new LineDataSet(entries, category);
                dataSet.setColor(colors[colorIndex % colors.length]);
                dataSet.setCircleColor(colors[colorIndex % colors.length]);
                dataSet.setLineWidth(LINE_WIDTH);
                dataSet.setCircleRadius(CIRCLE_RADIUS);
                dataSet.setDrawCircleHole(false);
                dataSet.setValueTextSize(0f);
                dataSet.setDrawFilled(false);
                dataSet.setMode(LineDataSet.Mode.LINEAR);
                dataSets.add(dataSet);
                colorIndex++;
            }
        }
        if (!dataSets.isEmpty())
        {
            LineData lineData = new LineData(dataSets.toArray(new LineDataSet[0]));
            chart.setData(lineData);
            final Date finalRangeStart = rangeStart;
            final Date finalRangeEnd = rangeEnd;
            final String finalXAxisFormat = xAxisFormat;
            XAxis xAxis = chart.getXAxis();
            if (timeRangeMode == TimeRangeMode.WEEK)
            {
                xAxis.setLabelCount(WEEK_DAYS, true);
                xAxis.setAxisMinimum(0f);
                xAxis.setAxisMaximum(WEEK_MAX_INDEX);
            }
            else
            {
                xAxis.resetAxisMinimum();
                xAxis.resetAxisMaximum();
            }
            xAxis.setValueFormatter(new ValueFormatter()
            {
                private SimpleDateFormat dateFormat = new SimpleDateFormat(finalXAxisFormat, Locale.getDefault());

                @Override
                public String getFormattedValue(float value)
                {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(finalRangeStart);
                    switch (timeRangeMode)
                    {
                        case WEEK:
                            calendar.add(Calendar.DAY_OF_YEAR, (int)value);
                            break;
                        case MONTH:
                            int startDay = (int)value * DAYS_PER_MONTH_PERIOD + 1;
                            int endDay = Math.min(startDay + DAYS_PER_MONTH_PERIOD - 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                            return startDay + "." + "-" + endDay + ".";
                        case YEAR:
                            if ((int)value % YEAR_LABEL_SKIP != 0)
                            {
                                return "";
                            }
                            calendar.add(Calendar.MONTH, (int)value);
                            break;
                        case FULL:
                        default:
                            if ((int)value % FULL_LABEL_SKIP != 0)
                            {
                                return "";
                            }
                            int totalDays = TimeUtils.daysBetween(finalRangeStart, finalRangeEnd) + 1;
                            int daysPerPeriod = totalDays / FULL_MODE_PERIODS;
                            if (daysPerPeriod < 1) daysPerPeriod = 1;
                            int periodIndex = (int)value;
                            Calendar periodStart = Calendar.getInstance();
                            periodStart.setTime(finalRangeStart);
                            periodStart.add(Calendar.DAY_OF_YEAR, periodIndex * daysPerPeriod);
                            Calendar periodEnd = Calendar.getInstance();
                            periodEnd.setTime(periodStart.getTime());
                            if (periodIndex == FULL_MODE_PERIODS - 1)
                            {
                                periodEnd.setTime(finalRangeEnd);
                            }
                            else
                            {
                                periodEnd.add(Calendar.DAY_OF_YEAR, daysPerPeriod - 1);
                            }
                            SimpleDateFormat rangeFormat = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());
                            return rangeFormat.format(periodStart.getTime()) + "-" + rangeFormat.format(periodEnd.getTime());
                    }
                    return dateFormat.format(calendar.getTime());
                }
            });
            YAxis leftAxis = chart.getAxisLeft();
            leftAxis.setValueFormatter(new ValueFormatter()
            {
                @Override
                public String getFormattedValue(float value)
                {
                    return String.format(Locale.getDefault(), "%.1fh", value);
                }
            });
            chart.invalidate();
        }
        else
        {
            chart.clear();
            chart.invalidate();
            // Auto-navigate to nearest period with data if requested and current period is empty:
            if (autoNavigateToData && timeRangeMode != TimeRangeMode.FULL)
            {
                // Check previous periods first (more recent data):
                boolean foundData = false;
                for (int offset = -1; offset >= -MAX_PERIOD_SEARCH; offset--)
                {
                    if (hasDataInPeriod(offset))
                    {
                        currentTimeOffset = offset;
                        foundData = true;
                        loadChartData(false);
                        return;
                    }
                }
                // If no data in past, check future periods:
                if (!foundData)
                {
                    for (int offset = 1; offset <= MAX_PERIOD_SEARCH; offset++)
                    {
                        if (hasDataInPeriod(offset))
                        {
                            currentTimeOffset = offset;
                            loadChartData(false);
                            return;
                        }
                    }
                }
            }
        }
    }

    public void setTimeRangeMode(TimeRangeMode mode)
    {
        timeRangeMode = mode;
        currentTimeOffset = 0;
        // Auto-navigate to nearest period with data when switching modes:
        loadChartData(true);
    }

    public void navigateTimePrevious()
    {
        if (timeRangeMode == TimeRangeMode.FULL)
        {
            return;
        }
        // Find the next previous period with data (skip empty periods):
        for (int offset = currentTimeOffset - 1; offset >= -MAX_PERIOD_SEARCH; offset--)
        {
            if (hasDataInPeriod(offset))
            {
                currentTimeOffset = offset;
                loadChartData();
                return;
            }
        }
    }

    public void navigateTimeNext()
    {
        if (timeRangeMode == TimeRangeMode.FULL)
        {
            return;
        }
        // Find the next future period with data (skip empty periods):
        for (int offset = currentTimeOffset + 1; offset <= MAX_PERIOD_SEARCH; offset++)
        {
            if (hasDataInPeriod(offset))
            {
                currentTimeOffset = offset;
                loadChartData();
                return;
            }
        }
    }

    public TimeRangeMode getTimeRangeMode()
    {
        return timeRangeMode;
    }

    private boolean hasDataInPeriod(int offset)
    {
        List<TimeEntry> allEntries = timeEntryRepository.getAllEntries();
        if (allEntries.isEmpty())
        {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        Date rangeStart;
        Date rangeEnd;
        switch (timeRangeMode)
        {
            case WEEK:
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                int daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7;
                calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
                calendar.add(Calendar.WEEK_OF_YEAR, offset);
                rangeStart = getDayStart(calendar.getTime());
                calendar.add(Calendar.DAY_OF_YEAR, 6);
                rangeEnd = getDayEnd(calendar.getTime());
                break;
            case MONTH:
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.MONTH, offset);
                rangeStart = getDayStart(calendar.getTime());
                calendar.add(Calendar.MONTH, 1);
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                rangeEnd = getDayEnd(calendar.getTime());
                break;
            case YEAR:
                calendar.set(Calendar.DAY_OF_YEAR, 1);
                calendar.add(Calendar.YEAR, offset);
                rangeStart = getDayStart(calendar.getTime());
                calendar.add(Calendar.YEAR, 1);
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                rangeEnd = getDayEnd(calendar.getTime());
                break;
            default:
                return false;
        }
        for (TimeEntry entry : allEntries)
        {
            if (entry.getStartTime() != null &&
                !entry.getStartTime().before(rangeStart) &&
                !entry.getStartTime().after(rangeEnd) &&
                entry.getDurationSeconds() > 0)
            {
                return true;
            }
        }
        return false;
    }

    private Date getDayEnd(Date date)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    private Date getDayStart(Date date)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
