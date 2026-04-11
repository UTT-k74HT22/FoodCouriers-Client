package com.utt.foodcouriers_client.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.FoodCategory;
import com.utt.foodcouriers_client.data.repository.CatalogRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SearchViewModel extends BaseViewModel {

    private static final String ALL_CATEGORY_ID = "all";

    private final CatalogRepository catalogRepository = CatalogRepository.getInstance();
    private final MutableLiveData<List<FoodCategory>> categoryFilters = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<FoodCategory>> visibleCategories = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> selectedCategoryId = new MutableLiveData<>(ALL_CATEGORY_ID);
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private List<FoodCategory> sourceCategories = Collections.emptyList();

    public SearchViewModel() {
        refreshCategories();
    }

    public LiveData<List<FoodCategory>> getCategoryFilters() {
        return categoryFilters;
    }

    public LiveData<List<FoodCategory>> getVisibleCategories() {
        return visibleCategories;
    }

    public LiveData<String> getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public LiveData<String> getQuery() {
        return query;
    }

    public void refreshCategories() {
        setLoading(true);
        catalogRepository.getCategories(new RepositoryCallback<List<FoodCategory>>() {
            @Override
            public void onSuccess(List<FoodCategory> result) {
                sourceCategories = result != null ? result : Collections.emptyList();
                setLoading(false);
                errorMessage.setValue(null);
                publishState();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }

    public void setQuery(String value) {
        query.setValue(value != null ? value.trim() : "");
        publishState();
    }

    public void selectCategory(FoodCategory category) {
        if (category == null) {
            return;
        }
        selectedCategoryId.setValue(category.getId());
        publishState();
    }

    private void publishState() {
        List<FoodCategory> filters = new ArrayList<>();
        filters.add(new FoodCategory(ALL_CATEGORY_ID, "All", null, 0));
        filters.addAll(sourceCategories);
        categoryFilters.setValue(filters);

        String selectedId = selectedCategoryId.getValue();
        String rawQuery = query.getValue();
        String normalizedQuery = rawQuery == null ? "" : rawQuery.toLowerCase(Locale.ROOT);

        List<FoodCategory> filtered = new ArrayList<>();
        for (FoodCategory category : sourceCategories) {
            boolean matchesSelection = ALL_CATEGORY_ID.equals(selectedId) || category.getId().equals(selectedId);
            boolean matchesQuery = normalizedQuery.isEmpty()
                    || category.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery);
            if (matchesSelection && matchesQuery) {
                filtered.add(category);
            }
        }
        visibleCategories.setValue(filtered);
    }
}
