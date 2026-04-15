package com.utt.foodcouriers_client.data.model;

public class PaymentCallbackResult {
    private final String txnRef;
    private final String result;
    private final String responseCode;
    private final String transactionNo;
    private final String amount;
    private final boolean checksumValid;

    public PaymentCallbackResult(String txnRef, String result, String responseCode,
                                 String transactionNo, String amount, boolean checksumValid) {
        this.txnRef = txnRef;
        this.result = result;
        this.responseCode = responseCode;
        this.transactionNo = transactionNo;
        this.amount = amount;
        this.checksumValid = checksumValid;
    }

    public String getTxnRef() { return txnRef; }
    public String getResult() { return result; }
    public String getResponseCode() { return responseCode; }
    public String getTransactionNo() { return transactionNo; }
    public String getAmount() { return amount; }
    public boolean isChecksumValid() { return checksumValid; }

    /** Trả về true khi server xác nhận checksum hợp lệ VÀ kết quả thanh toán thành công */
    public boolean isSuccess() {
        return checksumValid && "success".equals(result);
    }
}
