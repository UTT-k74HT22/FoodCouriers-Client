package com.utt.foodcouriers_client.utils.payment;

/**
 *
 */
public final class PaymentConfig {

    private PaymentConfig() {

    }

    public static final boolean FEATURE_VNPAY_ENABLED = true;
    public static final String DEEP_LINK_SCHEME = "com.utt.foodcouriers.client";
    public static final String DEEP_LINK_HOST = "payment";
    public static final String DEEP_LINK_PATH = "/vnpay/callback";
}
