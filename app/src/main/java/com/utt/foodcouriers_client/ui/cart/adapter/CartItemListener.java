package com.utt.foodcouriers_client.ui.cart.adapter;

/**
 * Callback để báo ngược các tương tác từ item UI về Fragment/ViewModel.
 */
public interface CartItemListener {
    /**
     * Được gọi khi người dùng bấm tăng số lượng một item trong cart.
     *
     * @param cartItemId id của bản ghi cart item
     */
    void onIncrease(String cartItemId);

    /**
     * Được gọi khi người dùng bấm giảm số lượng một item trong cart.
     *
     * @param cartItemId id của bản ghi cart item
     */
    void onDecrease(String cartItemId);

    /**
     * Được gọi khi trạng thái chọn item hoặc nhà hàng thay đổi.
     *
     * @param selectionState snapshot summary của phần đang chọn
     */
    void onSelectionChanged(CartItemAdapter.SelectionState selectionState);
}