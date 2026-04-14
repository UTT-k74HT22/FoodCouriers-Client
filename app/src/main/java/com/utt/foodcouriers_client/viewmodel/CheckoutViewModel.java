package com.utt.foodcouriers_client.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.CartItem;
import com.utt.foodcouriers_client.data.model.CartRestaurantGroup;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.model.PromotionValidationResult;
import com.utt.foodcouriers_client.data.repository.CartRepository;
import com.utt.foodcouriers_client.data.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;

public class CheckoutViewModel extends BaseViewModel {

    private final MutableLiveData<List<CartRestaurantGroup>> restaurantGroups = new MutableLiveData<>();
    private final MutableLiveData<CartRepository.CartSummary> checkoutSummary = new MutableLiveData<>();
    private final MutableLiveData<List<OrderSummary>> createdOrders = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isOrderSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<PromotionValidationResult> appliedPromotion = new MutableLiveData<>();
    private String appliedPromoCode = null;

    public LiveData<List<CartRestaurantGroup>> getRestaurantGroups() {
        return restaurantGroups;
    }

    public LiveData<CartRepository.CartSummary> getCheckoutSummary() {
        return checkoutSummary;
    }

    public LiveData<List<OrderSummary>> getCreatedOrders() {
        return createdOrders;
    }

    public LiveData<Boolean> getIsOrderSuccess() {
        return isOrderSuccess;
    }

    public LiveData<PromotionValidationResult> getAppliedPromotion() {
        return appliedPromotion;
    }

    public void loadCheckoutData(Context context, List<String> selectedIds) {
        setLoading(true);
        CartRepository.getInstance().getCart(context, new RepositoryCallback<CartRepository.CartState>() {
            @Override
            public void onSuccess(CartRepository.CartState state) {
                List<CartRestaurantGroup> filteredGroups = new ArrayList<>();
                int subtotal = 0;
                int deliveryFee = 0;
                int itemCount = 0;

                for (CartRestaurantGroup group : state.getRestaurantGroups()) {
                    List<CartItem> selectedInGroup = new ArrayList<>();
                    for (CartItem item : group.getItems()) {
                        if (selectedIds.contains(item.getId())) {
                            selectedInGroup.add(item);
                            subtotal += item.getPrice() * item.getQuantity();
                            itemCount += item.getQuantity();
                        }
                    }
                    if (!selectedInGroup.isEmpty()) {
                        filteredGroups.add(new CartRestaurantGroup(
                                group.getRestaurantId(),
                                group.getRestaurantName(),
                                group.getDeliveryFee(),
                                selectedInGroup
                        ));
                        deliveryFee += group.getDeliveryFee();
                    }
                }

                restaurantGroups.setValue(filteredGroups);
                updateSummary(itemCount, subtotal, deliveryFee, 0);
                setLoading(false);
            }

            @Override
            public void onError(String error) {
                postError(error);
                setLoading(false);
            }
        });
    }

    private void updateSummary(int itemCount, int subtotal, int deliveryFee, int discount) {
        checkoutSummary.setValue(new CartRepository.CartSummary(
                itemCount, subtotal, deliveryFee, 0, discount, subtotal + deliveryFee - discount, ""
        ));
    }

    public void validatePromotion(Context context, String code) {
        if (code == null || code.isBlank()) {
            appliedPromotion.setValue(null);
            appliedPromoCode = null;
            CartRepository.CartSummary current = checkoutSummary.getValue();
            if (current != null) {
                updateSummary(current.getItemCount(), current.getSubtotal(), current.getDeliveryFee(), 0);
            }
            return;
        }

        CartRepository.CartSummary current = checkoutSummary.getValue();
        if (current == null) return;

        setLoading(true);
        OrderRepository.getInstance().validatePromotion(context, code, current.getSubtotal(), new RepositoryCallback<PromotionValidationResult>() {
            @Override
            public void onSuccess(PromotionValidationResult result) {
                setLoading(false);
                if (result.isValid()) {
                    appliedPromotion.setValue(result);
                    appliedPromoCode = code;
                    updateSummary(current.getItemCount(), current.getSubtotal(), current.getDeliveryFee(), result.getDiscount());
                } else {
                    appliedPromotion.setValue(result); // result.isValid() is false
                    appliedPromoCode = null;
                    updateSummary(current.getItemCount(), current.getSubtotal(), current.getDeliveryFee(), 0);
                }
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }

    public void placeOrders(Context context, String address, String note, String paymentMethod) {
        List<CartRestaurantGroup> groups = restaurantGroups.getValue();
        if (groups == null || groups.isEmpty()) {
            postError("Không có món ăn nào để đặt.");
            return;
        }

        setLoading(true);
        List<OrderSummary> results = new ArrayList<>();
        placeOrderSequentially(context, groups, 0, address, note, paymentMethod, appliedPromoCode, results);
    }

    private void placeOrderSequentially(Context context, List<CartRestaurantGroup> groups, int index, 
                                        String address, String note, String paymentMethod, String promoCode, List<OrderSummary> results) {
        if (index >= groups.size()) {
            createdOrders.setValue(results);
            isOrderSuccess.setValue(true);
            setLoading(false);
            return;
        }

        CartRestaurantGroup group = groups.get(index);
        JsonArray items = new JsonArray();
        for (CartItem item : group.getItems()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("menu_item_id", item.getMenuItemId());
            obj.addProperty("quantity", item.getQuantity());
            obj.addProperty("note", item.getNote());
            items.add(obj);
        }

        OrderRepository.getInstance().createOrder(
                context,
                group.getRestaurantId(),
                address, 21.002, 105.843, // Mock lat/lon
                note,
                paymentMethod,
                promoCode,
                items,
                new RepositoryCallback<OrderSummary>() {
                    @Override
                    public void onSuccess(OrderSummary order) {
                        results.add(order);
                        // Khi thanh toán qua nhiều nhà hàng, hiện tại ta chỉ apply code cho đơn đầu tiên hoặc cho tất cả?
                        // Theo logic của rpc_create_order, nó sẽ trừ tiền dựa trên subtotal của mỗi đơn.
                        // Nếu dùng 1 code cho nhiều đơn, mỗi đơn sẽ được giảm nếu thỏa điều kiện.
                        placeOrderSequentially(context, groups, index + 1, address, note, paymentMethod, promoCode, results);
                    }

                    @Override
                    public void onError(String error) {
                        postError("Lỗi khi đặt đơn tại " + group.getRestaurantName() + ": " + error);
                        setLoading(false);
                    }
                }
        );
    }
}
