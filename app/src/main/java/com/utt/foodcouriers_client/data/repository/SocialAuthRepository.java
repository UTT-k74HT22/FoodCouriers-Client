package com.utt.foodcouriers_client.data.repository;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.utt.foodcouriers_client.data.auth.SocialAuthManager;
import com.utt.foodcouriers_client.data.auth.SocialAuthProvider;
import com.utt.foodcouriers_client.data.auth.SocialAuthResult;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;

/**
 * Repository mỏng cho luồng đăng nhập mạng xã hội.
 *
 * <p>Lớp này giữ Activity/Fragment không phụ thuộc trực tiếp vào chi tiết Supabase OAuth.
 * UI chỉ gọi {@link #startAuth(Context, SocialAuthProvider, RepositoryCallback)} hoặc
 * {@link #handleCallback(Context, Uri, RepositoryCallback)}; toàn bộ phần tạo authorize URL,
 * parse deep link, bootstrap profile và lưu session được chuyển xuống {@link SocialAuthManager}.</p>
 */
public class SocialAuthRepository {
    private static SocialAuthRepository instance;
    private final SocialAuthManager socialAuthManager;

    private SocialAuthRepository() {
        socialAuthManager = SocialAuthManager.getInstance();
    }

    /**
     * Trả về singleton repository dùng chung cho Login/Register/Callback screen.
     *
     * @return instance duy nhất của {@link SocialAuthRepository}
     */
    public static synchronized SocialAuthRepository getInstance() {
        if (instance == null) {
            instance = new SocialAuthRepository();
        }
        return instance;
    }

    /**
     * Bắt đầu OAuth từ UI.
     *
     * @param context context màn hình hiện tại để mở browser
     * @param provider provider user chọn
     * @param callback nhận kết quả mở browser hoặc lỗi provider chưa hỗ trợ
     */
    public void startAuth(
            @NonNull Context context,
            @NonNull SocialAuthProvider provider,
            @NonNull RepositoryCallback<Boolean> callback
    ) {
        socialAuthManager.launchProvider(context, provider, callback);
    }

    /**
     * Chuyển deep link callback cho manager xử lý và bootstrap session.
     *
     * @param context context màn hình callback
     * @param uri URI nhận từ intent callback
     * @param callback kết quả OAuth đã parse và bootstrap
     */
    public void handleCallback(
            @NonNull Context context,
            @Nullable Uri uri,
            @NonNull RepositoryCallback<SocialAuthResult> callback
    ) {
        socialAuthManager.handleCallback(context, uri, callback);
    }

    /**
     * Thử lại bước lấy/tạo profile nghiệp vụ khi token OAuth đã được lưu tạm.
     *
     * @param context context dùng để đọc session tạm
     * @param callback trả thành công khi session chính đã được lưu
     */
    public void retryProfileBootstrap(
            @NonNull Context context,
            @NonNull RepositoryCallback<Boolean> callback
    ) {
        socialAuthManager.retryProfileBootstrap(context, callback);
    }

    /**
     * Kiểm tra session social auth tạm còn khả dụng hay không.
     *
     * @param context context dùng để đọc session tạm
     * @param callback trả thành công nếu có session tạm
     */
    public void refreshSession(
            @NonNull Context context,
            @NonNull RepositoryCallback<Boolean> callback
    ) {
        socialAuthManager.refreshSession(context, callback);
    }
}
