package com.utt.foodcouriers_client.data.remote;

import com.utt.foodcouriers_client.data.model.Restaurant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class RestaurantClient extends BaseSupabaseClient {

    private static final String TAG = "RestaurantClient";
    private static final String TABLE = "restaurants";
    private static RestaurantClient instance;

    private RestaurantClient() {
        super();
    }

    public static synchronized RestaurantClient getInstance() {
        if (instance == null) {
            instance = new RestaurantClient();
        }
        return instance;
    }

    public void fetchActiveRestaurants(ApiCallback<List<Restaurant>> callback) {
        fetchRestaurants(null, null, null, 50, 0, callback);
    }

    public void fetchRestaurantsByCategory(String categoryId, ApiCallback<List<Restaurant>> callback) {
        fetchRestaurants(null, categoryId, null, 50, 0, callback);
    }

    public void fetchPopularRestaurants(ApiCallback<List<Restaurant>> callback) {
        fetchRestaurants("popular", null, null, 10, 0, callback);
    }

    public void fetchNearbyRestaurants(ApiCallback<List<Restaurant>> callback) {
        fetchRestaurants("nearby", null, null, 20, 0, callback);
    }

    private void fetchRestaurants(
            String filterType,
            String categoryId,
            Boolean isActive,
            int limit,
            int offset,
            ApiCallback<List<Restaurant>> callback
    ) {
        if (!SupabaseConfig.isConfigured()) {
            postError(callback, "Supabase is not configured");
            return;
        }

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(SupabaseConfig.REST_URL).append("/").append(TABLE);
        urlBuilder.append("?select=*");

        if (isActive != null) {
            urlBuilder.append("&is_active=eq.").append(isActive ? "true" : "false");
        }

        if ("popular".equals(filterType)) {
            urlBuilder.append("&order=rating.desc");
        } else if ("nearby".equals(filterType)) {
            urlBuilder.append("&order=created_at.desc");
        } else {
            urlBuilder.append("&order=name.asc");
        }

        if (limit > 0) {
            urlBuilder.append("&limit=").append(limit);
        }
        if (offset > 0) {
            urlBuilder.append("&offset=").append(offset);
        }

        String url = urlBuilder.toString();

        String accessToken = AuthClient.getInstance().getAccessToken();
        String authToken = (accessToken != null && !accessToken.isEmpty()) 
                ? accessToken 
                : SupabaseConfig.SUPABASE_ANON_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + authToken)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
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
                        postError(callback, parseRestError("Failed to fetch restaurants", response.code(), json));
                        return;
                    }

                    Restaurant[] restaurants = gson.fromJson(json, Restaurant[].class);
                    List<Restaurant> restaurantList = restaurants != null ? Arrays.asList(restaurants) : new ArrayList<>();
                    
                    postSuccess(callback, restaurantList);
                } catch (Exception e) {
                    postError(callback, "Failed to parse restaurants");
                }
            }
        });
    }
}