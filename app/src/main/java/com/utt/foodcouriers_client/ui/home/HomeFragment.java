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
import com.utt.foodcouriers_client.ui.category.CategoryChipAdapter;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.viewmodel.HomeViewModel;

public class HomeFragment extends BaseFragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private CategoryChipAdapter chipAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        setupRecyclerViews();
        bindViewModel();
        return binding.getRoot();
    }

    private void setupRecyclerViews() {
        chipAdapter = new CategoryChipAdapter(category -> viewModel.selectCategory(category));
        binding.rvCategoryFilters.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategoryFilters.setAdapter(chipAdapter);
    }

    private void bindViewModel() {
        viewModel.getCategoryFilters().observe(getViewLifecycleOwner(), categories -> {
            String selectedId = viewModel.getSelectedCategoryId().getValue();
            chipAdapter.submitList(categories, selectedId);
        });
        viewModel.getSelectedCategoryId().observe(getViewLifecycleOwner(), selectedId -> {
            chipAdapter.submitList(viewModel.getCategoryFilters().getValue(), selectedId);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
