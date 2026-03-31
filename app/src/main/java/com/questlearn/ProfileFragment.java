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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.questlearn.db.ProgressDbHelper;
import java.util.ArrayList;

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
        ProgressDbHelper progressDbHelper = new ProgressDbHelper(requireContext());
        ProgressDbHelper.ProgressData progress = progressDbHelper.getProgress();
        String userName = prefs.getString("user_name", "NTU Student");
        String initials = prefs.getString("user_initials", "U");

        // Hero section
        TextView tvName = view.findViewById(R.id.tvProfileName);
        if (tvName != null) tvName.setText(userName);

        TextView tvInitials = view.findViewById(R.id.tvAvatarInitials);
        if (tvInitials != null) tvInitials.setText(initials);

        View btnLogout = view.findViewById(R.id.btnLogout);
        View btnLogoutSecondary = view.findViewById(R.id.btnLogoutSecondary);
        View.OnClickListener logoutClick = v -> {
            try {
                if (btnLogout != null) btnLogout.setEnabled(false);
                if (btnLogoutSecondary != null) btnLogoutSecondary.setEnabled(false);
                prefs.edit()
                        .putBoolean("user_logged_in", false)
                        .remove("remember_me")
                        .remove("user_name")
                        .remove("user_initials")
                        .remove("user_email")
                        .remove("is_guest")
                        .apply();
                progressDbHelper.resetProgress();

                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(requireActivity(), SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                requireActivity().finish();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Unable to log out.", Toast.LENGTH_SHORT).show();
                if (btnLogout != null) btnLogout.setEnabled(true);
                if (btnLogoutSecondary != null) btnLogoutSecondary.setEnabled(true);
            }
        };
        if (btnLogout != null) btnLogout.setOnClickListener(logoutClick);
        if (btnLogoutSecondary != null) btnLogoutSecondary.setOnClickListener(logoutClick);

        View btnChangePassword = view.findViewById(R.id.btnChangePassword);
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(requireActivity(), ChangePasswordActivity.class);
                    startActivity(intent);
                    requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Unable to open password settings.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Profile stats
        setProfileStat(view, R.id.statXp, String.valueOf(progress.xp), "XP");
        setProfileStat(view, R.id.statDone, String.valueOf(progress.challengesDone), "DONE");
        setProfileStat(view, R.id.statBadges, String.valueOf(progress.badges), "BADGES");
        setProfileStat(view, R.id.statRank, "#" + progress.rankValue, "RANK");

        // XP progress bar (0–100 scale)
        ProgressBar xpBar = view.findViewById(R.id.xpProgressBar);
        if (xpBar != null) {
            int pct = progress.xpTarget > 0 ? (progress.xp * 100) / progress.xpTarget : 0;
            xpBar.setProgress(Math.min(pct, 100));
        }

        TextView tvLevelChip = view.findViewById(R.id.tvLevelChip);
        if (tvLevelChip != null) {
            tvLevelChip.setText("⚡ Level " + progress.level + " · Beginner");
        }

        TextView tvCurrentXp = view.findViewById(R.id.tvCurrentXp);
        if (tvCurrentXp != null) {
            tvCurrentXp.setText(progress.xp + " XP");
        }

        TextView tvTargetXp = view.findViewById(R.id.tvTargetXp);
        if (tvTargetXp != null) {
            tvTargetXp.setText(progress.xpTarget + " XP");
        }

        // Badges grid (4 columns)
        RecyclerView rvBadges = view.findViewById(R.id.rvBadges);
        if (rvBadges != null) {
            rvBadges.setLayoutManager(new GridLayoutManager(requireContext(), 4));
            rvBadges.setAdapter(new BadgeAdapter(new ArrayList<>()));
            rvBadges.setNestedScrollingEnabled(false);
        }

        // Achievements list
        RecyclerView rvAchievements = view.findViewById(R.id.rvAchievements);
        if (rvAchievements != null) {
            rvAchievements.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvAchievements.setAdapter(new AchievementAdapter(new ArrayList<>()));
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

}
