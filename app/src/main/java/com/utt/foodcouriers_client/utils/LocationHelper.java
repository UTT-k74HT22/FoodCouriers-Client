package com.utt.foodcouriers_client.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationHelper {

    private static final String TAG = "LocationHelper";
    private static final long UPDATE_INTERVAL = 10000;
    private static final long FASTEST_INTERVAL = 5000;

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final Geocoder geocoder;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private LocationCallback locationCallback;
    private LocationListener listener;

    public interface LocationListener {
        void onLocationReceived(LocationData locationData);
        void onLocationError(String error);
    }

    public static class LocationData {
        public final double latitude;
        public final double longitude;
        public final float accuracy;
        public final String address;

        public LocationData(double latitude, double longitude, float accuracy, String address) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
            this.address = address;
        }
    }

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.geocoder = new Geocoder(context, Locale.getDefault());
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setLocationListener(LocationListener listener) {
        this.listener = listener;
    }

    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void getLastLocation() {
        if (!hasLocationPermission()) {
            if (listener != null) {
                listener.onLocationError("Location permission not granted");
            }
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            processLocation(location);
                        } else {
                            requestNewLocation();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "getLastLocation failed", e);
                        if (listener != null) {
                            listener.onLocationError("Cannot get last location: " + e.getMessage());
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception", e);
            if (listener != null) {
                listener.onLocationError("Permission denied");
            }
        }
    }

    private void requestNewLocation() {
        if (!hasLocationPermission()) {
            if (listener != null) {
                listener.onLocationError("Location permission not granted");
            }
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    processLocation(location);
                    stopLocationUpdates();
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception", e);
            if (listener != null) {
                listener.onLocationError("Permission denied");
            }
        }
    }

private void processLocation(Location location) {
        if (executor.isShutdown()) {
            return;
        }

        final double lat = location.getLatitude();
        final double lng = location.getLongitude();
        final float acc = (float) location.getAccuracy();

        executor.execute(() -> {
            if (executor.isShutdown()) {
                return;
            }
            String resolvedAddress = "";
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    resolvedAddress = addr.getAddressLine(0);
                    if (resolvedAddress == null || resolvedAddress.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        if (addr.getThoroughfare() != null) {
                            sb.append(addr.getThoroughfare());
                        }
                        if (addr.getSubThoroughfare() != null) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(addr.getSubThoroughfare());
                        }
                        if (addr.getLocality() != null) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(addr.getLocality());
                        }
                        resolvedAddress = sb.toString();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoder failed", e);
            }

            final String finalAddress = resolvedAddress;
            LocationData locationData = new LocationData(lat, lng, acc, finalAddress);

            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onLocationReceived(locationData);
                }
            });
        });
    }

    public void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    public void destroy() {
        stopLocationUpdates();
        executor.shutdown();
    }
}