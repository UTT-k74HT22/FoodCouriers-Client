package com.utt.foodcouriers_client.data.model;

public enum OrderStatus {
    PENDING("pending", "Cho xac nhan"),
    CONFIRMED("confirmed", "Da xac nhan"),
    PREPARING("preparing", "Dang chuan bi"),
    READY_FOR_PICKUP("ready_for_pickup", "San sang lay"),
    DELIVERING("delivering", "Dang giao"),
    DELIVERED("delivered", "Hoan thanh"),
    CANCELLED("cancelled", "Da huy");

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
        return this == PENDING || this == CONFIRMED || this == PREPARING || this == READY_FOR_PICKUP || this == DELIVERING;
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
