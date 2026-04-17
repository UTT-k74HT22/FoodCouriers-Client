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
import com.utt.foodcouriers_client.utils.CartDTO.CartState;
import com.utt.foodcouriers_client.utils.CartDTO.CartSummary;

import java.util.List;
import java.util.Map;

/**
 * ViewModel điều phối trạng thái và thao tác của màn hình giỏ hàng.
 *
 * <p>Trách nhiệm chính:
 * <ul>
 *     <li>Gọi {@link CartRepository} để tải, thêm, cập nhật và xóa item trong cart.</li>
 *     <li>Chuyển {@link CartState} từ repository thành các {@link LiveData} nhỏ cho UI quan sát.</li>
 *     <li>Quản lý trạng thái loading, success message và error thông qua {@link BaseViewModel}.</li>
 *     <li>Ẩn chi tiết xử lý authentication và callback bất đồng bộ khỏi tầng Fragment.</li>
 * </ul>
 *
 * <p>ViewModel này không tự tính toán dữ liệu cart. Repository là nơi gom nhóm,
 * tính summary và tạo map quantity; ViewModel chỉ publish state an toàn cho UI.
 */
public class CartViewModel extends BaseViewModel {

    private final CartRepository repository = CartRepository.getInstance();
    private final MutableLiveData<List<CartItem>> cartItems = new MutableLiveData<>();
    private final MutableLiveData<List<CartRestaurantGroup>> restaurantGroups = new MutableLiveData<>();
    private final MutableLiveData<CartSummary> cartSummary = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Integer>> menuItemQuantities = new MutableLiveData<>();
    private final MutableLiveData<Boolean> emptyState = new MutableLiveData<>(true);
    private final MutableLiveData<Integer> selectedItemCount = new MutableLiveData<>(0);

    /**
     * Trả về danh sách item phẳng trong cart.
     *
     * @return LiveData chứa danh sách {@link CartItem}
     */
    public LiveData<List<CartItem>> getCartItems() {
        return cartItems;
    }

    /**
     * Trả về summary tổng quan của cart.
     *
     * @return LiveData chứa {@link CartSummary}
     */
    public LiveData<CartSummary> getCartSummary() {
        return cartSummary;
    }

    /**
     * Trả về danh sách item đã được gom theo nhà hàng.
     *
     * @return LiveData chứa danh sách {@link CartRestaurantGroup}
     */
    public LiveData<List<CartRestaurantGroup>> getRestaurantGroups() {
        return restaurantGroups;
    }

    /**
     * Trả về map số lượng item theo menuItemId.
     *
     * <p>Dữ liệu này thường được dùng ở màn hình chi tiết món để biết món đã có trong cart hay chưa.
     *
     * @return LiveData map với key là menuItemId và value là quantity
     */
    public LiveData<Map<String, Integer>> getMenuItemQuantities() {
        return menuItemQuantities;
    }

    /**
     * Cho biết cart hiện tại có rỗng hay không.
     *
     * @return LiveData true nếu cart không có item
     */
    public LiveData<Boolean> getEmptyState() {
        return emptyState;
    }

    /**
     * Trả về số lượng item đang được chọn trên UI.
     *
     * @return LiveData chứa số item được chọn
     */
    public LiveData<Integer> getSelectedItemCount() {
        return selectedItemCount;
    }

    /**
     * Tải cart hiện tại của người dùng.
     *
     * <p>Nếu người dùng chưa đăng nhập, ViewModel publish cart rỗng và không hiển thị lỗi chung.
     * Các lỗi khác sẽ được chuyển lên UI qua {@link BaseViewModel#getErrorMessage()}.
     *
     * @param context context hiện tại, sẽ được chuyển về application context trước khi gọi repository
     */
    public void loadCart(Context context) {
        setLoading(true);
        repository.getCart(context.getApplicationContext(), new RepositoryCallback<CartState>() {
            @Override
            public void onSuccess(CartState result) {
                publishState(result);
                setLoading(false);
            }

            @Override
            public void onError(String error) {
                if (!"AUTH_REQUIRED".equals(error)) {
                    postError(error);
                }
                publishState(CartState.empty());
                setLoading(false);
            }
        });
    }

    /**
     * Thêm một menu item vào cart.
     *
     * <p>Nếu item đã tồn tại, repository sẽ cộng dồn quantity theo logic của data layer.
     *
     * @param context context hiện tại
     * @param menuItem món cần thêm
     * @param restaurant nhà hàng của món
     * @param quantity số lượng cần thêm
     * @param note ghi chú của item
     */
    public void addMenuItem(Context context, MenuItem menuItem, Restaurant restaurant, int quantity, String note) {
        setLoading(true);
        repository.addToCart(context.getApplicationContext(), menuItem, restaurant, quantity, note, new RepositoryCallback<CartState>() {
            @Override
            public void onSuccess(CartState result) {
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

    /**
     * Cập nhật quantity của một cart item theo id bản ghi cart item.
     *
     * <p>Nếu quantity <= 0, repository sẽ xóa item khỏi cart.
     *
     * @param context context hiện tại
     * @param cartItemId id của bản ghi cart item
     * @param quantity số lượng mới
     */
    public void updateCartItemQuantity(Context context, String cartItemId, int quantity) {
        setLoading(true);
        repository.updateCartItemQuantity(context.getApplicationContext(), cartItemId, quantity, new RepositoryCallback<CartState>() {
            @Override
            public void onSuccess(CartState result) {
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

    /**
     * Đặt quantity cuối cùng cho một menu item.
     *
     * <p>Hàm này phù hợp với các màn hình không nắm cartItemId nhưng có {@link MenuItem}.
     * Nếu quantity <= 0, item tương ứng sẽ bị xóa khỏi cart nếu đang tồn tại.
     *
     * @param context context hiện tại
     * @param menuItem món cần cập nhật
     * @param restaurant nhà hàng của món
     * @param quantity số lượng cuối cùng cần đặt
     * @param note ghi chú của item
     */
    public void setMenuItemQuantity(Context context, MenuItem menuItem, Restaurant restaurant, int quantity, String note) {
        setLoading(true);
        repository.setMenuItemQuantity(context.getApplicationContext(), menuItem, restaurant, quantity, note, new RepositoryCallback<CartState>() {
            @Override
            public void onSuccess(CartState result) {
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

    /**
     * Kiểm tra trạng thái đăng nhập hiện tại.
     *
     * @param context context dùng để truy cập session
     * @return true nếu user đã đăng nhập, ngược lại false
     */
    public boolean isLoggedIn(Context context) {
        return repository.isLoggedIn(context.getApplicationContext());
    }

    /**
     * Publish state từ repository thành các LiveData nhỏ cho Fragment quan sát.
     *
     * <p>Nếu state null, ViewModel sẽ dùng {@link CartState#empty()} để đảm bảo UI luôn nhận dữ liệu hợp lệ.
     *
     * @param state trạng thái cart mới nhất từ repository
     */
    private void publishState(CartState state) {
        CartState safeState = state == null ? CartState.empty() : state;
        cartItems.setValue(safeState.getItems());
        restaurantGroups.setValue(safeState.getRestaurantGroups());
        cartSummary.setValue(safeState.getSummary());
        menuItemQuantities.setValue(safeState.getMenuItemQuantities());
        emptyState.setValue(safeState.getItems() == null || safeState.getItems().isEmpty());
    }

    /**
     * Xóa nhiều cart item đang được chọn.
     *
     * <p>Nếu danh sách id null hoặc rỗng thì bỏ qua để tránh gọi repository không cần thiết.
     *
     * @param context context hiện tại
     * @param cartItemIds danh sách id cart item cần xóa
     */
    public void removeSelectedItems(Context context, List<String> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return;
        }
        setLoading(true);
        repository.removeItems(context.getApplicationContext(), cartItemIds, new RepositoryCallback<CartState>() {
            @Override
            public void onSuccess(CartState result) {
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
