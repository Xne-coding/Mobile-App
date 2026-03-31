package com.questlearn;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.HashSet;
import java.util.Set;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final LatLng NTU_CLIFTON = new LatLng(52.9126, -1.1866);
    private static final float PROXIMITY_METERS = 80f;

    private static final int PIN_GPS = 0;
    private static final int PIN_QR = 1;
    private static final int PIN_BEACON = 2;
    private static final int PIN_LOCKED = 4;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private final Set<String> proximityNotified = new HashSet<>();

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
            new BuildingData("clifton_library", "Clifton Library QR", new LatLng(52.9129, -1.1862), PIN_QR, "ch1"),
            new BuildingData("erasmus_darwin", "Erasmus Labs GPS", new LatLng(52.9135, -1.1875), PIN_GPS, "ch2"),
            new BuildingData("sports_village", "Sports Village Beacon", new LatLng(52.9118, -1.1880), PIN_BEACON, "ch3"),
            new BuildingData("clifton_centre", "Locked", new LatLng(52.9125, -1.1855), PIN_LOCKED, null)
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
                    requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } catch (Exception e) {
                    showToast("Unable to open scanner.");
                }
            });
        }

        setupLegend(view);
        View searchBar = view.findViewById(R.id.searchBar);
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> showToast("Search coming soon."));
        }
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
            } catch (Exception ignored) {
            }
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
            case PIN_QR:
                hue = BitmapDescriptorFactory.HUE_GREEN;
                break;
            case PIN_BEACON:
                hue = BitmapDescriptorFactory.HUE_ORANGE;
                break;
            case PIN_LOCKED:
                hue = BitmapDescriptorFactory.HUE_VIOLET;
                break;
            default:
                hue = BitmapDescriptorFactory.HUE_AZURE;
                break;
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
        switch (pinType) {
            case PIN_LOCKED:
                showToast(label + " is locked. Complete previous challenge first.");
                return;
            case PIN_BEACON:
                showToast("Beacon available near " + label + ".");
                return;
            default:
                try {
                    Intent intent = new Intent(requireContext(), ChallengeDetailActivity.class);
                    if (challengeId != null) {
                        intent.putExtra(ChallengeDetailActivity.EXTRA_CHALLENGE_ID, challengeId);
                    }
                    startActivity(intent);
                    requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } catch (Exception e) {
                    showToast("Unable to open challenge details.");
                }
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
        if (tvLabel != null) {
            tvLabel.setText(label);
        }
    }

    private void showToast(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}