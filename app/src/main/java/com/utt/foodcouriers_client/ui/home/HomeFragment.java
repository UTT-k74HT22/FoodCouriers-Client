package com.utt.foodcouriers_client.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.utt.foodcouriers_client.databinding.FragmentHomeBinding;
import com.utt.foodcouriers_client.ui.category.CategoryCardAdapter;
import com.utt.foodcouriers_client.ui.category.CategoryChipAdapter;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.viewmodel.HomeViewModel;

public class HomeFragment extends BaseFragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private CategoryChipAdapter chipAdapter;
    private CategoryCardAdapter cardAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        setupRecyclerViews();
        bindViewModel();
        binding.btnHomeRetry.setOnClickListener(v -> viewModel.refreshCategories());
        return binding.getRoot();
    }

    private void setupRecyclerViews() {
        chipAdapter = new CategoryChipAdapter(category -> viewModel.selectCategory(category));
        binding.rvCategoryFilters.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategoryFilters.setAdapter(chipAdapter);

        cardAdapter = new CategoryCardAdapter(CategoryCardAdapter.MODE_HOME, category -> viewModel.selectCategory(category));
        binding.rvCategoryCards.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategoryCards.setAdapter(cardAdapter);
    }

    private void bindViewModel() {
        viewModel.getCategoryFilters().observe(getViewLifecycleOwner(), categories -> {
            String selectedId = viewModel.getSelectedCategoryId().getValue();
            chipAdapter.submitList(categories, selectedId);
        });
        viewModel.getSpotlightCategories().observe(getViewLifecycleOwner(), categories -> {
            String selectedId = viewModel.getSelectedCategoryId().getValue();
            cardAdapter.submitList(categories, selectedId);
            boolean hasData = categories != null && !categories.isEmpty();
            binding.rvCategoryCards.setVisibility(hasData ? View.VISIBLE : View.GONE);
            binding.tvHomeEmpty.setVisibility(hasData ? View.GONE : View.VISIBLE);
        });
        viewModel.getSelectedCategoryId().observe(getViewLifecycleOwner(), selectedId -> {
            chipAdapter.submitList(viewModel.getCategoryFilters().getValue(), selectedId);
            cardAdapter.submitList(viewModel.getSpotlightCategories().getValue(), selectedId);
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            binding.progressHomeCategories.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) {
                binding.tvHomeError.setVisibility(View.GONE);
                binding.btnHomeRetry.setVisibility(View.GONE);
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            boolean hasError = error != null && !error.trim().isEmpty();
            binding.tvHomeError.setVisibility(hasError ? View.VISIBLE : View.GONE);
            binding.btnHomeRetry.setVisibility(hasError ? View.VISIBLE : View.GONE);
            binding.tvHomeError.setText(error);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
