package com.utt.foodcouriers_client.data.repository;

public class NotificationRepository {

    private static NotificationRepository instance;

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }
}
