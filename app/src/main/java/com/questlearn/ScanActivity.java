package com.questlearn;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ScanActivity extends AppCompatActivity {

    private boolean flashOn = false;
    private TextView activeChip = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            onBackPressed();
        });

        // Flash toggle
        findViewById(R.id.btnFlash).setOnClickListener(v -> toggleFlash(v));

        // Mode chips
        TextView chipQr      = findViewById(R.id.chipQr);
        TextView chipBarcode = findViewById(R.id.chipBarcode);
        TextView chipNfc     = findViewById(R.id.chipNfc);

        activeChip = chipQr; // QR is default active

        chipQr.setOnClickListener(v -> activateChip(chipQr,
                new TextView[]{chipBarcode, chipNfc}));
        chipBarcode.setOnClickListener(v -> activateChip(chipBarcode,
                new TextView[]{chipQr, chipNfc}));
        chipNfc.setOnClickListener(v -> activateChip(chipNfc,
                new TextView[]{chipQr, chipBarcode}));

        // Manual entry button
        findViewById(R.id.btnManualEntry).setOnClickListener(v -> showManualEntryDialog());

        // QR overlay - simulate scan after 3 seconds for demo
        QrOverlayView qrOverlay = findViewById(R.id.qrOverlay);
        if (qrOverlay != null) {
            qrOverlay.startScanAnimation();
        }
    }

    private void toggleFlash(android.view.View btn) {
        flashOn = !flashOn;
        btn.setBackgroundResource(flashOn
                ? R.drawable.bg_flash_btn_on
                : R.drawable.bg_flash_btn);
        Toast.makeText(this,
                flashOn ? "💡 Flash ON" : "🔦 Flash OFF",
                Toast.LENGTH_SHORT).show();

        // In a real app: CameraManager.setTorchMode(cameraId, flashOn)
    }

    private void activateChip(TextView selected, TextView[] others) {
        // Active style
        selected.setBackgroundResource(R.drawable.bg_scan_chip_active);
        selected.setTextColor(Color.WHITE);

        // Inactive style for others
        for (TextView chip : others) {
            chip.setBackgroundResource(R.drawable.bg_scan_chip_inactive);
            chip.setTextColor(Color.parseColor("#A6FFFFFF"));
        }

        activeChip = selected;
        Toast.makeText(this, "Mode: " + selected.getText(), Toast.LENGTH_SHORT).show();
    }

    private void showManualEntryDialog() {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(this, R.style.Theme_QuestLearn);
        builder.setTitle("Enter Location Code");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("e.g. LIB-3F-001");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        builder.setView(input);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (!code.isEmpty()) {
                Toast.makeText(this, "✅ Code accepted: " + code, Toast.LENGTH_SHORT).show();
                // In production: validate code against backend, then navigate to challenge
                Intent intent = new Intent(this, ChallengeDetailActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                Toast.makeText(this, "Please enter a valid code", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
