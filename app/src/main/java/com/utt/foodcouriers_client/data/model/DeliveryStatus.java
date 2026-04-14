package com.utt.foodcouriers_client.data.model;

public enum DeliveryStatus {
    UNASSIGNED("unassigned", "Chua phan cong"),
    SEARCHING("searching", "Dang tim tai xe"),
    ASSIGNED("assigned", "Da gan tai xe"),
    ARRIVING_PICKUP("arriving_pickup", "Dang den nha hang"),
    WAITING_PICKUP("waiting_pickup", "Dang cho lay hang"),
    PICKED_UP("picked_up", "Da lay hang"),
    COMPLETED("completed", "Hoan tat"),
    FAILED("failed", "That bai");

    private final String value;
    private final String label;

    DeliveryStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static DeliveryStatus fromValue(String value) {
        if (value == null) {
            return UNASSIGNED;
        }
        for (DeliveryStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return UNASSIGNED;
    }
}
