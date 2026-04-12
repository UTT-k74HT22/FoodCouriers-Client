package com.utt.foodcouriers_client.ui.checkout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.repository.OrderRepository;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.order.OrderSuccessActivity;

public class CheckoutActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        configureToolbar(toolbar, true);
        setToolbarTitle(getString(R.string.checkout_title));

        Button placeOrderButton = findViewById(R.id.btn_place_order);
        placeOrderButton.setOnClickListener(v -> {
            OrderSummary order = OrderRepository.getInstance().createCheckoutOrder();
            Intent intent = new Intent(this, OrderSuccessActivity.class);
            intent.putExtra(OrderSuccessActivity.EXTRA_ORDER_ID, order.getId());
            startActivity(intent);
        });
    }
}
