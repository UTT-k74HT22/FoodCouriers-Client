package com.utt.foodcouriers_client.ui.checkout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.Address;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.model.PaymentInitResult;
import com.utt.foodcouriers_client.data.remote.AddressClient;
import com.utt.foodcouriers_client.data.remote.AddressClient;
import com.utt.foodcouriers_client.ui.cart.CartFragment;
import com.utt.foodcouriers_client.ui.checkout.adapter.AddressSelectAdapter;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.order.OrderSuccessActivity;
import com.utt.foodcouriers_client.ui.profile.AddressFormActivity;
import com.utt.foodcouriers_client.ui.cart.adapter.CartItemAdapter;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;
import com.utt.foodcouriers_client.ui.order.OrderSuccessActivity;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.payment.PaymentMethodEnum;
import com.utt.foodcouriers_client.viewmodel.CheckoutViewModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends BaseActivity {
    private static final String TAG = "CheckoutFlow";

    private CheckoutViewModel viewModel;
    private CartItemAdapter adapter;
    private SessionManager sessionManager;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private TextView tvSubtotal, tvDeliveryFee, tvTotal, tvDiscount, tvAddress, tvPromoError, tvVnpayNote;
    private View layoutDiscount;
    private EditText etNote, etPromoCode;
    private View btnApplyPromo, cardAddress;

    private Address selectedAddress;
    private Button btnPlaceOrder;
    private RadioGroup rgPaymentMethod;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        configureToolbar(toolbar, true);
        setToolbarTitle(getString(R.string.checkout_title));

        viewModel = new ViewModelProvider(this).get(CheckoutViewModel.class);
        sessionManager = SessionManager.getInstance(this);
        initViews();
        setupRecyclerView();
        setupPaymentMethodSelector();
        bindObservers();
        loadDefaultAddress();

        ArrayList<String> selectedIds = getIntent().getStringArrayListExtra(CartFragment.EXTRA_SELECTED_CART_ITEM_IDS);
        if (selectedIds != null) {
            // Sử dụng tọa độ mặc định là 0,0 nếu chưa có địa chỉ để tránh tính phí sai lệch lớn (Hà Nội vs HCM)
            // CheckoutViewModel sẽ xử lý trường hợp này
            double deliveryLat = 0;
            double deliveryLon = 0;
            if (selectedAddress != null && selectedAddress.getLatitude() != null) {
                deliveryLat = selectedAddress.getLatitude();
                deliveryLon = selectedAddress.getLongitude();
            }
            viewModel.loadCheckoutData(this, selectedIds, deliveryLat, deliveryLon);
        }

        findViewById(R.id.btn_place_order).setOnClickListener(v -> {
            if (selectedAddress == null) {
                showErrorBanner("Vui lòng chọn địa chỉ giao hàng");
                return;
            }
            showOrderConfirmationDialog();
        // btnPlaceOrder được init trong initViews() — gọi sau setupRecyclerView() là an toàn
        btnPlaceOrder.setOnClickListener(v -> {
            String address = tvAddress.getText().toString();
            String note = etNote.getText().toString();
            String paymentMethod = getSelectedPaymentMethod();
            double latitude = selectedAddress != null && selectedAddress.getLatitude() != null
                    ? selectedAddress.getLatitude() : 0d;
            double longitude = selectedAddress != null && selectedAddress.getLongitude() != null
                    ? selectedAddress.getLongitude() : 0d;
            Log.d(TAG, "Step 1: Confirm checkout paymentMethod=" + paymentMethod
                    + ", address=" + address + ", lat=" + latitude + ", lon=" + longitude);
            viewModel.placeOrders(this, address, latitude, longitude, note, paymentMethod);
        });

        btnApplyPromo.setOnClickListener(v -> {
            String code = etPromoCode.getText().toString();
            viewModel.validatePromotion(this, code);
        });
    }

    private void showOrderConfirmationDialog() {
        String phone = sessionManager.getUserPhone();
        String address = tvAddress.getText().toString().trim();

        if (phone == null || phone.isEmpty()) {
            ToastBanner.showError(getString(R.string.checkout_error_no_phone));
            return;
        }

        if (address.isEmpty() || address.equals("Chưa có địa chỉ")) {
            ToastBanner.showError(getString(R.string.checkout_error_no_address));
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.checkout_confirm_title)
                .setMessage(R.string.checkout_confirm_message)
                .setPositiveButton(R.string.checkout_confirm_yes, (dialog, which) -> {
                    String note = etNote.getText().toString();
                    double lat = selectedAddress != null && selectedAddress.getLatitude() != null ? selectedAddress.getLatitude() : 21.002;
                    double lon = selectedAddress != null && selectedAddress.getLongitude() != null ? selectedAddress.getLongitude() : 105.843;
                    viewModel.placeOrders(this, address, note, "cod", lat, lon);
                })
                .setNegativeButton(R.string.checkout_confirm_no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void initViews() {
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDeliveryFee = findViewById(R.id.tv_delivery_fee);
        tvTotal = findViewById(R.id.tv_total);
        tvDiscount = findViewById(R.id.tv_discount);
        tvDeliveryDistance = findViewById(R.id.tv_delivery_distance);
        layoutDiscount = findViewById(R.id.layout_discount);
        tvAddress = findViewById(R.id.tv_address);
        tvAddressLabel = findViewById(R.id.tv_address_label);
        etNote = findViewById(R.id.et_note);
        etPromoCode = findViewById(R.id.et_promo_code);
        btnApplyPromo = findViewById(R.id.btn_apply_promo);
        tvPromoError = findViewById(R.id.tv_promo_error);
        tvVnpayNote = findViewById(R.id.tv_vnpay_note);
        rgPaymentMethod = findViewById(R.id.rg_payment_method);
        btnPlaceOrder = findViewById(R.id.btn_place_order);
        cardAddress = findViewById(R.id.card_address);

        cardAddress.setOnClickListener(v -> showAddressSelectionDialog());

        loadDefaultAddress();
        // Hiển thị thông báo nếu chưa có địa chỉ, ngược lại có thể lấy từ session/API
        // Hiện tại ta để mặc định là "Chưa có địa chỉ" để test logic validate
        tvAddress.setText("Chưa có địa chỉ");
        ((TextView) findViewById(R.id.tv_address_label)).setText("Địa chỉ giao hàng");
    }

    private void loadDefaultAddress() {
        String userId = SessionManager.getInstance(this).getUserId();
        if (userId == null || userId.isBlank()) {
            Log.w(TAG, "Step 0: Missing userId, skip default address lookup");
            return;
        }

        Log.d(TAG, "Step 0: Loading default address for userId=" + userId);
        AddressClient.getInstance().getAddressesByUserId(userId, new AddressClient.ApiCallback<Address[]>() {
            @Override
            public void onSuccess(Address[] result) {
                runOnUiThread(() -> {
                    selectedAddress = resolveDefaultAddress(result);
                    if (selectedAddress == null) {
                        Log.w(TAG, "Step 0: No saved address found, using placeholder");
                        return;
                    }

                    String label = selectedAddress.getLabel() != null && !selectedAddress.getLabel().isBlank()
                            ? selectedAddress.getLabel() : "Địa chỉ giao hàng";
                    tvAddress.setText(selectedAddress.getDisplayAddress());
                    ((TextView) findViewById(R.id.tv_address_label)).setText(label);
                    Log.d(TAG, "Step 0: Selected address id=" + selectedAddress.getId()
                            + ", lat=" + selectedAddress.getLatitude()
                            + ", lon=" + selectedAddress.getLongitude());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Log.w(TAG, "Step 0: Failed to load address: " + error));
            }
        });
    }

    @Nullable
    private Address resolveDefaultAddress(@Nullable Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        for (Address address : addresses) {
            if (address != null && address.isDefault()) {
                return address;
            }
        }
        return addresses[0];
    }

    private void setupRecyclerView() {
        RecyclerView rvItems = findViewById(R.id.rv_order_items);
        adapter = new CartItemAdapter(this, null);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);
    }

    private void setupPaymentMethodSelector() {
        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_vnpay) {
                btnPlaceOrder.setText("Đặt hàng & Thanh toán VNPAY");
                tvVnpayNote.setVisibility(View.VISIBLE);
            } else {
                btnPlaceOrder.setText("Đặt hàng");
                tvVnpayNote.setVisibility(View.GONE);
            }
        });
    }

    private String getSelectedPaymentMethod() {
        if (rgPaymentMethod.getCheckedRadioButtonId() == R.id.rb_vnpay) {
            return PaymentMethodEnum.VNPAY.getValue();
        }
        return PaymentMethodEnum.COD.getValue();
    }

    private void bindObservers() {
        viewModel.getRestaurantGroups().observe(this, groups -> adapter.submitGroups(groups));

        viewModel.getCheckoutSummary().observe(this, summary -> {
            tvSubtotal.setText(currencyFormatter.format(summary.getSubtotal()));
            tvDeliveryFee.setText(currencyFormatter.format(summary.getDeliveryFee()));
            tvTotal.setText(currencyFormatter.format(summary.getTotal()));
            if (summary.getSavings() > 0) {
                layoutDiscount.setVisibility(View.VISIBLE);
                tvDiscount.setText("-" + currencyFormatter.format(summary.getSavings()));
            } else {
                layoutDiscount.setVisibility(View.GONE);
            }
        });

        viewModel.getDeliveryDistance().observe(this, distance -> {
            if (distance != null && !distance.isEmpty()) {
                tvDeliveryDistance.setVisibility(View.VISIBLE);
                tvDeliveryDistance.setText("(" + distance + ")");
            } else {
                tvDeliveryDistance.setVisibility(View.GONE);
            }
        });

        viewModel.getAppliedPromotion().observe(this, result -> {
            if (result == null) {
                tvPromoError.setVisibility(View.GONE);
                return;
            }
            tvPromoError.setVisibility(View.VISIBLE);
            tvPromoError.setText(result.getMessage());
            if (result.isValid()) {
                tvPromoError.setTextColor(getResources().getColor(R.color.success, getTheme()));
            } else {
                tvPromoError.setTextColor(getResources().getColor(R.color.error, getTheme()));
            }
        });

        viewModel.getLoading().observe(this, isLoading -> {
            btnPlaceOrder.setEnabled(!isLoading);
            btnPlaceOrder.setAlpha(isLoading ? 0.5f : 1.0f);
            btnApplyPromo.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isBlank()) {
                showErrorBanner(error);
            }
        });

        // COD: đặt hàng thành công → navigate sang OrderSuccessActivity
        viewModel.getIsOrderSuccess().observe(this, success -> {
            if (success) {
                List<OrderSummary> orders = viewModel.getCreatedOrders().getValue();
                if (orders != null && !orders.isEmpty()) {
                    Intent intent = new Intent(this, OrderSuccessActivity.class);
                    intent.putExtra(OrderSuccessActivity.EXTRA_ORDER_ID, orders.get(0).getId());
                    startActivity(intent);
                    finish();
                }
            }
        });

        // VNPAY: lưu order_id rồi mở trình duyệt
        viewModel.getVnpayPaymentResult().observe(this, this::openVnpayBrowser);
    }

    /**
     * Lưu orderId vào SessionManager để PaymentCallbackActivity đọc sau callback,
     * sau đó mở trang thanh toán VNPAY bằng trình duyệt mặc định.
     */
    private void openVnpayBrowser(PaymentInitResult result) {
        if (result == null || result.getPaymentUrl().isEmpty()) {
            showErrorBanner("Không lấy được đường dẫn thanh toán.");
            return;
        }

        // Lưu orderId để PaymentCallbackActivity sử dụng sau deep link callback
        SessionManager.getInstance(this).setPendingPaymentOrderId(result.getOrderId());
        Log.d(TAG, "Step 5: Opening VNPAY browser for orderId=" + result.getOrderId()
                + ", txnRef=" + result.getProviderOrderRef());

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getPaymentUrl()));
        startActivity(browserIntent);

        // Kết thúc CheckoutActivity; khi user quay về app sẽ vào PaymentCallbackActivity
        finish();
    }

    private void loadDefaultAddress() {
        String userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) {
            showErrorBanner("Vui lòng đăng nhập để sử dụng");
            return;
        }

        AddressClient.getInstance().getAddressesByUserId(userId, new AddressClient.ApiCallback<Address[]>() {
            @Override
            public void onSuccess(Address[] result) {
                runOnUiThread(() -> {
                    if (result != null && result.length > 0) {
                        Address defaultAddr = null;
                        for (Address addr : result) {
                            if (addr.isDefault()) {
                                defaultAddr = addr;
                                break;
                            }
                        }
                        if (defaultAddr == null) {
                            defaultAddr = result[0];
                        }
                        setSelectedAddress(defaultAddr);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> showErrorBanner("Không thể tải địa chỉ: " + error));
            }
        });
    }

    private void setSelectedAddress(Address address) {
        selectedAddress = address;
        if (address != null) {
            tvAddressLabel.setText(address.getLabel());
            tvAddress.setText(address.getDisplayAddress());

            // Recalculate checkout data with new coordinates
            ArrayList<String> selectedIds = getIntent().getStringArrayListExtra(CartFragment.EXTRA_SELECTED_CART_ITEM_IDS);
            if (selectedIds != null) {
                viewModel.loadCheckoutData(this, selectedIds, address.getLatitude(), address.getLongitude());
            }
        } else {
            tvAddressLabel.setText("");
            tvAddress.setText("Chưa có địa chỉ");
        }
    }

    private void showAddressSelectionDialog() {
        String userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_address, null);
        RecyclerView rvAddresses = dialogView.findViewById(R.id.rv_addresses);
        TextView tvEmpty = dialogView.findViewById(R.id.tv_empty);
        Button btnAddAddress = dialogView.findViewById(R.id.btn_add_address);

        AddressSelectAdapter adapter = new AddressSelectAdapter();
        adapter.setOnAddressSelectedListener(address -> {
            setSelectedAddress(address);
        });
        rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        rvAddresses.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        AddressClient.getInstance().getAddressesByUserId(userId, new AddressClient.ApiCallback<Address[]>() {
            @Override
            public void onSuccess(Address[] result) {
                runOnUiThread(() -> {
                    if (result != null && result.length > 0) {
                        rvAddresses.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                        adapter.setAddresses(java.util.Arrays.asList(result));
                        if (selectedAddress != null) {
                            adapter.setSelectedAddressId(selectedAddress.getId());
                        }
                    } else {
                        rvAddresses.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    rvAddresses.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Lỗi: " + error);
                });
            }
        });

        btnAddAddress.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, AddressFormActivity.class);
            startActivityForResult(intent, 200);
        });

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadDefaultAddress();
        }
    }
}
