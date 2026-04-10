package com.utt.foodcouriers_client.ui.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.Nullable;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.ui.common.BaseActivity;

public class ResetPasswordActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        ImageButton backButton = findViewById(R.id.btnBack);
        Button resetButton = findViewById(R.id.btnResetPassword);

        backButton.setOnClickListener(v -> finish());
        resetButton.setOnClickListener(v -> showSuccessBanner(getString(R.string.reset_password_success)));
    }
}
