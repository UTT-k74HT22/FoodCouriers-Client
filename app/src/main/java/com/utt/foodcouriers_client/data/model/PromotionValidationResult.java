package com.utt.foodcouriers_client.data.model;

public class PromotionValidationResult {
    private boolean valid;
    private String message;
    private int discount;
    private String promotionId;

    public PromotionValidationResult(boolean valid, String message, int discount, String promotionId) {
        this.valid = valid;
        this.message = message;
        this.discount = discount;
        this.promotionId = promotionId;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public int getDiscount() {
        return discount;
    }

    public String getPromotionId() {
        return promotionId;
    }
}
