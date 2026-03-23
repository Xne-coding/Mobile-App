package com.questlearn;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class MapFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // FAB → ScanActivity
        ExtendedFloatingActionButton fabScan = view.findViewById(R.id.fabScan);
        fabScan.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ScanActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(
                    R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Set up legend items
        setupLegend(view);

        // MapCanvasView pin tap callbacks
        MapCanvasView mapCanvas = view.findViewById(R.id.mapCanvas);
        if (mapCanvas != null) {
            mapCanvas.setOnPinTappedListener((pinType, label) -> {
                switch (pinType) {
                    case MapCanvasView.PIN_DONE:
                        showToast("✅ " + label + " — Already completed! +50 XP");
                        break;
                    case MapCanvasView.PIN_LOCKED:
                        showToast("🔒 " + label + " — Complete previous challenge first");
                        break;
                    case MapCanvasView.PIN_ORANGE:
                        showToast("📡 " + label + " — Beacon detected, 340m away");
                        break;
                    default:
                        // Navigate to challenge detail for active pins
                        Intent intent = new Intent(requireContext(), ChallengeDetailActivity.class);
                        startActivity(intent);
                        requireActivity().overridePendingTransition(
                                R.anim.slide_in_right, R.anim.slide_out_left);
                        break;
                }
            });
        }

        // Search bar tap feedback
        view.findViewById(R.id.searchBar).setOnClickListener(v ->
                showToast("🔍 Search coming soon…"));
    }

    private void setupLegend(View view) {
        View legendGps    = view.findViewById(R.id.legendGps);
        View legendQr     = view.findViewById(R.id.legendQr);
        View legendBeacon = view.findViewById(R.id.legendBeacon);

        setLegendItem(legendGps,    0xFF2196F3, "GPS");
        setLegendItem(legendQr,     0xFF4CAF50, "QR Code");
        setLegendItem(legendBeacon, 0xFFFF9800, "Beacon");
    }

    private void setLegendItem(View container, int color, String label) {
        if (container == null) return;
        View dot = container.findViewById(R.id.legendDot);
        TextView tvLabel = container.findViewById(R.id.legendLabel);
        if (dot != null) {
            android.graphics.drawable.GradientDrawable circle =
                    new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setColor(color);
            dot.setBackground(circle);
        }
        if (tvLabel != null) tvLabel.setText(label);
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
