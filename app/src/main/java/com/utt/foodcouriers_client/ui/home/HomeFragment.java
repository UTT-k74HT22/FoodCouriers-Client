package com.utt.foodcouriers_client.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.BannerItem;
import com.utt.foodcouriers_client.data.model.Category;
import com.utt.foodcouriers_client.data.model.MenuItem;
import com.utt.foodcouriers_client.data.model.Restaurant;
import com.utt.foodcouriers_client.databinding.FragmentHomeBinding;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.ui.home.adapter.BannerAdapter;
import com.utt.foodcouriers_client.ui.home.adapter.CategoryAdapter;
import com.utt.foodcouriers_client.ui.home.adapter.MenuItemAdapter;
import com.utt.foodcouriers_client.ui.home.adapter.NearbyRestaurantAdapter;
import com.utt.foodcouriers_client.ui.restaurant.RestaurantDetailActivity;
import com.utt.foodcouriers_client.viewmodel.HomeViewModel;

import java.util.List;
import java.util.Locale;

public class HomeFragment extends BaseFragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private BannerAdapter bannerAdapter;
    private CategoryAdapter categoryAdapter;
    private MenuItemAdapter menuItemAdapter;
    private NearbyRestaurantAdapter nearbyRestaurantAdapter;
    private String currentCategoryId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        
        setupBannerViewPager();
        setupCategoryRecyclerView();
        setupPopularMenuItemsRecyclerView();
        setupNearbyRestaurantsRecyclerView();
        observeViewModel();
        
        viewModel.loadBanners();
        viewModel.loadCategories();
        viewModel.loadPopularMenuItems();
        viewModel.loadNearbyRestaurants();
    }

    private void setupBannerViewPager() {
        bannerAdapter = new BannerAdapter(requireContext(), this::handleBannerClick);
        
        binding.vpBanners.setAdapter(bannerAdapter);
        binding.vpBanners.setOffscreenPageLimit(1);
        
        binding.vpBanners.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicator(position);
            }
        });
    }

    private void setupCategoryRecyclerView() {
        categoryAdapter = new CategoryAdapter(requireContext(), (category, position) -> {
            categoryAdapter.setSelectedPosition(position);
            currentCategoryId = category != null ? category.getId() : null;
            handleCategoryClick(category);
        });

        binding.categoryContainer.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.categoryContainer.setAdapter(categoryAdapter);
    }

    private void setupPopularMenuItemsRecyclerView() {
        menuItemAdapter = new MenuItemAdapter(requireContext(), menuItem -> {
            openRestaurantDetailWithMenuItem(menuItem);
        });

        binding.popularMenuContainer.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.popularMenuContainer.setAdapter(menuItemAdapter);
    }

    private void setupNearbyRestaurantsRecyclerView() {
        nearbyRestaurantAdapter = new NearbyRestaurantAdapter(requireContext(), restaurant -> {
            openRestaurantDetail(restaurant.getId());
        });
        
        binding.nearbyRestaurantContainer.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.nearbyRestaurantContainer.setAdapter(nearbyRestaurantAdapter);
    }

    private void updateIndicator(int selectedPosition) {
        if (binding.bannerIndicator == null || binding.bannerIndicator.getChildCount() == 0) {
            return;
        }

        for (int i = 0; i < binding.bannerIndicator.getChildCount(); i++) {
            View dot = binding.bannerIndicator.getChildAt(i);
            if (dot != null) {
                dot.setSelected(i == selectedPosition);
            }
        }
    }

    private void setupIndicators(int count) {
        if (binding.bannerIndicator == null) return;

        binding.bannerIndicator.removeAllViews();
        
        if (count <= 1) {
            binding.bannerIndicator.setVisibility(View.GONE);
            return;
        }

        binding.bannerIndicator.setVisibility(View.VISIBLE);

        float density = requireContext().getResources().getDisplayMetrics().density;
        int dotSize = (int) (8 * density);
        int dotMargin = (int) (4 * density);

        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
            params.setMargins(dotMargin, 0, dotMargin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.banner_indicator_dot);
            dot.setSelected(i == 0);
            binding.bannerIndicator.addView(dot);
        }
    }

    private void observeViewModel() {
        viewModel.getBanners().observe(getViewLifecycleOwner(), this::displayBanners);
        viewModel.getCategories().observe(getViewLifecycleOwner(), this::displayCategories);
        viewModel.getPopularMenuItems().observe(getViewLifecycleOwner(), this::displayPopularMenuItems);
        viewModel.getNearbyRestaurants().observe(getViewLifecycleOwner(), this::displayNearbyRestaurants);
        
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding.vpBanners != null) {
                binding.vpBanners.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
            }
        });
        
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showBannerError();
                showErrorSnackbar(error);
            }
        });
    }

    private void displayBanners(List<BannerItem> banners) {
        if (banners == null || banners.isEmpty()) {
            binding.vpBanners.setVisibility(View.GONE);
            binding.bannerIndicator.setVisibility(View.GONE);
            return;
        }

        binding.vpBanners.setVisibility(View.VISIBLE);
        bannerAdapter.setBanners(banners);
        setupIndicators(banners.size());
        startAutoScroll(banners.size());
    }

    private void displayCategories(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            binding.categoryContainer.setVisibility(View.GONE);
            return;
        }

        binding.categoryContainer.setVisibility(View.VISIBLE);
        categoryAdapter.setCategories(categories);
    }

    private void displayPopularMenuItems(List<MenuItem> menuItems) {
        if (menuItems == null || menuItems.isEmpty()) {
            binding.popularMenuContainer.setVisibility(View.GONE);
            return;
        }

        binding.popularMenuContainer.setVisibility(View.VISIBLE);
        menuItemAdapter.setItems(menuItems);
    }

    private void displayNearbyRestaurants(List<Restaurant> restaurants) {
        if (restaurants == null || restaurants.isEmpty()) {
            binding.nearbyRestaurantContainer.setVisibility(View.GONE);
            return;
        }

        binding.nearbyRestaurantContainer.setVisibility(View.VISIBLE);
        nearbyRestaurantAdapter.setRestaurants(restaurants);
    }

    private void startAutoScroll(int bannerCount) {
        if (bannerCount <= 1) return;

        binding.vpBanners.postDelayed(new Runnable() {
            private int currentPage = 0;

            @Override
            public void run() {
                if (binding == null || binding.vpBanners == null || !isAdded()) return;
                
                currentPage = (currentPage + 1) % bannerCount;
                binding.vpBanners.setCurrentItem(currentPage, true);
                
                if (binding != null && binding.vpBanners != null) {
                    binding.vpBanners.postDelayed(this, 5000);
                }
            }
        }, 5000);
    }

    private void handleBannerClick(BannerItem banner) {
        BannerItem.LinkType linkType = banner.getLinkTypeEnum();
        String linkValue = banner.getLinkValue();
        
        switch (linkType) {
            case RESTAURANT:
                openRestaurantDetail(linkValue);
                break;
            case CATEGORY:
                navigateToCategory(linkValue);
                break;
            case PROMOTION:
                showPromotionDetail(linkValue);
                break;
            case URL:
                openUrl(linkValue);
                break;
            case NONE:
            default:
                break;
        }
    }

    private void handleCategoryClick(Category category) {
        if (category != null && category.getId() != null) {
            viewModel.filterMenuItemsByCategory(category.getId());
        } else {
            viewModel.loadPopularMenuItems();
        }
    }

    private void openRestaurantDetail(String restaurantId) {
        if (restaurantId == null || restaurantId.isEmpty()) return;
        
        Intent intent = new Intent(requireContext(), RestaurantDetailActivity.class);
        intent.putExtra("restaurant_id", restaurantId);
        startActivity(intent);
    }

    private void openRestaurantDetailWithMenuItem(MenuItem menuItem) {
        if (menuItem == null || menuItem.getRestaurantId() == null) return;
        
        Intent intent = new Intent(requireContext(), RestaurantDetailActivity.class);
        intent.putExtra("restaurant_id", menuItem.getRestaurantId());
        intent.putExtra("menu_item_id", menuItem.getId());
        startActivity(intent);
    }

    private void navigateToCategory(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) return;
        
        currentCategoryId = categoryId;
        viewModel.filterMenuItemsByCategory(categoryId);
    }

    private void showPromotionDetail(String promotionId) {
        if (promotionId == null || promotionId.isEmpty()) return;
        
    }

    private void openUrl(String url) {
        if (url == null || url.isEmpty()) return;
        
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
        }
    }

    private void showBannerError() {
        binding.vpBanners.setVisibility(View.GONE);
        binding.bannerIndicator.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}