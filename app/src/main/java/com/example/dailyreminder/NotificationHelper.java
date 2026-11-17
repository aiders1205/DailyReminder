package com.example.dailyreminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Builds and shows the reminder notification (title + description)
 * with "Snooze" and "Mark done" buttons.
 */
public final class NotificationHelper {

    private static final String DEFAULT_CHANNEL = "reminders_default";

    private NotificationHelper() { }

    /**
     * On Android 8+ the sound belongs to the notification channel,
     * so each custom sound gets its own channel.
     */
    private static String channelFor(Context ctx, String soundUri) {
        String id = soundUri == null ? DEFAULT_CHANNEL : "reminders_" + Math.abs(soundUri.hashCode());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(id) == null) {
                String name = soundUri == null ? "Reminders" : "Reminders (custom sound)";
                NotificationChannel ch = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription(ctx.getString(R.string.channel_desc));
                if (soundUri != null) {
                    ch.setSound(Uri.parse(soundUri), new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
                }
                nm.createNotificationChannel(ch);
            }
        }
        return id;
    }

    private static PendingIntent action(Context ctx, String action, Reminder r, int code) {
        Intent i = new Intent(ctx, NotificationActionReceiver.class);
        i.setAction(action);
        i.putExtra(ReminderScheduler.EXTRA_ID, r.id);
        return PendingIntent.getBroadcast(ctx, (int) r.id * 10 + code, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void show(Context ctx, Reminder r) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return; // the user did not allow notifications
        }

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(ctx, (int) r.id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, channelFor(ctx, r.soundUri))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(r.title)
                .setContentText(r.description)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(r.description))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(openPi)
                .setAutoCancel(true)
                .addAction(0, ctx.getString(R.string.snooze) + " " + r.snoozeMinutes + " min",
                        action(ctx, NotificationActionReceiver.ACTION_SNOOZE, r, 1))
                .addAction(0, ctx.getString(R.string.mark_done),
                        action(ctx, NotificationActionReceiver.ACTION_DONE, r, 2));
        if (r.soundUri != null) b.setSound(Uri.parse(r.soundUri)); // Android 7 and below

        NotificationManagerCompat.from(ctx).notify((int) r.id, b.build());
    }

    public static void cancel(Context ctx, long id) {
        NotificationManagerCompat.from(ctx).cancel((int) id);
    }
}
