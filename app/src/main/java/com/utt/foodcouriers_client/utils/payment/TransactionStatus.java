package com.utt.foodcouriers_client.utils.payment;

public class TransactionStatus {
    private final String id;
    private final String status;
    private final String providerOrderRef;
    private final String paymentUrl;
    private final String expiresAt;
    private final String failureReason;

    public TransactionStatus(String id, String status, String providerOrderRef,
                             String paymentUrl, String expiresAt, String failureReason) {
        this.id = id;
        this.status = status;
        this.providerOrderRef = providerOrderRef;
        this.paymentUrl = paymentUrl;
        this.expiresAt = expiresAt;
        this.failureReason = failureReason;
    }

    public String getId() { return id; }
    public String getStatus() { return status; }
    public String getProviderOrderRef() { return providerOrderRef; }
    public String getPaymentUrl() { return paymentUrl; }
    public String getExpiresAt() { return expiresAt; }
    public String getFailureReason() { return failureReason; }

    public boolean isPending() { return "pending".equals(status); }
    public boolean isSuccess() { return "success".equals(status); }
    public boolean isFailed() { return "failed".equals(status); }
    public boolean isExpired() {
        if (expiresAt == null || expiresAt.isEmpty()) return false;
        try {
            return System.currentTimeMillis() > java.util.Date.parse(expiresAt);
        } catch (Exception e) { return false; }
    }
}