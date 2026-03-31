package com.questlearn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnNext, btnSkip;
    private LinearLayout dotsContainer;
    private int totalSlides = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
        dotsContainer = findViewById(R.id.dotsContainer);

        // Set up adapter
        OnboardingAdapter adapter = new OnboardingAdapter(this);
        viewPager.setAdapter(adapter);

        // Create dots
        setupDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setupDots(position);
                updateButtons(position);
            }
        });

        btnNext.setOnClickListener(v -> {
            try {
                int current = viewPager.getCurrentItem();
                if (current < totalSlides - 1) {
                    viewPager.setCurrentItem(current + 1);
                } else {
                    finishOnboarding();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Unable to continue onboarding.", Toast.LENGTH_SHORT).show();
            }
        });

        btnSkip.setOnClickListener(v -> {
            try {
                finishOnboarding();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to skip onboarding.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDots(int currentIndex) {
        dotsContainer.removeAllViews();
        int size = (int) getResources().getDimension(R.dimen.spacing_sm);
        int margin = (int) getResources().getDimension(R.dimen.spacing_xs);

        for (int i = 0; i < totalSlides; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    i == currentIndex ? (int) getResources().getDimension(R.dimen.spacing_xl) / 3 : size,
                    size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == currentIndex ?
                    R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
            dotsContainer.addView(dot);
        }
    }

    private void updateButtons(int position) {
        if (position == totalSlides - 1) {
            btnNext.setText(getString(R.string.get_started));
            btnSkip.setVisibility(View.INVISIBLE);
        } else {
            btnNext.setText(getString(R.string.next));
            btnSkip.setVisibility(View.VISIBLE);
        }
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences("questlearn_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_complete", true).apply();

        try {
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open login screen.", Toast.LENGTH_SHORT).show();
        }
    }
}

