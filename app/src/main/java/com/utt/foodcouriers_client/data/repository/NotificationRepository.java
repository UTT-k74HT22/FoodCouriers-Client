package com.utt.foodcouriers_client.data.repository;

import android.content.Context;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.NotificationItem;
import com.utt.foodcouriers_client.data.remote.NotificationClient;
import java.util.List;
import java.util.ArrayList;

public class NotificationRepository {

    private static NotificationRepository instance;
    private final NotificationClient client;

    private NotificationRepository() {
        this.client = NotificationClient.getInstance();
    }

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

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
}