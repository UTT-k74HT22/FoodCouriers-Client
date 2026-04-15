package com.utt.foodcouriers_client.data.remote;

import android.util.Log;

import com.utt.foodcouriers_client.data.model.Address;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AddressClient extends BaseSupabaseClient {

    private static AddressClient instance;

    private AddressClient() {
        super();
    }

    public static synchronized AddressClient getInstance() {
        if (instance == null) {
            instance = new AddressClient();
        }
        return instance;
    }

    private void syncSession() {
        AuthClient authClient = AuthClient.getInstance();
        this.accessToken = authClient.getAccessToken();
        this.refreshToken = authClient.getRefreshToken();
    }

    public void getAddressesByUserId(String userId, ApiCallback<Address[]> callback) {
        syncSession();
        
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/user_addresses?user_id=eq." + userId + "&select=*&order=created_at.desc")
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (!response.isSuccessful()) {
                        postError(callback, parseRestError("Failed to get addresses", response.code(), json));
                        return;
                    }

                    Address[] addresses = gson.fromJson(json, Address[].class);
                    postSuccess(callback, addresses != null ? addresses : new Address[0]);
                }
            }
        });
    }

    public void createAddress(String userId, String label, String fullAddress, 
                             String district, String city, boolean isDefault, ApiCallback<Address> callback) {
        createAddressWithCoordinates(userId, label, fullAddress, district, city, isDefault, 0, 0, callback);
    }

    public void createAddressWithCoordinates(String userId, String label, String fullAddress, 
                             String district, String city, boolean isDefault, 
                             double latitude, double longitude, ApiCallback<Address> callback) {
        syncSession();
        
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }

        Map<String, Object> address = new HashMap<>();
        address.put("user_id", userId);
        address.put("label", label);
        address.put("full_address", fullAddress);
        address.put("district", district);
        address.put("city", city != null ? city : "HCM");
        address.put("is_default", isDefault);
        
        if (latitude != 0 && longitude != 0) {
            address.put("latitude", latitude);
            address.put("longitude", longitude);
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/user_addresses")
                .post(RequestBody.create(gson.toJson(address), MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_PREFER, SupabaseConfig.PREF_RETURN_REPRESENTATION)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (!response.isSuccessful()) {
                        postError(callback, parseRestError("Failed to create address", response.code(), json));
                        return;
                    }

                    Address[] addresses = gson.fromJson(json, Address[].class);
                    if (addresses == null || addresses.length == 0) {
                        postError(callback, "Failed to create address");
                        return;
                    }
                    postSuccess(callback, addresses[0]);
                }
            }
        });
    }

    public void updateAddress(String addressId, String label, String fullAddress,
                            String district, String city, boolean isDefault, ApiCallback<Address> callback) {
        updateAddressWithCoordinates(addressId, label, fullAddress, district, city, isDefault, 0, 0, callback);
    }

    public void updateAddressWithCoordinates(String addressId, String label, String fullAddress,
                            String district, String city, boolean isDefault,
                            double latitude, double longitude, ApiCallback<Address> callback) {
        syncSession();
        
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }

        Map<String, Object> address = new HashMap<>();
        if (label != null) address.put("label", label);
        if (fullAddress != null) address.put("full_address", fullAddress);
        if (district != null) address.put("district", district);
        if (city != null) address.put("city", city);
        address.put("is_default", isDefault);
        
        if (latitude != 0 && longitude != 0) {
            address.put("latitude", latitude);
            address.put("longitude", longitude);
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/user_addresses?id=eq." + addressId)
                .patch(RequestBody.create(gson.toJson(address), MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_PREFER, SupabaseConfig.PREF_RETURN_REPRESENTATION)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (!response.isSuccessful()) {
                        postError(callback, parseRestError("Failed to update address", response.code(), json));
                        return;
                    }

                    Address[] addresses = gson.fromJson(json, Address[].class);
                    if (addresses == null || addresses.length == 0) {
                        postError(callback, "Failed to update address");
                        return;
                    }
                    postSuccess(callback, addresses[0]);
                }
            }
        });
    }

    public void deleteAddress(String addressId, ApiCallback<Void> callback) {
        syncSession();
        
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/user_addresses?id=eq." + addressId)
                .delete()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        postSuccess(callback, null);
                    } else {
                        String json = responseBody != null ? responseBody.string() : "";
                        postError(callback, parseRestError("Failed to delete address", response.code(), json));
                    }
                }
            }
        });
    }

    public void setDefaultAddress(String userId, String addressId, ApiCallback<Void> callback) {
        syncSession();
        
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }

        getAddressesByUserId(userId, new ApiCallback<Address[]>() {
            @Override
            public void onSuccess(Address[] addresses) {
                if (addresses == null || addresses.length == 0) {
                    postError(callback, "No addresses found");
                    return;
                }

                updateAllAddressesToNonDefault(addresses, addressId, callback);
            }

            @Override
            public void onError(String error) {
                postError(callback, error);
            }
        });
    }

    private void updateAllAddressesToNonDefault(Address[] addresses, String defaultAddressId, ApiCallback<Void> callback) {
        int[] remaining = {addresses.length};
        boolean[] hasError = {false};

        for (Address addr : addresses) {
            boolean shouldBeDefault = addr.getId().equals(defaultAddressId);
            updateAddress(addr.getId(), addr.getLabel(), addr.getFullAddress(),
                    addr.getDistrict(), addr.getCity(), shouldBeDefault, new ApiCallback<Address>() {
                @Override
                public void onSuccess(Address result) {
                    remaining[0]--;
                    if (remaining[0] == 0 && !hasError[0]) {
                        postSuccess(callback, null);
                    }
                }

                @Override
                public void onError(String error) {
                    if (!hasError[0]) {
                        hasError[0] = true;
                        postError(callback, error);
                    }
                }
            });
        }
    }
}
