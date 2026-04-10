package com.utt.foodcouriers_client.ui.restaurant;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.ui.common.BaseActivity;

public class RestaurantDetailActivity extends BaseActivity {

    public static final String EXTRA_RESTAURANT_ID = "extra_restaurant_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        configureToolbar(toolbar, true);
        toolbar.setTitle(getString(R.string.home_section_featured));
    }
}
