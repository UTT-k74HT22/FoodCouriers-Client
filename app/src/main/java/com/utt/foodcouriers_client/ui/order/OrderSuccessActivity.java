package com.utt.foodcouriers_client.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.repository.OrderRepository;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.main.MainActivity;

/**
 * Màn hình xác nhận tạo đơn thành công.
 *
 * <p>Activity nhận order id từ checkout/payment flow, load lại order để hiển thị mã đơn
 * và tổng tiền, rồi cho user quay về trang chủ hoặc mở {@link OrderTrackingActivity}
 * để theo dõi realtime.</p>
 */
public class OrderSuccessActivity extends BaseActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    private TextView tvOrderCode;
    private TextView tvTotal;
    private String orderId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_success);

        tvOrderCode = findViewById(R.id.tv_order_code);
        tvTotal = findViewById(R.id.tv_total);
        Button backHomeButton = findViewById(R.id.btn_back_home);
        Button trackOrderButton = findViewById(R.id.btn_track_order);

        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        
        OrderRepository.getInstance().getOrderById(this, orderId, new RepositoryCallback<OrderSummary>() {
            @Override
            public void onSuccess(OrderSummary order) {
                if (order != null) {
                    tvOrderCode.setText(order.getOrderCode());
                    tvTotal.setText(String.format("%,d đ", order.getTotal()));
                }
            }

            @Override
            public void onError(String error) {
                tvOrderCode.setText("N/A");
                tvTotal.setText("N/A");
            }
        });

        backHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        trackOrderButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderTrackingActivity.class);
            intent.putExtra(OrderTrackingActivity.EXTRA_ORDER_ID, orderId);
            startActivity(intent);
        });
    }
}
