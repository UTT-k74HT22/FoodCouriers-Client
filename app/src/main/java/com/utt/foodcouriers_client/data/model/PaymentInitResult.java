package com.utt.foodcouriers_client.data.model;

public class PaymentInitResult {
    private final String orderId;
    private final String transactionId;
    private final String provider;
    private final String providerOrderRef;
    private final String paymentUrl;
    private final String expiresAt;

    public PaymentInitResult(String orderId, String transactionId, String provider,
                             String providerOrderRef, String paymentUrl, String expiresAt) {
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.provider = provider;
        this.providerOrderRef = providerOrderRef;
        this.paymentUrl = paymentUrl;
        this.expiresAt = expiresAt;
    }

    public String getOrderId() { return orderId; }
    public String getTransactionId() { return transactionId; }
    public String getProvider() { return provider; }
    public String getProviderOrderRef() { return providerOrderRef; }
    public String getPaymentUrl() { return paymentUrl; }
    public String getExpiresAt() { return expiresAt; }
}
