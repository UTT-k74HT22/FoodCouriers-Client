package com.utt.foodcouriers_client.utils.CartDTO;

/**
 * DTO metadata tối giản của cart, hiện chỉ chứa cartId.
 */
public class CartMeta {
    private final String cartId;

    public CartMeta(String cartId) {
        this.cartId = cartId;
    }

    public String getCartId() {
        return cartId;
    }
}