package com.questlearn;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.questlearn.db.ProgressDbHelper;

public class ScanActivity extends AppCompatActivity {

    public static final String EXTRA_CHALLENGE_XP = "challenge_xp";
    public static final String EXTRA_CHALLENGE_ID = "challenge_id";

    private boolean flashOn = false;
    private TextView activeChip = null;
    private boolean scanHandled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            try {
                onBackPressed();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to go back.", Toast.LENGTH_SHORT).show();
            }
        });

        // Flash toggle
        findViewById(R.id.btnFlash).setOnClickListener(v -> {
            try {
                toggleFlash(v);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to toggle flash.", Toast.LENGTH_SHORT).show();
            }
        });

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
        android.view.View btnManual = findViewById(R.id.btnManualEntry);
        btnManual.setOnClickListener(v -> {
            try {
                showManualEntryDialog();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open manual entry.", Toast.LENGTH_SHORT).show();
            }
        });
        btnManual.setOnLongClickListener(v -> {
            exportDbForSqliteViewer();
            return true;
        });

        // QR overlay - simulate scan success after 3 seconds for demo
        QrOverlayView qrOverlay = findViewById(R.id.qrOverlay);
        if (qrOverlay != null) {
            qrOverlay.startScanAnimation();
        }
        // Real scanner integration not wired yet; use manual code entry for completion.
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
        input.setHint("e.g. CLIFTON-LIB-001");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        builder.setView(input);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (!code.isEmpty()) {
                if (scanHandled) return;
                try {
                    ProgressDbHelper db = new ProgressDbHelper(this);
                    String expectedChallengeId = getIntent().getStringExtra(EXTRA_CHALLENGE_ID);
                    ProgressDbHelper.CodeRedemptionResult res =
                            db.redeemHardcodedCode(code, expectedChallengeId);
                    if (res.success) {
                        scanHandled = true;
                        Toast.makeText(this,
                                "✅ Code accepted! +" + res.xpReward + " XP",
                                Toast.LENGTH_SHORT).show();
                        exportDbForSqliteViewer();
                        finish();
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    } else {
                        Toast.makeText(this, "❌ " + res.message, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Code verification failed.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter a valid code", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.setMessage("Try: CLIFTON-LIB-001, ERASMUS-LABS-220, SPORTS-VILLAGE-340");
        builder.show();
    }

    private void exportDbForSqliteViewer() {
        try {
            ProgressDbHelper db = new ProgressDbHelper(this);
            String path = db.exportDatabaseForSqliteViewer(this);
            Toast.makeText(this, "DB exported: " + path, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "DB export failed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
