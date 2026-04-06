package com.utt.foodcouriers_client.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_client.ClientSystemMain;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.remote.AuthSession;
import com.utt.foodcouriers_client.data.remote.BaseResponse;
import com.utt.foodcouriers_client.data.repository.AuthRepository;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Button loginButton;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailLayout = findViewById(R.id.tilEmail);
        passwordLayout = findViewById(R.id.tilPassword);
        emailInput = findViewById(R.id.etEmail);
        passwordInput = findViewById(R.id.etPassword);
        loginButton = findViewById(R.id.btnLogin);
        authRepository = new AuthRepository(this);

        ImageButton backButton = findViewById(R.id.btnBack);
        TextView registerAction = findViewById(R.id.tvSwitchToRegister);

        backButton.setOnClickListener(v -> finish());
        registerAction.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        clearAuthErrors();

        if (!validateForm()) {
            return;
        }

        String email = getText(emailInput);
        String password = getText(passwordInput);

        Log.d(TAG, "Start authenticate user with email: " + email);
        setLoading(true);

        authRepository.login(email, password, result ->
                runOnUiThread(() -> handleLoginResult(email, result))
        );
    }

    private void handleLoginResult(String email, BaseResponse<AuthSession> result) {
        setLoading(false);

        if (result instanceof BaseResponse.Error) {
            BaseResponse.Error<AuthSession> error = (BaseResponse.Error<AuthSession>) result;
            passwordLayout.setError(error.getMessage());
            passwordInput.requestFocus();
            Log.w(TAG, "Authenticate failed for email: " + email + ", code=" + error.getCode());
            return;
        }

        clearAuthErrors();
        Log.d(TAG, "Authenticate successfully with email: " + email);
        startActivity(new Intent(this, ClientSystemMain.class));
        finish();
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

    private void clearAuthErrors() {
        emailLayout.setError(null);
        passwordLayout.setError(null);
    }

    private String getText(TextInputEditText inputEditText) {
        return inputEditText.getText() == null ? "" : inputEditText.getText().toString().trim();
    }

    private void setLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        loginButton.setText(isLoading ? "Loading..." : getString(R.string.login_cta));
    }
}
