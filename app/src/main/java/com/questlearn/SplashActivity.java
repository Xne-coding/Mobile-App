package com.questlearn;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 2800L;
    private static final String PREFS_NAME = "questlearn_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_complete";
    private static final String KEY_LOGGED_IN = "user_logged_in";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View content = findViewById(R.id.splash_content);
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        // Fade in content
        content.setAlpha(0f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(content, "alpha", 0f, 1f);
        fadeIn.setDuration(700);
        fadeIn.setStartDelay(200);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator slideUp = ObjectAnimator.ofFloat(content, "translationY", 40f, 0f);
        slideUp.setDuration(700);
        slideUp.setStartDelay(200);
        slideUp.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeIn, slideUp);
        set.start();

        // Animate loader dots
        animateDots(dot1, dot2, dot3);

        // Navigate after delay
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DURATION);
    }

    private void animateDots(View dot1, View dot2, View dot3) {
        View[] dots = {dot1, dot2, dot3};
        for (int i = 0; i < dots.length; i++) {
            final View dot = dots[i];
            final int delay = i * 200;
            dot.postDelayed(() -> {
                ObjectAnimator pulse = ObjectAnimator.ofFloat(dot, "alpha", 0.4f, 1f);
                pulse.setDuration(600);
                pulse.setRepeatCount(ObjectAnimator.INFINITE);
                pulse.setRepeatMode(ObjectAnimator.REVERSE);
                pulse.start();
            }, delay);
        }
    }

    private void navigateNext() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean onboardingDone = prefs.getBoolean(KEY_ONBOARDING_DONE, false);
        boolean loggedIn = prefs.getBoolean(KEY_LOGGED_IN, false);

        Intent intent;
        if (loggedIn) {
            intent = new Intent(this, MainActivity.class);
        } else if (onboardingDone) {
            intent = new Intent(this, LoginActivity.class);
        } else {
            intent = new Intent(this, OnboardingActivity.class);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
