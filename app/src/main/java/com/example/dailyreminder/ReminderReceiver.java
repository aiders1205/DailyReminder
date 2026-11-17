package com.example.dailyreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Runs when an alarm goes off: shows the notification and books the next day's alarm. */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ConsentManager.hasConsent(context)) return; // no consent, no processing
        long id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1);
        boolean isSnooze = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_SNOOZE, false);
        Reminder r = ReminderDbHelper.get(context).getById(id);
        if (r == null || r.completed) return;

        NotificationHelper.show(context, r);

        // A snooze is a one-off; the regular daily alarm is still booked separately.
        if (!isSnooze) ReminderScheduler.schedule(context, r);
    }
}
