package com.questlearn;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Locale;

/**
 * Text-to-speech when {@link DisplayPreferences#isReadAloudEnabled} is on.
 * Long-press a {@link TextView} (not password fields) to hear its text.
 */
public final class ReadAloud {

    private static final Object LOCK = new Object();
    private static volatile ReadAloud instance;

    private TextToSpeech tts;
    private volatile boolean ready;

    private ReadAloud() {}

    public static ReadAloud getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ReadAloud();
                }
            }
        }
        return instance;
    }

    /** Warm up the engine at app start so the first speak() is usually ready. */
    public void init(android.content.Context applicationContext) {
        synchronized (LOCK) {
            if (tts != null) {
                return;
            }
            tts = new TextToSpeech(applicationContext.getApplicationContext(), status -> {
                synchronized (LOCK) {
                    ready = (status == TextToSpeech.SUCCESS);
                    if (ready && tts != null) {
                        tts.setLanguage(Locale.getDefault());
                    }
                }
            });
        }
    }

    public void speak(@NonNull Activity activity, @Nullable String text) {
        if (!DisplayPreferences.isReadAloudEnabled(activity)) {
            return;
        }
        if (text == null) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        synchronized (LOCK) {
            if (tts == null) {
                init(activity.getApplicationContext());
            }
            if (!ready || tts == null) {
                return;
            }
            tts.stop();
            Bundle params = new Bundle();
            tts.speak(trimmed, TextToSpeech.QUEUE_FLUSH, params, "ql-" + System.nanoTime());
        }
    }

    public void stop() {
        synchronized (LOCK) {
            if (tts != null) {
                tts.stop();
            }
        }
    }

    /**
     * Adds long-press handlers under {@code root} so visible text can be read aloud.
     */
    public static void attachToHierarchy(@NonNull Activity activity, @Nullable View root) {
        if (root == null || activity.isFinishing()) {
            return;
        }
        if (root instanceof RecyclerView) {
            hookRecyclerView(activity, (RecyclerView) root);
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                attachToHierarchy(activity, vg.getChildAt(i));
            }
        }
        maybeAttachTextView(activity, root);
    }

    private static void hookRecyclerView(Activity activity, RecyclerView rv) {
        if (Boolean.TRUE.equals(rv.getTag(R.id.read_aloud_rv_listener))) {
            return;
        }
        rv.setTag(R.id.read_aloud_rv_listener, true);
        rv.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                attachToHierarchy(activity, view);
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {}
        });
    }

    private static void maybeAttachTextView(Activity activity, View view) {
        if (!(view instanceof TextView) || view instanceof EditText) {
            return;
        }
        TextView tv = (TextView) view;
        if (Boolean.TRUE.equals(tv.getTag(R.id.read_aloud_attached))) {
            return;
        }
        tv.setTag(R.id.read_aloud_attached, true);
        tv.setOnLongClickListener(v -> {
            if (!DisplayPreferences.isReadAloudEnabled(activity)) {
                return false;
            }
            CharSequence t = tv.getText();
            if (t == null || t.toString().trim().isEmpty()) {
                return false;
            }
            getInstance().speak(activity, t.toString());
            return true;
        });
    }
}
