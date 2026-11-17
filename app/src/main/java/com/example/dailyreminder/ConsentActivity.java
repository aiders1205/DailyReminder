package com.example.dailyreminder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

/**
 * Shows the Data Privacy Notice and the Terms of Use.
 *
 * First launch (or after the notice changes): the user must tick both boxes
 * before "Agree and Continue" is enabled. "Decline" closes the app, because
 * the app cannot store reminders without the user's consent.
 *
 * View mode (EXTRA_VIEW_ONLY, opened from the menu): shows the same text and
 * the date consent was given, without the check boxes.
 */
public class ConsentActivity extends AppCompatActivity {

    public static final String EXTRA_VIEW_ONLY = "view_only";

    private CheckBox privacyCheck, termsCheck;
    private MaterialButton agreeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consent);

        boolean viewOnly = getIntent().getBooleanExtra(EXTRA_VIEW_ONLY, false);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TextView noticeText = findViewById(R.id.textNotice);
        TextView termsText = findViewById(R.id.textTerms);
        TextView consentDate = findViewById(R.id.textConsentDate);
        View consentPanel = findViewById(R.id.consentPanel);
        privacyCheck = findViewById(R.id.checkPrivacy);
        termsCheck = findViewById(R.id.checkTerms);
        agreeButton = findViewById(R.id.buttonAgree);

        noticeText.setText(HtmlCompat.fromHtml(getString(R.string.privacy_notice_html),
                HtmlCompat.FROM_HTML_MODE_COMPACT));
        termsText.setText(HtmlCompat.fromHtml(getString(R.string.terms_of_use_html),
                HtmlCompat.FROM_HTML_MODE_COMPACT));

        if (viewOnly) {
            toolbar.setNavigationIcon(R.drawable.ic_back);
            toolbar.setNavigationOnClickListener(v -> finish());
            consentPanel.setVisibility(View.GONE);
            String date = ConsentManager.getConsentDateText(this);
            if (date != null) {
                consentDate.setText(getString(R.string.consent_given_on, date));
                consentDate.setVisibility(View.VISIBLE);
            }
            return;
        }

        // The button is enabled only when BOTH boxes are ticked.
        privacyCheck.setOnCheckedChangeListener((b, checked) -> updateButton());
        termsCheck.setOnCheckedChangeListener((b, checked) -> updateButton());
        updateButton();

        agreeButton.setOnClickListener(v -> {
            ConsentManager.giveConsent(this);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.buttonDecline).setOnClickListener(v -> decline());

        // Pressing Back without agreeing is the same as declining.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                decline();
            }
        });
    }

    private void updateButton() {
        agreeButton.setEnabled(privacyCheck.isChecked() && termsCheck.isChecked());
    }

    private void decline() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.decline_title)
                .setMessage(R.string.decline_message)
                .setPositiveButton(R.string.close_app, (d, w) -> finishAffinity())
                .setNegativeButton(R.string.go_back, null)
                .show();
    }
}
