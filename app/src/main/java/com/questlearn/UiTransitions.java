package com.questlearn;

import android.app.Activity;

/**
 * Activity transitions respect “Reduce slide animations” in Settings.
 */
public final class UiTransitions {

    private UiTransitions() {}

    public static void openForward(Activity activity) {
        if (DisplayPreferences.isReduceMotion(activity)) {
            activity.overridePendingTransition(0, 0);
        } else {
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    public static void closeBackward(Activity activity) {
        if (DisplayPreferences.isReduceMotion(activity)) {
            activity.overridePendingTransition(0, 0);
        } else {
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    public static void splashCrossFade(Activity activity) {
        if (DisplayPreferences.isReduceMotion(activity)) {
            activity.overridePendingTransition(0, 0);
        } else {
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }
}
