package com.example.dailyreminder;

import java.util.Calendar;
import java.util.Locale;

/**
 * Model class: one daily reminder.
 *
 * daysMask stores the days of the week the reminder repeats on.
 * Bit 0 = Sunday, bit 1 = Monday ... bit 6 = Saturday.
 * A mask of 0 means "every day".
 */
public class Reminder {

    public static final String[] DAY_SHORT = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    public long id;
    public String title;
    public String description;
    public int hour;          // 0 - 23
    public int minute;        // 0 - 59
    public int daysMask;      // 0 = every day
    public boolean completed;
    public String soundUri;   // null = default notification sound
    public int snoozeMinutes;

    public Reminder() {
        hour = 8;
        minute = 0;
        snoozeMinutes = 10;
    }

    /** True if the reminder should ring on the given Calendar.DAY_OF_WEEK (1 = Sunday). */
    public static boolean ringsOn(int daysMask, int calendarDayOfWeek) {
        if (daysMask == 0) return true;
        return (daysMask & (1 << (calendarDayOfWeek - Calendar.SUNDAY))) != 0;
    }

    /** e.g. "08:30 AM" */
    public static String formatTime(int hour, int minute) {
        int h12 = hour % 12 == 0 ? 12 : hour % 12;
        return String.format(Locale.US, "%02d:%02d %s", h12, minute, hour < 12 ? "AM" : "PM");
    }

    /** e.g. "Every day" or "Mon, Wed" */
    public static String formatDays(int daysMask) {
        if (daysMask == 0 || daysMask == 0b1111111) return "Every day";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((daysMask & (1 << i)) != 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(DAY_SHORT[i]);
            }
        }
        return sb.toString();
    }

    public String getTimeText() {
        return formatTime(hour, minute);
    }

    public String getDaysText() {
        return formatDays(daysMask);
    }
}
