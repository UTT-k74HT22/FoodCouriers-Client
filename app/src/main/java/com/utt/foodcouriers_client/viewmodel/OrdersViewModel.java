package com.utt.foodcouriers_client.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.repository.OrderRepository;

import java.util.List;

/**
 * ViewModel điều phối state cho lịch sử đơn, chi tiết đơn và màn tracking.
 *
 * <p>ViewModel giữ filter hiện tại của danh sách. Khi realtime báo có thay đổi,
 * Fragment/Activity gọi {@link #refresh(Context)} hoặc {@link #loadOrderDetail(Context, String)}
 * để kéo lại dữ liệu đầy đủ từ {@link OrderRepository}.</p>
 */
public class OrdersViewModel extends BaseViewModel {

    private final OrderRepository repository = OrderRepository.getInstance();

    private final MutableLiveData<List<OrderSummary>> orders = new MutableLiveData<>();
    private final MutableLiveData<OrderSummary> selectedOrder = new MutableLiveData<>();

    private OrderRepository.OrderFilter currentFilter = OrderRepository.OrderFilter.ALL;

    /**
     * @return danh sách đơn hàng theo filter hiện tại
     */
    public LiveData<List<OrderSummary>> getOrders() {
        return orders;
    }

    /**
     * @return đơn hàng đang được mở ở detail/tracking
     */
    public LiveData<OrderSummary> getSelectedOrder() {
        return selectedOrder;
    }

    /**
     * Load danh sách đơn hàng theo filter được chọn.
     *
     * @param context context màn hình để repository lấy session
     * @param filter filter tab hiện tại; null sẽ mặc định là ALL
     */
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

    /**
     * Refresh danh sách bằng filter đang lưu.
     *
     * @param context context màn hình
     */
    public void refresh(Context context) {
        loadOrders(context, currentFilter);
    }

    /**
     * Load chi tiết một đơn hàng cho detail/tracking.
     *
     * @param context context màn hình
     * @param orderId id đơn hàng cần load
     */
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

    /**
     * Refresh cả danh sách và chi tiết đang chọn nếu có.
     *
     * @param context context màn hình
     */
    public void refreshAll(Context context) {
        loadOrders(context, currentFilter);

        if (selectedOrder.getValue() != null) {
            loadOrderDetail(context, selectedOrder.getValue().getId());
        }
    }
}
