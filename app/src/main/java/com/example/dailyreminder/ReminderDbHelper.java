package com.example.dailyreminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite database that stores the reminders.
 *
 * The database file lives in the app's private storage (/data/data/app/databases),
 * which other apps cannot open. Title and description are also encrypted with
 * CryptoHelper before being written.
 */
public class ReminderDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "reminders.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE = "reminders";
    private static final String COL_ID = "_id";
    private static final String COL_TITLE = "title";
    private static final String COL_DESC = "description";
    private static final String COL_HOUR = "hour";
    private static final String COL_MINUTE = "minute";
    private static final String COL_DAYS = "days_mask";
    private static final String COL_COMPLETED = "completed";
    private static final String COL_SOUND = "sound_uri";
    private static final String COL_SNOOZE = "snooze_minutes";

    private static ReminderDbHelper instance;

    public static synchronized ReminderDbHelper get(Context context) {
        if (instance == null) instance = new ReminderDbHelper(context.getApplicationContext());
        return instance;
    }

    /** Closes and permanently deletes the whole database file (used when consent is withdrawn). */
    public static synchronized void deleteDatabase(Context context) {
        if (instance != null) {
            instance.close();
            instance = null;
        }
        context.getApplicationContext().deleteDatabase(DB_NAME);
    }

    private ReminderDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TITLE + " TEXT NOT NULL, "
                + COL_DESC + " TEXT, "
                + COL_HOUR + " INTEGER NOT NULL, "
                + COL_MINUTE + " INTEGER NOT NULL, "
                + COL_DAYS + " INTEGER NOT NULL DEFAULT 0, "
                + COL_COMPLETED + " INTEGER NOT NULL DEFAULT 0, "
                + COL_SOUND + " TEXT, "
                + COL_SNOOZE + " INTEGER NOT NULL DEFAULT 10)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    private ContentValues toValues(Reminder r) {
        ContentValues v = new ContentValues();
        v.put(COL_TITLE, CryptoHelper.encrypt(r.title));
        v.put(COL_DESC, CryptoHelper.encrypt(r.description));
        v.put(COL_HOUR, r.hour);
        v.put(COL_MINUTE, r.minute);
        v.put(COL_DAYS, r.daysMask);
        v.put(COL_COMPLETED, r.completed ? 1 : 0);
        v.put(COL_SOUND, r.soundUri);
        v.put(COL_SNOOZE, r.snoozeMinutes);
        return v;
    }

    private Reminder fromCursor(Cursor c) {
        Reminder r = new Reminder();
        r.id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
        r.title = CryptoHelper.decrypt(c.getString(c.getColumnIndexOrThrow(COL_TITLE)));
        r.description = CryptoHelper.decrypt(c.getString(c.getColumnIndexOrThrow(COL_DESC)));
        r.hour = c.getInt(c.getColumnIndexOrThrow(COL_HOUR));
        r.minute = c.getInt(c.getColumnIndexOrThrow(COL_MINUTE));
        r.daysMask = c.getInt(c.getColumnIndexOrThrow(COL_DAYS));
        r.completed = c.getInt(c.getColumnIndexOrThrow(COL_COMPLETED)) == 1;
        r.soundUri = c.getString(c.getColumnIndexOrThrow(COL_SOUND));
        r.snoozeMinutes = c.getInt(c.getColumnIndexOrThrow(COL_SNOOZE));
        return r;
    }

    /** Inserts a reminder and returns its new id. */
    public long insert(Reminder r) {
        r.id = getWritableDatabase().insert(TABLE, null, toValues(r));
        return r.id;
    }

    public void update(Reminder r) {
        getWritableDatabase().update(TABLE, toValues(r), COL_ID + "=?",
                new String[]{String.valueOf(r.id)});
    }

    public void delete(long id) {
        getWritableDatabase().delete(TABLE, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public Reminder getById(long id) {
        try (Cursor c = getReadableDatabase().query(TABLE, null, COL_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? fromCursor(c) : null;
        }
    }

    /** All reminders: unfinished first, then ordered by time of day. */
    public List<Reminder> getAll() {
        List<Reminder> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE, null, null, null, null, null,
                COL_COMPLETED + " ASC, " + COL_HOUR + " ASC, " + COL_MINUTE + " ASC")) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }
}
