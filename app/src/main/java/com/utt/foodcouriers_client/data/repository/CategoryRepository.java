package com.utt.foodcouriers_client.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public class CategoryRepository extends BaseSupabaseRepository {

    public static final String TAG = "category_repository";
    private static final String TABLE = "categories";
    private static CategoryRepository instance;

    public static synchronized CategoryRepository getInstance() {
        if (instance == null) {
            instance = new CategoryRepository();
        }
        return instance;
    }

    public void getCategories(RepositoryCallback<List<FoodCategory>> callback) {

        Log.d(TAG, "Fetching categories from Supabase");

        String url = SupabaseConfig.REST_URL + "/" + TABLE
                + "?select=id, name, image_url, sort_order, description"
                + "&is_active=eq.true"
                + "&order=sort_order.asc, name.asc";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to fetch categories", e);
                mainHandler.post(() -> callback.onError("Failed to fetch categories"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "Received response for categories: " + response.code());
                String body = response.body().string();
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Failed to fetch categories: " + body);
                    mainHandler.post(() -> callback.onError("Failed to fetch categories"));
                    return;
                }
                List<FoodCategory> foodCategoryList = parseCategories(new JsonParser().parse(body).getAsJsonArray());
                mainHandler.post(() -> callback.onSuccess(foodCategoryList));
            }
        });
    }

    private List<FoodCategory> parseCategories(JsonArray array) {
        List<FoodCategory> categories = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            FoodCategory category = new FoodCategory();
            category.setId(object.get("id").getAsString());
            category.setName(object.get("name").getAsString());
            category.setImageUrl(object.get("image_url").getAsString());
            category.setSortOrder(object.get("sort_order").getAsInt());
            category.setDescription(object.get("description").getAsString());
            categories.add(category);
        }
        categories.sort(Comparator.comparingInt(FoodCategory::getSortOrder));
        return categories;
    }
}