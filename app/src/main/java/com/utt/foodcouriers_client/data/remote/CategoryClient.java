package com.utt.foodcouriers_client.data.remote;

import com.utt.foodcouriers_client.data.model.Category;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CategoryClient extends BaseSupabaseClient {

    private static final String TAG = "CategoryClient";
    private static final String TABLE = "categories";
    private static CategoryClient instance;

    private CategoryClient() {
        super();
    }

    public static synchronized CategoryClient getInstance() {
        if (instance == null) {
            instance = new CategoryClient();
        }
        return instance;
    }

    public void fetchActiveCategories(ApiCallback<List<Category>> callback) {
        if (!SupabaseConfig.isConfigured()) {
            postError(callback, "Supabase is not configured");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/" + TABLE +
                "?select=*" +
                "&is_active=eq.true" +
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
                        postError(callback, parseRestError("Failed to fetch categories", response.code(), json));
                        return;
                    }

                    Category[] categories = gson.fromJson(json, Category[].class);
                    List<Category> categoryList = categories != null ? Arrays.asList(categories) : new ArrayList<>();
                    
                    postSuccess(callback, categoryList);
                } catch (Exception e) {
                    postError(callback, "Failed to parse categories");
                }
            }
        });
    }
}