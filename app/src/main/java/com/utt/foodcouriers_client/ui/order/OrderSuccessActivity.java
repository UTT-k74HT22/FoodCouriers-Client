package com.utt.foodcouriers_client.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.repository.OrderRepository;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.main.MainActivity;

public class OrderSuccessActivity extends BaseActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_success);

        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        OrderSummary order = OrderRepository.getInstance().getOrderById(orderId);

        TextView tvOrderCode = findViewById(R.id.tv_order_code);
        TextView tvTotal = findViewById(R.id.tv_total);
        Button backHomeButton = findViewById(R.id.btn_back_home);
        Button trackOrderButton = findViewById(R.id.btn_track_order);

        if (order != null) {
            tvOrderCode.setText(order.getOrderCode());
            tvTotal.setText(String.format("%,d đ", order.getTotal()));
        }

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
