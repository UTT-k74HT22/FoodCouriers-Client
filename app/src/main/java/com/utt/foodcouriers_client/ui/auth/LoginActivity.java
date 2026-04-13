package com.utt.foodcouriers_client.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.auth.SocialAuthProvider;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.UserProfile;
import com.utt.foodcouriers_client.data.repository.AuthRepository;
import com.utt.foodcouriers_client.data.repository.SocialAuthRepository;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.main.MainActivity;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;

public class LoginActivity extends BaseActivity {
    private static final String TAG = "LoginActivity";
    private static final long TOKEN_EXPIRY_MILLIS = 3600000L;

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private TextView tvSwitchToRegister;
    private TextView tvForgotPassword;
    private android.view.View cardGoogleAuth;
    private android.view.View cardFacebookAuth;

    private AuthRepository authRepository;
    private SocialAuthRepository socialAuthRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        authRepository = AuthRepository.getInstance();
        socialAuthRepository = SocialAuthRepository.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSwitchToRegister = findViewById(R.id.tvSwitchToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cardGoogleAuth = findViewById(R.id.cardGoogleAuth);
        cardFacebookAuth = findViewById(R.id.cardFacebookAuth);
    }

    private void setupListeners() {
        ImageButton backButton = findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> finish());

        registerAction();
        forgotPasswordAction();
        socialAuthActions();
        loginAction();
    }

    private void socialAuthActions() {
        cardGoogleAuth.setOnClickListener(v -> startSocialAuth(SocialAuthProvider.GOOGLE));
        cardFacebookAuth.setOnClickListener(v -> startSocialAuth(SocialAuthProvider.FACEBOOK));
    }

    private void registerAction() {
        tvSwitchToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void forgotPasswordAction() {
        tvForgotPassword.setOnClickListener(v -> 
            startActivity(new Intent(this, ResetPasswordActivity.class))
        );
    }

    private void loginAction() {
        btnLogin.setOnClickListener(v -> {
            Log.d(TAG, "loginAction called - attempting login");
            if (validateInput()) {
                performLogin();
            }
        });
    }

    private boolean validateInput() {
        String email = getEmail();
        String password = getPassword();

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_email_required));
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_password_required));
            return false;
        }

        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_min_length));
            return false;
        }

        return true;
    }

    private void performLogin() {
        String email = getEmail().trim();
        String password = getPassword();

        Log.d(TAG, "performLogin: email=" + email);
        showLoading(true);

        authRepository.login(email, password, new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile user) {
                Log.d(TAG, "performLogin onSuccess: user=" + (user != null ? user.getEmail() : "null"));
                showLoading(false);

                if (!user.isActive()) {
                    Log.w(TAG, "performLogin: user account is disabled");
                    authRepository.logout(new RepositoryCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                        }

                        @Override
                        public void onError(String error) {
                        }
                    });
                    ToastBanner.showError(getString(R.string.error_account_disabled));
                    return;
                }

                String accessToken = com.utt.foodcouriers_client.data.remote.AuthClient.getInstance().getAccessToken();
                String refreshToken = com.utt.foodcouriers_client.data.remote.AuthClient.getInstance().getRefreshToken();

                Log.d(TAG, "performLogin: accessToken=" + (accessToken != null ? "present" : "null") + ", refreshToken=" + (refreshToken != null ? "present" : "null"));
                sessionManager.saveSession(accessToken, refreshToken, user, TOKEN_EXPIRY_MILLIS);

                ToastBanner.showSuccess(getString(R.string.login_success));
                navigateToMain();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "performLogin onError: " + error);
                showLoading(false);
                ToastBanner.showError(error);
            }
        });
    }

    private void startSocialAuth(SocialAuthProvider provider) {
        socialAuthRepository.startAuth(this, provider, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
            }

            @Override
            public void onError(String error) {
                showWarningBanner(error);
            }
        });
    }

    private String getEmail() {
        return etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
    }

    private String getPassword() {
        return etPassword.getText() != null ? etPassword.getText().toString() : "";
    }

    private void showLoading(boolean show) {
        btnLogin.setEnabled(!show);
        btnLogin.setText(show ? "" : getString(R.string.login_cta));
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
