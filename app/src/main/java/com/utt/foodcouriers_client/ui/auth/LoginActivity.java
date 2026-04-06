package com.utt.foodcouriers_client.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_client.ClientSystemMain;
import com.utt.foodcouriers_client.R;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailLayout = findViewById(R.id.tilEmail);
        passwordLayout = findViewById(R.id.tilPassword);
        emailInput = findViewById(R.id.etEmail);
        passwordInput = findViewById(R.id.etPassword);

        ImageButton backButton = findViewById(R.id.btnBack);
        Button loginButton = findViewById(R.id.btnLogin);
        TextView registerAction = findViewById(R.id.tvSwitchToRegister);

        backButton.setOnClickListener(v -> finish());
        registerAction.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
        loginButton.setOnClickListener(v -> {
            if (validateForm()) {
                startActivity(new Intent(this, ClientSystemMain.class));
                finish();
            }
        });
    }

    private boolean validateForm() {
        String email = getText(emailInput);
        String password = getText(passwordInput);

        emailLayout.setError(null);
        passwordLayout.setError(null);

        boolean valid = true;
        TextInputEditText firstInvalidField = null;

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.auth_validation_required));
            valid = false;
            firstInvalidField = emailInput;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.auth_validation_email));
            valid = false;
            firstInvalidField = emailInput;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.auth_validation_required));
            valid = false;
            if (firstInvalidField == null) {
                firstInvalidField = passwordInput;
            }
        } else if (password.length() < 6) {
            passwordLayout.setError(getString(R.string.auth_validation_password));
            valid = false;
            if (firstInvalidField == null) {
                firstInvalidField = passwordInput;
            }
        }

        if (firstInvalidField != null) {
            firstInvalidField.requestFocus();
        }

        return valid;
    }

    private String getText(TextInputEditText inputEditText) {
        return inputEditText.getText() == null ? "" : inputEditText.getText().toString().trim();
    }
}
