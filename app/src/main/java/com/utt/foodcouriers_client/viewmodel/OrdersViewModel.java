package com.utt.foodcouriers_client.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.repository.OrderRepository;

import java.util.List;

public class OrdersViewModel extends BaseViewModel {

    private final OrderRepository repository = OrderRepository.getInstance();
    private final MutableLiveData<List<OrderSummary>> orders = new MutableLiveData<>();
    private final MutableLiveData<OrderSummary> selectedOrder = new MutableLiveData<>();
    private OrderRepository.OrderFilter currentFilter = OrderRepository.OrderFilter.ALL;

    public LiveData<List<OrderSummary>> getOrders() {
        return orders;
    }

    public LiveData<OrderSummary> getSelectedOrder() {
        return selectedOrder;
    }

    public void loadOrders(OrderRepository.OrderFilter filter) {
        currentFilter = filter != null ? filter : OrderRepository.OrderFilter.ALL;
        setLoading(true);
        orders.setValue(repository.getOrders(currentFilter));
        setLoading(false);
    }

    public void refresh() {
        loadOrders(currentFilter);
    }

    public void loadOrderDetail(String orderId) {
        setLoading(true);
        OrderSummary order = repository.getOrderById(orderId);
        if (order == null) {
            postError("Khong tim thay don hang.");
        } else {
            selectedOrder.setValue(order);
        }
        setLoading(false);
    }
}
