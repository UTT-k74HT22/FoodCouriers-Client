package com.utt.foodcouriers_client.ui.discover;

import android.content.Intent;
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

import com.utt.foodcouriers_client.data.model.MenuItem;
import com.utt.foodcouriers_client.data.model.Restaurant;
import com.utt.foodcouriers_client.databinding.FragmentDiscoverBinding;
import com.utt.foodcouriers_client.ui.auth.LoginActivity;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.ui.discover.adapter.RestaurantWithMenuAdapter;
import com.utt.foodcouriers_client.viewmodel.CartViewModel;
import com.utt.foodcouriers_client.viewmodel.DiscoverViewModel;

import java.util.List;

public class DiscoverFragment extends BaseFragment {

    private FragmentDiscoverBinding binding;
    private DiscoverViewModel viewModel;
    private CartViewModel cartViewModel;
    private RestaurantWithMenuAdapter adapter;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDiscoverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(DiscoverViewModel.class);
        cartViewModel = new ViewModelProvider(requireActivity()).get(CartViewModel.class);
        
        setupRecyclerView();
        setupSearch();
        observeViewModel();
        
        viewModel.loadRestaurantsWithMenus();
    }

    private void setupRecyclerView() {
        adapter = new RestaurantWithMenuAdapter(requireContext(), (menuItem, restaurant) -> {
            handleFoodAddClick(menuItem, restaurant);
        });
        
        binding.rvRestaurants.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRestaurants.setAdapter(adapter);
        binding.rvRestaurants.setNestedScrollingEnabled(false);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.search(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getRestaurantsWithMenus().observe(getViewLifecycleOwner(), this::displayRestaurants);
        
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Có thể thêm progress indicator nếu cần
        });
        
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showErrorSnackbar(error);
            }
        });

        cartViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error == null || error.isEmpty()) {
                return;
            }
            showErrorSnackbar(error);
        });
    }

    private void displayRestaurants(List<RestaurantWithMenuAdapter.RestaurantWithMenu> restaurants) {
        if (restaurants == null || restaurants.isEmpty()) {
            binding.rvRestaurants.setVisibility(View.GONE);
            return;
        }

        binding.rvRestaurants.setVisibility(View.VISIBLE);
        adapter.setItems(restaurants);
    }

    private void handleFoodAddClick(MenuItem menuItem, Restaurant restaurant) {
        if (!cartViewModel.isLoggedIn(requireContext())) {
            startActivity(new Intent(requireContext(), LoginActivity.class));
            return;
        }
        cartViewModel.addMenuItem(requireContext(), menuItem, restaurant, 1, "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
