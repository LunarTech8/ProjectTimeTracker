package com.romanbrunner.apps.projecttimetracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "time_tracker_reminders";
    private static final int NOTIFICATION_ID = 1001;
    private static final int BEEP_COUNT = 12;
    private static final int BEEP_INTERVAL = 250; // milliseconds
    private static final int BEEP_DURATION = 200; // milliseconds
    private static final int BEEP_VOLUME = 100;
    private static final int NOTIFICATION_TIMEOUT = 3000; // milliseconds

    @Override
    public void onReceive(Context context, Intent intent) {
        // Create notification channel (required for Android O+)
        createNotificationChannel(context);

        // Trigger flashing in MainActivity if app is active
        MainActivity.triggerFlashIfActive();

        // Play beeps
        playBeeps(context);

        // Vibrate
        vibrate(context);

        // Show a subtle notification (optional, for user awareness)
        showNotification(context);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Time Tracker Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Reminder alerts for time tracking");
                channel.enableVibration(true);
                channel.setSound(null, null); // We'll play our own sound
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void playBeeps(Context context) {
        new Thread(() -> {
            try {
                ToneGenerator toneGenerator = new ToneGenerator(
                    AudioManager.STREAM_ALARM,
                    BEEP_VOLUME
                );

                for (int i = 0; i < BEEP_COUNT; i++) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_DURATION);
                    Thread.sleep(BEEP_INTERVAL);
                }

                toneGenerator.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = new long[BEEP_COUNT * 2];
            for (int i = 0; i < BEEP_COUNT; i++) {
                pattern[i * 2] = 0;
                pattern[i * 2 + 1] = BEEP_DURATION;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    private void showNotification(Context context) {
        NotificationManager notificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

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
