package com.utt.foodcouriers_client.ui.restaurant;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.MenuItem;
import com.utt.foodcouriers_client.data.model.Restaurant;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.viewmodel.RestaurantDetailViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestaurantDetailActivity extends BaseActivity {

    public static final String EXTRA_RESTAURANT_ID = "restaurant_id";
    public static final String EXTRA_MENU_ITEM_ID = "menu_item_id";

    private RestaurantDetailViewModel viewModel;
    private RecyclerView rvMenuItems;
    private RestaurantMenuAdapter menuAdapter;
    private TextView tvRestaurantName, tvRestaurantAddress, tvRating, tvReviewCount;
    private TextView tvOpenStatus, tvOpenTime, tvDeliveryFee, tvDeliveryTime, tvMinOrder;
    private TextView tvDescription;
    private ImageView ivRestaurantImage, ivOpenStatus;
    private ExtendedFloatingActionButton fabCart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_detail);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        loadData();
    }

    private void initViews() {
        tvRestaurantName = findViewById(R.id.tv_restaurant_name);
        tvRestaurantAddress = findViewById(R.id.tv_restaurant_address);
        tvRating = findViewById(R.id.tv_rating);
        tvReviewCount = findViewById(R.id.tv_review_count);
        ivOpenStatus = findViewById(R.id.iv_open_status);
        tvOpenStatus = findViewById(R.id.tv_open_status);
        tvOpenTime = findViewById(R.id.tv_open_time);
        tvDeliveryFee = findViewById(R.id.tv_delivery_fee);
        tvDeliveryTime = findViewById(R.id.tv_delivery_time);
        tvMinOrder = findViewById(R.id.tv_min_order);
        tvDescription = findViewById(R.id.tv_description);
        ivRestaurantImage = findViewById(R.id.iv_restaurant_image);
        rvMenuItems = findViewById(R.id.rv_menu_items);
        fabCart = findViewById(R.id.fab_cart);

        fabCart.setOnClickListener(v -> {
            showToast("View cart clicked");
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        configureToolbar(toolbar, true);
    }

    private void setupRecyclerView() {
        menuAdapter = new RestaurantMenuAdapter(
                this,
                this::handleMenuItemClick,
                this::handleQuantityChange
        );
        rvMenuItems.setLayoutManager(new LinearLayoutManager(this));
        rvMenuItems.setNestedScrollingEnabled(false);
        rvMenuItems.setAdapter(menuAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(RestaurantDetailViewModel.class);

        viewModel.getRestaurant().observe(this, this::displayRestaurant);
        viewModel.getMenuByCategory().observe(this, this::displayMenu);
        viewModel.getLoading().observe(this, this::handleLoading);
        viewModel.getErrorMessage().observe(this, this::handleError);
    }

    private void loadData() {
        String restaurantId = getIntent().getStringExtra(EXTRA_RESTAURANT_ID);
        String menuItemId = getIntent().getStringExtra(EXTRA_MENU_ITEM_ID);

        if (restaurantId != null && !restaurantId.isEmpty()) {
            viewModel.loadRestaurantDetail(restaurantId, menuItemId);
        } else {
            showErrorSnackbar("Invalid restaurant");
            finish();
        }
    }

    private void displayRestaurant(Restaurant restaurant) {
        if (restaurant == null) return;

        tvRestaurantName.setText(restaurant.getName());
        tvRestaurantAddress.setText(restaurant.getAddress());
        tvRating.setText(restaurant.getFormattedRating());
        tvReviewCount.setText("(" + restaurant.getReviewCount() + ")");

        if (restaurant.isOpen()) {
            ivOpenStatus.setColorFilter(getColor(R.color.success));
            tvOpenStatus.setText("Open");
            tvOpenStatus.setTextColor(getColor(R.color.success));
        } else {
            ivOpenStatus.setColorFilter(getColor(R.color.error));
            tvOpenStatus.setText("Closed");
            tvOpenStatus.setTextColor(getColor(R.color.error));
        }

        if (restaurant.getOpenTime() != null && restaurant.getCloseTime() != null) {
            tvOpenTime.setText(restaurant.getOpenTime() + " - " + restaurant.getCloseTime());
        } else {
            tvOpenTime.setVisibility(View.GONE);
        }

        tvDeliveryFee.setText(restaurant.getFormattedDeliveryFee());
        tvDeliveryTime.setText("20-30 min");
        tvMinOrder.setText(restaurant.getMinOrder() + "k");
        tvDescription.setText(restaurant.getDescription());

        if (restaurant.getImageUrl() != null && !restaurant.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(restaurant.getImageUrl())
                    .placeholder(R.drawable.food_placeholder_burger)
                    .error(R.drawable.food_placeholder_burger)
                    .centerCrop()
                    .into(ivRestaurantImage);
        }

        getSupportActionBar().setTitle(restaurant.getName());
    }

    private void displayMenu(Map<String, List<MenuItem>> menuByCategory) {
        if (menuByCategory == null || menuByCategory.isEmpty()) {
            rvMenuItems.setVisibility(View.GONE);
            return;
        }

        rvMenuItems.setVisibility(View.VISIBLE);
        List<RestaurantMenuAdapter.MenuSection> sections = new ArrayList<>();

        for (Map.Entry<String, List<MenuItem>> entry : menuByCategory.entrySet()) {
            sections.add(new RestaurantMenuAdapter.MenuSection(entry.getKey(), entry.getValue()));
        }

        menuAdapter.setSections(sections);
    }

    private void handleMenuItemClick(MenuItem menuItem) {
        showToast("Added: " + menuItem.getName());
    }

    private void handleQuantityChange(MenuItem menuItem, int newQuantity) {
        if (newQuantity > 0) {
            fabCart.setVisibility(View.VISIBLE);
            fabCart.setText("View Cart (" + newQuantity + ")");
        } else {
            fabCart.setVisibility(View.GONE);
        }
    }

    private void handleLoading(Boolean isLoading) {
        if (isLoading != null && isLoading) {
            showLoading();
        } else {
            hideLoading();
        }
    }

    private void handleError(String error) {
        if (error != null && !error.isEmpty()) {
            showErrorSnackbar(error);
        }
    }
}