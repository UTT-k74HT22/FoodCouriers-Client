package com.utt.foodcouriers_client.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.NotificationItem;
import com.utt.foodcouriers_client.data.repository.NotificationRepository;
import com.utt.foodcouriers_client.utils.SessionManager;
import java.util.List;

/**
 * ViewModel quản lý state cho màn danh sách thông báo.
 *
 * <p>Lớp này là nơi nối UI với {@link NotificationRepository}: lấy user id từ
 * {@link SessionManager}, gọi repository, rồi expose danh sách thông báo, số chưa đọc,
 * trạng thái loading/lỗi và action thành công cho Fragment observe.</p>
 */
public class NotificationViewModel extends BaseViewModel {

    private final NotificationRepository repository = NotificationRepository.getInstance();
    private final MutableLiveData<List<NotificationItem>> notifications = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> successAction = new MutableLiveData<>();

    public static final int ACTION_MARK_ALL_READ = 1;
    public static final int ACTION_DELETE_ALL = 2;

    public NotificationViewModel() {
    }

    /**
     * @return danh sách thông báo hiện tại để adapter render
     */
    public LiveData<List<NotificationItem>> getNotifications() {
        return notifications;
    }

    /**
     * @return số lượng thông báo chưa đọc để cập nhật badge
     */
    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }

    /**
     * @return mã action vừa thành công để Fragment hiện toast phù hợp
     */
    public LiveData<Integer> getSuccessAction() {
        return successAction;
    }

    /**
     * Load thông báo của user đang đăng nhập.
     *
     * @param context context dùng để lấy {@link SessionManager} và gọi repository
     */
    public void loadNotifications(Context context) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            postError("User not found");
            return;
        }

        setLoading(true);
        repository.getNotifications(context, userId, new RepositoryCallback<List<NotificationItem>>() {
            @Override
            public void onSuccess(List<NotificationItem> result) {
                setLoading(false);
                notifications.setValue(result);
                updateUnreadCount(result);
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }

    /**
     * Đánh dấu một thông báo là đã đọc, sau đó refresh lại danh sách.
     *
     * @param context context màn hình
     * @param notificationId id thông báo user vừa bấm
     */
    public void markAsRead(Context context, String notificationId) {
        repository.markAsRead(context, notificationId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                refreshNotifications(context);
            }

            @Override
            public void onError(String error) {
                postError(error);
            }
        });
    }

    /**
     * Đánh dấu toàn bộ thông báo chưa đọc của user hiện tại là đã đọc.
     *
     * @param context context màn hình
     */
    public void markAllAsRead(Context context) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            return;
        }

        setLoading(true);
        repository.markAllAsRead(context, userId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                setLoading(false);
                successAction.setValue(ACTION_MARK_ALL_READ);
                refreshNotifications(context);
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }

    /**
     * Xóa toàn bộ thông báo của user hiện tại và clear state local khi thành công.
     *
     * @param context context màn hình
     */
    public void deleteAll(Context context) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            return;
        }

        setLoading(true);
        repository.deleteAll(context, userId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                setLoading(false);
                successAction.setValue(ACTION_DELETE_ALL);
                notifications.setValue(java.util.Collections.emptyList());
                unreadCount.setValue(0);
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }

    /**
     * Reload danh sách thông báo. Được gọi bởi swipe refresh, realtime listener và sau khi mark read.
     *
     * @param context context màn hình
     */
    public void refreshNotifications(Context context) {
        loadNotifications(context);
    }

    private void updateUnreadCount(List<NotificationItem> items) {
        if (items == null) {
            unreadCount.setValue(0);
            return;
        }
        int count = 0;
        for (NotificationItem item : items) {
            if (item.isUnread()) {
                count++;
            }
        }
        unreadCount.setValue(count);
    }
}
