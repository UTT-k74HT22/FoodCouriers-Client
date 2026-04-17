package com.utt.foodcouriers_client.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.databinding.FragmentCartBinding;
import com.utt.foodcouriers_client.ui.auth.LoginActivity;
import com.utt.foodcouriers_client.ui.cart.adapter.CartItemAdapter;
import com.utt.foodcouriers_client.ui.cart.adapter.CartItemListener;
import com.utt.foodcouriers_client.ui.checkout.CheckoutActivity;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.utils.ToastBanner;
import com.utt.foodcouriers_client.viewmodel.CartViewModel;
import java.text.NumberFormat;
import java.util.Locale;


/**
 * Fragment hiển thị màn hình giỏ hàng của người dùng.
 *
 * <p>Trách nhiệm chính:
 * <ul>
 *     <li>Khởi tạo giao diện cart.</li>
 *     <li>Kiểm tra trạng thái đăng nhập trước khi cho phép truy cập.</li>
 *     <li>Lắng nghe dữ liệu từ {@code CartViewModel} để render danh sách món, tổng tiền và trạng thái rỗng.</li>
 *     <li>Xử lý các thao tác của người dùng như tăng/giảm số lượng, chọn món, xóa món đã chọn và chuyển sang checkout.</li>
 * </ul>
 *
 * <p>Luồng tổng quát:
 * UI mở fragment -> kiểm tra login -> setup adapter/action/observer -> load cart
 * -> nhận dữ liệu từ ViewModel -> render danh sách và summary.
 */
public class CartFragment extends BaseFragment {

    /**
     * Key dùng để truyền danh sách id cart item đã chọn sang màn hình checkout.
     */
    public static final String EXTRA_SELECTED_CART_ITEM_IDS = "selected_cart_item_ids";
    private FragmentCartBinding binding;
    private CartViewModel viewModel;
    private CartItemAdapter adapter;
    private CartItemAdapter.SelectionState currentSelection = new CartItemAdapter.SelectionState(new java.util.ArrayList<>(), 0, 0, 0, 0, 0);
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    /**
     * Inflate layout của fragment và khởi tạo ViewBinding. (Chuyển đổi XML qua đối tượng View<Java,Kotlin>)
     *
     * @param inflater đối tượng dùng để inflate layout
     * @param container ViewGroup cha của fragment
     * @param savedInstanceState trạng thái cũ nếu fragment được tạo lại
     * @return root view của fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Điểm khởi tạo logic chính của màn hình cart sau khi view đã được tạo.
     *
     * <p>Hàm này thực hiện:
     * <ol>
     *     <li>Đặt tiêu đề toolbar.</li>
     *     <li>Khởi tạo {@code CartViewModel}.</li>
     *     <li>Kiểm tra đăng nhập. Nếu chưa đăng nhập thì chuyển sang {@code LoginActivity}.</li>
     *     <li>Khởi tạo RecyclerView, các action click và observer.</li>
     *     <li>Yêu cầu ViewModel tải dữ liệu cart.</li>
     * </ol>
     *
     * @param view root view đã được tạo
     * @param savedInstanceState trạng thái cũ nếu có
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setToolbarTitle(getString(R.string.cart_title));

        viewModel = new ViewModelProvider(this).get(CartViewModel.class);
        if (!viewModel.isLoggedIn(requireContext())) {
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }
        setupRecyclerView();
        setupActions();
        observeViewModel();
        viewModel.loadCart(requireContext());
    }

    /**
     * Khởi tạo RecyclerView và gắn {@code CartItemAdapter}.
     *
     * <p>Adapter sẽ callback ngược về fragment khi:
     * <ul>
     *     <li>Người dùng tăng số lượng item.</li>
     *     <li>Người dùng giảm số lượng item.</li>
     *     <li>Người dùng thay đổi trạng thái chọn item/restaurant.</li>
     * </ul>
     *
     * <p>Fragment đóng vai trò điều phối:
     * lấy quantity hiện tại từ adapter rồi gọi ViewModel để cập nhật dữ liệu,
     * đồng thời render lại summary theo phần đang được chọn.
     */
    private void setupRecyclerView() {
        adapter = new CartItemAdapter(requireContext(), new CartItemListener() {
            @Override
            public void onIncrease(String cartItemId) {
                int currentQuantity = adapter.getQuantityForItem(cartItemId);
                viewModel.updateCartItemQuantity(requireContext(), cartItemId, currentQuantity + 1);
            }

            @Override
            public void onDecrease(String cartItemId) {
                int currentQuantity = adapter.getQuantityForItem(cartItemId);
                viewModel.updateCartItemQuantity(requireContext(), cartItemId, currentQuantity - 1);
            }

            @Override
            public void onSelectionChanged(CartItemAdapter.SelectionState selectionState) {
                currentSelection = selectionState;
                renderSelectionSummary(selectionState);
            }
        });

        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCartItems.setNestedScrollingEnabled(false);
        binding.rvCartItems.setAdapter(adapter);
    }

    /**
     * Gắn các action click cho nút xóa item đã chọn, checkout và quay lại khám phá món ăn.
     *
     * <p>Rule hiện tại:
     * <ul>
     *     <li>Nếu chưa chọn item nào thì không cho xóa.</li>
     *     <li>Nếu chưa chọn item nào thì không cho checkout.</li>
     *     <li>Khi checkout, danh sách id cart item đã chọn sẽ được truyền sang {@code CheckoutActivity}.</li>
     * </ul>
     */
    private void setupActions() {
        binding.btnRemoveSelected.setOnClickListener(v -> {
            if (currentSelection.getSelectedCartItemIds().isEmpty()) {
                showErrorSnackbar("Vui lòng chọn ít nhất một món để xóa.");
                return;
            }
            viewModel.removeSelectedItems(requireContext(), currentSelection.getSelectedCartItemIds());
        });

        binding.btnCheckout.setOnClickListener(v -> {
            if (currentSelection.getSelectedCartItemIds().isEmpty()) {
                showErrorSnackbar("Hãy chọn một món để tiếp tục.");
                return;
            }
            Intent intent = new Intent(requireContext(), CheckoutActivity.class);
            intent.putStringArrayListExtra(EXTRA_SELECTED_CART_ITEM_IDS, new java.util.ArrayList<>(currentSelection.getSelectedCartItemIds()));
            startActivity(intent);
        });

        binding.btnExploreMenus.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    /**
     * Đăng ký observer với dữ liệu từ {@code CartViewModel}.
     *
     * <p>Các observer hiện tại gồm:
     * <ul>
     *     <li>Danh sách nhóm nhà hàng trong cart.</li>
     *     <li>Thông tin tổng tiền của cart.</li>
     *     <li>Trạng thái cart rỗng hay có dữ liệu.</li>
     *     <li>Thông báo thành công để hiển thị banner/toast.</li>
     * </ul>
     *
     * <p>Hàm này là cầu nối giữa dữ liệu từ ViewModel và phần hiển thị UI thực tế.
     */
    private void observeViewModel() {
        viewModel.getRestaurantGroups().observe(getViewLifecycleOwner(), groups -> adapter.submitGroups(groups));

        viewModel.getCartSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary == null) {
                return;
            }

            binding.tvCartCount.setText(getString(R.string.cart_item_count, summary.getItemCount()));
            binding.tvSubtotalValue.setText(currencyFormatter.format(summary.getSubtotal()));
            binding.tvDeliveryValue.setText(currencyFormatter.format(summary.getDeliveryFee()));
            binding.tvTotalPrice.setText(currencyFormatter.format(summary.getTotal()));
        });

        viewModel.getEmptyState().observe(getViewLifecycleOwner(), isEmpty -> {
            boolean empty = Boolean.TRUE.equals(isEmpty);
            binding.layoutCartContent.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.cardCheckout.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.emptyState.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.btnExploreMenus.setVisibility(empty ? View.VISIBLE : View.GONE);

            if (empty) {
                binding.emptyState.tvEmptyTitle.setText(R.string.cart_empty_title);
                binding.emptyState.tvEmptyMessage.setText(R.string.cart_empty_message);
                binding.tvCartCount.setText("");
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.isEmpty()) {
                return;
            }
            ToastBanner.showSuccess(message);
        });
    }

    /**
     * Render lại phần summary theo trạng thái item đang được chọn trong adapter.
     *
     * <p>Lưu ý:
     * hàm này không hiển thị tổng của toàn bộ cart,
     * mà hiển thị tổng theo danh sách item hiện đang được chọn.
     *
     * @param selectionState snapshot trạng thái chọn hiện tại do adapter tính toán
     */
    private void renderSelectionSummary(CartItemAdapter.SelectionState selectionState) {
        binding.tvSubtotalValue.setText(currencyFormatter.format(selectionState.getSubtotal()));
        binding.tvDeliveryValue.setText(currencyFormatter.format(selectionState.getDeliveryFee()));
        binding.tvTotalPrice.setText(currencyFormatter.format(selectionState.getTotal()));
        binding.btnCheckout.setEnabled(!selectionState.getSelectedCartItemIds().isEmpty());
        binding.btnCheckout.setAlpha(selectionState.getSelectedCartItemIds().isEmpty() ? 0.5f : 1f);
        binding.tvCartCount.setText(getString(R.string.cart_item_count, selectionState.getSelectedItemCount()));
        
        boolean hasSelection = !selectionState.getSelectedCartItemIds().isEmpty();
        binding.btnRemoveSelected.setVisibility(hasSelection ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
