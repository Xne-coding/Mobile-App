package com.questlearn;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardEntry> entries;

    public LeaderboardAdapter(List<LeaderboardEntry> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(entries.get(position));
    }

    @Override
    public int getItemCount() { return entries.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View rowRoot;
        private final TextView tvRank, tvAvatar, tvName, tvPoints;
        private final FrameLayout avatarContainer;

        ViewHolder(View view) {
            super(view);
            rowRoot         = view.findViewById(R.id.rowRoot);
            tvRank          = view.findViewById(R.id.tvRank);
            tvAvatar        = view.findViewById(R.id.tvAvatar);
            tvName          = view.findViewById(R.id.tvName);
            tvPoints        = view.findViewById(R.id.tvPoints);
            avatarContainer = view.getParent() instanceof ViewGroup
                    ? (FrameLayout) ((ViewGroup) view).findViewById(R.id.tvAvatar).getParent()
                    : null;
        }

        void bind(LeaderboardEntry entry) {
            tvRank.setText(entry.getRankDisplay());
            tvAvatar.setText(entry.getInitials());
            tvPoints.setText(String.format("%,d XP", entry.getXpPoints()));

            // Current user styling
            if (entry.isCurrentUser()) {
                tvName.setText("You");
                tvName.setTextColor(Color.parseColor("#1565C0"));
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                rowRoot.setBackgroundResource(R.drawable.bg_leaderboard_self_row);
            } else {
                tvName.setText(entry.getName());
                tvName.setTextColor(Color.parseColor("#424242"));
                tvName.setTypeface(null, android.graphics.Typeface.NORMAL);
                rowRoot.setBackgroundResource(android.R.color.transparent);
            }

            // Rank colour
            switch (entry.getRank()) {
                case 1: tvRank.setTextColor(Color.parseColor("#FFD600")); break;
                case 2: tvRank.setTextColor(Color.parseColor("#9E9E9E")); break;
                case 3: tvRank.setTextColor(Color.parseColor("#FF8A65")); break;
                default:
                    tvRank.setTextColor(entry.isCurrentUser()
                            ? Color.parseColor("#2196F3")
                            : Color.parseColor("#BDBDBD"));
                    break;
            }

            // Set avatar background colour
            View avatarView = ((ViewGroup) tvAvatar.getParent());
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(entry.getAvatarColor());
            avatarView.setBackground(circle);
        }
    }
}
