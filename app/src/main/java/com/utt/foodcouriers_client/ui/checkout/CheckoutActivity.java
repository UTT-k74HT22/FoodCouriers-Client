package com.utt.foodcouriers_client.ui.checkout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.ui.cart.CartFragment;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.order.OrderSuccessActivity;
import com.utt.foodcouriers_client.ui.cart.adapter.CartItemAdapter;
import com.utt.foodcouriers_client.viewmodel.CheckoutViewModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends BaseActivity {

    private CheckoutViewModel viewModel;
    private CartItemAdapter adapter;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private TextView tvSubtotal, tvDeliveryFee, tvTotal, tvDiscount, tvAddress, tvPromoError;
    private View layoutDiscount;
    private EditText etNote, etPromoCode;
    private View btnApplyPromo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        configureToolbar(toolbar, true);
        setToolbarTitle(getString(R.string.checkout_title));

        viewModel = new ViewModelProvider(this).get(CheckoutViewModel.class);
        initViews();
        setupRecyclerView();
        bindObservers();

        ArrayList<String> selectedIds = getIntent().getStringArrayListExtra(CartFragment.EXTRA_SELECTED_CART_ITEM_IDS);
        if (selectedIds != null) {
            viewModel.loadCheckoutData(this, selectedIds);
        }

        findViewById(R.id.btn_place_order).setOnClickListener(v -> {
            String address = tvAddress.getText().toString();
            String note = etNote.getText().toString();
            viewModel.placeOrders(this, address, note, "cod");
        });

        btnApplyPromo.setOnClickListener(v -> {
            String code = etPromoCode.getText().toString();
            viewModel.validatePromotion(this, code);
        });
    }

    private void initViews() {
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDeliveryFee = findViewById(R.id.tv_delivery_fee);
        tvTotal = findViewById(R.id.tv_total);
        tvDiscount = findViewById(R.id.tv_discount);
        layoutDiscount = findViewById(R.id.layout_discount);
        tvAddress = findViewById(R.id.tv_address);
        etNote = findViewById(R.id.et_note);
        etPromoCode = findViewById(R.id.et_promo_code);
        btnApplyPromo = findViewById(R.id.btn_apply_promo);
        tvPromoError = findViewById(R.id.tv_promo_error);

        // Mock address for now
        tvAddress.setText("123 Phố Chùa Láng, Đống Đa, Hà Nội");
        ((TextView) findViewById(R.id.tv_address_label)).setText("Nhà riêng");
    }

    private void setupRecyclerView() {
        RecyclerView rvItems = findViewById(R.id.rv_order_items);
        // Tái sử dụng CartItemAdapter nhưng tắt chức năng sửa
        adapter = new CartItemAdapter(this, null);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);
    }

    private void bindObservers() {
        viewModel.getRestaurantGroups().observe(this, groups -> adapter.submitGroups(groups));
        viewModel.getCheckoutSummary().observe(this, summary -> {
            tvSubtotal.setText(currencyFormatter.format(summary.getSubtotal()));
            tvDeliveryFee.setText(currencyFormatter.format(summary.getDeliveryFee()));
            tvTotal.setText(currencyFormatter.format(summary.getTotal()));
            if (summary.getSavings() > 0) {
                layoutDiscount.setVisibility(View.VISIBLE);
                tvDiscount.setText("-" + currencyFormatter.format(summary.getSavings()));
            } else {
                layoutDiscount.setVisibility(View.GONE);
            }
        });

        viewModel.getAppliedPromotion().observe(this, result -> {
            if (result == null) {
                tvPromoError.setVisibility(View.GONE);
                return;
            }
            tvPromoError.setVisibility(View.VISIBLE);
            tvPromoError.setText(result.getMessage());
            if (result.isValid()) {
                tvPromoError.setTextColor(getResources().getColor(R.color.success, getTheme()));
            } else {
                tvPromoError.setTextColor(getResources().getColor(R.color.error, getTheme()));
            }
        });

        viewModel.getLoading().observe(this, isLoading -> {
            findViewById(R.id.btn_place_order).setEnabled(!isLoading);
            findViewById(R.id.btn_place_order).setAlpha(isLoading ? 0.5f : 1.0f);
            btnApplyPromo.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isBlank()) {
                showErrorBanner(error);
            }
        });

        viewModel.getIsOrderSuccess().observe(this, success -> {
            if (success) {
                List<OrderSummary> orders = viewModel.getCreatedOrders().getValue();
                if (orders != null && !orders.isEmpty()) {
                    Intent intent = new Intent(this, OrderSuccessActivity.class);
                    // Truyền ID của đơn đầu tiên để hiển thị (nếu có nhiều đơn, ta có thể hiển thị danh sách sau)
                    intent.putExtra(OrderSuccessActivity.EXTRA_ORDER_ID, orders.get(0).getId());
                    startActivity(intent);
                    finish();
                }
            }
        });
    }
}
