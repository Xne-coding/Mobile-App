package com.questlearn;

/*
 * ProfileFragment — user's profile screen.
 *
 * Displays name, chosen emoji avatar, XP progress, stats, badges (from completed
 * challenges), and achievements. Account actions are in Settings. Avatar picker
 * saves to prefs + Firestore.
 */

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.questlearn.db.FirebaseRepository;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String KEY_RESTORE_BOTTOM_NAV = "restore_bottom_nav";

    private FirebaseRepository repo;

    private String appliedThemeMode;
    private int appliedFontPercent;

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Activity a = getActivity();
                if (a == null) return;
                SharedPreferences p = DisplayPreferences.prefs(a);
                String nowTheme = p.getString(DisplayPreferences.KEY_THEME_MODE,
                        DisplayPreferences.THEME_LIGHT);
                int nowFont = DisplayPreferences.getFontScalePercent(a);
                if (nowTheme.equals(appliedThemeMode) && nowFont == appliedFontPercent) {
                    return;
                }
                appliedThemeMode = nowTheme;
                appliedFontPercent = nowFont;
                BottomNavigationView nav = a.findViewById(R.id.bottomNav);
                if (nav != null) {
                    p.edit().putInt(KEY_RESTORE_BOTTOM_NAV, nav.getSelectedItemId()).apply();
                }
                a.recreate();
            });

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
        repo = new FirebaseRepository();

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("questlearn_prefs", 0);
        appliedThemeMode = prefs.getString(DisplayPreferences.KEY_THEME_MODE,
                DisplayPreferences.THEME_LIGHT);
        appliedFontPercent = DisplayPreferences.getFontScalePercent(requireContext());
        String userName = prefs.getString("user_name", "NTU Student");
        String initials = prefs.getString("user_initials", "U");

        TextView tvName = view.findViewById(R.id.tvProfileName);
        if (tvName != null) tvName.setText(userName);

        TextView tvInitials = view.findViewById(R.id.tvAvatarInitials);
        if (tvInitials != null) tvInitials.setText(initials);

        loadAndApplyAvatar(view, prefs);

        View avatarContainer = view.findViewById(R.id.profileAvatarContainer);
        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v -> showAvatarPicker(view, prefs));
        }

        View btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                settingsLauncher.launch(new Intent(requireContext(), SettingsActivity.class));
                UiTransitions.openForward(requireActivity());
            });
        }

        RecyclerView rvBadges = view.findViewById(R.id.rvBadges);
        if (rvBadges != null) {
            rvBadges.setLayoutManager(new GridLayoutManager(requireContext(), 4));
            rvBadges.setAdapter(new BadgeAdapter(new ArrayList<>()));
            rvBadges.setNestedScrollingEnabled(false);
        }

        RecyclerView rvAchievements = view.findViewById(R.id.rvAchievements);
        if (rvAchievements != null) {
            rvAchievements.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvAchievements.setAdapter(new AchievementAdapter(new ArrayList<>()));
            rvAchievements.setNestedScrollingEnabled(false);
        }
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

                repo.getCompletedChallengeIds(new FirebaseRepository.Callback<List<String>>() {
                    @Override
                    public void onSuccess(List<String> completedIds) {
                        if (getView() == null) return;
                        int done = completedIds.size();
                        int totalXp = (progress.level * progress.xpTarget) + progress.xp;

                        boolean isGuest = repo.isGuest();
                        if (!isGuest) {
                            repo.syncUserXp(totalXp);
                        }

                        loadRankAndUpdateUI(v, progress, completedIds, done, totalXp);
                    }

                    @Override
                    public void onError(String msg) {}
                });
            }

            @Override
            public void onError(String msg) {}
        });
    }

    /** After I know the progress + completed challenges, fetch leaderboard for rank and paint UI. */
    private void loadRankAndUpdateUI(View view, FirebaseRepository.ProgressData progress,
                                     List<String> completedIds, int done, int totalXp) {
        bindRecentAchievements(view);

        repo.getLeaderboard(new FirebaseRepository.Callback<List<FirebaseRepository.LeaderboardRow>>() {
            @Override
            public void onSuccess(List<FirebaseRepository.LeaderboardRow> rows) {
                if (getView() == null) return;

                int rank = 0;
                for (int i = 0; i < rows.size(); i++) {
                    if (rows.get(i).isCurrentUser) { rank = i + 1; break; }
                }

                setProfileStat(view, R.id.statXp, String.valueOf(totalXp), "XP");
                setProfileStat(view, R.id.statDone, String.valueOf(done), "DONE");
                setProfileStat(view, R.id.statBadges, String.valueOf(progress.badges), "BADGES");
                setProfileStat(view, R.id.statRank, rank > 0 ? "#" + rank : "-", "RANK");

                ProgressBar xpBar = view.findViewById(R.id.xpProgressBar);
                if (xpBar != null) {
                    int pct = progress.xpTarget > 0 ? (progress.xp * 100) / progress.xpTarget : 0;
                    xpBar.setProgress(Math.min(pct, 100));
                }

                TextView tvLevelChip = view.findViewById(R.id.tvLevelChip);
                if (tvLevelChip != null)
                    tvLevelChip.setText("\u26A1 Level " + progress.level + " \u00B7 Beginner");

                TextView tvCurrentXp = view.findViewById(R.id.tvCurrentXp);
                if (tvCurrentXp != null) tvCurrentXp.setText(progress.xp + " XP");

                TextView tvTargetXp = view.findViewById(R.id.tvTargetXp);
                if (tvTargetXp != null) tvTargetXp.setText(progress.xpTarget + " XP");

                RecyclerView rvBadges = view.findViewById(R.id.rvBadges);
                if (rvBadges != null) rvBadges.setAdapter(new BadgeAdapter(buildBadges(completedIds)));
            }

            @Override
            public void onError(String msg) {}
        });
    }

    /** One badge per challenge id; greyed out until that challenge is in completedIds. */
    private List<Badge> buildBadges(List<String> completedIds) {
        String[][] badgeDefs = {
                {"ch1", "Library Explorer", "\uD83D\uDCDA", "0xFFE3F2FD"},
                {"ch2", "Scientist",        "\u2697\uFE0F", "0xFFC8E6C9"},
                {"ch3", "Sports Star",      "\uD83C\uDFDF\uFE0F", "0xFFFFE0B2"},
                {"ch4", "Social Bee",       "\uD83C\uDF89", "0xFFE8EAF6"},
                {"ch5", "Code Breaker",     "\uD83D\uDCBB", "0xFFE3F2FD"},
                {"ch6", "Scholar",          "\uD83C\uDF93", "0xFFFCE4EC"},
                {"ch7", "Learner",          "\uD83D\uDCD6", "0xFFC8E6C9"},
                {"ch8", "Lab Rat",          "\uD83D\uDD2C", "0xFFE0F7FA"},
                {"ch9", "Writer",           "\u270D\uFE0F", "0xFFEDE7F6"},
                {"ch10", "Arts Fan",        "\uD83D\uDCDD", "0xFFFFF3E0"},
                {"ch11", "Studious",        "\uD83D\uDCCA", "0xFFE3F2FD"},
                {"ch12", "Healer",          "\uD83E\uDDEC", "0xFFFCE4EC"},
                {"ch13", "Researcher",      "\uD83C\uDFE5", "0xFFC8E6C9"},
                {"ch14", "Techie",          "\u2699\uFE0F", "0xFFE0F7FA"},
                {"ch15", "Hall Runner",     "\uD83C\uDFE0", "0xFFFFE0B2"},
                {"ch16", "Socialite",       "\u2615", "0xFFEDE7F6"},
                {"ch17", "Cricketer",       "\uD83C\uDFCF", "0xFFC8E6C9"},
                {"ch18", "Engineer",        "\uD83D\uDD27", "0xFFE3F2FD"},
                {"ch19", "Innovator",       "\uD83E\uDD16", "0xFFE0F7FA"},
                {"ch20", "Foodie",          "\uD83C\uDF7D\uFE0F", "0xFFFFE0B2"},
                {"ch21", "Chemist",         "\uD83E\uDDEA", "0xFFFCE4EC"},
        };
        List<Badge> badges = new ArrayList<>();
        for (String[] def : badgeDefs) {
            boolean earned = completedIds.contains(def[0]);
            long color = Long.decode(def[3]);
            badges.add(new Badge(def[1], def[2], earned ? (int) color : 0xFFEEEEEE, earned));
        }
        return badges;
    }

    /** Fills “Recent achievements” from Firestore {@code scanHistory} (real QR / code redemptions). */
    private void bindRecentAchievements(View view) {
        repo.getRecentScanHistory(20, new FirebaseRepository.Callback<List<FirebaseRepository.RecentScanEntry>>() {
            @Override
            public void onSuccess(List<FirebaseRepository.RecentScanEntry> entries) {
                if (getView() == null) return;
                List<Achievement> achievements = mapRecentScansToAchievements(entries);
                RecyclerView rv = view.findViewById(R.id.rvAchievements);
                TextView tvEmpty = view.findViewById(R.id.tvRecentAchievementsEmpty);
                if (rv != null) {
                    rv.setAdapter(new AchievementAdapter(achievements));
                }
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(achievements.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onError(String msg) {}
        });
    }

    private static List<Achievement> mapRecentScansToAchievements(
            List<FirebaseRepository.RecentScanEntry> entries) {
        List<Achievement> list = new ArrayList<>();
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
        for (FirebaseRepository.RecentScanEntry e : entries) {
            Challenge ch = Challenge.findById(e.challengeId);
            String name = ch != null ? ch.getName() : "Challenge completed";
            StringBuilder desc = new StringBuilder();
            if (e.scannedAt > 0) {
                desc.append(df.format(new Date(e.scannedAt)));
            }
            if (!e.code.isEmpty()) {
                if (desc.length() > 0) desc.append(" · ");
                desc.append(e.code);
            }
            String icon = ch != null ? ch.getIcon() : "✅";
            int bg = ch != null ? accentToAchievementBg(ch.getAccentColor()) : 0xFFE3F2FD;
            list.add(new Achievement(name, desc.toString(), icon, e.xpReward, bg, true));
        }
        return list;
    }

    private static int accentToAchievementBg(Challenge.AccentColor ac) {
        if (ac == Challenge.AccentColor.GREEN) return 0xFFC8E6C9;
        if (ac == Challenge.AccentColor.ORANGE) return 0xFFFFE0B2;
        return 0xFFE3F2FD;
    }

    private void setProfileStat(View root, int viewId, String value, String label) {
        View statView = root.findViewById(viewId);
        if (statView == null) return;
        TextView tvVal = statView.findViewById(R.id.tvStatValue);
        TextView tvLbl = statView.findViewById(R.id.tvStatLabel);
        if (tvVal != null) tvVal.setText(value);
        if (tvLbl != null) tvLbl.setText(label);
    }

    private void loadAndApplyAvatar(View view, SharedPreferences prefs) {
        String cached = prefs.getString("avatar_id", null);
        if (cached != null) applyAvatarToProfile(view, cached);

        repo.getAvatarId(new FirebaseRepository.Callback<String>() {
            @Override
            public void onSuccess(String avatarId) {
                if (getView() == null || avatarId == null) return;
                prefs.edit().putString("avatar_id", avatarId).apply();
                applyAvatarToProfile(view, avatarId);
            }
            @Override
            public void onError(String msg) {}
        });
    }

    private void applyAvatarToProfile(View view, String avatarId) {
        AvatarOption opt = AvatarOption.findById(avatarId);
        if (opt == null) return;
        TextView tvInitials = view.findViewById(R.id.tvAvatarInitials);
        View circle = view.findViewById(R.id.profileAvatarCircle);
        if (tvInitials != null) {
            tvInitials.setText(opt.getEmoji());
            tvInitials.setTextSize(32);
        }
        if (circle != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(opt.getBackgroundColor());
            bg.setStroke(3, 0x80FFFFFF);
            circle.setBackground(bg);
        }
    }

    /** Simple dialog with a grid; saving updates prefs and Firestore for real accounts. */
    private void showAvatarPicker(View view, SharedPreferences prefs) {
        String currentId = prefs.getString("avatar_id", null);

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_avatar_picker, null);
        RecyclerView rv = dialogView.findViewById(R.id.rvAvatars);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 4));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        rv.setAdapter(new AvatarPickerAdapter(AvatarOption.getAll(), currentId, option -> {
            prefs.edit().putString("avatar_id", option.getId()).apply();
            applyAvatarToProfile(view, option.getId());
            if (!repo.isGuest()) {
                repo.saveAvatarId(option.getId(), null);
            }
            dialog.dismiss();
        }));

        dialog.show();
        SystemBarInsets.applyToDialog(dialog);
    }
}
