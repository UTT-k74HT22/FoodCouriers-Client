package com.utt.foodcouriers_client.ui.profile;

import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.ui.common.BaseActivity;

public class AddressFormActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_form);

        Toolbar toolbar = findViewById(R.id.toolbar);
        configureToolbar(toolbar, true);

        Button cancelButton = findViewById(R.id.btn_cancel);
        Button saveButton = findViewById(R.id.btn_save);
        cancelButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> finish());
    }
}
