package com.questlearn;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("questlearn_prefs", 0);
        String userName = prefs.getString("user_name", "Don Jacques Maseengo");
        String initials = prefs.getString("user_initials", "DM");

        // Hero section
        TextView tvName = view.findViewById(R.id.tvProfileName);
        if (tvName != null) tvName.setText(userName);

        TextView tvInitials = view.findViewById(R.id.tvAvatarInitials);
        if (tvInitials != null) tvInitials.setText(initials);

        // Profile stats
        setProfileStat(view, R.id.statXp,     "1,240", "XP");
        setProfileStat(view, R.id.statDone,   "18",    "DONE");
        setProfileStat(view, R.id.statBadges, "9",     "BADGES");
        setProfileStat(view, R.id.statRank,   "#4",    "RANK");

        // XP progress bar
        ProgressBar xpBar = view.findViewById(R.id.xpProgressBar);
        if (xpBar != null) {
            xpBar.setProgress(0);
            xpBar.postDelayed(() -> animateProgress(xpBar, 83), 300);
        }

        // Badges grid (4 columns)
        RecyclerView rvBadges = view.findViewById(R.id.rvBadges);
        if (rvBadges != null) {
            rvBadges.setLayoutManager(new GridLayoutManager(requireContext(), 4));
            rvBadges.setAdapter(new BadgeAdapter(Badge.getSampleData()));
            rvBadges.setNestedScrollingEnabled(false);
        }

        // Achievements list
        RecyclerView rvAchievements = view.findViewById(R.id.rvAchievements);
        if (rvAchievements != null) {
            rvAchievements.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvAchievements.setAdapter(new AchievementAdapter(Achievement.getSampleData()));
            rvAchievements.setNestedScrollingEnabled(false);
        }
    }

    private void setProfileStat(View root, int viewId, String value, String label) {
        View statView = root.findViewById(viewId);
        if (statView == null) return;
        TextView tvVal = statView.findViewById(R.id.tvStatValue);
        TextView tvLbl = statView.findViewById(R.id.tvStatLabel);
        if (tvVal != null) tvVal.setText(value);
        if (tvLbl != null) tvLbl.setText(label);
    }

    private void animateProgress(ProgressBar bar, int target) {
        android.animation.ObjectAnimator anim = android.animation.ObjectAnimator
                .ofInt(bar, "progress", 0, target);
        anim.setDuration(900);
        anim.setInterpolator(new android.view.animation.DecelerateInterpolator());
        anim.start();
    }
}
