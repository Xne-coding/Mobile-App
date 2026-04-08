package com.questlearn;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import com.google.android.material.color.MaterialColors;

/**
 * Theme-aware colors for views built in code (dialogs, programmatic EditTexts).
 * Use {@link #wrapAppTheme(Context)} when the activity uses a non-DayNight theme (e.g. scan screen).
 */
public final class UiTheme {

    private UiTheme() {}

    /** Context whose theme is {@link R.style#Theme_QuestLearn} (DayNight). */
    public static Context wrapAppTheme(Context base) {
        return new ContextThemeWrapper(base, R.style.Theme_QuestLearn);
    }

    public static int colorOnSurface(Context themedContext) {
        return MaterialColors.getColor(
                themedContext, com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
    }

    public static int textColorHint(Context themedContext) {
        TypedValue tv = new TypedValue();
        if (themedContext.getTheme().resolveAttribute(android.R.attr.textColorHint, tv, true)) {
            return tv.data;
        }
        int on = colorOnSurface(themedContext);
        return (on & 0x00FFFFFF) | 0x99000000;
    }
}
