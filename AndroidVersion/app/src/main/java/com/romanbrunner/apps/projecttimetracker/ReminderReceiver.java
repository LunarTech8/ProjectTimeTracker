package com.romanbrunner.apps.projecttimetracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;

/**
 * BroadcastReceiver for time tracking reminders.
 */
public class ReminderReceiver extends BroadcastReceiver
{
    // Constants:
    private static final String CHANNEL_ID = "time_tracker_reminders";
    private static final int NOTIFICATION_ID = 1001;
    private static final int BEEP_COUNT = 12;
    private static final int BEEP_INTERVAL = 250;
    private static final int BEEP_DURATION = 200;
    private static final int BEEP_VOLUME = 100;
    private static final int NOTIFICATION_TIMEOUT = 3000;
    private static final int VIBRATE_NO_REPEAT = -1;
    private static final int VIBRATE_PATTERN_ENTRIES_PER_BEEP = 2;  // Pause + vibration

    @Override
    public void onReceive(Context context, Intent intent)
    {
        createNotificationChannel(context);  // Required for Android O+
        ControlPanelManager.triggerFlashIfActive();
        playBeeps(context);
        vibrate(context);
        showNotification(context);
    }

    private void createNotificationChannel(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null)
            {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Time Tracker Reminders", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Reminder alerts for time tracking");
                channel.enableVibration(true);
                channel.setSound(null, null);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void playBeeps(Context context)
    {
        new Thread(() ->
        {
            try
            {
                ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, BEEP_VOLUME);
                for (int i = 0; i < BEEP_COUNT; i++)
                {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_DURATION);
                    Thread.sleep(BEEP_INTERVAL);
                }
                toneGenerator.release();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }).start();
    }

    private void vibrate(Context context)
    {
        Vibrator vibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator())
        {
            long[] pattern = new long[BEEP_COUNT * VIBRATE_PATTERN_ENTRIES_PER_BEEP];
            for (int i = 0; i < BEEP_COUNT; i++)
            {
                int patternIndex = i * VIBRATE_PATTERN_ENTRIES_PER_BEEP;
                pattern[patternIndex] = 0;  // Pause before vibration
                pattern[patternIndex + 1] = BEEP_DURATION;  // Vibration duration
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, VIBRATE_NO_REPEAT));
            }
            else
            {
                vibrator.vibrate(pattern, VIBRATE_NO_REPEAT);
            }
        }
    }

    private void showNotification(Context context)
    {
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Time Tracker Reminder")
                .setContentText("Your reminder interval has elapsed")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setTimeoutAfter(NOTIFICATION_TIMEOUT);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
