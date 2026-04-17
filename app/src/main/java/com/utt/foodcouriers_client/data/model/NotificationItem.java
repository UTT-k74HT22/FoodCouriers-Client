package com.utt.foodcouriers_client.data.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model ánh xạ một row trong bảng {@code public.notifications}.
 *
 * <p>Backend tạo thông báo khi đơn hàng đổi trạng thái hoặc có sự kiện hệ thống/khuyến mãi.
 * Client dùng model này cho cả REST response và render UI. Field {@code data} được giữ dạng
 * {@link JsonElement} để chứa payload linh hoạt như order id, promotion id hoặc metadata khác.</p>
 */
public class NotificationItem implements Serializable {

    private String id;
    @SerializedName("user_id")
    private String userId;
    private String title;
    private String body;
    private String type;
    private JsonElement data;
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

    public JsonElement getDataElement() {
        return data;
    }

    public void setDataElement(JsonElement data) {
        this.data = data;
    }

    /**
     * @return payload phụ dạng JSON string, hoặc {@code null} nếu backend không gửi data
     */
    public String getData() {
        if (data == null || data.isJsonNull()) {
            return null;
        }
        return data.toString();
    }

    /**
     * Set payload phụ từ JSON string.
     *
     * @param data chuỗi JSON hợp lệ; chuỗi rỗng/null sẽ clear payload
     */
    public void setData(String data) {
        if (data == null || data.trim().isEmpty()) {
            this.data = null;
            return;
        }
        this.data = JsonParser.parseString(data);
    }

    /**
     * @return {@code true} khi notification chưa được user đọc
     */
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

    /**
     * Chuyển {@code created_at} từ Supabase sang định dạng ngày giờ dễ đọc cho người Việt.
     *
     * @return chuỗi thời gian đã format, hoặc raw {@code created_at} nếu parse lỗi
     */
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

    /**
     * @return nội dung hiển thị chính của thông báo
     */
    public String getMessage() {
        return body;
    }
}
