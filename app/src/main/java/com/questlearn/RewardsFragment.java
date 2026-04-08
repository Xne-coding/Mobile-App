package com.questlearn;

/*
 * RewardsFragment — reward points, XP conversion, and vouchers.
 *
 * Lets users spend accumulated "reward" currency on vouchers (shows a QR in a dialog),
 * converts XP to points at a fixed rate, and shows weekly leaderboard + voucher history.
 * Guests see a reduced experience (no weekly XP sync in the same way).
 */

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.questlearn.db.FirebaseRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RewardsFragment extends Fragment {

    private static final int VOUCHER_COST = 500;
    private static final int XP_PER_POINT = 10;
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.UK);

    private FirebaseRepository repo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rewards, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new FirebaseRepository();

        RecyclerView rvWeekly = view.findViewById(R.id.rvWeeklyLeaderboard);
        rvWeekly.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWeekly.setAdapter(new LeaderboardAdapter(new ArrayList<>()));
        rvWeekly.setNestedScrollingEnabled(false);

        RecyclerView rvHistory = view.findViewById(R.id.rvVoucherHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(new VoucherHistoryAdapter(new ArrayList<>(), null));
        rvHistory.setNestedScrollingEnabled(false);

        view.findViewById(R.id.btnConvertXp).setOnClickListener(v -> convertXp());
        view.findViewById(R.id.btnRedeemVoucher).setOnClickListener(v -> redeemVoucher());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUI();
    }

    /** Pull reward balances from Firestore and refill all labels + lists on screen. */
    private void refreshUI() {
        View view = getView();
        if (view == null || repo == null) return;

        boolean isGuest = repo.isGuest();
        if (!isGuest) {
            repo.syncWeeklyXp();
        }

        repo.getRewardData(new FirebaseRepository.Callback<FirebaseRepository.RewardData>() {
            @Override
            public void onSuccess(FirebaseRepository.RewardData data) {
                if (getView() == null) return;
                View v = getView();

                TextView tvPoints = v.findViewById(R.id.tvPointsBalance);
                tvPoints.setText(String.valueOf(data.rewardPoints));

                TextView tvDailyXp = v.findViewById(R.id.tvDailyXp);
                tvDailyXp.setText(data.dailyXp + " XP earned today");

                int pointsEquiv = data.availableToConvert / XP_PER_POINT;
                TextView tvAvailable = v.findViewById(R.id.tvAvailableToConvert);
                tvAvailable.setText(data.availableToConvert + " XP available (= " + pointsEquiv + " pts)");

                com.google.android.material.button.MaterialButton btnConvert =
                        v.findViewById(R.id.btnConvertXp);
                if (pointsEquiv > 0) {
                    btnConvert.setEnabled(true);
                    btnConvert.setText("Convert " + data.availableToConvert + " XP → " + pointsEquiv + " Points");
                } else {
                    btnConvert.setEnabled(false);
                    btnConvert.setText("No XP to convert");
                }

                TextView tvMyWeekly = v.findViewById(R.id.tvMyWeeklyXp);
                tvMyWeekly.setText("Your weekly XP: " + data.weeklyXp);

                ProgressBar voucherBar = v.findViewById(R.id.voucherProgress);
                voucherBar.setProgress(Math.min(data.rewardPoints, VOUCHER_COST));

                TextView tvVoucherProg = v.findViewById(R.id.tvVoucherProgress);
                tvVoucherProg.setText(data.rewardPoints + " / " + VOUCHER_COST + " points");

                com.google.android.material.button.MaterialButton btnRedeem =
                        v.findViewById(R.id.btnRedeemVoucher);
                btnRedeem.setEnabled(data.rewardPoints >= VOUCHER_COST);

                populateWeeklyLeaderboard(v);
                populateVoucherHistory(v);
            }

            @Override
            public void onError(String msg) {}
        });
    }

    /** Trades "daily XP" bucket into spendable reward points (server-side rules). */
    private void convertXp() {
        if (repo.isGuest()) {
            Toast.makeText(requireContext(),
                    "Create an account to earn reward points.", Toast.LENGTH_SHORT).show();
            return;
        }
        repo.convertDailyXpToPoints(new FirebaseRepository.Callback<Integer>() {
            @Override
            public void onSuccess(Integer points) {
                if (points > 0) {
                    Toast.makeText(requireContext(),
                            "Earned " + points + " reward points!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(),
                            "Need at least " + XP_PER_POINT + " XP to convert.", Toast.LENGTH_SHORT).show();
                }
                refreshUI();
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(requireContext(),
                        "Unable to convert XP.", Toast.LENGTH_SHORT).show();
                refreshUI();
            }
        });
    }

    /** Spend fixed points for a £5 style voucher and show its QR code. */
    private void redeemVoucher() {
        if (repo.isGuest()) {
            Toast.makeText(requireContext(),
                    "Create an account to redeem vouchers.", Toast.LENGTH_SHORT).show();
            return;
        }
        repo.redeemVoucher(VOUCHER_COST, "five_pound", 500,
                new FirebaseRepository.Callback<String>() {
                    @Override
                    public void onSuccess(String code) {
                        if (code != null) {
                            showQrDialog("£5 Voucher Redeemed!", code, System.currentTimeMillis());
                        } else {
                            Toast.makeText(requireContext(),
                                    "Not enough points.", Toast.LENGTH_SHORT).show();
                        }
                        refreshUI();
                    }

                    @Override
                    public void onError(String msg) {
                        Toast.makeText(requireContext(),
                                "Unable to redeem voucher.", Toast.LENGTH_SHORT).show();
                        refreshUI();
                    }
                });
    }

    /** Popup with human title, code text, date, and a generated QR bitmap. */
    void showQrDialog(String title, String voucherCode, long redeemedAt) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_voucher_qr, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvVoucherTitle);
        tvTitle.setText(title);

        TextView tvCode = dialogView.findViewById(R.id.tvVoucherCode);
        tvCode.setText(voucherCode);

        TextView tvDate = dialogView.findViewById(R.id.tvVoucherDate);
        tvDate.setText("Redeemed: " + DATE_FMT.format(new Date(redeemedAt)));

        ImageView ivQr = dialogView.findViewById(R.id.ivQrCode);
        Bitmap qrBitmap = QrCodeHelper.generate(voucherCode, 512);
        if (qrBitmap != null) {
            ivQr.setImageBitmap(qrBitmap);
        }

        AlertDialog qrDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();
        qrDialog.show();
        SystemBarInsets.applyToDialog(qrDialog);
    }

    /** Weekly XP leaderboard uses the same row model as home, different Firestore query. */
    private void populateWeeklyLeaderboard(View view) {
        repo.getWeeklyLeaderboard(new FirebaseRepository.Callback<List<FirebaseRepository.LeaderboardRow>>() {
            @Override
            public void onSuccess(List<FirebaseRepository.LeaderboardRow> rows) {
                if (getView() == null) return;
                RecyclerView rv = view.findViewById(R.id.rvWeeklyLeaderboard);
                List<LeaderboardEntry> entries = new ArrayList<>();
                for (int i = 0; i < rows.size(); i++) {
                    FirebaseRepository.LeaderboardRow row = rows.get(i);
                    int rank = i + 1;
                    entries.add(new LeaderboardEntry(
                            rank, row.displayName, row.initials, row.avatarId,
                            row.totalXp,
                            row.isCurrentUser ? 0xFF2196F3 : 0xFF9E9E9E,
                            row.isCurrentUser));
                }
                rv.setAdapter(new LeaderboardAdapter(entries));
            }

            @Override
            public void onError(String msg) {}
        });
    }

    /** List past vouchers; tapping an item re-opens the QR for that code. */
    private void populateVoucherHistory(View view) {
        repo.getVoucherHistory(new FirebaseRepository.Callback<List<FirebaseRepository.VoucherRecord>>() {
            @Override
            public void onSuccess(List<FirebaseRepository.VoucherRecord> records) {
                if (getView() == null) return;
                RecyclerView rv = view.findViewById(R.id.rvVoucherHistory);
                TextView tvNoHistory = view.findViewById(R.id.tvNoHistory);

                rv.setAdapter(new VoucherHistoryAdapter(records, record -> {
                    String label = "twenty_pound".equals(record.voucherType)
                            ? "£20 Weekly Prize" : "£5 Voucher";
                    showQrDialog(label, record.voucherCode, record.redeemedAt);
                }));

                if (records.isEmpty()) {
                    tvNoHistory.setVisibility(View.VISIBLE);
                    rv.setVisibility(View.GONE);
                } else {
                    tvNoHistory.setVisibility(View.GONE);
                    rv.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String msg) {}
        });
    }
}
