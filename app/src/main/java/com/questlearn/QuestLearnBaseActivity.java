package com.questlearn;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * Applies optional text scaling (0–100% in Settings) via {@link DisplayPreferences#wrapContextForFontScale(Context)}.
 * Theme (light/dark) is handled globally by {@link QuestLearnApp} and {@link AppCompatDelegate}.
 * When read-aloud is enabled in Settings, long-press {@link android.widget.TextView}s to speak their text.
 */
public abstract class QuestLearnBaseActivity extends AppCompatActivity {

    private final FragmentManager.FragmentLifecycleCallbacks readAloudFragmentCallbacks =
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                        @NonNull View view, @Nullable Bundle savedInstanceState) {
                    scheduleReadAloudAttach(view);
                }
            };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(DisplayPreferences.wrapContextForFontScale(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(readAloudFragmentCallbacks, true);
    }

    @Override
    protected void onDestroy() {
        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(readAloudFragmentCallbacks);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        scheduleReadAloudAttach(findViewById(android.R.id.content));
    }

    @Override
    protected void onPause() {
        ReadAloud.getInstance().stop();
        super.onPause();
    }

    private void scheduleReadAloudAttach(@Nullable View root) {
        if (root == null) {
            return;
        }
        root.post(() -> {
            if (isFinishing()) {
                return;
            }
            View decor = getWindow() != null ? getWindow().getDecorView() : null;
            if (decor != null) {
                ReadAloud.attachToHierarchy(this, decor);
            } else {
                ReadAloud.attachToHierarchy(this, root);
            }
        });
    }
}
