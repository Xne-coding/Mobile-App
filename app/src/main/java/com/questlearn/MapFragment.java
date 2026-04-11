package com.questlearn;

/*
 * MapFragment — Google Map of Clifton campus with challenge pins.
 *
 * Drops markers for buildings/challenges, handles taps to open details or scan,
 * optional live location updates, a search dialog to jump the camera, and a small
 * legend. Permission for fine location is requested when the user needs GPS features.
 */

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.questlearn.db.FirebaseRepository;
import java.util.HashSet;
import java.util.Set;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final LatLng NTU_CLIFTON = new LatLng(52.9115, -1.1858);
    private static final float PROXIMITY_METERS = 80f;

    private static final int PIN_GPS = 0;
    private static final int PIN_QR = 1;
    private static final int PIN_BEACON = 2;
    private static final int PIN_INFO = 3;
    private static final int PIN_LOCKED = 4;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private final Set<String> proximityNotified = new HashSet<>();
    private FirebaseRepository repo;

    private static class BuildingData {
        final String id;
        final String label;
        final LatLng latLng;
        final int pinType;
        final String challengeId;

        BuildingData(String id, String label, LatLng latLng, int pinType, String challengeId) {
            this.id = id;
            this.label = label;
            this.latLng = latLng;
            this.pinType = pinType;
            this.challengeId = challengeId;
        }
    }

    private final BuildingData[] buildings = new BuildingData[]{
            new BuildingData("clifton_library",  "Clifton Library",              new LatLng(52.91295, -1.18539), PIN_QR,     "ch1"),
            new BuildingData("erasmus_darwin",   "Erasmus Darwin",               new LatLng(52.91070, -1.18712), PIN_GPS,    "ch2"),
            new BuildingData("sports_centre",    "Lee Westwood Sports Centre",   new LatLng(52.91125, -1.18775), PIN_BEACON, "ch3"),
            new BuildingData("students_union",   "Students' Union",              new LatLng(52.91228, -1.18362), PIN_QR,     "ch4"),
            new BuildingData("ada_byron_king",   "Ada Byron King",               new LatLng(52.91117, -1.18504), PIN_GPS,    "ch5"),
            new BuildingData("john_clare",       "John Clare Lecture Theatre",    new LatLng(52.91151, -1.18534), PIN_QR,     "ch6"),
            new BuildingData("teaching_learning","Teaching & Learning",           new LatLng(52.91157, -1.18625), PIN_GPS,    "ch7"),
            new BuildingData("cels_nsrc",        "CELS / NSRC",                  new LatLng(52.91112, -1.18663), PIN_BEACON, "ch8"),
            new BuildingData("dh_lawrence",      "DH Lawrence",                  new LatLng(52.91200, -1.18404), PIN_QR,     "ch9"),
            new BuildingData("mary_ann_evans",   "Mary Ann Evans",               new LatLng(52.91155, -1.18424), PIN_GPS,    "ch10"),
            new BuildingData("lionel_robbins",   "Lionel Robbins",               new LatLng(52.91270, -1.18402), PIN_QR,     "ch11"),
            new BuildingData("anthony_nolan",    "Anthony Nolan",                new LatLng(52.91324, -1.18452), PIN_BEACON, "ch12"),
            new BuildingData("cancer_research",  "Cancer Research Centre",        new LatLng(52.91055, -1.18749), PIN_GPS,    "ch13"),
            new BuildingData("istec",            "ISTeC",                        new LatLng(52.91089, -1.18444), PIN_QR,     "ch14"),
            new BuildingData("new_hall_block",   "New Hall Block",               new LatLng(52.91254, -1.18607), PIN_BEACON, "ch15"),
            new BuildingData("the_clubhouse",    "The Clubhouse",                new LatLng(52.91216, -1.18813), PIN_QR,     "ch16"),
            new BuildingData("cricket_pavilion", "Cricket Pavilion",             new LatLng(52.91356, -1.18484), PIN_GPS,    "ch17"),
            new BuildingData("engineering",      "Engineering Buildings",         new LatLng(52.91093, -1.18597), PIN_BEACON, "ch18"),
            new BuildingData("ismart",           "iSMART",                       new LatLng(52.91042, -1.18650), PIN_GPS,    "ch19"),
            new BuildingData("pavilion_building","Pavilion Building",            new LatLng(52.91285, -1.18462), PIN_QR,     "ch20"),
            new BuildingData("rosalind_franklin","Rosalind Franklin",            new LatLng(52.91055, -1.18567), PIN_LOCKED, "ch21"),
    };

    private static class MarkerData {
        final int pinType;
        final String challengeId;

        MarkerData(int pinType, String challengeId) {
            this.pinType = pinType;
            this.challengeId = challengeId;
        }
    }

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) {
                    enableLocationFeatures();
                } else {
                    showToast("Location permission denied. Proximity alerts are off.");
                }
            });

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
        repo = new FirebaseRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        setupLocationCallback();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.googleMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        ExtendedFloatingActionButton fabScan = view.findViewById(R.id.fabScan);
        if (fabScan != null) {
            fabScan.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(requireContext(), ScanActivity.class));
                    UiTransitions.openForward(requireActivity());
                } catch (Exception e) {
                    showToast("Unable to open scanner.");
                }
            });
        }

        setupLegend(view);

        View tvSearchHint = view.findViewById(R.id.tvSearchHint);
        View searchBar = view.findViewById(R.id.searchBar);
        if (tvSearchHint != null) tvSearchHint.setOnClickListener(v -> showSearchDialog());
        if (searchBar != null) searchBar.setOnClickListener(v -> showSearchDialog());

        View btnMapSettings = view.findViewById(R.id.btnMapSettings);
        if (btnMapSettings != null) btnMapSettings.setOnClickListener(v -> showMapSettingsDialog());

        View btnAppBarSettings = view.findViewById(R.id.btnAppBarSettings);
        if (btnAppBarSettings != null) btnAppBarSettings.setOnClickListener(v -> showMapSettingsDialog());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(NTU_CLIFTON)
                .zoom(16f)
                .tilt(35f)
                .bearing(0f)
                .build();
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);

        for (BuildingData b : buildings) {
            addMarker(map, b);
        }

        map.setOnMarkerClickListener(marker -> {
            try {
                MarkerData data = (MarkerData) marker.getTag();
                if (data != null) {
                    handlePinTapped(data.pinType, marker.getTitle() == null ? "" : marker.getTitle(), data.challengeId);
                }
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                return true;
            } catch (Exception e) {
                showToast("Unable to open this marker.");
                return true;
            }
        });

        if (hasLocationPermission()) {
            enableLocationFeatures();
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        if (hasLocationPermission()) {
            startRealtimeLocationUpdates();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopRealtimeLocationUpdates();
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location last = locationResult.getLastLocation();
                if (last != null) {
                    checkBuildingProximity(last);
                }
            }
        };
    }

    private void enableLocationFeatures() {
        if (googleMap == null || !hasLocationPermission()) return;
        try {
            googleMap.setMyLocationEnabled(true);
            startRealtimeLocationUpdates();
        } catch (SecurityException e) {
            showToast("Location permission issue.");
        } catch (Exception e) {
            showToast("Could not enable location.");
        }
    }

    private void startRealtimeLocationUpdates() {
        if (!hasLocationPermission() || fusedLocationClient == null || locationCallback == null) return;
        try {
            LocationRequest request = new LocationRequest.Builder(5000)
                    .setMinUpdateIntervalMillis(2500)
                    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                    .build();
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            showToast("Location permission issue.");
        } catch (Exception e) {
            showToast("Unable to start realtime location updates.");
        }
    }

    private void stopRealtimeLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            } catch (Exception ignored) {}
        }
    }

    private void checkBuildingProximity(@NonNull Location userLocation) {
        float[] results = new float[1];
        for (BuildingData b : buildings) {
            Location.distanceBetween(
                    userLocation.getLatitude(), userLocation.getLongitude(),
                    b.latLng.latitude, b.latLng.longitude, results
            );
            float distanceMeters = results[0];
            boolean isNear = distanceMeters <= PROXIMITY_METERS;
            if (isNear && !proximityNotified.contains(b.id)) {
                proximityNotified.add(b.id);
                showToast("You are near " + b.label + " (" + Math.round(distanceMeters) + "m)");
            } else if (!isNear && proximityNotified.contains(b.id) && distanceMeters > PROXIMITY_METERS + 30f) {
                proximityNotified.remove(b.id);
            }
        }
    }

    private void addMarker(@NonNull GoogleMap map, @NonNull BuildingData building) {
        float hue;
        switch (building.pinType) {
            case PIN_QR:     hue = BitmapDescriptorFactory.HUE_GREEN;  break;
            case PIN_BEACON: hue = BitmapDescriptorFactory.HUE_ORANGE; break;
            case PIN_LOCKED: hue = BitmapDescriptorFactory.HUE_VIOLET; break;
            case PIN_INFO:   hue = BitmapDescriptorFactory.HUE_CYAN;   break;
            default:         hue = BitmapDescriptorFactory.HUE_AZURE;  break;
        }
        Marker marker = map.addMarker(new MarkerOptions()
                .position(building.latLng)
                .title(building.label)
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        if (marker != null) {
            marker.setTag(new MarkerData(building.pinType, building.challengeId));
        }
    }

    private void handlePinTapped(int pinType, String label, @Nullable String challengeId) {
        if (pinType == PIN_LOCKED) {
            repo.countCompletedChallenges(new FirebaseRepository.Callback<Integer>() {
                @Override
                public void onSuccess(Integer done) {
                    if (done >= 20) {
                        openChallengeDetail(challengeId);
                    } else {
                        int remaining = 20 - done;
                        showToast("🔒 Complete " + remaining + " more challenge"
                                + (remaining != 1 ? "s" : "") + " to unlock " + label + ".");
                    }
                }
                @Override
                public void onError(String msg) {
                    showToast("Unable to check challenge progress.");
                }
            });
            return;
        }
        openChallengeDetail(challengeId);
    }

    private void openChallengeDetail(@Nullable String challengeId) {
        try {
            Intent intent = new Intent(requireContext(), ChallengeDetailActivity.class);
            if (challengeId != null) {
                intent.putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, challengeId);
            }
            startActivity(intent);
            UiTransitions.openForward(requireActivity());
        } catch (Exception e) {
            showToast("Unable to open challenge details.");
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void setupLegend(View view) {
        setLegendItem(view.findViewById(R.id.legendGps), 0xFF2196F3, "GPS");
        setLegendItem(view.findViewById(R.id.legendQr), 0xFF4CAF50, "QR Code");
        setLegendItem(view.findViewById(R.id.legendBeacon), 0xFFFF9800, "Beacon");
        setLegendItem(view.findViewById(R.id.legendBuilding), 0xFF9C27B0, "Locked");
    }

    private void setLegendItem(View container, int color, String label) {
        if (container == null) return;
        View dot = container.findViewById(R.id.legendDot);
        TextView tvLabel = container.findViewById(R.id.legendLabel);
        if (dot != null) {
            android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setColor(color);
            dot.setBackground(circle);
        }
        if (tvLabel != null) tvLabel.setText(label);
    }

    private void showSearchDialog() {
        Context ctx = requireContext();
        float dp = ctx.getResources().getDisplayMetrics().density;
        int pad = (int) (16 * dp);

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, 0);

        EditText editSearch = new EditText(ctx);
        editSearch.setHint("Search campus buildings…");
        editSearch.setTextColor(UiTheme.colorOnSurface(ctx));
        editSearch.setHintTextColor(UiTheme.textColorHint(ctx));
        editSearch.setSingleLine(true);
        layout.addView(editSearch);

        ListView listView = new ListView(ctx);
        listView.setPadding(0, (int) (8 * dp), 0, 0);
        layout.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (300 * dp)));

        String[] names = new String[buildings.length];
        for (int i = 0; i < buildings.length; i++) names[i] = buildings[i].label;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx,
                android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle("Search Locations")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .create();

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, v, position, id) -> {
            String selected = adapter.getItem(position);
            if (selected != null) {
                for (BuildingData b : buildings) {
                    if (b.label.equals(selected)) { zoomToBuilding(b); break; }
                }
            }
            dialog.dismiss();
        });

        dialog.show();
        SystemBarInsets.applyToDialog(dialog);
    }

    private void zoomToBuilding(BuildingData building) {
        if (googleMap == null) return;
        CameraPosition pos = new CameraPosition.Builder()
                .target(building.latLng)
                .zoom(18f)
                .tilt(45f)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
        showToast(building.label);
    }

    private void showMapSettingsDialog() {
        String[] types = {"Normal", "Satellite", "Hybrid", "Terrain"};
        int[] mapTypes = {
                GoogleMap.MAP_TYPE_NORMAL,
                GoogleMap.MAP_TYPE_SATELLITE,
                GoogleMap.MAP_TYPE_HYBRID,
                GoogleMap.MAP_TYPE_TERRAIN
        };

        int currentIdx = 0;
        if (googleMap != null) {
            int current = googleMap.getMapType();
            for (int i = 0; i < mapTypes.length; i++) {
                if (mapTypes[i] == current) { currentIdx = i; break; }
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Map Style")
                .setSingleChoiceItems(types, currentIdx, (dialog, which) -> {
                    if (googleMap != null) googleMap.setMapType(mapTypes[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showToast(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
