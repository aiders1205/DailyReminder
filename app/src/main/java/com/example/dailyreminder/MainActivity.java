package com.example.dailyreminder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Screen 1: the list of daily reminders.
 * Tap a reminder (or the pencil) to edit, the trash can to delete,
 * the check box to mark it completed, and + to add a new one.
 */
public class MainActivity extends AppCompatActivity implements ReminderAdapter.Listener {

    /** Stays true until the app process is closed, so the user unlocks once per session. */
    private static boolean unlocked = false;

    private ReminderDbHelper db;
    private ReminderAdapter adapter;
    private TextView emptyView;

    /** Privacy: asks for the phone's PIN / pattern / fingerprint before showing reminders. */
    private final ActivityResultLauncher<Intent> unlockLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    unlocked = true;
                    loadReminders();
                } else {
                    Toast.makeText(this, "Authentication required to view reminders", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this, "Notifications are off. Reminders will not appear.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Data Privacy Act (RA 10173): no reminder data is processed until the user
        // has read the Privacy Notice and ticked the consent boxes.
        if (!ConsentManager.hasConsent(this)) {
            startActivity(new Intent(this, ConsentActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        db = ReminderDbHelper.get(this);
        emptyView = findViewById(R.id.emptyView);

        RecyclerView list = findViewById(R.id.reminderList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReminderAdapter(this);
        list.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddEditReminderActivity.class)));

        requireUnlock();
        askForPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (unlocked) loadReminders();
    }

    // ----- Menu: view the Privacy Notice, or withdraw consent -----

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_privacy) {
            Intent i = new Intent(this, ConsentActivity.class);
            i.putExtra(ConsentActivity.EXTRA_VIEW_ONLY, true);
            startActivity(i);
            return true;
        }
        if (item.getItemId() == R.id.action_withdraw) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.withdraw_title)
                    .setMessage(R.string.withdraw_message)
                    .setPositiveButton(R.string.withdraw_confirm, (d, w) -> {
                        ConsentManager.withdrawConsentAndDeleteAllData(this);
                        Toast.makeText(this, R.string.withdraw_done, Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, ConsentActivity.class));
                        finish();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requireUnlock() {
        if (unlocked) return;
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km != null && km.isDeviceSecure()) {
            Intent intent = km.createConfirmDeviceCredentialIntent(
                    getString(R.string.app_name), "Confirm it's you to view your reminders");
            if (intent != null) {
                unlockLauncher.launch(intent);
                return;
            }
        }
        unlocked = true; // phone has no screen lock set, nothing to check against
    }

    private void askForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Allow exact reminders")
                        .setMessage("To get reminders at the exact minute you choose, allow \"Alarms & reminders\" for this app.")
                        .setPositiveButton("Open settings", (d, w) -> startActivity(new Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + getPackageName()))))
                        .setNegativeButton("Not now", null)
                        .show();
            }
        }
    }

    private void loadReminders() {
        List<Reminder> reminders = db.getAll();
        adapter.setItems(reminders);
        emptyView.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ----- ReminderAdapter.Listener -----

    @Override
    public void onEdit(Reminder r) {
        Intent i = new Intent(this, AddEditReminderActivity.class);
        i.putExtra(AddEditReminderActivity.EXTRA_ID, r.id);
        startActivity(i);
    }

    @Override
    public void onDelete(Reminder r) {
        new AlertDialog.Builder(this)
                .setTitle("Delete reminder?")
                .setMessage("\"" + r.title + "\" will be removed and you will stop getting its notification.")
                .setPositiveButton(R.string.delete, (d, w) -> {
                    ReminderScheduler.cancel(this, r.id);
                    NotificationHelper.cancel(this, r.id);
                    db.delete(r.id);
                    loadReminders();
                    Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onCompletedChanged(Reminder r, boolean completed) {
        r.completed = completed;
        db.update(r);
        ReminderScheduler.schedule(this, r); // cancels if completed, re-books if not
        loadReminders();
    }
}
