package com.utt.foodcouriers_client.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.NotificationItem;
import com.utt.foodcouriers_client.data.repository.NotificationRepository;
import com.utt.foodcouriers_client.utils.SessionManager;
import java.util.List;

public class NotificationViewModel extends BaseViewModel {

    private final NotificationRepository repository = NotificationRepository.getInstance();
    private final MutableLiveData<List<NotificationItem>> notifications = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> successAction = new MutableLiveData<>();

    public static final int ACTION_MARK_ALL_READ = 1;
    public static final int ACTION_DELETE_ALL = 2;

    public NotificationViewModel() {
    }

    public LiveData<List<NotificationItem>> getNotifications() {
        return notifications;
    }

    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }

    public LiveData<Integer> getSuccessAction() {
        return successAction;
    }

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
