package com.utt.foodcouriers_client.ui.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.auth.SocialAuthResult;
import com.utt.foodcouriers_client.data.repository.SocialAuthRepository;
import com.utt.foodcouriers_client.ui.common.BaseActivity;

/**
 * Màn hình nhận deep link sau khi user đăng nhập Google trên browser.
 *
 * <p>Activity này là điểm nối giữa Android intent và tầng repository:</p>
 *
 * <ol>
 *     <li>Android mở activity bằng URI {@code com.utt.foodcouriers.client://auth/callback...}.</li>
 *     <li>{@link #processCallback(Uri)} chuyển URI cho {@link SocialAuthRepository}.</li>
 *     <li>Repository/manager parse token, bootstrap profile, lưu session và bật realtime.</li>
 *     <li>Nếu thành công, activity mở {@link com.utt.foodcouriers_client.ui.main.MainActivity};
 *     nếu lỗi, màn hình hiển thị trạng thái để user retry hoặc quay lại đăng nhập.</li>
 * </ol>
 */
public class SocialAuthCallbackActivity extends BaseActivity {
    private static final String TAG = "SocialAuthCallback";

    private ImageView ivStatusIcon;
    private TextView tvStatusEyebrow;
    private TextView tvStatusTitle;
    private TextView tvStatusSubtitle;
    private TextView tvProviderValue;
    private TextView tvStateValue;
    private TextView tvCallbackMeta;
    private CircularProgressIndicator progressCallback;
    private Button btnPrimaryAction;
    private Button btnSecondaryAction;
    private SocialAuthRepository socialAuthRepository;
    private SocialAuthResult lastResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_auth_callback);

        socialAuthRepository = SocialAuthRepository.getInstance();

        bindViews();
        bindActions();
        renderProcessing();
        Log.d(TAG, "Step 1: Callback activity created | uri=" + describeUri(getIntent() != null ? getIntent().getData() : null));
        processCallback(getIntent() != null ? getIntent().getData() : null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        renderProcessing();
        Log.d(TAG, "Step 1: Callback activity received new intent | uri=" + describeUri(intent != null ? intent.getData() : null));
        processCallback(intent != null ? intent.getData() : null);
    }

    private void bindViews() {
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        tvStatusEyebrow = findViewById(R.id.tvStatusEyebrow);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusSubtitle = findViewById(R.id.tvStatusSubtitle);
        tvProviderValue = findViewById(R.id.tvProviderValue);
        tvStateValue = findViewById(R.id.tvStateValue);
        tvCallbackMeta = findViewById(R.id.tvCallbackMeta);
        progressCallback = findViewById(R.id.progressCallback);
        btnPrimaryAction = findViewById(R.id.btnPrimaryAction);
        btnSecondaryAction = findViewById(R.id.btnSecondaryAction);
    }

    private void bindActions() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> openLogin());

        btnPrimaryAction.setOnClickListener(v -> {
            if (lastResult != null && lastResult.isSuccessful()) {
                retryProfileBootstrap();
                return;
            }
            renderProcessing();
            processCallback(getIntent() != null ? getIntent().getData() : null);
        });

        btnSecondaryAction.setOnClickListener(v -> openLogin());
    }

    /**
     * Gửi URI callback xuống repository và render kết quả.
     *
     * @param data deep link data lấy từ intent hiện tại
     */
    private void processCallback(Uri data) {
        Log.d(TAG, "Step 2: Forwarding callback to repository | uri=" + describeUri(data));
        socialAuthRepository.handleCallback(this, data, new com.utt.foodcouriers_client.data.common.RepositoryCallback<SocialAuthResult>() {
            @Override
            public void onSuccess(SocialAuthResult result) {
                lastResult = result;
                Log.d(
                        TAG,
                        "Step 3: Repository completed callback handling | provider="
                                + (result.getProvider() != null ? result.getProvider().getValue() : "unknown")
                                + ", success=" + result.isSuccessful()
                                + ", errorCode=" + (result.getErrorCode() != null ? result.getErrorCode() : "n/a")
                );

                if (result.isSuccessful()) {
                    showSuccessBanner(getString(R.string.login_success));
                    openMain();
                } else {
                    renderError(result);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Step 3: Repository failed callback handling | error=" + error);
                renderFallbackError(error);
            }
        });
    }

    private String describeUri(Uri uri) {
        return uri != null ? uri.toString() : "null";
    }

    private void openMain() {
        Intent intent = new Intent(this, com.utt.foodcouriers_client.ui.main.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Hiển thị trạng thái đang xử lý trong lúc app parse URI và bootstrap profile.
     */
    private void renderProcessing() {
        lastResult = null;
        ivStatusIcon.setImageResource(R.drawable.ic_route_path);
        ivStatusIcon.setColorFilter(getColor(R.color.primary));
        progressCallback.show();
        tvStatusEyebrow.setText(R.string.social_auth_processing_eyebrow);
        tvStatusTitle.setText(R.string.social_auth_processing_title);
        tvStatusSubtitle.setText(R.string.social_auth_processing_subtitle);
        tvProviderValue.setText(R.string.social_auth_provider_unknown);
        tvStateValue.setText(R.string.social_auth_state_pending);
        tvCallbackMeta.setText(R.string.social_auth_meta_waiting);
        btnPrimaryAction.setText(R.string.social_auth_primary_retry);
        btnSecondaryAction.setText(R.string.social_auth_secondary_sign_in);
    }

    /**
     * Hiển thị trạng thái thành công nếu cần giữ user ở màn callback để bootstrap lại.
     *
     * @param result kết quả OAuth đã có đủ token
     */
    private void renderSuccess(@NonNull SocialAuthResult result) {
        progressCallback.hide();
        ivStatusIcon.setImageResource(R.drawable.ic_receipt);
        ivStatusIcon.setColorFilter(getColor(R.color.primary));
        tvStatusEyebrow.setText(R.string.social_auth_success_eyebrow);
        tvStatusTitle.setText(R.string.social_auth_success_title);
        tvStatusSubtitle.setText(R.string.social_auth_success_subtitle);
        tvProviderValue.setText(resolveProviderName(result));
        tvStateValue.setText(R.string.social_auth_state_tokens_detected);
        tvCallbackMeta.setText(R.string.social_auth_meta_success);
        btnPrimaryAction.setText(R.string.social_auth_primary_bootstrap);
        btnSecondaryAction.setText(R.string.social_auth_secondary_sign_in);
    }

    /**
     * Hiển thị lỗi OAuth đã parse được từ callback.
     *
     * @param result kết quả chứa provider/lỗi/missing payload để user dễ hiểu đang hỏng ở bước nào
     */
    private void renderError(@NonNull SocialAuthResult result) {
        progressCallback.hide();
        ivStatusIcon.setImageResource(R.drawable.ic_warning_circle);
        ivStatusIcon.clearColorFilter();
        tvStatusEyebrow.setText(R.string.social_auth_error_eyebrow);
        tvStatusTitle.setText(R.string.social_auth_error_title);
        tvStatusSubtitle.setText(resolveErrorMessage(result));
        tvProviderValue.setText(resolveProviderName(result));
        tvStateValue.setText(result.hasError()
                ? R.string.social_auth_state_error
                : R.string.social_auth_state_missing_payload);
        if (result.hasError() && result.getErrorCode() != null && !result.getErrorCode().trim().isEmpty()) {
            tvCallbackMeta.setText(getString(R.string.social_auth_meta_error, result.getErrorCode()));
        } else {
            tvCallbackMeta.setText(R.string.social_auth_meta_missing);
        }
        btnPrimaryAction.setText(R.string.social_auth_primary_retry);
        btnSecondaryAction.setText(R.string.social_auth_secondary_sign_in);
    }

    private void renderFallbackError(@NonNull String error) {
        progressCallback.hide();
        ivStatusIcon.setImageResource(R.drawable.ic_warning_circle);
        ivStatusIcon.clearColorFilter();
        tvStatusEyebrow.setText(R.string.social_auth_error_eyebrow);
        tvStatusTitle.setText(R.string.social_auth_error_title);
        tvStatusSubtitle.setText(error);
        tvProviderValue.setText(R.string.social_auth_provider_unknown);
        tvStateValue.setText(R.string.social_auth_state_error);
        tvCallbackMeta.setText(R.string.social_auth_meta_missing);
        btnPrimaryAction.setText(R.string.social_auth_primary_retry);
        btnSecondaryAction.setText(R.string.social_auth_secondary_sign_in);
    }

    /**
     * Thử lại bước lấy/tạo profile bằng token OAuth đã lưu tạm, không mở browser lần nữa.
     */
    private void retryProfileBootstrap() {
        socialAuthRepository.retryProfileBootstrap(this, new com.utt.foodcouriers_client.data.common.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                showSuccessBanner(getString(R.string.login_success));
                openMain();
            }

            @Override
            public void onError(String error) {
                showWarningBanner(error);
            }
        });
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String resolveProviderName(@NonNull SocialAuthResult result) {
        return result.getProvider() != null
                ? result.getProvider().getDisplayName()
                : getString(R.string.social_auth_provider_unknown);
    }

    private String resolveErrorMessage(@NonNull SocialAuthResult result) {
        if (result.getErrorDescription() != null && !result.getErrorDescription().trim().isEmpty()) {
            return result.getErrorDescription();
        }
        return getString(R.string.social_auth_error_subtitle);
    }
}
