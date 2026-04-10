package com.utt.foodcouriers_client.ui.order;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.ui.common.BaseActivity;

public class OrderDetailActivity extends BaseActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        configureToolbar(toolbar, true);
    }
}
