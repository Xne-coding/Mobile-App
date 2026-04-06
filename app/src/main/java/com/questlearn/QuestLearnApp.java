package com.questlearn;

import android.app.Application;

/**
 * Ensures saved light/dark mode is applied before any activity is shown.
 */
public class QuestLearnApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DisplayPreferences.applyStoredNightMode(this);
        ReadAloud.getInstance().init(this);
    }
}
