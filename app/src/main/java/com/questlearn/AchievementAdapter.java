package com.questlearn;

/** Recycler rows for profile achievements (locked vs unlocked styling). */

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private final List<Achievement> achievements;

    public AchievementAdapter(List<Achievement> achievements) {
        this.achievements = achievements;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(achievements.get(position));
    }

    @Override
    public int getItemCount() { return achievements.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout iconContainer;
        private final TextView tvIcon, tvName, tvDesc, tvXp, tvStatus;

        ViewHolder(View view) {
            super(view);
            iconContainer = view.findViewById(R.id.achIconContainer);
            tvIcon        = view.findViewById(R.id.tvAchIcon);
            tvName        = view.findViewById(R.id.tvAchName);
            tvDesc        = view.findViewById(R.id.tvAchDesc);
            tvXp          = view.findViewById(R.id.tvAchXp);
            tvStatus      = view.findViewById(R.id.tvAchStatus);
        }

        void bind(Achievement ach) {
            tvIcon.setText(ach.getIcon());
            tvName.setText(ach.getName());
            tvDesc.setText(ach.getDescription());

            // Set icon background
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(16f);
            bg.setColor(ach.getIconBackgroundColor());
            iconContainer.setBackground(bg);

            if (ach.isUnlocked()) {
                tvXp.setText("+" + ach.getXpEarned() + " XP earned");
                tvXp.setTextColor(0xFF2196F3);
                tvStatus.setText("✅");
                itemView.setAlpha(1f);
            } else {
                tvXp.setText(ach.getXpEarned() + " XP remaining");
                tvXp.setTextColor(0xFFBDBDBD);
                tvStatus.setText("🔒");
                itemView.setAlpha(0.5f);
            }
        }
    }
}
