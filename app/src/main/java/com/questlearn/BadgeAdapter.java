package com.questlearn;

/** Small grid cells for earned / locked challenge badges on the profile screen. */

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.ViewHolder> {

    private final List<Badge> badges;

    public BadgeAdapter(List<Badge> badges) {
        this.badges = badges;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(badges.get(position));
    }

    @Override
    public int getItemCount() { return badges.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout badgeCircle;
        private final TextView tvEmoji, tvName;

        ViewHolder(View view) {
            super(view);
            badgeCircle = view.findViewById(R.id.badgeCircle);
            tvEmoji     = view.findViewById(R.id.tvBadgeEmoji);
            tvName      = view.findViewById(R.id.tvBadgeName);
        }

        void bind(Badge badge) {
            tvEmoji.setText(badge.getEmoji());
            tvName.setText(badge.getName());

            // Circle background
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(badge.getBackgroundColor());
            badgeCircle.setBackground(circle);

            // Dim locked badges
            if (!badge.isEarned()) {
                badgeCircle.setAlpha(0.3f);
                tvEmoji.setAlpha(0.3f);
                tvName.setAlpha(0.5f);
                tvName.setTextColor(0xFFBDBDBD);
            } else {
                badgeCircle.setAlpha(1f);
                tvEmoji.setAlpha(1f);
                tvName.setAlpha(1f);
                tvName.setTextColor(0xFF757575);
            }

            // Bounce animation on tap for earned badges
            if (badge.isEarned()) {
                badgeCircle.setOnClickListener(v -> {
                    v.animate()
                            .scaleX(0.85f).scaleY(0.85f)
                            .setDuration(100)
                            .withEndAction(() ->
                                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                            .start();
                });
            }
        }
    }
}
