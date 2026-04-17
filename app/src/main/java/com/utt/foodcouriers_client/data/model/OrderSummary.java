package com.utt.foodcouriers_client.data.model;

import java.io.Serializable;
import java.util.List;

/**
 * Model tổng hợp dùng cho cả danh sách đơn, chi tiết đơn và tracking.
 *
 * <p>Repository parse model này từ Supabase REST response. Với màn detail, object có thêm
 * {@code items}; với màn list, danh sách item có thể rỗng tùy query select. Các trạng thái
 * vẫn giữ dạng raw string để UI tự chuyển sang {@link OrderStatus} hoặc
 * {@link DeliveryStatus} khi cần label.</p>
 */
public class OrderSummary implements Serializable {
    private final String id;
    private final String orderCode;
    private final String restaurantName;
    private final String restaurantAddress;
    private final String status;
    private final String deliveryStatus;
    private final String createdAtLabel;
    private final int subtotal;
    private final int deliveryFee;
    private final int discount;
    private final int total;
    private final String deliveryAddress;
    private final String note;
    private final List<OrderLineItem> items;
    private final String paymentMethod;
    private final String paymentStatus;

    /**
     * Tạo order summary đã parse từ REST response.
     */
    public OrderSummary(
            String id,
            String orderCode,
            String restaurantName,
            String restaurantAddress,
            String status,
            String deliveryStatus,
            String createdAtLabel,
            int subtotal,
            int deliveryFee,
            int discount,
            int total,
            String deliveryAddress,
            String note,
            List<OrderLineItem> items,
            String paymentMethod,
            String paymentStatus
    ) {
        this.id = id;
        this.orderCode = orderCode;
        this.restaurantName = restaurantName;
        this.restaurantAddress = restaurantAddress;
        this.status = status;
        this.deliveryStatus = deliveryStatus;
        this.createdAtLabel = createdAtLabel;
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.discount = discount;
        this.total = total;
        this.deliveryAddress = deliveryAddress;
        this.note = note;
        this.items = items;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }

    public String getId() {
        return id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public String getRestaurantAddress() {
        return restaurantAddress;
    }

    public String getStatus() {
        return status;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public String getCreatedAtLabel() {
        return createdAtLabel;
    }

    public int getSubtotal() {
        return subtotal;
    }

    public int getDeliveryFee() {
        return deliveryFee;
    }

    public int getDiscount() {
        return discount;
    }

    public int getTotal() {
        return total;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getNote() {
        return note;
    }

    public List<OrderLineItem> getItems() {
        return items;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }
}
