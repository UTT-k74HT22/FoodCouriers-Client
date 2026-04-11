package com.utt.foodcouriers_client.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.BannerItem;
import com.utt.foodcouriers_client.databinding.FragmentHomeBinding;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.ui.home.adapter.BannerAdapter;
import com.utt.foodcouriers_client.ui.restaurant.RestaurantDetailActivity;
import com.utt.foodcouriers_client.viewmodel.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends BaseFragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private BannerAdapter bannerAdapter;

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
        observeViewModel();
        
        viewModel.loadBanners();
    }

    private void setupBannerViewPager() {
        bannerAdapter = new BannerAdapter(requireContext(), this::handleBannerClick);
        
        binding.vpBanners.setAdapter(bannerAdapter);
        binding.vpBanners.setOffscreenPageLimit(1);
        
        BannerAdapter.BannerPageChangeCallback pageCallback = 
                new BannerAdapter.BannerPageChangeCallback(binding.vpBanners, binding.bannerIndicator);
        binding.vpBanners.registerOnPageChangeCallback(pageCallback);
        
        binding.vpBanners.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicator(position);
            }
        });
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
        
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding.vpBanners != null) {
                binding.vpBanners.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
            }
        });
        
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showBannerError();
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

    private void startAutoScroll(int bannerCount) {
        if (bannerCount <= 1) return;

        binding.vpBanners.postDelayed(new Runnable() {
            private int currentPage = 0;

            @Override
            public void run() {
                if (binding.vpBanners == null || !isAdded()) return;
                
                currentPage = (currentPage + 1) % bannerCount;
                binding.vpBanners.setCurrentItem(currentPage, true);
                
                binding.vpBanners.postDelayed(this, 5000);
            }
        }, 5000);
    }

    private void handleBannerClick(BannerItem banner) {
        BannerItem.LinkType linkType = banner.getLinkTypeEnum();
        String linkValue = banner.getLinkValue();
        
        switch (linkType) {
            case RESTAURANT:
                navigateToRestaurant(linkValue);
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

    private void navigateToRestaurant(String restaurantId) {
        if (restaurantId == null || restaurantId.isEmpty()) return;
        
        Intent intent = new Intent(requireContext(), RestaurantDetailActivity.class);
        intent.putExtra("restaurant_id", restaurantId);
        startActivity(intent);
    }

    private void navigateToCategory(String categoryId) {
        // TODO: Implement category navigation
    }

    private void showPromotionDetail(String promotionId) {
        // TODO: Implement promotion detail
    }

    private void openUrl(String url) {
        // TODO: Implement URL opening with intent
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
