package com.questlearn;

/*
 * ChallengeDetailActivity — full page for one campus challenge.
 *
 * Loads challenge text from the sample list, shows steps, distance to the building
 * (using GPS when allowed), opens the QR scanner to complete the challenge, and can
 * jump the user back to the map tab. Coordinates and step text are static demo data.
 */

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.questlearn.db.FirebaseRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChallengeDetailActivity extends QuestLearnBaseActivity {

    public static final String EXTRA_CHALLENGE_ID = "challenge_id";

    // Approximate lat/lng for each challenge building — used only for "how far am I?" text.
    private static final Map<String, double[]> COORDS = new HashMap<>();
    static {
        COORDS.put("ch1",  new double[]{52.91295, -1.18539});
        COORDS.put("ch2",  new double[]{52.91070, -1.18712});
        COORDS.put("ch3",  new double[]{52.91125, -1.18775});
        COORDS.put("ch4",  new double[]{52.91228, -1.18362});
        COORDS.put("ch5",  new double[]{52.91117, -1.18504});
        COORDS.put("ch6",  new double[]{52.91151, -1.18534});
        COORDS.put("ch7",  new double[]{52.91157, -1.18625});
        COORDS.put("ch8",  new double[]{52.91112, -1.18663});
        COORDS.put("ch9",  new double[]{52.91200, -1.18404});
        COORDS.put("ch10", new double[]{52.91155, -1.18424});
        COORDS.put("ch11", new double[]{52.91270, -1.18402});
        COORDS.put("ch12", new double[]{52.91324, -1.18452});
        COORDS.put("ch13", new double[]{52.91055, -1.18749});
        COORDS.put("ch14", new double[]{52.91089, -1.18444});
        COORDS.put("ch15", new double[]{52.91254, -1.18607});
        COORDS.put("ch16", new double[]{52.91216, -1.18813});
        COORDS.put("ch17", new double[]{52.91356, -1.18484});
        COORDS.put("ch18", new double[]{52.91093, -1.18597});
        COORDS.put("ch19", new double[]{52.91042, -1.18650});
        COORDS.put("ch20", new double[]{52.91285, -1.18462});
        COORDS.put("ch21", new double[]{52.91055, -1.18567});
    }

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TextView tvDistance;
    private String challengeId;
    private double buildingLat;
    private double buildingLng;
    private FirebaseRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_detail);
        SystemBarInsets.applyToRoot(this, R.id.challengeDetailRoot);

        repo = new FirebaseRepository();
        challengeId = getIntent().getStringExtra(EXTRA_CHALLENGE_ID);
        Challenge challenge = getChallengeById(challengeId);

        setBuildingCoords(challengeId);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            try {
                finish();
                UiTransitions.closeBackward(this);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to go back.", Toast.LENGTH_SHORT).show();
            }
        });

        if (challenge != null) {
            bindChallenge(challenge);
        }

        Button btnBegin = findViewById(R.id.btnBeginChallenge);

        if (challengeId != null) {
            repo.isChallengeCompleted(challengeId, new FirebaseRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean completed) {
                    updateBeginButton(btnBegin, completed, challenge);
                    RecyclerView rvSteps = findViewById(R.id.rvSteps);
                    if (rvSteps != null) {
                        rvSteps.setLayoutManager(new LinearLayoutManager(ChallengeDetailActivity.this));
                        rvSteps.setAdapter(new StepAdapter(getSteps(challengeId, completed)));
                        rvSteps.setNestedScrollingEnabled(false);
                    }
                }
                @Override
                public void onError(String msg) {
                    updateBeginButton(btnBegin, false, challenge);
                }
            });
        } else {
            updateBeginButton(btnBegin, false, challenge);
        }

        TextView tvOpenMap = findViewById(R.id.tvOpenMap);
        if (tvOpenMap != null) {
            tvOpenMap.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("open_tab", R.id.mapFragment);
                    startActivity(intent);
                    UiTransitions.closeBackward(this);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open map.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        MiniMapView miniMap = findViewById(R.id.miniMap);
        if (miniMap != null) {
            miniMap.startPinAnimation();
        }

        tvDistance = findViewById(R.id.tvDistance);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallback();

        if (hasLocationPermission()) {
            fetchLastKnownLocation();
            startLocationUpdates();
        } else {
            if (tvDistance != null && challenge != null) {
                tvDistance.setText("📍 " + challenge.getLocation() + ", Clifton Campus");
            }
        }
    }

    private void updateBeginButton(Button btnBegin, boolean completed, Challenge challenge) {
        if (completed) {
            btnBegin.setText("✅  Challenge Completed");
            btnBegin.setEnabled(false);
            btnBegin.setAlpha(0.7f);
        } else {
            btnBegin.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, ScanActivity.class);
                    if (challenge != null) {
                        intent.putExtra(ScanActivity.EXTRA_CHALLENGE_XP, challenge.getXpPoints());
                        intent.putExtra(ScanActivity.EXTRA_CHALLENGE_ID, challenge.getId());
                    }
                    startActivity(intent);
                    UiTransitions.openForward(this);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open scanner.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (challengeId != null && repo != null) {
            repo.isChallengeCompleted(challengeId, new FirebaseRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean completed) {
                    Button btnBegin = findViewById(R.id.btnBeginChallenge);
                    if (btnBegin != null && completed) {
                        btnBegin.setText("✅  Challenge Completed");
                        btnBegin.setEnabled(false);
                        btnBegin.setAlpha(0.7f);
                    }
                    RecyclerView rvSteps = findViewById(R.id.rvSteps);
                    if (rvSteps != null) {
                        rvSteps.setAdapter(new StepAdapter(getSteps(challengeId, completed)));
                    }
                }
                @Override
                public void onError(String msg) {}
            });
        }
        if (hasLocationPermission()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void bindChallenge(Challenge challenge) {
        TextView tvIcon = findViewById(R.id.tvChallengeIcon);
        TextView tvName = findViewById(R.id.tvChallengeName);
        TextView tvOrg = findViewById(R.id.tvChallengeOrg);
        TextView tvXp = findViewById(R.id.tvXpChip);

        if (tvIcon != null) tvIcon.setText(challenge.getIcon());
        if (tvName != null) tvName.setText(challenge.getName());
        if (tvOrg != null) tvOrg.setText("\uD83C\uDFDB\uFE0F " + challenge.getLocation() + " · NTU Clifton");
        if (tvXp != null) tvXp.setText("⚡ " + challenge.getXpPoints() + " XP");
    }

    private Challenge getChallengeById(String id) {
        List<Challenge> all = Challenge.getSampleData();
        if (id == null) return all.get(0);
        for (Challenge c : all) {
            if (c.getId().equals(id)) return c;
        }
        return all.get(0);
    }

    private void setBuildingCoords(String id) {
        double[] c = COORDS.get(id);
        if (c == null) c = COORDS.get("ch1");
        buildingLat = c[0];
        buildingLng = c[1];
    }

    // Four short instructions per challenge id (demo copy, not loaded from network).
    private static final Map<String, String[]> STEPS = new HashMap<>();
    static {
        STEPS.put("ch1",  new String[]{"Find the main entrance information board",        "Head to the Clifton Library welcome desk",     "Scan the QR code near the study hub",          "Answer the NTU Clifton trivia question"});
        STEPS.put("ch2",  new String[]{"Walk to the Erasmus Darwin Building",              "Find the labs entrance on the 2nd floor",      "Scan the QR code near Room 220",               "Complete the Science Trail trivia"});
        STEPS.put("ch3",  new String[]{"Head to the Lee Westwood Sports Centre",           "Locate the QR code near the main arena",       "Scan the QR code at checkpoint 340",           "Complete the Sports Route challenge"});
        STEPS.put("ch4",  new String[]{"Walk to the Students' Union building",             "Find the main entrance foyer",                 "Scan the QR code near The Point bar",          "Complete the Union Trail quiz"});
        STEPS.put("ch5",  new String[]{"Navigate to the Ada Byron King building",          "Head to the ground floor computing labs",       "Scan the QR code outside the lecture hall",     "Answer the Digital Discovery question"});
        STEPS.put("ch6",  new String[]{"Locate the John Clare Lecture Theatre",            "Find the entrance noticeboard",                "Scan the QR code inside the foyer",            "Complete the literary trivia question"});
        STEPS.put("ch7",  new String[]{"Walk to the Teaching & Learning building",         "Head to the main reception area",              "Scan the QR code near the seminar rooms",      "Complete the Knowledge Hub quiz"});
        STEPS.put("ch8",  new String[]{"Navigate to the CELS / NSRC building",             "Find the science labs corridor",               "Scan the QR code near the research labs",      "Answer the Lab Quest trivia"});
        STEPS.put("ch9",  new String[]{"Head to the DH Lawrence building",                 "Locate the main study area",                   "Scan the QR code near the common room",        "Complete the Lawrence Walk quiz"});
        STEPS.put("ch10", new String[]{"Walk to the Mary Ann Evans building",              "Find the arts and humanities corridor",         "Scan the QR code outside the studios",         "Answer the Evans Explorer question"});
        STEPS.put("ch11", new String[]{"Navigate to the Lionel Robbins building",          "Head to the main entrance hall",               "Scan the QR code near the study spaces",       "Complete the Robbins Route trivia"});
        STEPS.put("ch12", new String[]{"Walk to the Anthony Nolan centre",                 "Locate the Cell Therapy reception",            "Scan the QR code in the main corridor",        "Complete the Nolan Mission quiz"});
        STEPS.put("ch13", new String[]{"Navigate to the Cancer Research Centre",           "Find the John van Geest building entrance",    "Scan the QR code near the research wing",      "Answer the Research Quest trivia"});
        STEPS.put("ch14", new String[]{"Head to the ISTeC building",                       "Find the innovation hub entrance",             "Scan the QR code near the tech labs",          "Complete the Tech Trail quiz"});
        STEPS.put("ch15", new String[]{"Walk to the New Hall Block",                       "Locate the main entrance",                     "Scan the QR code in the hallway",              "Complete the Hall Hunt challenge"});
        STEPS.put("ch16", new String[]{"Navigate to The Clubhouse",                        "Find the main lounge area",                    "Scan the QR code near the counter",            "Answer the Club Quest trivia"});
        STEPS.put("ch17", new String[]{"Walk to the Cricket Pavilion",                     "Locate the pavilion entrance",                 "Scan the QR code near the scoreboard",         "Complete the Cricket Challenge quiz"});
        STEPS.put("ch18", new String[]{"Head to the Engineering Buildings",                "Find the workshop corridor",                   "Scan the QR code near the main lab",           "Complete the Engineer's Path challenge"});
        STEPS.put("ch19", new String[]{"Navigate to the iSMART building",                  "Head to the research lab entrance",            "Scan the QR code near the equipment room",     "Answer the Smart Lab trivia"});
        STEPS.put("ch20", new String[]{"Walk to the Pavilion Building",                    "Find the food court area",                     "Scan the QR code near the seating",            "Complete the Pavilion Quest quiz"});
        STEPS.put("ch21", new String[]{"Navigate to the Rosalind Franklin Building",       "Locate the chemistry lab entrance",            "Scan the QR code near the periodic table",     "Complete Franklin's Lab challenge"});
    }

    private List<ChallengeStep> getSteps(String id, boolean completed) {
        String[] s = STEPS.get(id);
        if (s == null) s = STEPS.get("ch1");
        return Arrays.asList(
                new ChallengeStep(1, s[0], completed),
                new ChallengeStep(2, s[1], completed),
                new ChallengeStep(3, s[2], completed),
                new ChallengeStep(4, s[3], completed)
        );
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location last = locationResult.getLastLocation();
                if (last != null) {
                    updateDistance(last);
                }
            }
        };
    }

    private void fetchLastKnownLocation() {
        if (!hasLocationPermission()) return;
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    updateDistance(location);
                }
            });
        } catch (SecurityException ignored) {}
    }

    private void startLocationUpdates() {
        if (!hasLocationPermission() || fusedLocationClient == null || locationCallback == null)
            return;
        try {
            LocationRequest request = new LocationRequest.Builder(5000)
                    .setMinUpdateIntervalMillis(2500)
                    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                    .build();
            fusedLocationClient.requestLocationUpdates(request, locationCallback,
                    Looper.getMainLooper());
        } catch (SecurityException ignored) {}
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            } catch (Exception ignored) {}
        }
    }

    private void updateDistance(Location userLocation) {
        if (tvDistance == null) return;

        float[] results = new float[1];
        Location.distanceBetween(
                userLocation.getLatitude(), userLocation.getLongitude(),
                buildingLat, buildingLng, results);
        int meters = Math.round(results[0]);

        Challenge challenge = getChallengeById(challengeId);
        String locationName = challenge != null ? challenge.getLocation() : "Building";

        String distanceText;
        if (meters >= 1000) {
            distanceText = String.format("%.1f km away", meters / 1000f);
        } else {
            distanceText = meters + "m away";
        }
        tvDistance.setText("📍 " + locationName + ", Clifton Campus · " + distanceText);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        UiTransitions.closeBackward(this);
    }
}
