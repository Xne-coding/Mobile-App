package com.questlearn;

import android.animation.ObjectAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class OnboardingSlideFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_DESC = "desc";
    private static final String ARG_EMOJI = "emoji";
    private static final String ARG_COLOR = "color";

    public static OnboardingSlideFragment newInstance(String title, String desc,
                                                       String emoji, int bgColor) {
        OnboardingSlideFragment fragment = new OnboardingSlideFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESC, desc);
        args.putString(ARG_EMOJI, emoji);
        args.putInt(ARG_COLOR, bgColor);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_slide, container, false);

        if (getArguments() == null) return view;

        String title = getArguments().getString(ARG_TITLE, "");
        String desc = getArguments().getString(ARG_DESC, "");
        String emoji = getArguments().getString(ARG_EMOJI, "");
        int bgColor = getArguments().getInt(ARG_COLOR, 0xFFE3F2FD);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvDesc = view.findViewById(R.id.tvDescription);
        TextView tvEmoji = view.findViewById(R.id.illustrationEmoji);
        FrameLayout mainCircle = view.findViewById(R.id.mainCircle);

        tvTitle.setText(title);
        tvDesc.setText(desc);
        tvEmoji.setText(emoji);

        // Set circle background colour
        GradientDrawable circleDrawable = new GradientDrawable();
        circleDrawable.setShape(GradientDrawable.OVAL);
        circleDrawable.setColor(bgColor);
        mainCircle.setBackground(circleDrawable);

        // Rotate the dashed ring view
        View ringView = view.findViewById(R.id.ringView);
        if (ringView != null) {
            ObjectAnimator rotator = ObjectAnimator.ofFloat(ringView, "rotation", 0f, 360f);
            rotator.setDuration(10000);
            rotator.setRepeatCount(ObjectAnimator.INFINITE);
            rotator.setRepeatMode(ObjectAnimator.RESTART);
            rotator.start();
        }

        return view;
    }
}
