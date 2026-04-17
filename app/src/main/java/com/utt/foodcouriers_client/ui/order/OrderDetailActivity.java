package com.utt.foodcouriers_client.ui.order;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.JsonObject;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.DeliveryStatus;
import com.utt.foodcouriers_client.data.model.OrderStatus;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.remote.SupabaseRealtimeClient;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.order.adapter.OrderLineItemAdapter;
import com.utt.foodcouriers_client.utils.websocket.RealtimeChannel;
import com.utt.foodcouriers_client.utils.websocket.RealtimeListener;
import com.utt.foodcouriers_client.viewmodel.OrdersViewModel;

public class OrderDetailActivity extends BaseActivity {

    public static final String TAG_REAL_TIME = "RealTimeWebSocket";
    public static final String EXTRA_ORDER_ID = "extra_order_id";
    public static final String EXTRA_FROM_PAYMENT_CALLBACK = "extra_from_payment_callback";

    private OrdersViewModel viewModel;
    private OrderLineItemAdapter lineItemAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String currentOrderId;
    private RealtimeChannel orderChannel;
    private boolean shouldDelayedRefresh;
    private final Runnable delayedRefreshRunnable = () -> {
        if (currentOrderId != null && !currentOrderId.isBlank()) {
            viewModel.loadOrderDetail(this, currentOrderId);
        }
    };

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

        currentOrderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        shouldDelayedRefresh = getIntent().getBooleanExtra(EXTRA_FROM_PAYMENT_CALLBACK, false);
        if (currentOrderId == null || currentOrderId.isBlank()) {
            showErrorBanner("Không có thông tin đơn hàng.");
            finish();
            return;
        }
        viewModel.loadOrderDetail(this, currentOrderId);
        subscribeToOrderUpdates(currentOrderId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentOrderId != null && !currentOrderId.isBlank()) {
            viewModel.loadOrderDetail(this, currentOrderId);
            if (shouldDelayedRefresh) {
                handler.removeCallbacks(delayedRefreshRunnable);
                handler.postDelayed(delayedRefreshRunnable, 2500L);
                shouldDelayedRefresh = false;
            }
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(delayedRefreshRunnable);
        if (orderChannel != null) {
            SupabaseRealtimeClient.getInstance().unsubscribe(orderChannel);
            orderChannel = null;
        }
        super.onDestroy();
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
        
        OrderStatus status = OrderStatus.fromValue(order.getStatus());
        String statusText = status.getLabel();
        if (order.getDeliveryStatus() != null && !order.getDeliveryStatus().isEmpty()) {
            statusText += " · " + DeliveryStatus.fromValue(order.getDeliveryStatus()).getLabel();
        }
        TextView tvStatus = findViewById(R.id.tv_status);
        tvStatus.setText(statusText);
        tvStatus.setBackgroundResource(resolveBadge(status));
        
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
        
        // Link to tracking if order is active
        findViewById(R.id.tv_status).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, OrderTrackingActivity.class);
            intent.putExtra(OrderTrackingActivity.EXTRA_ORDER_ID, order.getId());
            startActivity(intent);
        });

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
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.DELIVERING,
                OrderStatus.DELIVERED
        }) {
            TextView step = new TextView(this);
            step.setText(status.getLabel());
            step.setGravity(Gravity.CENTER);
            step.setTextSize(10f); // Smaller text to fit
            step.setPadding(8, 8, 8, 8);
            boolean isReached = isReached(currentStatus, status);
            step.setAlpha(isReached ? 1f : 0.45f);
            step.setTextColor(isReached ? getResources().getColor(R.color.white) : getResources().getColor(R.color.text_secondary));
            step.setBackgroundResource(isReached ? resolveBadge(status) : R.drawable.button_outline);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (timeline.getChildCount() > 0) {
                params.setMarginStart(4);
            }
            timeline.addView(step, params);
        }
    }

    private int resolveBadge(OrderStatus status) {
        switch (status) {
            case DELIVERED:
                return R.drawable.badge_success;
            case CANCELLED:
                return R.drawable.badge_error;
            case PENDING:
                return R.drawable.badge_warning;
            case DELIVERING:
                return R.drawable.badge_info;
            case CONFIRMED:
            case PREPARING:
            case READY_FOR_PICKUP:
            default:
                return R.drawable.badge_primary;
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

    /**
     * Subscribes to real-time updates for the current order.
     * When the order status or delivery status changes in the database,
     * this method receives the update via Supabase Realtime and refreshes the UI.
     *
     * @param orderId The unique identifier of the order to track.
     */
    private void subscribeToOrderUpdates(String orderId) {
        Log.d(TAG_REAL_TIME, "Subscribing to order updates: " + orderId);
        
        RealtimeListener listener = new RealtimeListener() {
            @Override
            public void onInsert(JsonObject record) {
                Log.d(TAG_REAL_TIME, "Order created: " + orderId);
                refreshOrder();
            }

            @Override
            public void onUpdate(JsonObject record, JsonObject oldRecord) {
                String newStatus = record.has("status") ? record.get("status").getAsString() : "unknown";
                String newDeliveryStatus = record.has("delivery_status") ? record.get("delivery_status").getAsString() : "unknown";
                Log.d(TAG_REAL_TIME, "Order updated - status: " + newStatus + ", delivery: " + newDeliveryStatus);
                refreshOrder();
            }

            @Override
            public void onDelete(JsonObject oldRecord) {
                Log.d(TAG_REAL_TIME, "Order deleted: " + orderId);
                runOnUiThread(() -> {
                    showErrorBanner("Đơn hàng đã bị huỷ !!!");
                    finish();
                });
            }

            @Override
            public void onConnected() {
                Log.d(TAG_REAL_TIME, "Connected to realtime for order: " + orderId);
            }

            @Override
            public void onDisconnected() {
                Log.w(TAG_REAL_TIME, "Disconnected from realtime for order: " + orderId);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG_REAL_TIME, "Realtime error for order " + orderId + ": " + error);
            }
        };
        
        orderChannel = SupabaseRealtimeClient.getInstance().subscribe("public:orders", "id=eq." + orderId, listener);
        Log.d(TAG_REAL_TIME, "Subscribed to order channel: " + (orderChannel != null ? "success" : "failed"));
    }

    /**
     * Refreshes the order detail by reloading from the database.
     * This is called when a real-time update is received from Supabase.
     */
    private void refreshOrder() {
        runOnUiThread(() -> {
            Log.d(TAG_REAL_TIME, "Refreshing order: " + currentOrderId);
            if (currentOrderId != null && !currentOrderId.isBlank()) {
                viewModel.loadOrderDetail(this, currentOrderId);
            }
        });
    }

}
