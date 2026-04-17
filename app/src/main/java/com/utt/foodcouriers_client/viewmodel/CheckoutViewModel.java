package com.utt.foodcouriers_client.viewmodel;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.CartItem;
import com.utt.foodcouriers_client.data.model.CartRestaurantGroup;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.model.PaymentInitResult;
import com.utt.foodcouriers_client.data.model.PromotionValidationResult;
import com.utt.foodcouriers_client.data.repository.CartRepository;
import com.utt.foodcouriers_client.data.repository.OrderRepository;
import com.utt.foodcouriers_client.data.repository.PaymentRepository;
import com.utt.foodcouriers_client.utils.CartDTO.CartState;
import com.utt.foodcouriers_client.utils.CartDTO.CartSummary;
import com.utt.foodcouriers_client.utils.DistanceUtils;
import com.utt.foodcouriers_client.utils.payment.PaymentMethodEnum;

import java.util.ArrayList;
import java.util.List;

public class CheckoutViewModel extends BaseViewModel {
    private static final String TAG = "CheckoutFlow";
    private final MutableLiveData<List<CartRestaurantGroup>> restaurantGroups = new MutableLiveData<>();
    private final MutableLiveData<CartSummary> checkoutSummary = new MutableLiveData<>();
    private final MutableLiveData<List<OrderSummary>> createdOrders = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isOrderSuccess = new MutableLiveData<>(false);
    // Kết quả xác thực mã khuyến mãi
    private final MutableLiveData<PromotionValidationResult> appliedPromotion = new MutableLiveData<>();
    // Chuỗi hiển thị khoảng cách giao hàng
    private final MutableLiveData<String> deliveryDistance = new MutableLiveData<>();
    private final MutableLiveData<PaymentInitResult> vnpayPaymentResult = new MutableLiveData<>();

    private String appliedPromoCode = null;

    public LiveData<List<CartRestaurantGroup>> getRestaurantGroups() {
        return restaurantGroups;
    }

    public LiveData<CartSummary> getCheckoutSummary() {
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

    public LiveData<String> getDeliveryDistance() {
        return deliveryDistance;
    }

    public LiveData<PaymentInitResult> getVnpayPaymentResult() {
        return vnpayPaymentResult;
    }

    /**
     * Tải dữ liệu thanh toán dựa trên các món đã chọn trong giỏ hàng
     */
    public void loadCheckoutData(Context context, List<String> selectedIds, double deliveryLat, double deliveryLon) {
        setLoading(true);
        CartRepository.getInstance().getCart(context, new RepositoryCallback<CartState>() {
            @Override
            public void onSuccess(CartState state) {
                List<CartRestaurantGroup> filteredGroups = new ArrayList<>();
                int subtotal = 0;
                int deliveryFee = 0;
                int itemCount = 0;
                double totalDistance = 0d;

                for (CartRestaurantGroup group : state.getRestaurantGroups()) {
                    List<CartItem> selectedInGroup = new ArrayList<>();
                    for (CartItem item : group.getItems()) {
                        if (selectedIds.contains(item.getId())) {
                            selectedInGroup.add(item);
                        }
                    }

                    // Chỉ xử lý nhà hàng đầu tiên có món được chọn (Giới hạn đơn hàng đơn shop)
                    if (!selectedInGroup.isEmpty() && filteredGroups.isEmpty()) {
                        subtotal = 0;
                        itemCount = 0;

                        for (CartItem item : selectedInGroup) {
                            subtotal += item.getPrice() * item.getQuantity();
                            itemCount += item.getQuantity();
                        }

                        Double restLat = group.getRestaurantLatitude();
                        Double restLon = group.getRestaurantLongitude();
                        int pricePerKm = group.getDeliveryFee();

                        int calculatedDeliveryFee = 0;
                        if (restLat != null && restLon != null && deliveryLat != 0 && deliveryLon != 0) {
                            // Tính khoảng cách
                            double distanceKm = DistanceUtils.calculateDistanceKm(restLat, restLon, deliveryLat, deliveryLon);
                            // Tính phí ship
                            calculatedDeliveryFee = (int) (distanceKm * pricePerKm);
                            totalDistance = distanceKm;
                        }

                        filteredGroups.add(new CartRestaurantGroup(
                                group.getRestaurantId(),
                                group.getRestaurantName(),
                                group.getDeliveryFee(),
                                calculatedDeliveryFee,
                                selectedInGroup,
                                restLat,
                                restLon
                        ));
                        deliveryFee += calculatedDeliveryFee;

                        restaurantGroups.setValue(filteredGroups);
                        deliveryDistance.setValue(totalDistance > 0 ? DistanceUtils.formatDistance(totalDistance) : "");
                        updateSummary(itemCount, subtotal, deliveryFee, pricePerKm, 0);
                        setLoading(false);
                        return; // Chỉ xử lý shop đầu tiên
                    }
                }

                restaurantGroups.setValue(filteredGroups);
                updateSummary(itemCount, subtotal, deliveryFee, 0, 0);
                setLoading(false);
            }

            @Override
            public void onError(String error) {
                postError(error);
                setLoading(false);
            }
        });
    }

    private String calculateTotalDistance(List<CartRestaurantGroup> groups, double deliveryLat, double deliveryLon) {
        if (groups == null || groups.isEmpty() || deliveryLat == 0d || deliveryLon == 0d) {
            return "";
        }

        double totalDistance = 0d;
        for (CartRestaurantGroup group : groups) {
            Double restLat = group.getRestaurantLatitude();
            Double restLon = group.getRestaurantLongitude();
            if (restLat != null && restLon != null) {
                totalDistance += DistanceUtils.calculateDistanceKm(restLat, restLon, deliveryLat, deliveryLon);
            }
        }

        return totalDistance > 0d ? DistanceUtils.formatDistance(totalDistance) : "";
    }

    private void updateSummary(int itemCount, int subtotal, int deliveryFee, int discount) {
        checkoutSummary.setValue(new CartSummary(
                itemCount,
                subtotal,
                deliveryFee,
                deliveryFeePerKm,
                discount,
                subtotal + deliveryFee - discount, // total
                ""
        ));
    }

    /**
     * Kiểm tra và áp dụng mã giảm giá
     */
    public void validatePromotion(Context context, String code) {
        if (code == null || code.isBlank()) {
            appliedPromotion.setValue(null);
            appliedPromoCode = null;
            CartSummary current = checkoutSummary.getValue();
            if (current != null) {
                updateSummary(current.getItemCount(), current.getSubtotal(), current.getDeliveryFee(), current.getServiceFee(), 0);
            }
            return;
        }

        CartSummary current = checkoutSummary.getValue();
        if (current == null) {
            return;
        }

        setLoading(true);
        OrderRepository.getInstance().validatePromotion(context, code, current.getSubtotal(), new RepositoryCallback<PromotionValidationResult>() {
            @Override
            public void onSuccess(PromotionValidationResult result) {
                setLoading(false);
                if (result.isValid()) {
                    appliedPromotion.setValue(result);
                    appliedPromoCode = code;
                    updateSummary(current.getItemCount(), current.getSubtotal(), current.getDeliveryFee(), current.getServiceFee(), result.getDiscount());
                } else {
                    appliedPromotion.setValue(result);
                    appliedPromoCode = null;
                    updateSummary(current.getItemCount(), current.getSubtotal(), current.getDeliveryFee(), current.getServiceFee(), 0);
                }
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }

    /**
     * Bắt đầu quy trình đặt hàng
     */
    public void placeOrders(Context context, String address, double latitude, double longitude, String note, String paymentMethod) {
        List<CartRestaurantGroup> groups = restaurantGroups.getValue();
        if (groups == null || groups.isEmpty()) {
            postError(context.getString(R.string.checkout_error_no_items));
            return;
        }

        // Chặn đặt đơn đa shop nếu dùng VNPay (Hệ thống hiện tại ưu tiên xử lý đơn lẻ)
        if (PaymentMethodEnum.VNPAY.getValue().equals(paymentMethod) && groups.size() > 1) {
            postError(context.getString(R.string.checkout_payment_multi_restaurant_error));
            return;
        }

        Log.d(TAG, "Step 2: Checkout validated groups=" + groups.size() + ", paymentMethod=" + paymentMethod);
        setLoading(true);
        List<OrderSummary> results = new ArrayList<>();
        placeOrderSequentially(context, groups, 0, address, latitude, longitude, note, paymentMethod, appliedPromoCode, results);
    }

    /**
     * Tạo đơn hàng tuần tự cho từng nhà hàng
     */
    private void placeOrderSequentially(Context context,
                                        List<CartRestaurantGroup> groups,
                                        int index,
                                        String address,
                                        double latitude,
                                        double longitude,
                                        String note,
                                        String paymentMethod,
                                        String promoCode,
                                        List<OrderSummary> results) {
        if (index >= groups.size()) {
            // Tất cả đơn hàng đã tạo thành công, tiến hành xóa món khỏi giỏ hàng
            List<String> cartItemIdsToRemove = new ArrayList<>();
            for (CartRestaurantGroup group : groups) {
                for (CartItem item : group.getItems()) {
                    cartItemIdsToRemove.add(item.getId());
                }
            }

            if (!cartItemIdsToRemove.isEmpty()) {
                CartRepository.getInstance().removeItems(context, cartItemIdsToRemove, new RepositoryCallback<CartRepository.CartState>() {
                    @Override
                    public void onSuccess(CartRepository.CartState result) {
                        Log.d(TAG, "Các món đã đặt đã được xóa khỏi giỏ hàng");
                        completeCheckout(context, paymentMethod, results);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Lỗi khi xóa món khỏi giỏ hàng sau khi đặt: " + error);
                        // Vẫn tiếp tục quy trình hoàn tất thanh toán dù xóa giỏ hàng lỗi
                        completeCheckout(context, paymentMethod, results);
                    }
                });
            } else {
                clearCartAfterOrderSuccess(context);
                isOrderSuccess.setValue(true);
                setLoading(false);
                completeCheckout(context, paymentMethod, results);
            }
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

        int deliveryFee = group.getCalculatedDeliveryFee();

        OrderRepository.getInstance().createOrder(
                context,
                group.getRestaurantId(),
                address,
                latitude,
                longitude,
                note,
                paymentMethod,
                promoCode,
                items,
                deliveryFee,
                new RepositoryCallback<OrderSummary>() {
                    @Override
                    public void onSuccess(OrderSummary order) {
                        Log.d(TAG, "Step 3: Order created id=" + order.getId()
                                + ", restaurant=" + group.getRestaurantName()
                                + ", paymentMethod=" + order.getPaymentMethod()
                                + ", paymentStatus=" + order.getPaymentStatus());
                        results.add(order);
                        placeOrderSequentially(context, groups, index + 1, address, latitude, longitude, note, paymentMethod, promoCode, results);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Step 3: Create order failed for restaurant=" + group.getRestaurantName()
                                + ", error=" + error);
                        postError("Lỗi khi đặt đơn tại " + group.getRestaurantName() + ": " + error);
                        setLoading(false);
                    }
                }
        );
    }

    /**
     * Hoàn tất quy trình thanh toán: hiển thị thành công hoặc chuyển sang VNPay
     */
    private void completeCheckout(Context context, String paymentMethod, List<OrderSummary> results) {
        createdOrders.setValue(results);
        if (PaymentMethodEnum.VNPAY.getValue().equals(paymentMethod)) {
            String orderId = results.get(0).getId();
            Log.d(TAG, "Bước 4: Đang yêu cầu URL thanh toán VNPay cho đơn hàng=" + orderId);
            fetchVnpayPaymentUrl(context, orderId);
        } else {
            isOrderSuccess.setValue(true);
            setLoading(false);
        }
    }

    private void clearCartAfterOrderSuccess(Context context) {
        CartRepository.getInstance().clearCart(context, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Log.d(TAG, "Cart cleared after successful order");
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to clear cart: " + error);
            }
        });
    }

    private void fetchVnpayPaymentUrl(Context context, String orderId) {
        PaymentRepository.getInstance().createVnpayPayment(context, orderId, new RepositoryCallback<PaymentInitResult>() {
            @Override
            public void onSuccess(PaymentInitResult result) {
                Log.d(TAG, "Step 4: VNPAY payment initialized txnRef=" + result.getProviderOrderRef());
                clearCartAfterOrderSuccess(context);
                setLoading(false);
                vnpayPaymentResult.setValue(result);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Step 4: VNPAY payment initialization failed: " + error);
                setLoading(false);
                postError(context.getString(R.string.payment_init_error, error));
            }
        });
    }
}
