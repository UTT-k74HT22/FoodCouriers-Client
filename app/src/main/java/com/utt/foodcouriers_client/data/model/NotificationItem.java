package com.utt.foodcouriers_client.data.model;

import java.io.Serializable;

public class NotificationItem implements Serializable {
    private final String id;
    private final String title;
    private final String message;
    private final String timeLabel;
    private final boolean unread;

    public NotificationItem(String id, String title, String message, String timeLabel, boolean unread) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timeLabel = timeLabel;
        this.unread = unread;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public boolean isUnread() {
        return unread;
    }
}
