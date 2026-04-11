package com.utt.foodcouriers_client.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.utt.foodcouriers_client.databinding.FragmentSearchBinding;
import com.utt.foodcouriers_client.ui.category.CategoryCardAdapter;
import com.utt.foodcouriers_client.ui.category.CategoryChipAdapter;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.viewmodel.SearchViewModel;

public class SearchFragment extends BaseFragment {

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private CategoryChipAdapter chipAdapter;
    private CategoryCardAdapter cardAdapter;
    private final TextWatcher searchWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            viewModel.setQuery(s != null ? s.toString() : "");
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        setupRecyclerViews();
        bindViewModel();
        binding.etSearch.addTextChangedListener(searchWatcher);
        binding.btnSearchRetry.setOnClickListener(v -> viewModel.refreshCategories());
        return binding.getRoot();
    }

    private void setupRecyclerViews() {
        chipAdapter = new CategoryChipAdapter(category -> viewModel.selectCategory(category));
        binding.rvSearchCategoryFilters.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvSearchCategoryFilters.setAdapter(chipAdapter);

        cardAdapter = new CategoryCardAdapter(CategoryCardAdapter.MODE_SEARCH, category -> viewModel.selectCategory(category));
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSearchResults.setAdapter(cardAdapter);
    }

    private void bindViewModel() {
        viewModel.getCategoryFilters().observe(getViewLifecycleOwner(), categories -> {
            String selectedId = viewModel.getSelectedCategoryId().getValue();
            chipAdapter.submitList(categories, selectedId);
        });
        viewModel.getVisibleCategories().observe(getViewLifecycleOwner(), categories -> {
            String selectedId = viewModel.getSelectedCategoryId().getValue();
            cardAdapter.submitList(categories, selectedId);
            boolean hasData = categories != null && !categories.isEmpty();
            binding.rvSearchResults.setVisibility(hasData ? View.VISIBLE : View.GONE);
            binding.tvSearchEmpty.setVisibility(hasData ? View.GONE : View.VISIBLE);
        });
        viewModel.getSelectedCategoryId().observe(getViewLifecycleOwner(), selectedId -> {
            chipAdapter.submitList(viewModel.getCategoryFilters().getValue(), selectedId);
            cardAdapter.submitList(viewModel.getVisibleCategories().getValue(), selectedId);
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            binding.progressSearchCategories.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) {
                binding.tvSearchError.setVisibility(View.GONE);
                binding.btnSearchRetry.setVisibility(View.GONE);
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            boolean hasError = error != null && !error.trim().isEmpty();
            binding.tvSearchError.setVisibility(hasError ? View.VISIBLE : View.GONE);
            binding.btnSearchRetry.setVisibility(hasError ? View.VISIBLE : View.GONE);
            binding.tvSearchError.setText(error);
        });
    }

    @Override
    public void onDestroyView() {
        binding.etSearch.removeTextChangedListener(searchWatcher);
        super.onDestroyView();
        binding = null;
    }
}
