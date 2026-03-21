package com.questlearn;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ViewHolder> {

    private final List<Challenge> challenges;

    public ChallengeAdapter(List<Challenge> challenges) {
        this.challenges = challenges;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_challenge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Challenge ch = challenges.get(position);
        holder.bind(ch);
    }

    @Override
    public int getItemCount() { return challenges.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardRoot;
        private final View accentStripe;
        private final TextView tvName, tvPoints, tvLocation, tvDistance, tvProgress;
        private final ProgressBar progressBar;

        ViewHolder(View view) {
            super(view);
            cardRoot     = view.findViewById(R.id.cardRoot);
            accentStripe = view.findViewById(R.id.accentStripe);
            tvName       = view.findViewById(R.id.tvChallengeName);
            tvPoints     = view.findViewById(R.id.tvPoints);
            tvLocation   = view.findViewById(R.id.tvLocation);
            tvDistance   = view.findViewById(R.id.tvDistance);
            tvProgress   = view.findViewById(R.id.tvProgress);
            progressBar  = view.findViewById(R.id.progressBar);
        }

        void bind(Challenge ch) {
            tvName.setText(ch.getName());
            tvPoints.setText("+" + ch.getXpPoints() + " XP");
            tvLocation.setText(ch.getLocation());
            tvDistance.setText(ch.getDistanceMeters() + "m away");
            progressBar.setProgress(ch.getProgressPercent());
            tvProgress.setText(ch.getProgressPercent() + "%");

            // Apply accent colour
            int accentColor;
            int textColor;
            int chipBg;

            switch (ch.getAccentColor()) {
                case GREEN:
                    accentColor = Color.parseColor("#4CAF50");
                    textColor   = Color.parseColor("#2E7D32");
                    chipBg      = Color.parseColor("#C8E6C9");
                    progressBar.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(accentColor));
                    break;
                case ORANGE:
                    accentColor = Color.parseColor("#FF9800");
                    textColor   = Color.parseColor("#E65100");
                    chipBg      = Color.parseColor("#FFE0B2");
                    progressBar.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(accentColor));
                    break;
                default: // BLUE
                    accentColor = Color.parseColor("#2196F3");
                    textColor   = Color.parseColor("#1565C0");
                    chipBg      = Color.parseColor("#E3F2FD");
                    progressBar.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(accentColor));
                    break;
            }

            accentStripe.setBackgroundColor(accentColor);
            tvPoints.setTextColor(textColor);
            tvPoints.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(chipBg));

            // Navigate to ChallengeDetailActivity on tap
            cardRoot.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ChallengeDetailActivity.class);
                intent.putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, ch.getId());
                v.getContext().startActivity(intent);
                if (v.getContext() instanceof android.app.Activity) {
                    ((android.app.Activity) v.getContext()).overridePendingTransition(
                            R.anim.slide_in_right, R.anim.slide_out_left);
                }
            });
        }
    }
}
