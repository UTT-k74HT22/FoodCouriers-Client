package com.utt.foodcouriers_client.data.repository;

import com.utt.foodcouriers_client.data.model.OrderStatus;
import com.utt.foodcouriers_client.data.model.OrderSummary;

import java.util.ArrayList;
import java.util.List;

public class OrderRepository {

    private static OrderRepository instance;

    public enum OrderFilter {
        ALL,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }

    public static synchronized OrderRepository getInstance() {
        if (instance == null) {
            instance = new OrderRepository();
        }
        return instance;
    }

    public List<OrderSummary> getOrders(OrderFilter filter) {
        List<OrderSummary> orders = OrderMockDataSource.getOrders();
        if (filter == null || filter == OrderFilter.ALL) {
            return orders;
        }
        List<OrderSummary> filtered = new ArrayList<>();
        for (OrderSummary order : orders) {
            OrderStatus status = OrderStatus.fromValue(order.getStatus());
            boolean matches = filter == OrderFilter.ACTIVE && status.isActive()
                    || filter == OrderFilter.COMPLETED && status == OrderStatus.DELIVERED
                    || filter == OrderFilter.CANCELLED && status == OrderStatus.CANCELLED;
            if (matches) {
                filtered.add(order);
            }
        }
        return filtered;
    }

    public OrderSummary getOrderById(String orderId) {
        return OrderMockDataSource.getOrderById(orderId);
    }

    public OrderSummary createCheckoutOrder() {
        return OrderMockDataSource.createCheckoutOrder();
    }
}
