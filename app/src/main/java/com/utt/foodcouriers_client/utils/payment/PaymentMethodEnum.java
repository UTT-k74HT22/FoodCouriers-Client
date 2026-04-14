package com.utt.foodcouriers_client.utils.payment;

public enum PaymentMethodEnum {
    COD("cod", "Thanh toán khi nhận hàng"),
    VNPAY("vnpay", "Thanh toán qua VNPAY");

    private final String value;
    private final String displayName;

    PaymentMethodEnum(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PaymentMethodEnum fromValue(String value) {
        for (PaymentMethodEnum method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        return COD;
    }
}