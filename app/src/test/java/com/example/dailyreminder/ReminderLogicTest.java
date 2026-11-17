package com.example.dailyreminder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

/** Unit tests for the reminder time calculations (run with: gradlew test). */
public class ReminderLogicTest {

    private static Calendar at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, day, hour, minute, 0);
        return c;
    }

    private static Calendar fromMillis(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c;
    }

    // 2026-11-16 is a Monday.

    @Test
    public void dailyReminder_laterToday_ringsToday() {
        Calendar now = at(2026, Calendar.NOVEMBER, 16, 7, 0);
        Calendar next = fromMillis(ReminderScheduler.nextTriggerMillis(now, 8, 30, 0));
        assertEquals(16, next.get(Calendar.DAY_OF_MONTH));
        assertEquals(8, next.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, next.get(Calendar.MINUTE));
    }

    @Test
    public void dailyReminder_timeAlreadyPassed_ringsTomorrow() {
        Calendar now = at(2026, Calendar.NOVEMBER, 16, 9, 0);
        Calendar next = fromMillis(ReminderScheduler.nextTriggerMillis(now, 8, 30, 0));
        assertEquals(17, next.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void dailyReminder_exactSameMinute_ringsTomorrow() {
        Calendar now = at(2026, Calendar.NOVEMBER, 16, 8, 30);
        Calendar next = fromMillis(ReminderScheduler.nextTriggerMillis(now, 8, 30, 0));
        assertEquals(17, next.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void mondayAndWednesday_fromMondayEvening_ringsWednesday() {
        int monWed = (1 << 1) | (1 << 3);
        Calendar now = at(2026, Calendar.NOVEMBER, 16, 20, 0); // Monday 8 PM
        Calendar next = fromMillis(ReminderScheduler.nextTriggerMillis(now, 8, 0, monWed));
        assertEquals(Calendar.WEDNESDAY, next.get(Calendar.DAY_OF_WEEK));
        assertEquals(18, next.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void mondayOnly_fromTuesday_ringsNextMonday() {
        int mon = 1 << 1;
        Calendar now = at(2026, Calendar.NOVEMBER, 17, 10, 0); // Tuesday
        Calendar next = fromMillis(ReminderScheduler.nextTriggerMillis(now, 8, 0, mon));
        assertEquals(Calendar.MONDAY, next.get(Calendar.DAY_OF_WEEK));
        assertEquals(23, next.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void endOfMonth_rollsOverToNextMonth() {
        Calendar now = at(2026, Calendar.NOVEMBER, 30, 23, 0);
        Calendar next = fromMillis(ReminderScheduler.nextTriggerMillis(now, 6, 0, 0));
        assertEquals(Calendar.DECEMBER, next.get(Calendar.MONTH));
        assertEquals(1, next.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void ringsOn_everyDayMask() {
        for (int d = Calendar.SUNDAY; d <= Calendar.SATURDAY; d++) assertTrue(Reminder.ringsOn(0, d));
    }

    @Test
    public void ringsOn_specificDays() {
        int monWed = (1 << 1) | (1 << 3);
        assertTrue(Reminder.ringsOn(monWed, Calendar.MONDAY));
        assertTrue(Reminder.ringsOn(monWed, Calendar.WEDNESDAY));
        assertFalse(Reminder.ringsOn(monWed, Calendar.TUESDAY));
        assertFalse(Reminder.ringsOn(monWed, Calendar.SUNDAY));
    }

    @Test
    public void formatTime_12HourClock() {
        assertEquals("12:00 AM", Reminder.formatTime(0, 0));
        assertEquals("08:05 AM", Reminder.formatTime(8, 5));
        assertEquals("12:30 PM", Reminder.formatTime(12, 30));
        assertEquals("09:45 PM", Reminder.formatTime(21, 45));
    }

    @Test
    public void formatDays_text() {
        assertEquals("Every day", Reminder.formatDays(0));
        assertEquals("Every day", Reminder.formatDays(0b1111111));
        assertEquals("Mon, Wed", Reminder.formatDays((1 << 1) | (1 << 3)));
    }
}
