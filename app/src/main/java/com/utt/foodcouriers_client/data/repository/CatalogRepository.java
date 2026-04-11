package com.utt.foodcouriers_client.data.repository;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.BannerItem;
import com.utt.foodcouriers_client.data.model.Category;
import com.utt.foodcouriers_client.data.remote.BannerClient;
import com.utt.foodcouriers_client.data.remote.CategoryClient;

import java.util.List;

public class CatalogRepository {

    private static CatalogRepository instance;
    private final BannerClient bannerClient;
    private final CategoryClient categoryClient;

    public static synchronized CatalogRepository getInstance() {
        if (instance == null) {
            instance = new CatalogRepository();
        }
        return instance;
    }

    private CatalogRepository() {
        this.bannerClient = BannerClient.getInstance();
        this.categoryClient = CategoryClient.getInstance();
    }

    public void getActiveBanners(RepositoryCallback<List<BannerItem>> callback) {
        bannerClient.fetchActiveBanners(new BannerClient.ApiCallback<List<BannerItem>>() {
            @Override
            public void onSuccess(List<BannerItem> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getBannersWithTiming(RepositoryCallback<List<BannerItem>> callback) {
        bannerClient.fetchBannersWithTiming(new BannerClient.ApiCallback<List<BannerItem>>() {
            @Override
            public void onSuccess(List<BannerItem> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getActiveCategories(RepositoryCallback<List<Category>> callback) {
        categoryClient.fetchActiveCategories(new CategoryClient.ApiCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}
