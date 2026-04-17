package com.utt.foodcouriers_client.data.model;

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

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return this == AWAITING_PAYMENT || this == PENDING || this == CONFIRMED || this == PREPARING || this == READY_FOR_PICKUP || this == DELIVERING;
    }

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
