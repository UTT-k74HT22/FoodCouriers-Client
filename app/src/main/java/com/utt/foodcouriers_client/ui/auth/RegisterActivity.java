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

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameLayout = findViewById(R.id.tilName);
        emailLayout = findViewById(R.id.tilEmail);
        phoneLayout = findViewById(R.id.tilPhone);
        passwordLayout = findViewById(R.id.tilPassword);
        confirmPasswordLayout = findViewById(R.id.tilConfirmPassword);

        nameInput = findViewById(R.id.etName);
        emailInput = findViewById(R.id.etEmail);
        phoneInput = findViewById(R.id.etPhone);
        passwordInput = findViewById(R.id.etPassword);
        confirmPasswordInput = findViewById(R.id.etConfirmPassword);

        ImageButton backButton = findViewById(R.id.btnBack);
        Button registerButton = findViewById(R.id.btnRegister);
        TextView loginAction = findViewById(R.id.tvSwitchToLogin);

        backButton.setOnClickListener(v -> finish());
        loginAction.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        registerButton.setOnClickListener(v -> {
            if (validateForm()) {
                startActivity(new Intent(this, ClientSystemMain.class));
                finish();
            }
        });
    }

    private boolean validateForm() {
        String name = getText(nameInput);
        String email = getText(emailInput);
        String phone = getText(phoneInput).replaceAll("\\s+", "");
        String normalizedPhone = phone.replaceAll("[^0-9]", "");
        String password = getText(passwordInput);
        String confirmPassword = getText(confirmPasswordInput);

        nameLayout.setError(null);
        emailLayout.setError(null);
        phoneLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);

        boolean valid = true;
        TextInputEditText firstInvalidField = null;

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError(getString(R.string.auth_validation_required));
            valid = false;
            firstInvalidField = nameInput;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.auth_validation_required));
            valid = false;
            if (firstInvalidField == null) {
                firstInvalidField = emailInput;
            }
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.auth_validation_email));
            valid = false;
            if (firstInvalidField == null) {
                firstInvalidField = emailInput;
            }
        }

        if (TextUtils.isEmpty(phone)) {
            phoneLayout.setError(getString(R.string.auth_validation_required));
            valid = false;
            if (firstInvalidField == null) {
                firstInvalidField = phoneInput;
            }
        } else if (normalizedPhone.length() < 9 || normalizedPhone.length() != phone.length()) {
            phoneLayout.setError(getString(R.string.auth_validation_phone));
            valid = false;
            if (firstInvalidField == null) {
                firstInvalidField = phoneInput;
            }
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

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.auth_validation_required));
            valid = false;
            if (firstInvalidField == null) {
                firstInvalidField = confirmPasswordInput;
            }
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.auth_validation_password_match));
            valid = false;
            if (firstInvalidField == null) {
                firstInvalidField = confirmPasswordInput;
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
