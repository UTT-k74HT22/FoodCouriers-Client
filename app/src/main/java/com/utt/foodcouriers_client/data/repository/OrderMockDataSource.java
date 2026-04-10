package com.utt.foodcouriers_client.data.repository;

import com.utt.foodcouriers_client.data.model.OrderLineItem;
import com.utt.foodcouriers_client.data.model.OrderStatus;
import com.utt.foodcouriers_client.data.model.OrderSummary;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class OrderMockDataSource {

    private static final List<OrderSummary> ORDERS = new ArrayList<>();

    static {
        if (ORDERS.isEmpty()) {
            ORDERS.add(buildOrder(
                    "order-client-001",
                    "ORD-20260410-1001",
                    "Com Tam Sai Gon",
                    "79 Nguyen Trai, Quan 1",
                    OrderStatus.PENDING,
                    "10 Apr 2026, 10:15",
                    120000,
                    15000,
                    0,
                    "123 Nguyen Hue, Quan 1",
                    "Khong hanh",
                    Arrays.asList(
                            new OrderLineItem("Com suon bi cha", 2, 45000),
                            new OrderLineItem("Tra dao", 1, 30000)
                    )
            ));
            ORDERS.add(buildOrder(
                    "order-client-002",
                    "ORD-20260410-1002",
                    "Pho Thin 13 Lo Duc",
                    "12 Cach Mang Thang 8, Quan 3",
                    OrderStatus.DELIVERING,
                    "10 Apr 2026, 11:40",
                    95000,
                    18000,
                    5000,
                    "58 Vo Thi Sau, Quan 3",
                    "Giao truoc 12h30",
                    Arrays.asList(
                            new OrderLineItem("Pho tai lan", 1, 65000),
                            new OrderLineItem("Tra tac", 1, 30000)
                    )
            ));
            ORDERS.add(buildOrder(
                    "order-client-003",
                    "ORD-20260409-2071",
                    "Bun Bo Hue O Xuan",
                    "218 Le Van Sy, Phu Nhuan",
                    OrderStatus.DELIVERED,
                    "09 Apr 2026, 19:05",
                    78000,
                    15000,
                    0,
                    "25 Truong Dinh, Quan 3",
                    "",
                    Arrays.asList(
                            new OrderLineItem("Bun bo dac biet", 1, 78000)
                    )
            ));
            ORDERS.add(buildOrder(
                    "order-client-004",
                    "ORD-20260409-2099",
                    "Pizza Town",
                    "99 Tran Hung Dao, Quan 1",
                    OrderStatus.CANCELLED,
                    "09 Apr 2026, 20:20",
                    210000,
                    20000,
                    20000,
                    "44 Pasteur, Quan 1",
                    "Khong ot",
                    Arrays.asList(
                            new OrderLineItem("Pizza hai san", 1, 160000),
                            new OrderLineItem("Khoai tay chien", 1, 50000)
                    )
            ));
        }
    }

    private OrderMockDataSource() {
    }

    static synchronized List<OrderSummary> getOrders() {
        return new ArrayList<>(ORDERS);
    }

    static synchronized OrderSummary getOrderById(String orderId) {
        for (OrderSummary order : ORDERS) {
            if (order.getId().equals(orderId)) {
                return order;
            }
        }
        return null;
    }

    static synchronized OrderSummary createCheckoutOrder() {
        String orderId = UUID.randomUUID().toString();
        String orderCode = "ORD-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date());
        OrderSummary order = buildOrder(
                orderId,
                orderCode,
                "Mafuy Kitchen",
                "188 Dien Bien Phu, Binh Thanh",
                OrderStatus.PENDING,
                new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(new Date()),
                132000,
                18000,
                0,
                "45 Nguyen Dinh Chieu, Quan 3",
                "Them muong dua",
                Arrays.asList(
                        new OrderLineItem("Mi tron ga nuong", 2, 54000),
                        new OrderLineItem("Tra vai", 1, 24000)
                )
        );
        ORDERS.add(0, order);
        return order;
    }

    private static OrderSummary buildOrder(
            String id,
            String orderCode,
            String restaurantName,
            String restaurantAddress,
            OrderStatus status,
            String createdAtLabel,
            int subtotal,
            int deliveryFee,
            int discount,
            String deliveryAddress,
            String note,
            List<OrderLineItem> items
    ) {
        int total = subtotal + deliveryFee - discount;
        return new OrderSummary(
                id,
                orderCode,
                restaurantName,
                restaurantAddress,
                status.getValue(),
                createdAtLabel,
                subtotal,
                deliveryFee,
                discount,
                total,
                deliveryAddress,
                note,
                items
        );
    }
}
