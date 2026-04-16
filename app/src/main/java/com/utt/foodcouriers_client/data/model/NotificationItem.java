package com.utt.foodcouriers_client.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationItem implements Serializable {

    private String id;
    @SerializedName("user_id")
    private String userId;
    private String title;
    private String body;
    private String type;
    private String data;
    @SerializedName("is_read")
    private boolean isRead;
    @SerializedName("created_at")
    private String createdAt;

    public NotificationItem() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isUnread() {
        return !isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public boolean isRead() {
        return isRead;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getTimeLabel() {
        if (createdAt == null || createdAt.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
            Date date = inputFormat.parse(createdAt.replace("+00", ""));
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            return createdAt;
        }
        return createdAt;
    }

    public String getMessage() {
        return body;
    }
}
