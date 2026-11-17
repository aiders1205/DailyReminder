package com.example.dailyreminder;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Screen 2: add a new reminder, or edit an existing one.
 * The user enters a title and description, picks a time,
 * and can optionally choose repeat days, snooze length and a sound.
 */
public class AddEditReminderActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "edit_id";
    private static final int[] SNOOZE_CHOICES = {5, 10, 15, 30};

    private ReminderDbHelper db;
    private Reminder reminder;
    private boolean isEdit;

    private TextInputLayout titleLayout;
    private TextInputEditText titleInput, descriptionInput;
    private MaterialButton timeButton, soundButton;
    private Spinner snoozeSpinner;
    private final Chip[] dayChips = new Chip[7];

    /** Opens Android's built-in sound picker. */
    private final ActivityResultLauncher<Intent> soundPicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    reminder.soundUri = uri == null ? null : uri.toString();
                    showSoundName();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        db = ReminderDbHelper.get(this);
        long id = getIntent().getLongExtra(EXTRA_ID, -1);
        isEdit = id != -1;
        reminder = isEdit ? db.getById(id) : new Reminder();
        if (reminder == null) { finish(); return; }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(isEdit ? R.string.edit_reminder : R.string.add_reminder);
        toolbar.setNavigationOnClickListener(v -> finish());

        titleLayout = findViewById(R.id.titleLayout);
        titleInput = findViewById(R.id.inputTitle);
        descriptionInput = findViewById(R.id.inputDescription);
        timeButton = findViewById(R.id.buttonTime);
        soundButton = findViewById(R.id.buttonSound);
        snoozeSpinner = findViewById(R.id.spinnerSnooze);
        int[] chipIds = {R.id.chipSun, R.id.chipMon, R.id.chipTue, R.id.chipWed,
                R.id.chipThu, R.id.chipFri, R.id.chipSat};
        for (int i = 0; i < 7; i++) dayChips[i] = findViewById(chipIds[i]);

        String[] snoozeLabels = new String[SNOOZE_CHOICES.length];
        for (int i = 0; i < SNOOZE_CHOICES.length; i++) snoozeLabels[i] = SNOOZE_CHOICES[i] + " minutes";
        ArrayAdapter<String> snoozeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, snoozeLabels);
        snoozeSpinner.setAdapter(snoozeAdapter);

        // Fill the form with the reminder's current values.
        titleInput.setText(reminder.title);
        descriptionInput.setText(reminder.description);
        for (int i = 0; i < 7; i++) dayChips[i].setChecked((reminder.daysMask & (1 << i)) != 0);
        for (int i = 0; i < SNOOZE_CHOICES.length; i++) {
            if (SNOOZE_CHOICES[i] == reminder.snoozeMinutes) snoozeSpinner.setSelection(i);
        }
        showTime();
        showSoundName();

        timeButton.setOnClickListener(v -> new TimePickerDialog(this, (picker, hour, minute) -> {
            reminder.hour = hour;
            reminder.minute = minute;
            showTime();
        }, reminder.hour, reminder.minute, false).show());

        soundButton.setOnClickListener(v -> {
            Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_sound));
            i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            if (reminder.soundUri != null) {
                i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(reminder.soundUri));
            }
            soundPicker.launch(i);
        });

        // Remove the "Please enter a title" error as soon as the user starts typing.
        titleInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) titleLayout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        findViewById(R.id.buttonSave).setOnClickListener(v -> save());
    }

    private void showTime() {
        timeButton.setText(reminder.getTimeText());
    }

    private void showSoundName() {
        if (reminder.soundUri == null) {
            soundButton.setText(R.string.default_sound);
            return;
        }
        Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(reminder.soundUri));
        soundButton.setText(ringtone != null ? ringtone.getTitle(this) : "Custom sound");
    }

    private void save() {
        String title = titleInput.getText() == null ? "" : titleInput.getText().toString().trim();
        String description = descriptionInput.getText() == null ? "" : descriptionInput.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            titleLayout.setError("Please enter a title");
            titleInput.requestFocus();
            return;
        }
        titleLayout.setError(null);

        int mask = 0;
        for (int i = 0; i < 7; i++) if (dayChips[i].isChecked()) mask |= (1 << i);

        reminder.title = title;
        reminder.description = description;
        reminder.daysMask = mask;
        reminder.snoozeMinutes = SNOOZE_CHOICES[snoozeSpinner.getSelectedItemPosition()];

        if (isEdit) {
            reminder.completed = false; // editing re-activates a reminder
            db.update(reminder);
        } else {
            db.insert(reminder);
        }
        ReminderScheduler.schedule(this, reminder);

        Toast.makeText(this, "Reminder set for " + reminder.getTimeText() + " (" + reminder.getDaysText() + ")",
                Toast.LENGTH_SHORT).show();
        finish();
    }
}
