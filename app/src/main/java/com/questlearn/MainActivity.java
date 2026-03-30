package com.questlearn;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            return handleBottomNavSelection(item.getItemId());
        });

        applyRequestedTab(getIntent());
    }

    private boolean handleBottomNavSelection(int id) {
        if (id == R.id.nav_scan) {
            try {
                startActivity(new Intent(this, ScanActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open scanner.", Toast.LENGTH_SHORT).show();
            }
            return false; // Keep current selected destination
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

    private void applyRequestedTab(Intent intent) {
        if (intent == null || !intent.hasExtra("open_tab")) return;
        int tabId = intent.getIntExtra("open_tab", R.id.homeFragment);
        if (tabId == R.id.homeFragment || tabId == R.id.mapFragment || tabId == R.id.profileFragment) {
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

