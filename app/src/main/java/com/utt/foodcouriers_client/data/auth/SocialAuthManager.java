package com.utt.foodcouriers_client.data.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.utt.foodcouriers_client.FoodCouriersClientApp;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.UserProfile;
import com.utt.foodcouriers_client.data.remote.AuthClient;
import com.utt.foodcouriers_client.data.remote.SupabaseConfig;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.SessionStore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Điều phối toàn bộ luồng đăng nhập mạng xã hội qua Supabase OAuth.
 *
 * <p>Hiện tại app chỉ mở luồng Google thật sự; Facebook vẫn được giữ trong enum/UI để
 * dễ mở rộng sau. Luồng Google chạy như sau:</p>
 *
 * <ol>
 *     <li>{@link com.utt.foodcouriers_client.ui.auth.LoginActivity} hoặc
 *     {@link com.utt.foodcouriers_client.ui.auth.RegisterActivity} gọi
 *     {@link #launchProvider(Context, SocialAuthProvider, RepositoryCallback)}.</li>
 *     <li>Manager tạo URL {@code /auth/v1/authorize} của Supabase, lưu provider đang chờ,
 *     rồi mở browser để user đăng nhập Google.</li>
 *     <li>Supabase redirect về deep link {@code com.utt.foodcouriers.client://auth/callback}
 *     và {@link com.utt.foodcouriers_client.ui.auth.SocialAuthCallbackActivity} chuyển
 *     URI đó vào {@link #handleCallback(Context, Uri, RepositoryCallback)}.</li>
 *     <li>Manager đọc token trong query hoặc fragment của URI, lưu session thô vào
 *     {@link SessionStore}, gọi {@link AuthClient#bootstrapSocialSession(String, String, AuthClient.ApiCallback)}
 *     để lấy/tạo hồ sơ nghiệp vụ trong bảng {@code public.users}.</li>
 *     <li>Khi profile hợp lệ, session chính được lưu vào {@link SessionManager} và
 *     realtime được bật qua {@link FoodCouriersClientApp#initializeRealtime(String)}.</li>
 * </ol>
 */
public class SocialAuthManager {
    public static final String TAG = "SocialAuthManager";

    public static final String CALLBACK_SCHEME = "com.utt.foodcouriers.client";
    public static final String CALLBACK_HOST = "auth";
    public static final String CALLBACK_PATH = "/callback";

    private static final String PREF_SOCIAL_AUTH = "social_auth_pref";
    private static final String KEY_PENDING_PROVIDER = "pending_provider";
    private static final long DEFAULT_TOKEN_EXPIRY_MILLIS = 3600000L;

    private static SocialAuthManager instance;

    /**
     * Trả về singleton dùng chung cho mọi màn hình auth.
     *
     * @return instance duy nhất của {@link SocialAuthManager}
     */
    public static synchronized SocialAuthManager getInstance() {
        if (instance == null) {
            instance = new SocialAuthManager();
        }
        return instance;
    }

    /**
     * Bắt đầu đăng nhập Google bằng cách mở browser đến Supabase OAuth authorize URL.
     *
     * <p>Phương thức này không tự lấy token. Nó chỉ chuẩn bị redirect URI, lưu provider
     * đang chờ để callback có thể nhận diện lại, rồi giao quyền cho browser/Supabase.</p>
     *
     * @param context context dùng để mở browser và lấy string resource
     * @param provider provider mà user chọn; hiện chỉ {@link SocialAuthProvider#GOOGLE} được hỗ trợ
     * @param callback trả {@code true} khi browser được mở, hoặc lỗi nếu provider chưa hỗ trợ
     */
    public void launchProvider(
            @NonNull Context context,
            @NonNull SocialAuthProvider provider,
            @NonNull RepositoryCallback<Boolean> callback
    ) {
        logStep(1, "Preparing Supabase OAuth launch", "provider=" + provider.getValue());

        if (provider != SocialAuthProvider.GOOGLE) {
            callback.onError(context.getString(R.string.social_auth_stub_facebook));
            return;
        }

        try {
            String redirectTo = buildRedirectUri();
            savePendingProvider(context, provider);

            String authorizeUrl = SupabaseConfig.SUPABASE_URL
                    + "/auth/v1/authorize"
                    + "?provider=" + Uri.encode(provider.getValue())
                    + "&redirect_to=" + Uri.encode(redirectTo);

            logStep(
                    2,
                    "Opening browser for Supabase OAuth",
                    "redirectTo=" + redirectTo + ", authorizeUrl=" + authorizeUrl
            );

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(browserIntent);

            callback.onSuccess(true);
        } catch (Exception e) {
            logStepError(2, "Failed to open Supabase OAuth browser flow", e);
            callback.onError("Could not open the Google sign-in flow: " + e.getMessage());
        }
    }

    /**
     * Xử lý deep link callback sau khi Supabase hoàn tất hoặc hủy OAuth.
     *
     * <p>Supabase có thể trả token trong phần query hoặc fragment của URI. Vì vậy
     * {@link #parseCallback(Uri)} đọc cả hai phần, sau đó phương thức này kiểm tra deep link
     * có đúng scheme/host/path của app không. Nếu token hợp lệ, flow tiếp tục bootstrap
     * profile nghiệp vụ và lưu session đăng nhập.</p>
     *
     * @param context context dùng để đọc pending provider, lưu session và lấy message lỗi
     * @param uri deep link trả về từ browser/Supabase
     * @param callback trả {@link SocialAuthResult}; nếu bootstrap profile lỗi thì trả qua {@code onError}
     */
    public void handleCallback(
            @NonNull Context context,
            @Nullable Uri uri,
            @NonNull RepositoryCallback<SocialAuthResult> callback
    ) {
        try {
            logStep(3, "Received OAuth callback", "uri=" + (uri != null ? uri.toString() : "null"));

            SocialAuthResult result = enrichProvider(parseCallback(uri), getPendingProvider(context));

            if (!isExpectedCallbackUri(uri)) {
                clearPendingProvider(context);
                callback.onSuccess(new SocialAuthResult(
                        null,
                        null,
                        null,
                        "invalid_callback_uri",
                        "The callback deep link does not match the configured app redirect.",
                        uri != null ? uri.toString() : null,
                        0L
                ));
                return;
            }
            clearPendingProvider(context);

            if (result.isSuccessful()) {
                logStep(
                        4,
                        "Supabase callback contained session payload",
                        "provider=" + resolveProviderValue(result)
                                + ", accessToken=present, refreshToken=present"
                );
                SessionStore.saveSession(context, result);
                bootstrapSession(context, result, callback);
                return;
            }

            logStep(
                    4,
                    "Supabase callback did not complete login",
                    "provider=" + resolveProviderValue(result)
                            + ", errorCode=" + safeValue(result.getErrorCode())
                            + ", errorDescription=" + safeValue(result.getErrorDescription())
            );
            callback.onSuccess(result);
        } catch (Exception e) {
            clearPendingProvider(context);
            logStepError(4, "Failed to process Supabase OAuth callback", e);
            callback.onError("Failed to process the social auth callback: " + e.getMessage());
        }
    }

    /**
     * Chạy lại bước bootstrap profile khi app đã có token OAuth thô trong {@link SessionStore}.
     *
     * <p>Trường hợp này dùng cho màn hình callback khi URI đã có access token nhưng việc gọi
     * Supabase để lấy/tạo profile bị lỗi tạm thời. User bấm thử lại thì không cần mở lại Google.</p>
     *
     * @param context context dùng để đọc session thô và lưu session chính
     * @param callback trả {@code true} khi bootstrap lại thành công
     */
    public void retryProfileBootstrap(@NonNull Context context, @NonNull RepositoryCallback<Boolean> callback) {
        if (SessionStore.hasSession(context)) {
            bootstrapSession(context, buildStoredSessionResult(context), new RepositoryCallback<SocialAuthResult>() {
                @Override
                public void onSuccess(SocialAuthResult result) {
                    callback.onSuccess(true);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
        } else {
            callback.onError("No social auth session is available to bootstrap.");
        }
    }

    /**
     * Kiểm tra nhanh app còn session OAuth thô để tiếp tục luồng social auth hay không.
     *
     * @param context context dùng để đọc {@link SessionStore}
     * @param callback trả {@code true} nếu session thô còn tồn tại
     */
    public void refreshSession(@NonNull Context context, @NonNull RepositoryCallback<Boolean> callback) {
        if (SessionStore.hasSession(context)) {
            callback.onSuccess(true);
        } else {
            callback.onError("No social auth session is available to refresh.");
        }
    }

    /**
     * Chuyển deep link OAuth thành object kết quả dễ xử lý trong app.
     *
     * <p>OAuth redirect thường có hai dạng dữ liệu: query
     * ({@code ?error=...}) hoặc fragment ({@code #access_token=...}). Hàm này gom cả hai
     * vào một map, đọc provider/token/lỗi, rồi trả về {@link SocialAuthResult} thống nhất.</p>
     *
     * @param uri URI callback nhận từ Activity intent
     * @return kết quả đã parse; không ném lỗi khi thiếu token mà trả message mô tả trạng thái
     */
    @NonNull
    public SocialAuthResult parseCallback(@Nullable Uri uri) {
        if (uri == null) {
            return new SocialAuthResult(
                    null,
                    null,
                    null,
                    null,
                    "No callback data was received from the browser redirect.",
                    null,
                    0L
            );
        }

        Map<String, String> parameters = extractParameters(uri);

        String rawProvider = firstNonBlank(
                parameters.get("provider"),
                parameters.get("provider_name"),
                resolveProviderFromState(parameters.get("state"))
        );

        String accessToken = parameters.get("access_token");
        String refreshToken = parameters.get("refresh_token");
        String errorCode = firstNonBlank(parameters.get("error_code"), parameters.get("error"));
        String errorDescription = firstNonBlank(
                parameters.get("error_description"),
                parameters.get("error"),
                parameters.get("message")
        );

        if (TextUtils.isEmpty(errorDescription) && TextUtils.isEmpty(accessToken) && TextUtils.isEmpty(refreshToken)) {
            errorDescription = "Callback reached the app, but no session payload was found yet.";
        }

        return new SocialAuthResult(
                SocialAuthProvider.fromValue(rawProvider),
                accessToken,
                refreshToken,
                errorCode,
                errorDescription,
                uri.toString(),
                parseLong(parameters.get("expires_in"))
        );
    }

    @NonNull
    private Map<String, String> extractParameters(@NonNull Uri uri) {
        Map<String, String> parameters = new LinkedHashMap<>();
        appendParameters(parameters, uri.getQuery());
        appendParameters(parameters, uri.getFragment());
        return parameters;
    }

    private boolean isExpectedCallbackUri(@Nullable Uri uri) {
        if (uri == null) {
            return false;
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        String path = uri.getPath();

        return CALLBACK_SCHEME.equalsIgnoreCase(scheme)
                && CALLBACK_HOST.equalsIgnoreCase(host)
                && path != null
                && path.startsWith(CALLBACK_PATH);
    }

    @NonNull
    private String buildRedirectUri() {
        return CALLBACK_SCHEME + "://" + CALLBACK_HOST + CALLBACK_PATH;
    }

    private void savePendingProvider(@NonNull Context context, @NonNull SocialAuthProvider provider) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SOCIAL_AUTH, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_PENDING_PROVIDER, provider.getValue())
                .apply();
    }

    private void clearPendingProvider(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SOCIAL_AUTH, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_PENDING_PROVIDER)
                .apply();
    }

    @Nullable
    private SocialAuthProvider getPendingProvider(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SOCIAL_AUTH, Context.MODE_PRIVATE);
        return SocialAuthProvider.fromValue(prefs.getString(KEY_PENDING_PROVIDER, null));
    }

    /**
     * Hoàn tất đăng nhập sau OAuth bằng cách liên kết token Supabase Auth với profile app.
     *
     * <p>Token callback chỉ chứng minh user đã đăng nhập Supabase Auth. App vẫn cần
     * {@link AuthClient#bootstrapSocialSession(String, String, AuthClient.ApiCallback)} để lấy
     * {@code auth.users.id}, đồng bộ/tạo profile trong {@code public.users}, kiểm tra trạng thái
     * active, rồi mới lưu session chính và bật realtime.</p>
     */
    private void bootstrapSession(
            @NonNull Context context,
            @NonNull SocialAuthResult result,
            @NonNull RepositoryCallback<SocialAuthResult> callback
    ) {
        AuthClient.getInstance().bootstrapSocialSession(
                result.getAccessToken(),
                result.getRefreshToken(),
                new AuthClient.ApiCallback<UserProfile>() {
                    @Override
                    public void onSuccess(UserProfile userProfile) {
                        if (!userProfile.isActive()) {
                            SessionStore.clearSession(context);
                            SessionManager.getInstance(context).clearSession();
                            AuthClient.getInstance().clearSession();
                            logStep(6, "Bootstrap rejected disabled account", "provider=" + resolveProviderValue(result));
                            callback.onError(context.getString(R.string.error_account_disabled));
                            return;
                        }

                        SessionManager.getInstance(context).saveSession(
                                result.getAccessToken(),
                                result.getRefreshToken(),
                                userProfile,
                                resolveExpiryMillis(result)
                        );
                        FoodCouriersClientApp.initializeRealtime(result.getAccessToken());
                        logStep(
                                6,
                                "Bootstrap completed and session saved",
                                "provider=" + resolveProviderValue(result)
                                        + ", userId=" + safeValue(userProfile.getId())
                                        + ", email=" + safeValue(userProfile.getEmail())
                        );
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onError(String error) {
                        SessionManager.getInstance(context).clearSession();
                        AuthClient.getInstance().clearSession();
                        logStep(6, "Bootstrap failed while loading profile", "error=" + safeValue(error));
                        callback.onError(error);
                    }
                }
        );
    }

    @NonNull
    private SocialAuthResult buildStoredSessionResult(@NonNull Context context) {
        return new SocialAuthResult(
                SocialAuthProvider.fromValue(SessionStore.getProvider(context)),
                SessionStore.getAccessToken(context),
                SessionStore.getRefreshToken(context),
                null,
                null,
                SessionStore.getRawUri(context),
                SessionStore.getExpiresInSeconds(context)
        );
    }

    private long resolveExpiryMillis(@NonNull SocialAuthResult result) {
        if (result.getExpiresInSeconds() > 0) {
            return result.getExpiresInSeconds() * 1000L;
        }
        return DEFAULT_TOKEN_EXPIRY_MILLIS;
    }

    private void appendParameters(@NonNull Map<String, String> parameters, @Nullable String encodedSegment) {
        if (TextUtils.isEmpty(encodedSegment)) {
            return;
        }

        String[] pairs = encodedSegment.split("&");
        for (String pair : pairs) {
            if (TextUtils.isEmpty(pair)) {
                continue;
            }

            String[] parts = pair.split("=", 2);
            String key = Uri.decode(parts[0]);
            String value = parts.length > 1 ? Uri.decode(parts[1]) : "";
            parameters.put(key, value);
        }
    }

    @Nullable
    private String resolveProviderFromState(@Nullable String state) {
        if (TextUtils.isEmpty(state)) {
            return null;
        }

        String normalized = state.toLowerCase();
        if (normalized.contains(SocialAuthProvider.GOOGLE.getValue())) {
            return SocialAuthProvider.GOOGLE.getValue();
        }
        if (normalized.contains(SocialAuthProvider.FACEBOOK.getValue())) {
            return SocialAuthProvider.FACEBOOK.getValue();
        }
        return null;
    }

    @NonNull
    private SocialAuthResult enrichProvider(
            @NonNull SocialAuthResult result,
            @Nullable SocialAuthProvider fallbackProvider
    ) {
        if (result.getProvider() != null || fallbackProvider == null) {
            return result;
        }

        return new SocialAuthResult(
                fallbackProvider,
                result.getAccessToken(),
                result.getRefreshToken(),
                result.getErrorCode(),
                result.getErrorDescription(),
                result.getRawUri(),
                result.getExpiresInSeconds()
        );
    }

    @Nullable
    private String firstNonBlank(@Nullable String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private long parseLong(@Nullable String rawValue) {
        if (TextUtils.isEmpty(rawValue)) {
            return 0L;
        }
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private boolean notBlank(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }

    @NonNull
    private String resolveProviderValue(@NonNull SocialAuthResult result) {
        return result.getProvider() != null ? result.getProvider().getValue() : "unknown";
    }

    @NonNull
    private String safeValue(@Nullable String value) {
        return notBlank(value) ? value : "n/a";
    }

    private void logStep(int step, @NonNull String description, @Nullable String details) {
        String message = "Step " + step + ": " + description;
        if (notBlank(details)) {
            message += " | " + details;
        }
        Log.d(TAG, message);
    }

    private void logStepError(int step, @NonNull String description, @NonNull Throwable throwable) {
        Log.e(TAG, "Step " + step + ": " + description + " | error=" + throwable.getMessage(), throwable);
    }
}
