package com.example.dailyreminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/**
 * Uses AlarmManager to wake the app at the chosen time every day
 * (or on the chosen days of the week). When the alarm fires,
 * ReminderReceiver shows the notification and books the next one.
 */
public final class ReminderScheduler {

    public static final String EXTRA_ID = "reminder_id";
    public static final String EXTRA_IS_SNOOZE = "is_snooze";

    /** Snooze alarms use a different request code so they do not replace the daily alarm. */
    private static final int SNOOZE_OFFSET = 1_000_000;

    private ReminderScheduler() { }

    /**
     * Pure calculation (easy to unit test): the next time, strictly after "now",
     * that a reminder set for hour:minute on the given days should ring.
     */
    public static long nextTriggerMillis(Calendar now, int hour, int minute, int daysMask) {
        Calendar next = (Calendar) now.clone();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) next.add(Calendar.DAY_OF_MONTH, 1);
        // Move forward until we land on an allowed day (at most 7 steps).
        for (int i = 0; i < 7 && !Reminder.ringsOn(daysMask, next.get(Calendar.DAY_OF_WEEK)); i++) {
            next.add(Calendar.DAY_OF_MONTH, 1);
        }
        return next.getTimeInMillis();
    }

    private static PendingIntent buildIntent(Context ctx, long id, boolean snooze) {
        Intent intent = new Intent(ctx, ReminderReceiver.class);
        intent.putExtra(EXTRA_ID, id);
        intent.putExtra(EXTRA_IS_SNOOZE, snooze);
        int requestCode = (int) id + (snooze ? SNOOZE_OFFSET : 0);
        return PendingIntent.getBroadcast(ctx, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void setAlarm(Context ctx, long triggerAt, PendingIntent pi) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        boolean exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
        if (exactAllowed) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            // Fallback when the user has not allowed exact alarms: may ring a few minutes late.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    /** Books (or re-books) the next daily alarm for this reminder. */
    public static void schedule(Context ctx, Reminder r) {
        if (r.completed) {
            cancel(ctx, r.id);
            return;
        }
        long at = nextTriggerMillis(Calendar.getInstance(), r.hour, r.minute, r.daysMask);
        setAlarm(ctx, at, buildIntent(ctx, r.id, false));
    }

    /** Rings again after the reminder's snooze length. */
    public static void scheduleSnooze(Context ctx, Reminder r) {
        long at = System.currentTimeMillis() + r.snoozeMinutes * 60_000L;
        setAlarm(ctx, at, buildIntent(ctx, r.id, true));
    }

    public static void cancel(Context ctx, long id) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(buildIntent(ctx, id, false));
        am.cancel(buildIntent(ctx, id, true));
    }

    public static void rescheduleAll(Context ctx) {
        for (Reminder r : ReminderDbHelper.get(ctx).getAll()) schedule(ctx, r);
    }
}
