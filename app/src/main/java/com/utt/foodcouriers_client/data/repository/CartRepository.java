package com.utt.foodcouriers_client.data.repository;

import com.utt.foodcouriers_client.data.model.CartItem;

import java.util.ArrayList;
import java.util.List;

public class CartRepository {

    private static CartRepository instance;
    private final List<CartItem> cartItems = new ArrayList<>();

    private CartRepository() {
        seedCart();
    }

    public static synchronized CartRepository getInstance() {
        if (instance == null) {
            instance = new CartRepository();
        }
        return instance;
    }

    public synchronized List<CartItem> getCartItems() {
        List<CartItem> items = new ArrayList<>();
        for (CartItem item : cartItems) {
            items.add(copyOf(item));
        }
        return items;
    }

    public synchronized void increaseQuantity(String cartItemId) {
        CartItem item = findById(cartItemId);
        if (item == null) {
            return;
        }
        item.setQuantity(item.getQuantity() + 1);
    }

    public synchronized void decreaseQuantity(String cartItemId) {
        CartItem item = findById(cartItemId);
        if (item == null) {
            return;
        }

        int nextQuantity = item.getQuantity() - 1;
        if (nextQuantity <= 0) {
            cartItems.remove(item);
            return;
        }
        item.setQuantity(nextQuantity);
    }

    public synchronized CartSummary getSummary() {
        int itemCount = 0;
        int subtotal = 0;
        for (CartItem item : cartItems) {
            itemCount += item.getQuantity();
            subtotal += item.getPrice() * item.getQuantity();
        }

        int deliveryFee = cartItems.isEmpty() ? 0 : (subtotal >= 200_000 ? 0 : 18_000);
        int serviceFee = cartItems.isEmpty() ? 0 : 7_000;
        int savings = cartItems.isEmpty() ? 0 : calculateSavings(itemCount, subtotal, deliveryFee);
        int total = Math.max(0, subtotal + deliveryFee + serviceFee - savings);
        String restaurantName = cartItems.isEmpty() ? "" : cartItems.get(0).getRestaurantName();
        return new CartSummary(itemCount, subtotal, deliveryFee, serviceFee, savings, total, restaurantName);
    }

    private int calculateSavings(int itemCount, int subtotal, int deliveryFee) {
        int savings = 0;
        if (itemCount >= 3) {
            savings += 12_000;
        }
        if (subtotal >= 200_000) {
            savings += deliveryFee;
        }
        return savings;
    }

    private CartItem findById(String cartItemId) {
        for (CartItem item : cartItems) {
            if (item.getId().equals(cartItemId)) {
                return item;
            }
        }
        return null;
    }

    private void seedCart() {
        if (!cartItems.isEmpty()) {
            return;
        }

        cartItems.add(new CartItem(
                "cart_1",
                "menu_1",
                "restaurant_1",
                "Urban Bites Kitchen",
                "Smoky Beef Burger",
                89_000,
                2,
                "Extra cheese, less onion",
                ""
        ));
        cartItems.add(new CartItem(
                "cart_2",
                "menu_2",
                "restaurant_1",
                "Urban Bites Kitchen",
                "Citrus Chicken Bowl",
                74_000,
                1,
                "Sauce packed separately",
                ""
        ));
        cartItems.add(new CartItem(
                "cart_3",
                "menu_3",
                "restaurant_1",
                "Urban Bites Kitchen",
                "Honey Toast Box",
                56_000,
                1,
                "",
                ""
        ));
    }

    private CartItem copyOf(CartItem item) {
        return new CartItem(
                item.getId(),
                item.getMenuItemId(),
                item.getRestaurantId(),
                item.getRestaurantName(),
                item.getName(),
                item.getPrice(),
                item.getQuantity(),
                item.getNote(),
                item.getImageUrl()
        );
    }

    public static class CartSummary {
        private final int itemCount;
        private final int subtotal;
        private final int deliveryFee;
        private final int serviceFee;
        private final int savings;
        private final int total;
        private final String restaurantName;

        public CartSummary(int itemCount, int subtotal, int deliveryFee, int serviceFee, int savings, int total, String restaurantName) {
            this.itemCount = itemCount;
            this.subtotal = subtotal;
            this.deliveryFee = deliveryFee;
            this.serviceFee = serviceFee;
            this.savings = savings;
            this.total = total;
            this.restaurantName = restaurantName;
        }

        public int getItemCount() {
            return itemCount;
        }

        public int getSubtotal() {
            return subtotal;
        }

        public int getDeliveryFee() {
            return deliveryFee;
        }

        public int getServiceFee() {
            return serviceFee;
        }

        public int getSavings() {
            return savings;
        }

        public int getTotal() {
            return total;
        }

        public String getRestaurantName() {
            return restaurantName;
        }
    }
}
