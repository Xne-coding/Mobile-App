package com.questlearn;

/*
 * HomeFragment — main dashboard after login.
 *
 * Shows greeting, avatar shortcut to profile, XP bar, stat chips, the challenge list,
 * and a leaderboard snippet. On each resume it pulls fresh numbers from Firebase and
 * syncs the user's total XP to Firestore so the leaderboard stays up to date.
 */

import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
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
import com.questlearn.db.FirebaseRepository;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FirebaseRepository repo;

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
        repo = new FirebaseRepository();

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("questlearn_prefs", 0);
        String userName = prefs.getString("user_name", "NTU Student");
        String initials = prefs.getString("user_initials", "U");

        TextView tvName = view.findViewById(R.id.tvUserName);
        tvName.setText(userName);

        TextView tvInitials = view.findViewById(R.id.tvAvatarInitials);
        tvInitials.setText(initials);

        String avatarId = prefs.getString("avatar_id", null);
        if (avatarId != null) {
            AvatarOption opt = AvatarOption.findById(avatarId);
            if (opt != null) {
                tvInitials.setText(opt.getEmoji());
                tvInitials.setTextSize(18);
                View avatarBg = view.findViewById(R.id.avatarContainer);
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(opt.getBackgroundColor());
                avatarBg.setBackground(bg);
            }
        }

        View avatarContainer = view.findViewById(R.id.avatarContainer);
        avatarContainer.setOnClickListener(v -> {
            try {
                Navigation.findNavController(view).navigate(R.id.profileFragment);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Unable to open profile.", Toast.LENGTH_SHORT).show();
            }
        });

        TextView tvSeeMap = view.findViewById(R.id.tvSeeMap);
        tvSeeMap.setOnClickListener(v -> {
            try {
                Navigation.findNavController(view).navigate(R.id.mapFragment);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Unable to open map.", Toast.LENGTH_SHORT).show();
            }
        });

        RecyclerView rvChallenges = view.findViewById(R.id.rvChallenges);
        rvChallenges.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChallenges.setAdapter(new ChallengeAdapter(new ArrayList<>()));
        rvChallenges.setNestedScrollingEnabled(false);

        RecyclerView rvLeaderboard = view.findViewById(R.id.rvLeaderboard);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLeaderboard.setAdapter(new LeaderboardAdapter(new ArrayList<>()));
        rvLeaderboard.setNestedScrollingEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view == null || repo == null) return;

        repo.getProgress(new FirebaseRepository.Callback<FirebaseRepository.ProgressData>() {
            @Override
            public void onSuccess(FirebaseRepository.ProgressData progress) {
                if (getView() == null) return;
                View v = getView();

                setupStatCards(v, progress);

                ProgressBar xpBar = v.findViewById(R.id.xpProgressBar);
                int pct = progress.xpTarget > 0 ? (progress.xp * 100) / progress.xpTarget : 0;
                xpBar.setProgress(Math.min(pct, 100));

                TextView tvLevelLabel = v.findViewById(R.id.tvLevelLabel);
                if (tvLevelLabel != null)
                    tvLevelLabel.setText("Level " + progress.level + " \u00B7 Beginner");

                TextView tvXpSummary = v.findViewById(R.id.tvXpSummary);
                if (tvXpSummary != null)
                    tvXpSummary.setText(progress.xp + " / " + progress.xpTarget + " XP");

                int totalXp = (progress.level * progress.xpTarget) + progress.xp;
                boolean isGuest = repo.isGuest();
                if (!isGuest) {
                    repo.syncUserXp(totalXp);
                }

                loadLeaderboard(v, progress);
            }

            @Override
            public void onError(String message) {}
        });
    }

    /** Turn Firestore rows into LeaderboardEntry list and refresh the rank stat chip. */
    private void loadLeaderboard(View view, FirebaseRepository.ProgressData progress) {
        repo.getLeaderboard(new FirebaseRepository.Callback<List<FirebaseRepository.LeaderboardRow>>() {
            @Override
            public void onSuccess(List<FirebaseRepository.LeaderboardRow> rows) {
                if (getView() == null) return;
                RecyclerView rvLeaderboard = view.findViewById(R.id.rvLeaderboard);
                if (rvLeaderboard == null) return;

                List<LeaderboardEntry> entries = new ArrayList<>();
                int userRank = 0;
                for (int i = 0; i < rows.size(); i++) {
                    FirebaseRepository.LeaderboardRow row = rows.get(i);
                    int rank = i + 1;
                    if (row.isCurrentUser) userRank = rank;
                    entries.add(new LeaderboardEntry(
                            rank, row.displayName, row.initials, row.avatarId,
                            row.totalXp,
                            row.isCurrentUser ? 0xFF2196F3 : 0xFF9E9E9E,
                            row.isCurrentUser));
                }
                rvLeaderboard.setAdapter(new LeaderboardAdapter(entries));

                if (userRank > 0) {
                    progress.rankValue = userRank;
                    View statRank = view.findViewById(R.id.statRank);
                    if (statRank != null) {
                        TextView val = statRank.findViewById(R.id.tvStatValue);
                        if (val != null) val.setText("#" + userRank);
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Leaderboard: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /** Fill the four header stat includes (XP, challenges, rank, streak). */
    private void setupStatCards(View view, FirebaseRepository.ProgressData progress) {
        View statXp = view.findViewById(R.id.statXp);
        if (statXp != null) {
            TextView val = statXp.findViewById(R.id.tvStatValue);
            TextView lbl = statXp.findViewById(R.id.tvStatLabel);
            if (val != null) val.setText(String.valueOf(progress.xp));
            if (lbl != null) lbl.setText("XP POINTS");
        }
        View statCh = view.findViewById(R.id.statChallenges);
        if (statCh != null) {
            TextView val = statCh.findViewById(R.id.tvStatValue);
            TextView lbl = statCh.findViewById(R.id.tvStatLabel);
            if (val != null) val.setText(String.valueOf(progress.challengesDone));
            if (lbl != null) lbl.setText("CHALLENGES");
        }
        View statRank = view.findViewById(R.id.statRank);
        if (statRank != null) {
            TextView val = statRank.findViewById(R.id.tvStatValue);
            TextView lbl = statRank.findViewById(R.id.tvStatLabel);
            if (val != null) val.setText("#" + progress.rankValue);
            if (lbl != null) lbl.setText("RANK");
        }
        View statStreak = view.findViewById(R.id.statStreak);
        if (statStreak != null) {
            TextView val = statStreak.findViewById(R.id.tvStatValue);
            TextView lbl = statStreak.findViewById(R.id.tvStatLabel);
            if (val != null) val.setText(String.valueOf(progress.streak));
            if (lbl != null) lbl.setText("STREAK");
        }
    }
}
