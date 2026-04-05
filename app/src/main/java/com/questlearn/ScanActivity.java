package com.questlearn;

/*
 * ScanActivity — live camera preview + ML Kit QR decoding.
 *
 * Asks for camera permission, shows the back camera, runs QR detection on each frame,
 * and sends successful reads to FirebaseRepository.redeemHardcodedCode (same rules
 * as typing a code manually). Flash toggles the torch when the hardware supports it.
 */

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.questlearn.db.FirebaseRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends QuestLearnBaseActivity {

    public static final String EXTRA_CHALLENGE_XP = "challenge_xp";
    public static final String EXTRA_CHALLENGE_ID = "challenge_id";

    private PreviewView previewView;
    private View cameraBg;
    private boolean flashOn = false;
    private boolean scanHandled = false;
    private volatile boolean redeemInFlight = false;

    private ExecutorService cameraExecutor;
    private final BarcodeScanner qrScanner = BarcodeScanning.getClient(
            new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build());

    @Nullable
    private Camera camera;
    @Nullable
    private ProcessCameraProvider cameraProvider;

    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        SystemBarInsets.applyToRoot(this, R.id.scanRoot);

        previewView = findViewById(R.id.previewView);
        cameraBg = findViewById(R.id.cameraBg);
        cameraExecutor = Executors.newSingleThreadExecutor();

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startCamera();
                    } else {
                        Toast.makeText(this,
                                        "Camera permission is needed to scan QR codes.",
                                        Toast.LENGTH_LONG)
                                .show();
                    }
                });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            try {
                finish();
                UiTransitions.closeBackward(this);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to go back.", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnFlash).setOnClickListener(v -> {
            try {
                toggleFlash();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to toggle flash.", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnManualEntry).setOnClickListener(v -> {
            try {
                showManualEntryDialog();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open manual entry.", Toast.LENGTH_SHORT).show();
            }
        });

        QrOverlayView qrOverlay = findViewById(R.id.qrOverlay);
        if (qrOverlay != null && !DisplayPreferences.isReduceMotion(this)) {
            qrOverlay.startScanAnimation();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /** Ask CameraX for the ProcessCameraProvider, then bind preview + analysis on the main thread. */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCameraUseCases();
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Could not start camera.", Toast.LENGTH_LONG).show());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /** Hook preview to PreviewView and pipe frames to ML Kit QR scanner. */
    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            return;
        }
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

        try {
            camera = cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis);
            applyTorchState();
            runOnUiThread(() -> cameraBg.setVisibility(View.GONE));
        } catch (Exception e) {
            Toast.makeText(this, "Camera unavailable on this device.", Toast.LENGTH_LONG).show();
        }
    }

    /** Runs on a background thread; must close imageProxy when ML Kit finishes. */
    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        if (scanHandled || redeemInFlight) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        InputImage image = InputImage.fromMediaImage(mediaImage, rotation);

        qrScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (scanHandled || redeemInFlight || barcodes.isEmpty()) {
                        return;
                    }
                    String raw = barcodes.get(0).getRawValue();
                    if (raw != null && !raw.isEmpty()) {
                        runOnUiThread(() -> handleScannedCode(raw.trim()));
                    }
                })
                .addOnFailureListener(e -> { /* skip bad frames */ })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    /** Same redemption path as typing a code: one in-flight request at a time. */
    private void handleScannedCode(String code) {
        if (scanHandled || redeemInFlight) {
            return;
        }
        redeemInFlight = true;
        try {
            FirebaseRepository repo = new FirebaseRepository();
            String expectedChallengeId = getIntent().getStringExtra(EXTRA_CHALLENGE_ID);
            repo.redeemHardcodedCode(code, expectedChallengeId,
                    new FirebaseRepository.Callback<FirebaseRepository.CodeRedemptionResult>() {
                        @Override
                        public void onSuccess(FirebaseRepository.CodeRedemptionResult res) {
                            redeemInFlight = false;
                            if (res.success) {
                                scanHandled = true;
                                Toast.makeText(ScanActivity.this,
                                        "✅ Code accepted! +" + res.xpReward + " XP",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                                UiTransitions.closeBackward(ScanActivity.this);
                            } else {
                                Toast.makeText(ScanActivity.this,
                                        "❌ " + res.message, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(String message) {
                            redeemInFlight = false;
                            Toast.makeText(ScanActivity.this,
                                    "Code verification failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            redeemInFlight = false;
            Toast.makeText(this, "Code verification failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFlash() {
        flashOn = !flashOn;
        View btn = findViewById(R.id.btnFlash);
        btn.setBackgroundResource(flashOn
                ? R.drawable.bg_flash_btn_on
                : R.drawable.bg_flash_btn);

        ImageView flashIcon = findViewById(R.id.flashIcon);
        if (flashIcon != null) {
            flashIcon.setColorFilter(
                    flashOn ? Color.parseColor("#FFC107") : Color.WHITE,
                    PorterDuff.Mode.SRC_IN);
        }

        applyTorchState();
        Toast.makeText(this,
                flashOn ? "Flash ON" : "Flash OFF",
                Toast.LENGTH_SHORT).show();
    }

    private void applyTorchState() {
        if (camera == null) {
            return;
        }
        try {
            if (camera.getCameraInfo().hasFlashUnit()) {
                camera.getCameraControl().enableTorch(flashOn);
            }
        } catch (Exception ignored) {
        }
    }

    private void showManualEntryDialog() {
        Context inputCtx = UiTheme.wrapAppTheme(this);
        final android.widget.EditText input = new android.widget.EditText(inputCtx);
        input.setHint("e.g. CLIFTON-LIB-001");
        input.setHintTextColor(UiTheme.textColorHint(inputCtx));
        input.setTextColor(UiTheme.colorOnSurface(inputCtx));
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_QuestLearn)
                .setTitle("Enter Location Code")
                .setView(input)
                .setPositiveButton("Verify", (d, which) -> {
            String code = input.getText().toString().trim();
            if (!code.isEmpty()) {
                if (scanHandled) {
                    return;
                }
                try {
                    FirebaseRepository repo = new FirebaseRepository();
                    String expectedChallengeId = getIntent().getStringExtra(EXTRA_CHALLENGE_ID);
                    repo.redeemHardcodedCode(code, expectedChallengeId,
                            new FirebaseRepository.Callback<FirebaseRepository.CodeRedemptionResult>() {
                                @Override
                                public void onSuccess(FirebaseRepository.CodeRedemptionResult res) {
                                    if (res.success) {
                                        scanHandled = true;
                                        Toast.makeText(ScanActivity.this,
                                                "✅ Code accepted! +" + res.xpReward + " XP",
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                        UiTransitions.closeBackward(ScanActivity.this);
                                    } else {
                                        Toast.makeText(ScanActivity.this,
                                                "❌ " + res.message, Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(ScanActivity.this,
                                            "Code verification failed.", Toast.LENGTH_SHORT).show();
                                }
                            });
                } catch (Exception e) {
                    Toast.makeText(this, "Code verification failed.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter a valid code", Toast.LENGTH_SHORT).show();
            }
                })
                .setNegativeButton("Cancel", null)
                .setMessage("Try: CLIFTON-LIB-001, ERASMUS-LABS-220, SPORTS-VILLAGE-340")
                .create();
        dialog.show();
        SystemBarInsets.applyToDialog(dialog);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            try {
                camera.getCameraControl().enableTorch(false);
            } catch (Exception ignored) {
            }
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        qrScanner.close();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        UiTransitions.closeBackward(this);
    }
}
