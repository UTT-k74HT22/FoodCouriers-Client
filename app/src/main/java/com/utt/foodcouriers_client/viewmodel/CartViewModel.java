package com.utt.foodcouriers_client.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.CartItem;
import com.utt.foodcouriers_client.data.model.CartRestaurantGroup;
import com.utt.foodcouriers_client.data.model.MenuItem;
import com.utt.foodcouriers_client.data.model.Restaurant;
import com.utt.foodcouriers_client.data.repository.CartRepository;

import java.util.List;
import java.util.Map;

public class CartViewModel extends BaseViewModel {

    private final CartRepository repository = CartRepository.getInstance();
    private final MutableLiveData<List<CartItem>> cartItems = new MutableLiveData<>();
    private final MutableLiveData<List<CartRestaurantGroup>> restaurantGroups = new MutableLiveData<>();
    private final MutableLiveData<CartRepository.CartSummary> cartSummary = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Integer>> menuItemQuantities = new MutableLiveData<>();
    private final MutableLiveData<Boolean> emptyState = new MutableLiveData<>(true);
    private final MutableLiveData<Integer> selectedItemCount = new MutableLiveData<>(0);

    public LiveData<List<CartItem>> getCartItems() {
        return cartItems;
    }

    public LiveData<CartRepository.CartSummary> getCartSummary() {
        return cartSummary;
    }

    public LiveData<List<CartRestaurantGroup>> getRestaurantGroups() {
        return restaurantGroups;
    }

    public LiveData<Map<String, Integer>> getMenuItemQuantities() {
        return menuItemQuantities;
    }

    public LiveData<Boolean> getEmptyState() {
        return emptyState;
    }

    public LiveData<Integer> getSelectedItemCount() {
        return selectedItemCount;
    }

    public void loadCart(Context context) {
        setLoading(true);
        repository.getCart(context.getApplicationContext(), new RepositoryCallback<CartRepository.CartState>() {
            @Override
            public void onSuccess(CartRepository.CartState result) {
                publishState(result);
                setLoading(false);
            }

            @Override
            public void onError(String error) {
                if (!"AUTH_REQUIRED".equals(error)) {
                    postError(error);
                }
                publishState(CartRepository.CartState.empty());
                setLoading(false);
            }
        });
    }

    public void addMenuItem(Context context, MenuItem menuItem, Restaurant restaurant, int quantity, String note) {
        setLoading(true);
        repository.addToCart(context.getApplicationContext(), menuItem, restaurant, quantity, note, new RepositoryCallback<CartRepository.CartState>() {
            @Override
            public void onSuccess(CartRepository.CartState result) {
                publishState(result);
                postSuccess("Đã thêm " + menuItem.getName() + " vào giỏ hàng");
                setLoading(false);
            }

            @Override
            public void onError(String error) {
                postError(error);
                setLoading(false);
            }
        });
    }

    public void updateCartItemQuantity(Context context, String cartItemId, int quantity) {
        setLoading(true);
        repository.updateCartItemQuantity(context.getApplicationContext(), cartItemId, quantity, new RepositoryCallback<CartRepository.CartState>() {
            @Override
            public void onSuccess(CartRepository.CartState result) {
                publishState(result);
                if (quantity <= 0) {
                    postSuccess("Đã xóa món khỏi giỏ hàng");
                } else {
                    postSuccess("Đã cập nhật giỏ hàng");
                }
                setLoading(false);
            }

            @Override
            public void onError(String error) {
                postError(error);
                setLoading(false);
            }
        });
    }

    public void setMenuItemQuantity(Context context, MenuItem menuItem, Restaurant restaurant, int quantity, String note) {
        setLoading(true);
        repository.setMenuItemQuantity(context.getApplicationContext(), menuItem, restaurant, quantity, note, new RepositoryCallback<CartRepository.CartState>() {
            @Override
            public void onSuccess(CartRepository.CartState result) {
                publishState(result);
                if (quantity <= 0) {
                    postSuccess("Đã xóa " + menuItem.getName() + " khỏi giỏ hàng");
                } else {
                    postSuccess("Đã cập nhật " + menuItem.getName());
                }
                setLoading(false);
            }

            @Override
            public void onError(String error) {
                postError(error);
                setLoading(false);
            }
        });
    }

    public boolean isLoggedIn(Context context) {
        return repository.isLoggedIn(context.getApplicationContext());
    }

    private void publishState(CartRepository.CartState state) {
        CartRepository.CartState safeState = state == null ? CartRepository.CartState.empty() : state;
        cartItems.setValue(safeState.getItems());
        restaurantGroups.setValue(safeState.getRestaurantGroups());
        cartSummary.setValue(safeState.getSummary());
        menuItemQuantities.setValue(safeState.getMenuItemQuantities());
        emptyState.setValue(safeState.getItems() == null || safeState.getItems().isEmpty());
    }

    public void removeSelectedItems(Context context, List<String> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return;
        }
        setLoading(true);
        repository.removeItems(context.getApplicationContext(), cartItemIds, new RepositoryCallback<CartRepository.CartState>() {
            @Override
            public void onSuccess(CartRepository.CartState result) {
                publishState(result);
                postSuccess("Đã xóa " + cartItemIds.size() + " món khỏi giỏ hàng");
                setLoading(false);
            }

            @Override
            public void onError(String error) {
                postError(error);
                setLoading(false);
            }
        });
    }
}
