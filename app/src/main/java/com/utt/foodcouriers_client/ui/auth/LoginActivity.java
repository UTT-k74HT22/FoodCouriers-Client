package com.utt.foodcouriers_client.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.main.MainActivity;

public class LoginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ImageButton backButton = findViewById(R.id.btnBack);
        Button loginButton = findViewById(R.id.btnLogin);
        TextView registerAction = findViewById(R.id.tvSwitchToRegister);
        TextView forgotPasswordAction = findViewById(R.id.tvForgotPassword);

        backButton.setOnClickListener(v -> finish());
        registerAction.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
        forgotPasswordAction.setOnClickListener(v -> startActivity(new Intent(this, ResetPasswordActivity.class)));
        loginButton.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
