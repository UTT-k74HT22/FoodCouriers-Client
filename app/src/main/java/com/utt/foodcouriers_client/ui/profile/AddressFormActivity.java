package com.utt.foodcouriers_client.ui.profile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.Address;
import com.utt.foodcouriers_client.data.remote.AddressClient;
import com.utt.foodcouriers_client.databinding.ActivityAddressFormBinding;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AddressFormActivity extends BaseActivity {

    private ActivityAddressFormBinding binding;
    private SessionManager sessionManager;
    private Address editingAddress;
    private boolean isEditMode = false;
    private int selectedLabelPosition = 0;
    private double currentLatitude = 21.0285;
    private double currentLongitude = 105.8542;
    private Marker currentMarker;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final OkHttpClient httpClient = new OkHttpClient();
    private AddressSuggestionAdapter suggestionAdapter;
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;
    private AutoCompleteTextView etAddress;

    private final String[] labelOptions = {"Nhà", "Công ty", "Khác"};

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocation = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarseLocation = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                if (fineLocation != null && fineLocation || coarseLocation != null && coarseLocation) {
                    getCurrentLocation();
                } else {
                    ToastBanner.showError("Cần quyền vị trí để lấy địa chỉ");
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        binding = ActivityAddressFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

sessionManager = SessionManager.getInstance(this);

        suggestionAdapter = new AddressSuggestionAdapter(this, new ArrayList<>());
        
        if (binding.etAddress instanceof AutoCompleteTextView) {
            etAddress = (AutoCompleteTextView) binding.etAddress;
            etAddress.setAdapter(suggestionAdapter);
            etAddress.setThreshold(1);
            etAddress.setOnItemClickListener((parent, view, position, id) -> {
                AddressSuggestionAdapter.SuggestionItem item = suggestionAdapter.getItem(position);
                if (item != null) {
                    etAddress.setText(item.displayName);
                    selectAddress(item.lat, item.lon);
                }
            });
        }

        setupMap();
        setupLabelSpinner();
        setupClickListeners();
        loadExistingAddress();
    }

    private void setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.setMultiTouchControls(true);
        binding.mapView.getController().setZoom(15.0);
        
        GeoPoint startPoint = new GeoPoint(currentLatitude, currentLongitude);
        binding.mapView.getController().setCenter(startPoint);
        
        addMarker(startPoint);
        
        binding.mapView.setOnClickListener(v -> {
            android.graphics.PointF tapPoint = new android.graphics.PointF(
                binding.mapView.getWidth() / 2f,
                binding.mapView.getHeight() / 2f
            );
            GeoPoint tapGeoPoint = (GeoPoint) binding.mapView.getProjection().fromPixels(
                (int) tapPoint.x, (int) tapPoint.y
            );
            currentLatitude = tapGeoPoint.getLatitude();
            currentLongitude = tapGeoPoint.getLongitude();
            addMarker(tapGeoPoint);
            updateCoordinatesText();
            reverseGeocode(currentLatitude, currentLongitude);
        });
    }

    private void addMarker(GeoPoint point) {
        if (currentMarker != null) {
            binding.mapView.getOverlays().remove(currentMarker);
        }
        
        currentMarker = new Marker(binding.mapView);
        currentMarker.setPosition(point);
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentMarker.setDraggable(true);
        
        currentMarker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                currentLatitude = marker.getPosition().getLatitude();
                currentLongitude = marker.getPosition().getLongitude();
                updateCoordinatesText();
                reverseGeocode(marker.getPosition().getLatitude(), marker.getPosition().getLongitude());
            }

            @Override
            public void onMarkerDragStart(Marker marker) {
            }
        });
        
        binding.mapView.getOverlays().add(currentMarker);
        binding.mapView.invalidate();
    }

    private void setupLabelSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                labelOptions
        );
        binding.spinnerLabel.setAdapter(adapter);
        binding.spinnerLabel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLabelPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        binding.btnCancel.setOnClickListener(v -> finish());

        binding.btnSave.setOnClickListener(v -> saveAddress());

        binding.btnSearchAddress.setOnClickListener(v -> searchAddress());

        if (etAddress != null) {
            etAddress.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (suggestionAdapter == null) return;
                    String text = s.toString().trim();
                    
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    
                    if (text.length() >= 3) {
                        searchRunnable = () -> searchSuggestions(text);
                        searchHandler.postDelayed(searchRunnable, 500);
                    } else {
                        suggestionAdapter.clearItems();
                    }
                }
            });
        }
    }

    private void searchAddress() {
        String address = etAddress.getText().toString().trim();
        String district = binding.etDistrict.getText().toString().trim();
        String city = binding.etCity.getText().toString().trim();
        
        if (TextUtils.isEmpty(address)) {
            etAddress.setHint(getString(R.string.auth_validation_required));
            etAddress.requestFocus();
            return;
        }
        
        String searchQuery = buildSearchQuery(address, district, city);
        
        binding.progressLocation.setVisibility(View.VISIBLE);
        
        geocodeAddress(searchQuery);
    }
    
    private String buildSearchQuery(String address, String district, String city) {
        StringBuilder sb = new StringBuilder(address);
        
        if (!TextUtils.isEmpty(district)) {
            sb.append(", ").append(district);
        }
        
        if (!TextUtils.isEmpty(city)) {
            sb.append(", ").append(city);
        } else {
            sb.append(", Hà Nội");
        }
        
        sb.append(", Việt Nam");
        
        return sb.toString();
    }
    
    private void geocodeAddress(String query) {
        binding.progressLocation.setVisibility(View.VISIBLE);
        
        executor.execute(() -> {
            try {
                String url = "https://nominatim.openstreetmap.org/search?format=json&q=" 
                        + java.net.URLEncoder.encode(query, "UTF-8")
                        + "&limit=5&addressdetails=1";
                
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "FoodCouriersApp/1.0")
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        binding.progressLocation.setVisibility(View.GONE);
                        ToastBanner.showError("Lỗi kết nối");
                    });
                    return;
                }
                
                String json = response.body() != null ? response.body().string() : "";
                response.close();
                
                if (TextUtils.isEmpty(json) || json.equals("[]")) {
                    runOnUiThread(() -> {
                        binding.progressLocation.setVisibility(View.GONE);
                        ToastBanner.showError("Không tìm thấy địa chỉ");
                    });
                    return;
                }
                
                parseGeocodingResult(json);
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    binding.progressLocation.setVisibility(View.GONE);
                    ToastBanner.showError("Lỗi: " + e.getMessage());
                });
            }
        });
    }
    
    private void parseGeocodingResult(String json) {
        try {
            if (json.contains("\"lat\"") && json.contains("\"lon\"")) {
                int latStart = json.indexOf("\"lat\":\"");
                int lonStart = json.indexOf("\"lon\":\"");
                
                if (latStart == -1 || lonStart == -1) {
                    showError("Không tìm thấy địa chỉ");
                    return;
                }
                
                latStart += 7;
                lonStart += 7;
                
                int latEnd = json.indexOf("\"", latStart);
                int lonEnd = json.indexOf("\"", lonStart);
                
                if (latEnd <= latStart || lonEnd <= lonStart) {
                    showError("Không tìm thấy địa chỉ");
                    return;
                }
                
                double lat = Double.parseDouble(json.substring(latStart, latEnd));
                double lon = Double.parseDouble(json.substring(lonStart, lonEnd));
                
                String displayName = "";
                int nameStart = json.indexOf("\"display_name\":\"");
                if (nameStart != -1) {
                    nameStart += 16;
                    int nameEnd = json.indexOf("\"", nameStart);
                    if (nameEnd > nameStart) {
                        displayName = json.substring(nameStart, nameEnd);
                    }
                }
                
                final String finalAddress = displayName;
                runOnUiThread(() -> {
                    currentLatitude = lat;
                    currentLongitude = lon;
                    
                    GeoPoint point = new GeoPoint(lat, lon);
                    addMarker(point);
                    binding.mapView.getController().animateTo(point);
                    binding.mapView.getController().setZoom(16.0);
                    updateCoordinatesText();
                    
                    if (!TextUtils.isEmpty(finalAddress)) {
                        etAddress.setText(finalAddress);
                    }
                    
                    binding.progressLocation.setVisibility(View.GONE);
                    ToastBanner.showSuccess("Đã tìm thấy địa chỉ");
                });
            } else {
                showError("Không tìm thấy địa chỉ");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi xử lý địa chỉ");
        }
    }
    
    private void showError(String message) {
        runOnUiThread(() -> {
            binding.progressLocation.setVisibility(View.GONE);
            ToastBanner.showError(message);
        });
    }

    private void reverseGeocode(double lat, double lon) {
        executor.execute(() -> {
            try {
                String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" 
                        + lat + "&lon=" + lon + "&addressdetails=1";
                
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "FoodCouriersApp/1.0")
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    String json = response.body().string();
                    
                    if (json.contains("\"display_name\"")) {
                        int start = json.indexOf("\"display_name\":\"") + 16;
                        int end = json.indexOf("\"", start);
                        String displayName = json.substring(start, end);
                        
                        runOnUiThread(() -> {
                            etAddress.setText(displayName);
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }
        
        binding.progressLocation.setVisibility(View.VISIBLE);
        
        try {
            android.location.LocationManager locationManager = 
                    (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
            
            android.location.Location location = locationManager.getLastKnownLocation(
                    android.location.LocationManager.GPS_PROVIDER);
            
            if (location == null) {
                location = locationManager.getLastKnownLocation(
                        android.location.LocationManager.NETWORK_PROVIDER);
            }
            
            if (location != null) {
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
                
                GeoPoint point = new GeoPoint(currentLatitude, currentLongitude);
                addMarker(point);
                binding.mapView.getController().animateTo(point);
                updateCoordinatesText();
                
                reverseGeocode(currentLatitude, currentLongitude);
            }
            
            binding.progressLocation.setVisibility(View.GONE);
            
        } catch (SecurityException e) {
            ToastBanner.showError("Không có quyền truy cập vị trí");
            binding.progressLocation.setVisibility(View.GONE);
        }
    }

    private void updateCoordinatesText() {
        binding.tvCoordinates.setText(String.format(Locale.getDefault(), 
                "Vĩ độ: %.6f, Kinh độ: %.6f", currentLatitude, currentLongitude));
    }

    private void loadExistingAddress() {
        if (getIntent().hasExtra("address")) {
            editingAddress = (Address) getIntent().getSerializableExtra("address");
            if (editingAddress != null) {
                isEditMode = true;
                binding.tvTitle.setText(R.string.address_edit_title);
                bindAddressData(editingAddress);
            }
        }
    }

    private void bindAddressData(Address address) {
        for (int i = 0; i < labelOptions.length; i++) {
            if (labelOptions[i].equals(address.getLabel())) {
                binding.spinnerLabel.setSelection(i);
                break;
            }
        }
        etAddress.setText(address.getFullAddress());
        binding.etDistrict.setText(address.getDistrict());
        binding.etCity.setText(address.getCity());
        binding.cbDefault.setChecked(address.isDefault());
        
        if (address.getLatitude() != null && address.getLongitude() != null) {
            currentLatitude = address.getLatitude();
            currentLongitude = address.getLongitude();
            
            GeoPoint point = new GeoPoint(currentLatitude, currentLongitude);
            addMarker(point);
            binding.mapView.getController().animateTo(point);
            updateCoordinatesText();
        }
    }

    private void saveAddress() {
        String label = labelOptions[selectedLabelPosition];
        String fullAddress = etAddress.getText().toString().trim();
        String district = binding.etDistrict.getText().toString().trim();
        String city = binding.etCity.getText().toString().trim();
        boolean isDefault = binding.cbDefault.isChecked();

        if (TextUtils.isEmpty(fullAddress)) {
            etAddress.setHint(getString(R.string.auth_validation_required));
            etAddress.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(city)) {
            city = "Hà Nội";
        }

        showLoading(true);

        String userId = sessionManager.getUserId();
        if (userId == null) {
            showLoading(false);
            ToastBanner.showError(getString(R.string.error_title));
            return;
        }

        if (isEditMode && editingAddress != null) {
            AddressClient.getInstance().updateAddressWithCoordinates(
                    editingAddress.getId(),
                    label, fullAddress, district, city, isDefault,
                    currentLatitude, currentLongitude,
                    new AddressClient.ApiCallback<Address>() {
                        @Override
                        public void onSuccess(Address result) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                ToastBanner.showSuccess(getString(R.string.address_update_success));
                                binding.getRoot().postDelayed(() -> {
                                    setResult(RESULT_OK);
                                    finish();
                                }, 800);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                ToastBanner.showError(error);
                            });
                        }
                    }
            );
        } else {
            AddressClient.getInstance().createAddressWithCoordinates(
                    userId, label, fullAddress, district, city, isDefault,
                    currentLatitude, currentLongitude,
                    new AddressClient.ApiCallback<Address>() {
                        @Override
                        public void onSuccess(Address result) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                ToastBanner.showSuccess(getString(R.string.address_create_success));
                                binding.getRoot().postDelayed(() -> {
                                    setResult(RESULT_OK);
                                    finish();
                                }, 800);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                ToastBanner.showError(error);
                            });
                        }
                    }
            );
        }
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!show);
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        binding = null;
    }

    private void searchSuggestions(String query) {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        
        searchRunnable = () -> {
            executor.execute(() -> {
                try {
                    android.location.Geocoder geocoder = new android.location.Geocoder(getApplicationContext(), Locale.getDefault());
                    
                    @SuppressWarnings("deprecation")
                    List<android.location.Address> addresses = geocoder.getFromLocationName(query + ", Vietnam", 5);
                    
                    List<AddressSuggestionAdapter.SuggestionItem> items = new ArrayList<>();
                    
                    if (addresses != null && !addresses.isEmpty()) {
                        for (android.location.Address addr : addresses) {
                            String fullAddress = addr.getAddressLine(0);
                            if (fullAddress != null && !fullAddress.isEmpty()) {
                                items.add(new AddressSuggestionAdapter.SuggestionItem(
                                    fullAddress, 
                                    addr.getLatitude(), 
                                    addr.getLongitude()
                                ));
                            }
                        }
                    }
                    
                    final List<AddressSuggestionAdapter.SuggestionItem> finalItems = items;
                    runOnUiThread(() -> {
                        suggestionAdapter.updateItems(finalItems);
                    });
                    
                } catch (Exception e) {
                    android.util.Log.e("AddressForm", "Geocoder error: " + e.getMessage());
                }
            });
        };
        
        searchHandler.postDelayed(searchRunnable, 500);
    }

    private void selectAddress(double lat, double lon) {
        currentLatitude = lat;
        currentLongitude = lon;
        
        GeoPoint point = new GeoPoint(lat, lon);
        addMarker(point);
        binding.mapView.getController().animateTo(point);
        binding.mapView.getController().setZoom(16.0);
        updateCoordinatesText();
        
        binding.progressLocation.setVisibility(View.GONE);
    }

    static class AddressSuggestionAdapter extends ArrayAdapter<AddressSuggestionAdapter.SuggestionItem> implements Filterable {
        
        static class SuggestionItem {
            String displayName;
            double lat;
            double lon;
            
            SuggestionItem(String displayName, double lat, double lon) {
                this.displayName = displayName;
                this.lat = lat;
                this.lon = lon;
            }
            
            @NonNull
            @Override
            public String toString() {
                return displayName;
            }
        }
        
        private List<SuggestionItem> items = new ArrayList<>();
        
        AddressSuggestionAdapter(@NonNull Context context, List<SuggestionItem> items) {
            super(context, android.R.layout.simple_dropdown_item_1line, items);
            this.items = new ArrayList<>(items);
        }
        
        void updateItems(List<SuggestionItem> newItems) {
            this.items = new ArrayList<>(newItems);
            clear();
            addAll(newItems);
            notifyDataSetChanged();
        }
        
        void clearItems() {
            items.clear();
            clear();
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    return null;
                }
                
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                }
            };
        }
    }
}