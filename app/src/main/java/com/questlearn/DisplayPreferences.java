package com.questlearn;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Persisted display options: font scale (0–100%), reduced slide animations, light/dark theme,
 * optional read-aloud preference.
 */
public final class DisplayPreferences {

    public static final String PREFS_NAME = "questlearn_prefs";
    /** 0 = system default scale in-app; 100 = maximum extra scaling. */
    public static final String KEY_FONT_SCALE_PERCENT = "display_font_scale_percent";
    /** Legacy boolean; migrated when reading {@link #getFontScalePercent(Context)}. */
    private static final String KEY_LARGE_TEXT_LEGACY = "display_large_text";

    public static final String KEY_REDUCE_MOTION = "display_reduce_motion";
    /** User allows read-aloud / TTS-style behaviour where the app supports it. */
    public static final String KEY_READ_ALOUD = "display_read_aloud";
    public static final String KEY_THEME_MODE = "display_theme_mode";

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    /** At 100%, fontScale is multiplied by (1 + this). */
    private static final float MAX_FONT_SCALE_EXTRA = 0.55f;

    private DisplayPreferences() {}

    public static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * @return 0–100; 0% keeps the system font scale only, 100% applies the largest in-app boost.
     */
    public static int getFontScalePercent(Context context) {
        SharedPreferences p = prefs(context);
        if (p.contains(KEY_FONT_SCALE_PERCENT)) {
            return clampPercent(p.getInt(KEY_FONT_SCALE_PERCENT, 0));
        }
        if (p.getBoolean(KEY_LARGE_TEXT_LEGACY, false)) {
            return 50;
        }
        return 0;
    }

    private static int clampPercent(int v) {
        if (v < 0) return 0;
        if (v > 100) return 100;
        return v;
    }

    public static boolean isReduceMotion(Context context) {
        return prefs(context).getBoolean(KEY_REDUCE_MOTION, false);
    }

    public static boolean isReadAloudEnabled(Context context) {
        return prefs(context).getBoolean(KEY_READ_ALOUD, false);
    }

    public static String getThemeMode(Context context) {
        return prefs(context).getString(KEY_THEME_MODE, THEME_LIGHT);
    }

    public static boolean isDarkTheme(Context context) {
        return THEME_DARK.equals(getThemeMode(context));
    }

    public static void applyStoredNightMode(Context appContext) {
        int mode = isDarkTheme(appContext)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * Applies extra font scaling on top of the user's system font size.
     * 0% → no change; 100% → up to {@value #MAX_FONT_SCALE_EXTRA} extra multiplier.
     */
    public static Context wrapContextForFontScale(Context base) {
        int percent = getFontScalePercent(base);
        if (percent <= 0) {
            return base;
        }
        float mult = 1f + (MAX_FONT_SCALE_EXTRA * (percent / 100f));
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.fontScale = config.fontScale * mult;
        return base.createConfigurationContext(config);
    }
}
