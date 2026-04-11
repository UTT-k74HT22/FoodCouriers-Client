package com.utt.foodcouriers_client.data.repository;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.FoodCategory;
import com.utt.foodcouriers_client.data.remote.SupabaseConfig;
import com.utt.foodcouriers_client.data.repository.base.BaseSupabaseRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class CatalogRepository extends BaseSupabaseRepository {

    private static final String TAG = "CatalogRepository";
    private static CatalogRepository instance;

    public static synchronized CatalogRepository getInstance() {
        if (instance == null) {
            instance = new CatalogRepository();
        }
        return instance;
    }

    public void getCategories(RepositoryCallback<List<FoodCategory>> callback) {
        if (!isRemoteConfigured()) {
            postRepositorySuccess(callback, getFallbackCategories());
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/categories?select=id,name,image_url,sort_order,is_active&is_active=eq.true&order=sort_order.asc.nullslast,name.asc")
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "getCategories:onFailure", e);
                postRepositoryError(callback, "Unable to load categories from Supabase.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String body = response.body() != null ? response.body().string() : "[]";
                    if (!response.isSuccessful()) {
                        postRepositoryError(
                                callback,
                                parseRestError("Unable to load categories from Supabase.", response.code(), body)
                        );
                        return;
                    }

                    List<FoodCategory> categories = parseCategories(body);
                    if (categories.isEmpty()) {
                        categories = getFallbackCategories();
                    }
                    postRepositorySuccess(callback, categories);
                }
            }
        });
    }

    private List<FoodCategory> parseCategories(String json) {
        List<FoodCategory> categories = new ArrayList<>();
        try {
            JsonArray array = gson.fromJson(json, JsonArray.class);
            if (array == null) {
                return categories;
            }
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                categories.add(new FoodCategory(
                        getString(object, "id"),
                        getString(object, "name"),
                        SupabaseConfig.buildPublicImageUrl(getString(object, "image_url")),
                        getInt(object, "sort_order")
                ));
            }
        } catch (Exception exception) {
            Log.e(TAG, "parseCategories:failed", exception);
        }

        categories.sort(Comparator.comparingInt(FoodCategory::getSortOrder).thenComparing(FoodCategory::getName));
        return categories;
    }

    private List<FoodCategory> getFallbackCategories() {
        List<FoodCategory> fallback = new ArrayList<>();
        fallback.add(new FoodCategory("main-dish", "Món chính", null, 1));
        fallback.add(new FoodCategory("side-dish", "Món phụ", null, 2));
        fallback.add(new FoodCategory("drink", "Đồ uống", null, 3));
        fallback.add(new FoodCategory("dessert", "Tráng miệng", null, 4));
        fallback.add(new FoodCategory("combo", "Combo", null, 5));
        return fallback;
    }

    private String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private int getInt(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return 0;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception exception) {
            return 0;
        }
    }

    private <T> void postRepositorySuccess(RepositoryCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void postRepositoryError(RepositoryCallback<T> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
}
