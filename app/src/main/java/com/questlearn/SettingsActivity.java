package com.questlearn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.questlearn.db.FirebaseRepository;

/**
 * Display settings: light/dark theme, text size 0–100%, reduced slide transitions,
 * change password (signed-in users), log out, and read-aloud preference.
 */
public class SettingsActivity extends QuestLearnBaseActivity {

    private boolean syncingUi;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SystemBarInsets.applyToRoot(this, R.id.settingsRoot);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
            UiTransitions.closeBackward(this);
        });

        SharedPreferences prefs = DisplayPreferences.prefs(this);

        TextView tvFontPercent = findViewById(R.id.tvFontScalePercent);
        Slider sliderFont = findViewById(R.id.sliderFontScale);
        SwitchMaterial switchReduceSlides = findViewById(R.id.switchReduceSlides);
        SwitchMaterial switchReadAloud = findViewById(R.id.switchReadAloud);
        MaterialButtonToggleGroup toggleTheme = findViewById(R.id.toggleTheme);

        int fontPercent = DisplayPreferences.getFontScalePercent(this);

        syncingUi = true;
        sliderFont.setValue(fontPercent);
        updateFontPercentLabel(tvFontPercent, fontPercent);
        switchReduceSlides.setChecked(prefs.getBoolean(DisplayPreferences.KEY_REDUCE_MOTION, false));
        switchReadAloud.setChecked(prefs.getBoolean(DisplayPreferences.KEY_READ_ALOUD, false));
        if (DisplayPreferences.isDarkTheme(this)) {
            toggleTheme.check(R.id.btnThemeDark);
        } else {
            toggleTheme.check(R.id.btnThemeLight);
        }
        syncingUi = false;

        sliderFont.addOnChangeListener((slider, value, fromUser) -> {
            if (syncingUi || !fromUser) {
                return;
            }
            updateFontPercentLabel(tvFontPercent, Math.round(value));
        });

        sliderFont.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(Slider slider) {}

            @Override
            public void onStopTrackingTouch(Slider slider) {
                if (syncingUi) {
                    return;
                }
                int v = Math.round(slider.getValue());
                v = Math.max(0, Math.min(100, v));
                prefs.edit()
                        .putInt(DisplayPreferences.KEY_FONT_SCALE_PERCENT, v)
                        .apply();
                recreate();
            }
        });

        switchReduceSlides.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (syncingUi) return;
            prefs.edit().putBoolean(DisplayPreferences.KEY_REDUCE_MOTION, isChecked).apply();
        });

        switchReadAloud.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (syncingUi) return;
            prefs.edit().putBoolean(DisplayPreferences.KEY_READ_ALOUD, isChecked).apply();
        });

        toggleTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (syncingUi || !isChecked) return;
            String mode = checkedId == R.id.btnThemeDark
                    ? DisplayPreferences.THEME_DARK
                    : DisplayPreferences.THEME_LIGHT;
            prefs.edit().putString(DisplayPreferences.KEY_THEME_MODE, mode).apply();
            int nightMode = DisplayPreferences.THEME_DARK.equals(mode)
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(nightMode);
            recreate();
        });

        FirebaseRepository repo = new FirebaseRepository();
        SharedPreferences accountPrefs = getSharedPreferences("questlearn_prefs", MODE_PRIVATE);

        MaterialButton btnChangePassword = findViewById(R.id.btnSettingsChangePassword);
        MaterialButton btnLogout = findViewById(R.id.btnSettingsLogout);

        if (repo.isGuest()) {
            btnChangePassword.setVisibility(View.GONE);
        } else {
            btnChangePassword.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, ChangePasswordActivity.class));
                    UiTransitions.openForward(this);
                } catch (Exception e) {
                    Toast.makeText(this, R.string.settings_change_password_error, Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnLogout.setOnClickListener(v -> {
            btnLogout.setEnabled(false);
            repo.signOut();
            accountPrefs.edit()
                    .putBoolean("user_logged_in", false)
                    .remove("remember_me")
                    .remove("user_name")
                    .remove("user_initials")
                    .remove("user_email")
                    .remove("is_guest")
                    .commit();
            Toast.makeText(this, R.string.settings_logged_out, Toast.LENGTH_SHORT).show();
            try {
                Intent intent = new Intent(this, SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                UiTransitions.closeBackward(this);
                finish();
            } catch (Exception e) {
                btnLogout.setEnabled(true);
            }
        });
    }

    private static void updateFontPercentLabel(TextView tv, int percent) {
        tv.setText(tv.getContext().getString(R.string.settings_font_percent, percent));
    }
}
