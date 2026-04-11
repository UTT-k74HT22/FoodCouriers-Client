package com.utt.foodcouriers_client.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.FoodCategory;
import com.utt.foodcouriers_client.data.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SearchViewModel extends BaseViewModel {

    private final CategoryRepository catalogRepository = CategoryRepository.getInstance();
    private final MutableLiveData<List<FoodCategory>> categoryFilters = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<FoodCategory>> visibleCategories = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> selectedCategoryId = new MutableLiveData<>(null);
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
        String currentId = selectedCategoryId.getValue();
        String nextId = category.getId();
        selectedCategoryId.setValue(nextId != null && nextId.equals(currentId) ? null : nextId);
        publishState();
    }

    private void publishState() {
        categoryFilters.setValue(new ArrayList<>(sourceCategories));

        String selectedId = selectedCategoryId.getValue();
        final String currentSelectedId = selectedId;
        if (currentSelectedId != null && sourceCategories.stream().noneMatch(category -> currentSelectedId.equals(category.getId()))) {
            selectedId = null;
            selectedCategoryId.setValue(null);
        }
        String rawQuery = query.getValue();
        String normalizedQuery = rawQuery == null ? "" : rawQuery.toLowerCase(Locale.ROOT);

        List<FoodCategory> filtered = new ArrayList<>();
        for (FoodCategory category : sourceCategories) {
            String categoryName = category.getName() != null ? category.getName() : "";
            boolean matchesSelection = selectedId == null || selectedId.equals(category.getId());
            boolean matchesQuery = normalizedQuery.isEmpty()
                    || categoryName.toLowerCase(Locale.ROOT).contains(normalizedQuery);
            if (matchesSelection && matchesQuery) {
                filtered.add(category);
            }
        }
        visibleCategories.setValue(filtered);
    }
}
