package com.utt.foodcouriers_client.data.model;

/**
 * Trạng thái nghiệp vụ của đơn hàng.
 *
 * <p>Giá trị {@link #value} khớp với enum/text trong Supabase. UI dùng {@link #label}
 * để hiển thị tiếng Việt và dùng thứ tự enum để render timeline đã đi tới bước nào.</p>
 */
public enum OrderStatus {
    AWAITING_PAYMENT("awaiting_payment", "Chờ thanh toán"),
    PENDING("pending", "Chờ nhận"),
    CONFIRMED("confirmed", "Đã nhận"),
    PREPARING("preparing", "Đang làm"),
    READY_FOR_PICKUP("ready_for_pickup", "Chờ lấy"),
    DELIVERING("delivering", "Đang giao"),
    DELIVERED("delivered", "Đã giao"),
    CANCELLED("cancelled", "Đã hủy");

    private final String value;
    private final String label;

    OrderStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * @return giá trị lưu trong database
     */
    public String getValue() {
        return value;
    }

    /**
     * @return nhãn tiếng Việt để hiển thị trên UI
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return {@code true} nếu đơn vẫn đang trong quá trình xử lý/giao hàng
     */
    public boolean isActive() {
        return this == AWAITING_PAYMENT || this == PENDING || this == CONFIRMED || this == PREPARING || this == READY_FOR_PICKUP || this == DELIVERING;
    }

    /**
     * Chuyển raw status từ Supabase sang enum an toàn.
     *
     * @param value raw status từ database
     * @return enum tương ứng, mặc định {@link #PENDING} nếu thiếu hoặc không nhận diện được
     */
    public static OrderStatus fromValue(String value) {
        if (value == null) {
            return PENDING;
        }
        for (OrderStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return PENDING;
    }
}
