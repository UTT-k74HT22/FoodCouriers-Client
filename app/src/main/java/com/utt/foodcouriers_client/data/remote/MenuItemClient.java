package com.utt.foodcouriers_client.data.remote;

import com.utt.foodcouriers_client.data.model.MenuItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MenuItemClient extends BaseSupabaseClient {

    private static final String TAG = "MenuItemClient";
    private static final String TABLE = "menu_items";
    private static MenuItemClient instance;

    private MenuItemClient() {
        super();
    }

    public static synchronized MenuItemClient getInstance() {
        if (instance == null) {
            instance = new MenuItemClient();
        }
        return instance;
    }

    public void fetchPopularMenuItems(ApiCallback<List<MenuItem>> callback) {
        if (!SupabaseConfig.isConfigured()) {
            postError(callback, "Supabase is not configured");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/" + TABLE +
                "?select=*,categories(name)" +
                "&is_available=eq.true" +
                "&is_featured=eq.true" +
                "&limit=10" +
                "&order=sort_order.asc";

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
                        postError(callback, parseRestError("Failed to fetch menu items", response.code(), json));
                        return;
                    }

                    MenuItem[] menuItems = gson.fromJson(json, MenuItem[].class);
                    List<MenuItem> itemList = menuItems != null ? Arrays.asList(menuItems) : new ArrayList<>();
                    
                    postSuccess(callback, itemList);
                } catch (Exception e) {
                    postError(callback, "Failed to parse menu items");
                }
            }
        });
    }

    public void fetchMenuItemsByCategory(String categoryId, ApiCallback<List<MenuItem>> callback) {
        if (!SupabaseConfig.isConfigured()) {
            postError(callback, "Supabase is not configured");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/" + TABLE +
                "?select=*" +
                "&category_id=eq." + categoryId +
                "&is_available=eq.true" +
                "&order=sort_order.asc";

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
                        postError(callback, parseRestError("Failed to fetch menu items", response.code(), json));
                        return;
                    }

                    MenuItem[] menuItems = gson.fromJson(json, MenuItem[].class);
                    List<MenuItem> itemList = menuItems != null ? Arrays.asList(menuItems) : new ArrayList<>();
                    
                    postSuccess(callback, itemList);
                } catch (Exception e) {
                    postError(callback, "Failed to parse menu items");
                }
            }
        });
    }

    public void fetchMenuItemsByRestaurant(String restaurantId, ApiCallback<List<MenuItem>> callback) {
        if (!SupabaseConfig.isConfigured()) {
            postError(callback, "Supabase is not configured");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/" + TABLE +
                "?select=*" +
                "&restaurant_id=eq." + restaurantId +
                "&is_available=eq.true" +
                "&order=sort_order.asc";

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
                        postError(callback, parseRestError("Failed to fetch menu items", response.code(), json));
                        return;
                    }

                    MenuItem[] menuItems = gson.fromJson(json, MenuItem[].class);
                    List<MenuItem> itemList = menuItems != null ? Arrays.asList(menuItems) : new ArrayList<>();
                    
                    postSuccess(callback, itemList);
                } catch (Exception e) {
                    postError(callback, "Failed to parse menu items");
                }
            }
        });
    }
}