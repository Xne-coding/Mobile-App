package com.questlearn;

/*
 * VoucherHistoryAdapter — past redemptions on the Rewards screen.
 * Optional click listener so the parent can reopen the QR for a voucher.
 */

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.questlearn.db.FirebaseRepository;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VoucherHistoryAdapter extends RecyclerView.Adapter<VoucherHistoryAdapter.VH> {

    public interface OnVoucherClickListener {
        void onVoucherClick(FirebaseRepository.VoucherRecord record);
    }

    private final List<FirebaseRepository.VoucherRecord> items;
    private final OnVoucherClickListener listener;
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("d MMM yyyy", Locale.UK);

    public VoucherHistoryAdapter(List<FirebaseRepository.VoucherRecord> items,
                                 @Nullable OnVoucherClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voucher_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FirebaseRepository.VoucherRecord record = items.get(position);

        String label;
        if ("twenty_pound".equals(record.voucherType)) {
            label = "£20 Weekly Prize";
        } else {
            label = "£5 Voucher";
        }
        holder.tvType.setText(label);

        String amount = String.format(Locale.UK, "£%.2f", record.amountPence / 100.0);
        holder.tvAmount.setText(amount);

        holder.tvDate.setText(DATE_FMT.format(new Date(record.redeemedAt)));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && record.voucherCode != null && !record.voucherCode.isEmpty()) {
                listener.onVoucherClick(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvType, tvDate, tvAmount;

        VH(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvVoucherType);
            tvDate = itemView.findViewById(R.id.tvVoucherDate);
            tvAmount = itemView.findViewById(R.id.tvVoucherAmount);
        }
    }
}
