package com.utt.foodcouriers_client.data.auth;

import androidx.annotation.Nullable;

/**
 * Kết quả chuẩn hóa sau khi app nhận deep link từ Supabase OAuth.
 *
 * <p>Object này có thể đại diện cho cả ba trạng thái: đăng nhập thành công
 * (có access token và refresh token), Supabase trả lỗi, hoặc callback hợp lệ nhưng
 * chưa có session payload. UI dựa vào {@link #isSuccessful()}, {@link #hasError()}
 * và {@link #hasSessionPayload()} để quyết định mở màn chính, hiển thị lỗi hay cho user thử lại.</p>
 */
public class SocialAuthResult {
    @Nullable
    private final SocialAuthProvider provider;
    @Nullable
    private final String accessToken;
    @Nullable
    private final String refreshToken;
    @Nullable
    private final String errorCode;
    @Nullable
    private final String errorDescription;
    @Nullable
    private final String rawUri;
    private final long expiresInSeconds;

    /**
     * Tạo kết quả OAuth đã parse từ callback URI.
     *
     * @param provider provider OAuth nếu xác định được
     * @param accessToken access token Supabase Auth
     * @param refreshToken refresh token Supabase Auth
     * @param errorCode mã lỗi từ Supabase/browser nếu có
     * @param errorDescription mô tả lỗi hoặc trạng thái thiếu payload
     * @param rawUri URI callback gốc để debug
     * @param expiresInSeconds số giây token còn hạn; có thể bằng 0 nếu Supabase không trả
     */
    public SocialAuthResult(
            @Nullable SocialAuthProvider provider,
            @Nullable String accessToken,
            @Nullable String refreshToken,
            @Nullable String errorCode,
            @Nullable String errorDescription,
            @Nullable String rawUri,
            long expiresInSeconds
    ) {
        this.provider = provider;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.rawUri = rawUri;
        this.expiresInSeconds = expiresInSeconds;
    }

    @Nullable
    public SocialAuthProvider getProvider() {
        return provider;
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getRefreshToken() {
        return refreshToken;
    }

    @Nullable
    public String getErrorCode() {
        return errorCode;
    }

    @Nullable
    public String getErrorDescription() {
        return errorDescription;
    }

    @Nullable
    public String getRawUri() {
        return rawUri;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    /**
     * @return {@code true} khi callback có đủ access token và refresh token
     */
    public boolean hasSessionPayload() {
        return notBlank(accessToken) && notBlank(refreshToken);
    }

    /**
     * @return {@code true} khi callback có mã lỗi hoặc mô tả lỗi
     */
    public boolean hasError() {
        return notBlank(errorCode) || notBlank(errorDescription);
    }

    /**
     * @return {@code true} khi có đủ token và không có lỗi OAuth
     */
    public boolean isSuccessful() {
        return hasSessionPayload() && !hasError();
    }

    private boolean notBlank(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }
}
