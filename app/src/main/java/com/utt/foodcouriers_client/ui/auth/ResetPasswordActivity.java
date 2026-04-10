package com.utt.foodcouriers_client.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.Nullable;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.repository.AuthRepository;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.utils.ToastBanner;

public class ResetPasswordActivity extends BaseActivity {

    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private Button btnResetPassword;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        authRepository = AuthRepository.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
    }

    private void setupListeners() {
        ImageButton backButton = findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> finish());

        btnResetPassword.setOnClickListener(v -> {
            if (validateInput()) {
                performResetPassword();
            }
        });
    }

    private boolean validateInput() {
        String email = getEmail();

        tilEmail.setError(null);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_email_required));
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            return false;
        }

        return true;
    }

    private void performResetPassword() {
        String email = getEmail().trim();

        showLoading(true);

        authRepository.resetPassword(email, new com.utt.foodcouriers_client.data.common.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                showLoading(false);
                ToastBanner.showSuccess(getString(R.string.reset_password_success));
                finish();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                ToastBanner.showError(error);
            }
        });
    }

    private String getEmail() {
        return etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
    }

    private void showLoading(boolean show) {
        btnResetPassword.setEnabled(!show);
        btnResetPassword.setText(show ? "" : getString(R.string.reset_password_cta));
    }
}
