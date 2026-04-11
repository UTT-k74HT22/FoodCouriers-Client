package com.utt.foodcouriers_client.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.BannerItem;
import com.utt.foodcouriers_client.data.repository.CatalogRepository;

import java.util.List;

public class HomeViewModel extends BaseViewModel {

    private final MutableLiveData<List<BannerItem>> banners = new MutableLiveData<>();
    private final CatalogRepository catalogRepository;

    public HomeViewModel() {
        this.catalogRepository = CatalogRepository.getInstance();
    }

    public MutableLiveData<List<BannerItem>> getBanners() {
        return banners;
    }

    public void loadBanners() {
        setLoading(true);
        errorMessage.setValue(null);
        
        catalogRepository.getBannersWithTiming(new RepositoryCallback<List<BannerItem>>() {
            @Override
            public void onSuccess(List<BannerItem> result) {
                setLoading(false);
                banners.postValue(result);
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }

    public void refreshBanners() {
        loadBanners();
    }
}
