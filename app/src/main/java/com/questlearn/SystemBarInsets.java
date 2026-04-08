package com.questlearn;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Pads an activity's root so content clears status bar, display cutout, and navigation bar
 * on edge-to-edge devices (e.g. Pixel 9, Android 15+).
 */
public final class SystemBarInsets {

    private SystemBarInsets() {}

    public static void applyToRoot(Activity activity, int rootViewId) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        View root = activity.findViewById(rootViewId);
        if (root == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });
    }

    /** Edge-to-edge padding for alert dialogs (status bar / cutout / nav bar). */
    public static void applyToDialog(Dialog dialog) {
        if (dialog == null || dialog.getWindow() == null) {
            return;
        }
        WindowCompat.setDecorFitsSystemWindows(dialog.getWindow(), false);
        View decor = dialog.getWindow().getDecorView();
        ViewCompat.setOnApplyWindowInsetsListener(decor, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            decor.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(decor);
    }
}
