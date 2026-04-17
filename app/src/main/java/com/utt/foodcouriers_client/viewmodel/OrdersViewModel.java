package com.utt.foodcouriers_client.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.repository.OrderRepository;

import java.util.List;
/** Điều phối các trạng thái đơn hàng */
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

    // Load list orders
    public void loadOrders(Context context, OrderRepository.OrderFilter filter) {
        currentFilter = filter != null ? filter : OrderRepository.OrderFilter.ALL;
        setLoading(true);

        repository.getOrders(context, currentFilter, new RepositoryCallback<List<OrderSummary>>() {
            @Override
            public void onSuccess(List<OrderSummary> result) {
                orders.setValue(result);
                setLoading(false);
            }

            @Override
            public void onError(String error) {
                postError(error);
                setLoading(false);
            }
        });
    }

    // Refresh list
    public void refresh(Context context) {
        loadOrders(context, currentFilter);
    }

    // Load order detail
    public void loadOrderDetail(Context context, String orderId) {
        setLoading(true);

        repository.getOrderById(context, orderId, new RepositoryCallback<OrderSummary>() {
            @Override
            public void onSuccess(OrderSummary order) {
                selectedOrder.setValue(order);
                setLoading(false);
            }

            @Override
            public void onError(String error) {
                postError(error);
                setLoading(false);
            }
        });
    }

    // Refresh cả list + detail
    public void refreshAll(Context context) {
        loadOrders(context, currentFilter);

        if (selectedOrder.getValue() != null) {
            loadOrderDetail(context, selectedOrder.getValue().getId());
        }
    }
}