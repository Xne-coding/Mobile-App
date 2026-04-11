package com.questlearn;

/*
 * MainActivity — shell for the bottom navigation and four main tabs (Home, Map,
 * Rewards, Profile). The middle "Scan" item does not switch fragments; it launches
 * ScanActivity instead. Can open a specific tab when another screen passes open_tab.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends QuestLearnBaseActivity {

    private NavController navController;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SystemBarInsets.applyToRoot(this, R.id.mainContentRoot);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        bottomNav = findViewById(R.id.bottomNav);

        // When user taps a tab, either navigate inside the nav graph or handle Scan specially.
        bottomNav.setOnItemSelectedListener(item -> {
            return handleBottomNavSelection(item.getItemId());
        });

        SharedPreferences p = getSharedPreferences(DisplayPreferences.PREFS_NAME, MODE_PRIVATE);
        int restoreTab = p.getInt("restore_bottom_nav", 0);
        if (restoreTab != 0 && bottomNav != null) {
            bottomNav.setSelectedItemId(restoreTab);
            p.edit().remove("restore_bottom_nav").apply();
        }

        // Deep link from elsewhere overrides restored tab when present.
        applyRequestedTab(getIntent());
    }

    /**
     * Home / Map / Rewards / Profile use Navigation; Scan opens a separate Activity.
     * Returning false for Scan tells BottomNavigationView to leave the highlight as-is.
     */
    private boolean handleBottomNavSelection(int id) {
        if (id == R.id.nav_scan) {
            try {
                startActivity(new Intent(this, ScanActivity.class));
                UiTransitions.openForward(this);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open scanner.", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        if (navController == null) {
            return false;
        }
        try {
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != id) {
                navController.navigate(id);
            }
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Unable to switch tab.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp()
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyRequestedTab(intent);
    }

    /** If Intent carries open_tab, select that destination once MainActivity is shown. */
    private void applyRequestedTab(Intent intent) {
        if (intent == null || !intent.hasExtra("open_tab")) return;
        int tabId = intent.getIntExtra("open_tab", R.id.homeFragment);
        if (tabId == R.id.homeFragment || tabId == R.id.mapFragment
                || tabId == R.id.rewardsFragment || tabId == R.id.profileFragment) {
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(tabId);
            } else {
                try {
                    handleBottomNavSelection(tabId);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open requested tab.", Toast.LENGTH_SHORT).show();
                }
            }
        }
        intent.removeExtra("open_tab");
    }
}

