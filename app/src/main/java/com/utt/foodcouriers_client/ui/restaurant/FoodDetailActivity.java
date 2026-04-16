package com.utt.foodcouriers_client.ui.restaurant;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.MenuItem;
import com.utt.foodcouriers_client.data.model.Restaurant;
import com.utt.foodcouriers_client.ui.auth.LoginActivity;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.main.MainActivity;
import com.utt.foodcouriers_client.viewmodel.CartViewModel;

import java.text.NumberFormat;
import java.util.Locale;

public class FoodDetailActivity extends BaseActivity {

    public static final String EXTRA_MENU_ITEM = "menu_item";
    public static final String EXTRA_RESTAURANT = "restaurant";

    private MenuItem menuItem;
    private Restaurant restaurant;
    private CartViewModel cartViewModel;
    private NumberFormat formatter;

    private ImageView ivFoodImage, btnBack;
    private TextView tvFoodName, tvFoodPrice, tvFoodRating, tvFoodDescription;
    private Button btnAddToCart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_food_detail);

        formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        initViews();
        loadData();
        setupViewModel();
    }

    private void initViews() {
        ivFoodImage = findViewById(R.id.iv_food_image);
        btnBack = findViewById(R.id.btn_back);
        tvFoodName = findViewById(R.id.tv_food_name);
        tvFoodPrice = findViewById(R.id.tv_food_price);
        tvFoodRating = findViewById(R.id.tv_food_rating);
        tvFoodDescription = findViewById(R.id.tv_food_description);
        btnAddToCart = findViewById(R.id.btn_add_to_cart);

        btnBack.setOnClickListener(v -> finish());

        btnAddToCart.setOnClickListener(v -> addToCart());
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        menuItem = (MenuItem) getIntent().getSerializableExtra(EXTRA_MENU_ITEM);
        restaurant = (Restaurant) getIntent().getSerializableExtra(EXTRA_RESTAURANT);

        if (menuItem == null) {
            showErrorSnackbar("Invalid product");
            finish();
            return;
        }

        displayFoodDetail();
    }

    private void setupViewModel() {
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        cartViewModel.getSuccessMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                showSuccessBanner(message);
                openCartScreen();
            }
        });
        cartViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showErrorSnackbar(error);
            }
        });
    }

    private void displayFoodDetail() {
        tvFoodName.setText(menuItem.getName());
        tvFoodPrice.setText(formatter.format(menuItem.getPrice()));
        tvFoodRating.setText(String.valueOf(menuItem.getRating()));
        tvFoodDescription.setText(menuItem.getDescription() != null ? menuItem.getDescription() : "No description available");

        if (menuItem.getImageUrl() != null && !menuItem.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(menuItem.getImageUrl())
                    .placeholder(R.drawable.food_placeholder_burger)
                    .error(R.drawable.food_placeholder_burger)
                    .centerCrop()
                    .into(ivFoodImage);
        }

        if (!menuItem.isAvailable()) {
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Out of Stock");
        }
    }

    private void addToCart() {
        if (!cartViewModel.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        if (restaurant == null) {
            showErrorSnackbar("Restaurant information is missing");
            return;
        }

        cartViewModel.setMenuItemQuantity(this, menuItem, restaurant, 1, "");
    }

    private void openCartScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_CART, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}