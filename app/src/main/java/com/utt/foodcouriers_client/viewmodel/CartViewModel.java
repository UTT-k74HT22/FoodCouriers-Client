package com.utt.foodcouriers_client.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_client.data.model.CartItem;
import com.utt.foodcouriers_client.data.repository.CartRepository;

import java.util.List;

public class CartViewModel extends BaseViewModel {

    private final CartRepository repository = CartRepository.getInstance();
    private final MutableLiveData<List<CartItem>> cartItems = new MutableLiveData<>();
    private final MutableLiveData<CartRepository.CartSummary> cartSummary = new MutableLiveData<>();
    private final MutableLiveData<Boolean> emptyState = new MutableLiveData<>(true);

    public LiveData<List<CartItem>> getCartItems() {
        return cartItems;
    }

    public LiveData<CartRepository.CartSummary> getCartSummary() {
        return cartSummary;
    }

    public LiveData<Boolean> getEmptyState() {
        return emptyState;
    }

    public void loadCart() {
        setLoading(true);
        publishState();
        setLoading(false);
    }

    public void increaseQuantity(String cartItemId) {
        repository.increaseQuantity(cartItemId);
        publishState();
    }

    public void decreaseQuantity(String cartItemId) {
        repository.decreaseQuantity(cartItemId);
        publishState();
    }

    private void publishState() {
        List<CartItem> items = repository.getCartItems();
        cartItems.setValue(items);
        cartSummary.setValue(repository.getSummary());
        emptyState.setValue(items == null || items.isEmpty());
    }
}
