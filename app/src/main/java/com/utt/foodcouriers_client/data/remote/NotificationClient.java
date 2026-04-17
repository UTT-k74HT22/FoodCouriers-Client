package com.utt.foodcouriers_client.data.remote;

import android.util.Log;

import com.utt.foodcouriers_client.data.model.NotificationItem;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Client REST thao tác trực tiếp với bảng {@code public.notifications} trên Supabase.
 *
 * <p>Mỗi request gọi {@link #syncSession()} trước để lấy access token mới nhất từ
 * {@link AuthClient}. Supabase RLS dựa vào bearer token này để chỉ cho user đọc/sửa/xóa
 * thông báo của chính họ.</p>
 */
public class NotificationClient extends BaseSupabaseClient {

    private static final String TAG = "NotificationClient";
    private static NotificationClient instance;

    private NotificationClient() {
        super();
    }

    /**
     * @return singleton REST client cho module notification
     */
    public static synchronized NotificationClient getInstance() {
        if (instance == null) {
            instance = new NotificationClient();
        }
        return instance;
    }

    /**
     * Đồng bộ token hiện tại từ AuthClient trước khi gọi Supabase REST.
     */
    private void syncSession() {
        AuthClient authClient = AuthClient.getInstance();
        this.accessToken = authClient.getAccessToken();
        this.refreshToken = authClient.getRefreshToken();
    }

    /**
     * Gọi Supabase REST để lấy danh sách thông báo của user.
     *
     * @param userId id user trong bảng {@code public.users}
     * @param callback trả mảng {@link NotificationItem}; lỗi parse/network được chuyển thành message
     */
    public void getNotifications(String userId, ApiCallback<NotificationItem[]> callback) {
        syncSession();
        
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/notifications?user_id=eq." + userId 
                   + "&select=*&order=created_at.desc";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (!response.isSuccessful()) {
                        postError(callback, parseRestError("Failed to get notifications", response.code(), json));
                        return;
                    }

                    try {
                        NotificationItem[] items = gson.fromJson(json, NotificationItem[].class);
                        postSuccess(callback, items != null ? items : new NotificationItem[0]);
                    } catch (Exception exception) {
                        Log.e(TAG, "Failed to parse notifications response", exception);
                        postError(callback, "Không thể đọc dữ liệu thông báo");
                    }
                }
            }
        });
    }

    /**
     * PATCH một thông báo về trạng thái đã đọc.
     *
     * @param notificationId id thông báo cần cập nhật
     * @param callback callback rỗng khi PATCH thành công
     */
    public void markAsRead(String notificationId, ApiCallback<Void> callback) {
        syncSession();
        
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("is_read", true);

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/notifications?id=eq." + notificationId)
                .patch(RequestBody.create(gson.toJson(update), MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        postSuccess(callback, null);
                    } else {
                        String json = responseBody != null ? responseBody.string() : "";
                        postError(callback, parseRestError("Failed to mark as read", response.code(), json));
                    }
                }
            }
        });
    }

    /**
     * PATCH tất cả thông báo chưa đọc của user về {@code is_read=true}.
     *
     * @param userId id user trong bảng {@code public.users}
     * @param callback callback rỗng khi PATCH thành công
     */
    public void markAllAsRead(String userId, ApiCallback<Void> callback) {
        syncSession();
        
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("is_read", true);

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/notifications?user_id=eq." + userId + "&is_read=eq.false")
                .patch(RequestBody.create(gson.toJson(update), MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_PREFER, "return=minimal")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        postSuccess(callback, null);
                    } else {
                        String json = responseBody != null ? responseBody.string() : "";
                        postError(callback, parseRestError("Failed to mark all as read", response.code(), json));
                    }
                }
            }
        });
    }

    /**
     * DELETE toàn bộ thông báo của user.
     *
     * @param userId id user trong bảng {@code public.users}
     * @param callback callback rỗng khi DELETE thành công
     */
    public void deleteAll(String userId, ApiCallback<Void> callback) {
        syncSession();

        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/notifications?user_id=eq." + userId)
                .delete()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_PREFER, "return=minimal")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        postSuccess(callback, null);
                    } else {
                        String json = responseBody != null ? responseBody.string() : "";
                        postError(callback, parseRestError("Failed to delete notifications", response.code(), json));
                    }
                }
            }
        });
    }
}
