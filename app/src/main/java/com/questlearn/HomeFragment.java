package com.questlearn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.questlearn.db.ProgressDbHelper;
import java.util.ArrayList;

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
        ProgressDbHelper progressDbHelper = new ProgressDbHelper(requireContext());
        ProgressDbHelper.ProgressData progress = progressDbHelper.getProgress();
        String userName = prefs.getString("user_name", "NTU Student");

        // User name
        TextView tvName = view.findViewById(R.id.tvUserName);
        tvName.setText(userName);

        // Avatar initials
        TextView tvInitials = view.findViewById(R.id.tvAvatarInitials);
        String initials = prefs.getString("user_initials", "U");
        tvInitials.setText(initials);

        // Avatar tap → profile
        View avatarContainer = view.findViewById(R.id.avatarContainer);
        avatarContainer.setOnClickListener(v -> {
            try {
                Navigation.findNavController(view).navigate(R.id.profileFragment);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Unable to open profile.", Toast.LENGTH_SHORT).show();
            }
        });

        // Stat cards
        setupStatCards(view, progress);

        // See map link
        TextView tvSeeMap = view.findViewById(R.id.tvSeeMap);
        tvSeeMap.setOnClickListener(v -> {
            try {
                Navigation.findNavController(view).navigate(R.id.mapFragment);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Unable to open map.", Toast.LENGTH_SHORT).show();
            }
        });

        // Challenges RecyclerView
        RecyclerView rvChallenges = view.findViewById(R.id.rvChallenges);
        rvChallenges.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChallenges.setAdapter(new ChallengeAdapter(new ArrayList<>()));
        rvChallenges.setNestedScrollingEnabled(false);

        // Leaderboard RecyclerView
        RecyclerView rvLeaderboard = view.findViewById(R.id.rvLeaderboard);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLeaderboard.setAdapter(new LeaderboardAdapter(new ArrayList<>()));
        rvLeaderboard.setNestedScrollingEnabled(false);

        // XP progress bar (0–100 scale)
        ProgressBar xpBar = view.findViewById(R.id.xpProgressBar);
        int pct = progress.xpTarget > 0 ? (progress.xp * 100) / progress.xpTarget : 0;
        xpBar.setProgress(Math.min(pct, 100));

        TextView tvLevelLabel = view.findViewById(R.id.tvLevelLabel);
        if (tvLevelLabel != null) {
            tvLevelLabel.setText("Level " + progress.level + " · Beginner");
        }

        TextView tvXpSummary = view.findViewById(R.id.tvXpSummary);
        if (tvXpSummary != null) {
            tvXpSummary.setText(progress.xp + " / " + progress.xpTarget + " XP");
        }
    }

    private void setupStatCards(View view, ProgressDbHelper.ProgressData progress) {
        // XP stat
        View statXp = view.findViewById(R.id.statXp);
        if (statXp != null) {
            TextView val = statXp.findViewById(R.id.tvStatValue);
            TextView lbl = statXp.findViewById(R.id.tvStatLabel);
            if (val != null) val.setText(String.valueOf(progress.xp));
            if (lbl != null) lbl.setText("XP POINTS");
        }

        // Challenges stat
        View statCh = view.findViewById(R.id.statChallenges);
        if (statCh != null) {
            TextView val = statCh.findViewById(R.id.tvStatValue);
            TextView lbl = statCh.findViewById(R.id.tvStatLabel);
            if (val != null) val.setText(String.valueOf(progress.challengesDone));
            if (lbl != null) lbl.setText("CHALLENGES");
        }

        // Rank stat
        View statRank = view.findViewById(R.id.statRank);
        if (statRank != null) {
            TextView val = statRank.findViewById(R.id.tvStatValue);
            TextView lbl = statRank.findViewById(R.id.tvStatLabel);
            if (val != null) val.setText("#" + progress.rankValue);
            if (lbl != null) lbl.setText("RANK");
        }

        // Streak stat
        View statStreak = view.findViewById(R.id.statStreak);
        if (statStreak != null) {
            TextView val = statStreak.findViewById(R.id.tvStatValue);
            TextView lbl = statStreak.findViewById(R.id.tvStatLabel);
            if (val != null) val.setText(String.valueOf(progress.streak));
            if (lbl != null) lbl.setText("STREAK");
        }
    }
}
