package com.utt.foodcouriers_client.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utt.foodcouriers_client.FoodCouriersClientApp;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.auth.SocialAuthProvider;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.UserProfile;
import com.utt.foodcouriers_client.data.remote.AuthClient;
import com.utt.foodcouriers_client.data.repository.AuthRepository;
import com.utt.foodcouriers_client.data.repository.SocialAuthRepository;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.main.MainActivity;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;

/**
 * Màn hình đăng ký tài khoản.
 *
 * <p>Ngoài đăng ký email/password, màn này dùng chung luồng Google OAuth với
 * {@link LoginActivity}. Khi user chọn Google, activity chỉ mở browser qua
 * {@link SocialAuthRepository}; token và profile sẽ được xử lý ở
 * {@link SocialAuthCallbackActivity} sau khi Supabase redirect về app.</p>
 */
public class RegisterActivity extends BaseActivity {

    private static final String TAG = "RegisterActivity";
    private static final long TOKEN_EXPIRY_MILLIS = 3600000L;

    private TextInputLayout tilName;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPhone;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private Button btnRegister;
    private android.view.View cardGoogleAuth;
    private android.view.View cardFacebookAuth;

    private AuthRepository authRepository;
    private SocialAuthRepository socialAuthRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = AuthRepository.getInstance();
        socialAuthRepository = SocialAuthRepository.getInstance();
        sessionManager = SessionManager.getInstance(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        cardGoogleAuth = findViewById(R.id.cardGoogleAuth);
        cardFacebookAuth = findViewById(R.id.cardFacebookAuth);
    }

    private void setupListeners() {
        ImageButton backButton = findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> finish());

        TextView loginAction = findViewById(R.id.tvSwitchToLogin);
        loginAction.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        cardGoogleAuth.setOnClickListener(v -> startSocialAuth(SocialAuthProvider.GOOGLE));
        cardFacebookAuth.setOnClickListener(v -> startSocialAuth(SocialAuthProvider.FACEBOOK));

        btnRegister.setOnClickListener(v -> {
            if (validateInput()) {
                performRegister();
            }
        });
    }

    private boolean validateInput() {
        String name = getName();
        String email = getEmail();
        String phone = getPhone();
        String password = getPassword();
        String confirmPassword = getConfirmPassword();

        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.error_name_required));
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_email_required));
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            return false;
        }

        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError(getString(R.string.error_phone_required));
            return false;
        }

        if (phone.length() < 10) {
            tilPhone.setError(getString(R.string.error_phone_invalid));
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

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return false;
        }

        return true;
    }

    /**
     * Gửi thông tin đăng ký lên Supabase Auth/app profile, lưu session và bật realtime.
     */
    private void performRegister() {
        String name = getName().trim();
        String email = getEmail().trim();
        String phone = getPhone().trim();
        String password = getPassword();

        showLoading(true);

        authRepository.register(name, email, phone, password, new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile user) {
                showLoading(false);

                String accessToken = AuthClient.getInstance().getAccessToken();
                String refreshToken = AuthClient.getInstance().getRefreshToken();

                sessionManager.saveSession(accessToken, refreshToken, user, TOKEN_EXPIRY_MILLIS);
                FoodCouriersClientApp.initializeRealtime(accessToken);

                ToastBanner.showSuccess(getString(R.string.register_success));
                navigateToMain();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                if ("EMAIL_CONFIRM_REQUIRED".equals(error)) {
                    ToastBanner.showSuccess(getString(R.string.register_success_confirm_email));
                    finish();
                } else {
                    ToastBanner.showError(error);
                }
            }
        });
    }

    /**
     * Bắt đầu luồng OAuth từ màn Register.
     *
     * @param provider provider user vừa chọn
     */
    private void startSocialAuth(SocialAuthProvider provider) {
        Log.d(TAG, "Step 1: User requested social auth | provider=" + provider.getValue());
        socialAuthRepository.startAuth(this, provider, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Log.d(TAG, "Step 2: Social auth launch delegated to manager | provider=" + provider.getValue());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Step 2: Social auth launch failed | provider=" + provider.getValue() + ", error=" + error);
                showWarningBanner(error);
            }
        });
    }

    private String getName() {
        return etName.getText() != null ? etName.getText().toString().trim() : "";
    }

    private String getEmail() {
        return etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
    }

    private String getPhone() {
        return etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
    }

    private String getPassword() {
        return etPassword.getText() != null ? etPassword.getText().toString() : "";
    }

    private String getConfirmPassword() {
        return etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";
    }

    private void showLoading(boolean show) {
        btnRegister.setEnabled(!show);
        btnRegister.setText(show ? "" : getString(R.string.register_cta));
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
