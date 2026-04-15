package com.utt.foodcouriers_client.ui.order;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.DeliveryStatus;
import com.utt.foodcouriers_client.data.model.OrderStatus;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.viewmodel.OrdersViewModel;

public class OrderTrackingActivity extends BaseActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    private OrdersViewModel viewModel;
    private TextView tvOrderStatus;
    private View stepConfirmed;
    private View stepPreparing;
    private View stepDelivering;
    private View stepDelivered;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_order_tracking);

        viewModel = new ViewModelProvider(this).get(OrdersViewModel.class);
        bindViews();
        bindObservers();

        findViewById(R.id.btn_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderId == null || orderId.isBlank()) {
            showErrorBanner("Khong co thong tin don hang.");
            finish();
            return;
        }
        viewModel.loadOrderDetail(this, orderId);
    }

    private void bindViews() {
        tvOrderStatus = findViewById(R.id.tv_order_status);
        stepConfirmed = findViewById(R.id.step_confirmed);
        stepPreparing = findViewById(R.id.step_preparing);
        stepDelivering = findViewById(R.id.step_delivering);
        stepDelivered = findViewById(R.id.step_delivered);
        setupStep(stepConfirmed, "Đã nhận", "Nhà hàng đã nhận đơn");
        setupStep(stepPreparing, "Đang làm", "Món đang được chế biến");
        setupStep(stepDelivering, "Đang giao", "Shipper đang giao đơn");
        setupStep(stepDelivered, "Đã giao", "Đơn hàng giao thành công");
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
        OrderStatus status = OrderStatus.fromValue(order.getStatus());
        String statusLabel = status.getLabel();
        if (order.getDeliveryStatus() != null && !order.getDeliveryStatus().isEmpty()) {
            statusLabel += " · " + DeliveryStatus.fromValue(order.getDeliveryStatus()).getLabel();
        }
        tvOrderStatus.setText(order.getOrderCode() + " · " + statusLabel);
        
        renderStep(stepConfirmed, isReached(status, OrderStatus.CONFIRMED));
        renderStep(stepPreparing, isReached(status, OrderStatus.PREPARING));
        renderStep(stepDelivering, isReached(status, OrderStatus.DELIVERING));
        renderStep(stepDelivered, isReached(status, OrderStatus.DELIVERED));
    }
    
    private boolean isReached(OrderStatus current, OrderStatus target) {
        if (current == OrderStatus.CANCELLED) return target == OrderStatus.PENDING;
        return current.ordinal() >= target.ordinal();
    }

    private void setupStep(View root, String title, String description) {
        TextView tvTitle = root.findViewById(R.id.tv_step_title);
        TextView tvDescription = root.findViewById(R.id.tv_step_description);
        tvTitle.setText(title);
        tvDescription.setText(description);
    }

    private void renderStep(View root, boolean active) {
        View indicator = root.findViewById(R.id.view_indicator);
        TextView tvTitle = root.findViewById(R.id.tv_step_title);
        TextView tvDescription = root.findViewById(R.id.tv_step_description);
        indicator.setAlpha(active ? 1f : 0.35f);
        tvTitle.setAlpha(active ? 1f : 0.55f);
        tvDescription.setAlpha(active ? 1f : 0.55f);
    }
}
