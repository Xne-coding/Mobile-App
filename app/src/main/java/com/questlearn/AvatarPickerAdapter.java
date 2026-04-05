package com.questlearn;

/*
 * AvatarPickerAdapter — grid of AvatarOption cells; notifies listener when user taps one.
 * Selected option gets a dark ring so you can see what's active.
 */

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AvatarPickerAdapter extends RecyclerView.Adapter<AvatarPickerAdapter.ViewHolder> {

    public interface OnAvatarSelected {
        void onSelected(AvatarOption option);
    }

    private final List<AvatarOption> options;
    private final String selectedId;
    private final OnAvatarSelected listener;

    public AvatarPickerAdapter(List<AvatarOption> options, String selectedId,
                               OnAvatarSelected listener) {
        this.options = options;
        this.selectedId = selectedId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(options.get(position));
    }

    @Override
    public int getItemCount() { return options.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout avatarCircle;
        private final TextView tvEmoji;

        ViewHolder(View view) {
            super(view);
            avatarCircle = view.findViewById(R.id.avatarCircle);
            tvEmoji = view.findViewById(R.id.tvAvatarEmoji);
        }

        void bind(AvatarOption option) {
            tvEmoji.setText(option.getEmoji());

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(option.getBackgroundColor());
            if (option.getId().equals(selectedId)) {
                bg.setStroke(4, 0xFF212121);
            }
            avatarCircle.setBackground(bg);

            itemView.setOnClickListener(v -> listener.onSelected(option));
        }
    }
}
