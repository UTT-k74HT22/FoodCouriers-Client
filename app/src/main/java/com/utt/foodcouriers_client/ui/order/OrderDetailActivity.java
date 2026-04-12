package com.utt.foodcouriers_client.ui.order;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.OrderStatus;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.order.adapter.OrderLineItemAdapter;
import com.utt.foodcouriers_client.viewmodel.OrdersViewModel;

public class OrderDetailActivity extends BaseActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    private OrdersViewModel viewModel;
    private OrderLineItemAdapter lineItemAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        configureToolbar(toolbar, true);
        setToolbarTitle(getString(R.string.order_detail_title));

        viewModel = new ViewModelProvider(this).get(OrdersViewModel.class);
        setupRecyclerView();
        bindObservers();

        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderId == null || orderId.isBlank()) {
            showErrorBanner("Khong co thong tin don hang.");
            finish();
            return;
        }
        viewModel.loadOrderDetail(orderId);
    }

    private void setupRecyclerView() {
        androidx.recyclerview.widget.RecyclerView recyclerView = findViewById(R.id.rv_order_items);
        lineItemAdapter = new OrderLineItemAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(lineItemAdapter);
    }

    private void bindObservers() {
        viewModel.getSelectedOrder().observe(this, this::renderOrder);
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isBlank()) {
                showErrorBanner(error);
            }
        });
    }

    private void renderOrder(OrderSummary order) {
        ((TextView) findViewById(R.id.tv_order_code)).setText(order.getOrderCode());
        ((TextView) findViewById(R.id.tv_order_date)).setText(order.getCreatedAtLabel());
        ((TextView) findViewById(R.id.tv_status)).setText(OrderStatus.fromValue(order.getStatus()).getLabel());
        ((TextView) findViewById(R.id.tv_restaurant_name)).setText(order.getRestaurantName());
        ((TextView) findViewById(R.id.tv_restaurant_address)).setText(order.getRestaurantAddress());
        ((TextView) findViewById(R.id.tv_subtotal)).setText(formatCurrency(order.getSubtotal()));
        ((TextView) findViewById(R.id.tv_delivery_fee)).setText(formatCurrency(order.getDeliveryFee()));
        ((TextView) findViewById(R.id.tv_total)).setText(formatCurrency(order.getTotal()));
        ((TextView) findViewById(R.id.tv_delivery_address)).setText(order.getDeliveryAddress());
        lineItemAdapter.submitList(order.getItems());

        View layoutDiscount = findViewById(R.id.layout_discount);
        if (order.getDiscount() > 0) {
            layoutDiscount.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tv_discount)).setText("-" + formatCurrency(order.getDiscount()));
        } else {
            layoutDiscount.setVisibility(View.GONE);
        }

        renderTimeline(OrderStatus.fromValue(order.getStatus()));

        Button btnReview = findViewById(R.id.btn_review);
        Button btnReorder = findViewById(R.id.btn_reorder);
        btnReview.setOnClickListener(v -> showToast("Module review se noi tiep sau khi order flow on dinh."));
        btnReorder.setOnClickListener(v -> showToast("Re-order se duoc noi sang cart/use case o buoc tiep theo."));
    }

    private void renderTimeline(OrderStatus currentStatus) {
        LinearLayout timeline = findViewById(R.id.layout_status_timeline);
        timeline.removeAllViews();
        for (OrderStatus status : new OrderStatus[]{
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED,
                OrderStatus.PREPARING,
                OrderStatus.DELIVERING,
                OrderStatus.DELIVERED
        }) {
            TextView step = new TextView(this);
            step.setText(status.getLabel());
            step.setGravity(Gravity.CENTER);
            step.setTextSize(12f);
            step.setPadding(16, 12, 16, 12);
            step.setAlpha(isReached(currentStatus, status) ? 1f : 0.45f);
            step.setBackgroundResource(isReached(currentStatus, status) ? R.drawable.badge_featured : R.drawable.button_outline);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (timeline.getChildCount() > 0) {
                params.setMarginStart(8);
            }
            timeline.addView(step, params);
        }
    }

    private boolean isReached(OrderStatus currentStatus, OrderStatus checkpoint) {
        if (currentStatus == OrderStatus.CANCELLED) {
            return checkpoint == OrderStatus.PENDING;
        }
        return currentStatus.ordinal() >= checkpoint.ordinal();
    }

    private String formatCurrency(int amount) {
        return String.format("%,d đ", amount);
    }
}
