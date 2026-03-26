package com.questlearn;

import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class StepAdapter extends RecyclerView.Adapter<StepAdapter.ViewHolder> {

    private final List<ChallengeStep> steps;

    public StepAdapter(List<ChallengeStep> steps) {
        this.steps = steps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_step, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(steps.get(position));
    }

    @Override
    public int getItemCount() { return steps.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout numContainer;
        private final TextView tvNum, tvText;

        ViewHolder(View view) {
            super(view);
            numContainer = view.findViewById(R.id.stepNumContainer);
            tvNum        = view.findViewById(R.id.tvStepNum);
            tvText       = view.findViewById(R.id.tvStepText);
        }

        void bind(ChallengeStep step) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);

            if (step.isCompleted()) {
                circle.setColor(0xFFC8E6C9);
                tvNum.setTextColor(0xFF2E7D32);
                tvNum.setText("✓");
                tvText.setPaintFlags(tvText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvText.setTextColor(0xFFBDBDBD);
            } else {
                // Is this the active step? (first non-completed)
                boolean isActive = step.getNumber() == getActiveStepNumber();
                if (isActive) {
                    circle.setColor(0xFF2196F3);
                    tvNum.setTextColor(0xFFFFFFFF);
                } else {
                    circle.setColor(0xFFEEEEEE);
                    tvNum.setTextColor(0xFF9E9E9E);
                }
                tvNum.setText(String.valueOf(step.getNumber()));
                tvText.setPaintFlags(tvText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                tvText.setTextColor(isActive ? 0xFF212121 : 0xFF757575);
            }

            numContainer.setBackground(circle);
            tvText.setText(step.getText());
        }

        private int getActiveStepNumber() {
            // Returns the step number of the first non-completed step
            // In a real app this would come from the data layer
            return 3;
        }
    }
}
