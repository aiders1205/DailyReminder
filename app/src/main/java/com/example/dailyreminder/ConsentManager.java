package com.example.dailyreminder;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Keeps the record of the user's consent to the Data Privacy Notice
 * (Data Privacy Act of 2012, RA 10173) and agreement to the Terms of Use.
 *
 * The record (notice version + date and time) is saved in the app's private
 * storage so the app can show when consent was given. If the notice changes,
 * NOTICE_VERSION is increased and the user is asked to consent again.
 */
public final class ConsentManager {

    /** Increase this number whenever the Privacy Notice or Terms of Use change. */
    public static final int NOTICE_VERSION = 1;

    private static final String PREFS = "consent_record";
    private static final String KEY_VERSION = "accepted_version";
    private static final String KEY_TIME = "accepted_at";

    private ConsentManager() { }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** True if the user has accepted the current version of the notice and terms. */
    public static boolean hasConsent(Context ctx) {
        return prefs(ctx).getInt(KEY_VERSION, 0) == NOTICE_VERSION;
    }

    public static void giveConsent(Context ctx) {
        prefs(ctx).edit()
                .putInt(KEY_VERSION, NOTICE_VERSION)
                .putLong(KEY_TIME, System.currentTimeMillis())
                .apply();
    }

    /** e.g. "Nov 17, 2026, 9:30:00 AM", or null if no consent is recorded. */
    public static String getConsentDateText(Context ctx) {
        long time = prefs(ctx).getLong(KEY_TIME, 0);
        if (time == 0) return null;
        return DateFormat.getDateTimeInstance().format(new Date(time));
    }

    /**
     * Withdraw consent: cancels every alarm and notification, deletes the
     * database and the encryption key, and clears the consent record.
     * After this, no reminder data remains on the phone.
     */
    public static void withdrawConsentAndDeleteAllData(Context ctx) {
        List<Reminder> all = ReminderDbHelper.get(ctx).getAll();
        for (Reminder r : all) {
            ReminderScheduler.cancel(ctx, r.id);
            NotificationHelper.cancel(ctx, r.id);
        }
        ReminderDbHelper.deleteDatabase(ctx);
        CryptoHelper.deleteKey();
        prefs(ctx).edit().clear().commit();
    }
}
