package com.utt.foodcouriers_client.data.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Danh sách provider đăng nhập mạng xã hội mà UI có thể hiển thị.
 *
 * <p>{@link #value} là chuỗi Supabase Auth yêu cầu trong authorize URL
 * ({@code provider=google}). {@link #displayName} là tên thân thiện để hiển thị ở UI
 * hoặc màn hình callback.</p>
 */
public enum SocialAuthProvider {
    GOOGLE("google", "Google"),
    FACEBOOK("facebook", "Facebook");

    private final String value;
    private final String displayName;

    SocialAuthProvider(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    /**
     * @return mã provider gửi lên Supabase Auth
     */
    public String getValue() {
        return value;
    }

    /**
     * @return tên provider dùng để hiển thị cho user
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Tìm provider từ chuỗi trả về bởi Supabase hoặc được lưu trong pending state.
     *
     * @param rawValue chuỗi provider, ví dụ {@code google}
     * @return enum tương ứng, hoặc {@code null} nếu provider lạ/thiếu
     */
    @Nullable
    public static SocialAuthProvider fromValue(@Nullable String rawValue) {
        if (rawValue == null) {
            return null;
        }

        for (SocialAuthProvider provider : values()) {
            if (provider.value.equalsIgnoreCase(rawValue.trim())) {
                return provider;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return value;
    }
}
