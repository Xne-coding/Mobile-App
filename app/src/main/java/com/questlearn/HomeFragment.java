package com.questlearn;

import android.content.Intent;
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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load user prefs
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("questlearn_prefs", 0);
        String userName = prefs.getString("user_name", "Explorer");

        // User name
        TextView tvName = view.findViewById(R.id.tvUserName);
        tvName.setText(userName);

        // Avatar initials
        TextView tvInitials = view.findViewById(R.id.tvAvatarInitials);
        String initials = prefs.getString("user_initials", "U");
        tvInitials.setText(initials);

        // Avatar tap → profile
        View avatarContainer = view.findViewById(R.id.avatarContainer);
        avatarContainer.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.profileFragment));

        // Stat cards
        setupStatCards(view);

        // See map link
        TextView tvSeeMap = view.findViewById(R.id.tvSeeMap);
        tvSeeMap.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.mapFragment));

        // Challenges RecyclerView
        RecyclerView rvChallenges = view.findViewById(R.id.rvChallenges);
        rvChallenges.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChallenges.setAdapter(new ChallengeAdapter(Challenge.getSampleData()));
        rvChallenges.setNestedScrollingEnabled(false);

        // Leaderboard RecyclerView
        RecyclerView rvLeaderboard = view.findViewById(R.id.rvLeaderboard);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLeaderboard.setAdapter(new LeaderboardAdapter(LeaderboardEntry.getSampleData()));
        rvLeaderboard.setNestedScrollingEnabled(false);

        // Animate XP bar
        ProgressBar xpBar = view.findViewById(R.id.xpProgressBar);
        xpBar.setProgress(0);
        xpBar.postDelayed(() -> animateProgress(xpBar, 83), 400);
    }

    private void setupStatCards(View view) {
        // XP stat
        View statXp = view.findViewById(R.id.statXp);
        if (statXp != null) {
            TextView val = statXp.findViewById(R.id.tvStatValue);
            TextView lbl = statXp.findViewById(R.id.tvStatLabel);
            if (val != null) val.setText("1,240");
            if (lbl != null) lbl.setText("XP POINTS");
        }

        // Challenges stat
        View statCh = view.findViewById(R.id.statChallenges);
        if (statCh != null) {
            TextView val = statCh.findViewById(R.id.tvStatValue);
            TextView lbl = statCh.findViewById(R.id.tvStatLabel);
            if (val != null) val.setText("18");
            if (lbl != null) lbl.setText("CHALLENGES");
        }

        // Rank stat
        View statRank = view.findViewById(R.id.statRank);
        if (statRank != null) {
            TextView val = statRank.findViewById(R.id.tvStatValue);
            TextView lbl = statRank.findViewById(R.id.tvStatLabel);
            if (val != null) val.setText("#4");
            if (lbl != null) lbl.setText("RANK");
        }

        // Streak stat
        View statStreak = view.findViewById(R.id.statStreak);
        if (statStreak != null) {
            TextView val = statStreak.findViewById(R.id.tvStatValue);
            TextView lbl = statStreak.findViewById(R.id.tvStatLabel);
            if (val != null) val.setText("7🔥");
            if (lbl != null) lbl.setText("STREAK");
        }
    }

    private void animateProgress(ProgressBar bar, int targetProgress) {
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator
                .ofInt(bar, "progress", 0, targetProgress);
        animator.setDuration(800);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.start();
    }
}
