package com.utt.foodcouriers_client.ui.order;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.JsonObject;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.DeliveryStatus;
import com.utt.foodcouriers_client.data.model.OrderStatus;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.remote.SupabaseRealtimeClient;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.utils.websocket.RealtimeChannel;
import com.utt.foodcouriers_client.utils.websocket.RealtimeListener;
import com.utt.foodcouriers_client.viewmodel.OrdersViewModel;

public class OrderTrackingActivity extends BaseActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";
    public static final String TAG_REAL_TIME = "RealTimeWebSocket";

    private OrdersViewModel viewModel;
    private TextView tvOrderStatus;
    private View stepConfirmed;
    private View stepPreparing;
    private View stepDelivering;
    private View stepDelivered;
    private RealtimeChannel orderChannel;
    private String currentOrderId;


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
            showErrorBanner("Không có thông tin đơn hàng.");
            finish();
            return;
        }
        viewModel.loadOrderDetail(this, orderId);
        currentOrderId = orderId;
        subscribeToOrderUpdates(orderId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderChannel != null) {
            SupabaseRealtimeClient.getInstance().unsubscribe(orderChannel);
            orderChannel = null;
        }
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
            if (currentOrderId != null) {
                viewModel.loadOrderDetail(this, currentOrderId);
            }
        });
    }
}
