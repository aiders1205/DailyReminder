package com.example.dailyreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** Handles the "Snooze" and "Mark done" buttons on the notification. */
public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_SNOOZE = "com.example.dailyreminder.SNOOZE";
    public static final String ACTION_DONE = "com.example.dailyreminder.DONE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ConsentManager.hasConsent(context)) return; // no consent, no processing
        long id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1);
        ReminderDbHelper db = ReminderDbHelper.get(context);
        Reminder r = db.getById(id);
        NotificationHelper.cancel(context, id);
        if (r == null) return;

        if (ACTION_SNOOZE.equals(intent.getAction())) {
            ReminderScheduler.scheduleSnooze(context, r);
            Toast.makeText(context, "Snoozed for " + r.snoozeMinutes + " minutes", Toast.LENGTH_SHORT).show();
        } else if (ACTION_DONE.equals(intent.getAction())) {
            r.completed = true;
            db.update(r);
            ReminderScheduler.cancel(context, id);
            Toast.makeText(context, "Marked as completed", Toast.LENGTH_SHORT).show();
        }
    }
}
