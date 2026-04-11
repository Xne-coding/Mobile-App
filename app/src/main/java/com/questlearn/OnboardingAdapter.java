package com.questlearn;

/*
 * OnboardingAdapter — ViewPager2 glue: one OnboardingSlideFragment per intro page,
 * with title, body text, emoji, and background tint baked in as arguments.
 */

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OnboardingAdapter extends FragmentStateAdapter {

    private static final String[] TITLES = {
            "Explore Your Campus",
            "Complete Challenges",
            "Compete with Friends"
    };

    private static final String[] DESCRIPTIONS = {
            "Discover hidden gems and iconic landmarks around your university. Navigate with real-time GPS and interactive maps.",
            "Scan QR codes, check in at beacons, and complete fun learning missions to earn XP and unlock achievements.",
            "Climb the leaderboard, show off your badges, and race friends to discover the whole campus first."
    };

    private static final String[] EMOJIS = {"🏛️", "⚡", "🏆"};

    // Background colors for each slide (R.color references represented as ints)
    private static final int[] BG_COLORS = {
            0xFFE3F2FD,  // blue_surface
            0xFFC8E6C9,  // green_light
            0xFFFFE0B2   // orange_light
    };

    public OnboardingAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return OnboardingSlideFragment.newInstance(
                TITLES[position],
                DESCRIPTIONS[position],
                EMOJIS[position],
                BG_COLORS[position]
        );
    }

    @Override
    public int getItemCount() {
        return TITLES.length;
    }
}
