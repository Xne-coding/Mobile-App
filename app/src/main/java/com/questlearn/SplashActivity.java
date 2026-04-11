package com.questlearn;

/*
 * SplashActivity — first screen: short branded animation, then route the user to
 * MainActivity if already signed in, Login if onboarding was finished, or the
 * onboarding carousel if they're brand new.
 */

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends QuestLearnBaseActivity {

    private static final long SPLASH_DURATION = 2800L;
    private static final String PREFS_NAME = "questlearn_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_complete";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View content = findViewById(R.id.splash_content);
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        if (DisplayPreferences.isReduceMotion(this)) {
            content.setAlpha(1f);
            content.setTranslationY(0f);
        } else {
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
        }

        if (!DisplayPreferences.isReduceMotion(this)) {
            animateDots(dot1, dot2, dot3);
        }

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

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Intent intent;
        if (currentUser != null) {
            intent = new Intent(this, MainActivity.class);
        } else if (onboardingDone) {
            intent = new Intent(this, LoginActivity.class);
        } else {
            intent = new Intent(this, OnboardingActivity.class);
        }

        startActivity(intent);
        UiTransitions.splashCrossFade(this);
        finish();
    }
}
