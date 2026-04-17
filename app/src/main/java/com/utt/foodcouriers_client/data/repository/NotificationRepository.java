package com.utt.foodcouriers_client.data.repository;

import android.content.Context;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.NotificationItem;
import com.utt.foodcouriers_client.data.remote.NotificationClient;
import java.util.List;
import java.util.ArrayList;

/**
 * Repository nghiệp vụ cho thông báo của user hiện tại.
 *
 * <p>UI/ViewModel không gọi REST trực tiếp. Repository này chuyển request sang
 * {@link NotificationClient}, chuẩn hóa dữ liệu trả về và giữ callback ở dạng
 * {@link RepositoryCallback}. Luồng chính:</p>
 *
 * <ol>
 *     <li>{@link com.utt.foodcouriers_client.viewmodel.NotificationViewModel} lấy user id từ session.</li>
 *     <li>Repository gọi {@link NotificationClient} với user id hoặc notification id.</li>
 *     <li>Client gọi Supabase REST dưới quyền bearer token hiện tại.</li>
 *     <li>Kết quả được đẩy về ViewModel để cập nhật danh sách, badge unread và trạng thái action.</li>
 * </ol>
 */
public class NotificationRepository {

    private static NotificationRepository instance;
    private final NotificationClient client;

    private NotificationRepository() {
        this.client = NotificationClient.getInstance();
    }

    /**
     * @return singleton repository dùng chung cho badge ở MainActivity và màn thông báo
     */
    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    /**
     * Lấy toàn bộ thông báo của một user, mới nhất đứng trước.
     *
     * @param context context của màn gọi; hiện giữ trong signature để đồng bộ với các repository khác
     * @param userId id profile trong bảng {@code public.users}
     * @param callback trả danh sách thông báo; nếu Supabase trả rỗng thì trả list rỗng
     */
    public void getNotifications(Context context, String userId, RepositoryCallback<List<NotificationItem>> callback) {
        client.getNotifications(userId, new NotificationClient.ApiCallback<NotificationItem[]>() {
            @Override
            public void onSuccess(NotificationItem[] result) {
                callback.onSuccess(result != null ? java.util.Arrays.asList(result) : new ArrayList<>());
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Đánh dấu một thông báo là đã đọc.
     *
     * @param context context của màn gọi
     * @param notificationId id thông báo cần cập nhật
     * @param callback báo thành công để ViewModel reload danh sách
     */
    public void markAsRead(Context context, String notificationId, RepositoryCallback<Void> callback) {
        client.markAsRead(notificationId, new NotificationClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                callback.onSuccess(null);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Đánh dấu toàn bộ thông báo chưa đọc của user là đã đọc.
     *
     * @param context context của màn gọi
     * @param userId id profile trong bảng {@code public.users}
     * @param callback báo thành công để ViewModel hiện toast và refresh
     */
    public void markAllAsRead(Context context, String userId, RepositoryCallback<Void> callback) {
        client.markAllAsRead(userId, new NotificationClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                callback.onSuccess(null);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Xóa toàn bộ thông báo của user hiện tại.
     *
     * @param context context của màn gọi
     * @param userId id profile trong bảng {@code public.users}
     * @param callback báo thành công để ViewModel clear list và badge
     */
    public void deleteAll(Context context, String userId, RepositoryCallback<Void> callback) {
        client.deleteAll(userId, new NotificationClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                callback.onSuccess(null);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}
