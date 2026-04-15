package com.utt.foodcouriers_client.ui.checkout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.Address;
import com.utt.foodcouriers_client.data.model.OrderSummary;
import com.utt.foodcouriers_client.data.model.PaymentInitResult;
import com.utt.foodcouriers_client.data.remote.AddressClient;
import com.utt.foodcouriers_client.ui.cart.CartFragment;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.cart.adapter.CartItemAdapter;
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
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private TextView tvSubtotal, tvDeliveryFee, tvTotal, tvDiscount, tvAddress, tvPromoError, tvVnpayNote;
    private View layoutDiscount;
    private EditText etNote, etPromoCode;
    private View btnApplyPromo;
    private Button btnPlaceOrder;
    private RadioGroup rgPaymentMethod;
    private Address selectedAddress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        configureToolbar(toolbar, true);
        setToolbarTitle(getString(R.string.checkout_title));

        viewModel = new ViewModelProvider(this).get(CheckoutViewModel.class);
        initViews();
        setupRecyclerView();
        setupPaymentMethodSelector();
        bindObservers();
        loadDefaultAddress();

        ArrayList<String> selectedIds = getIntent().getStringArrayListExtra(CartFragment.EXTRA_SELECTED_CART_ITEM_IDS);
        if (selectedIds != null) {
            viewModel.loadCheckoutData(this, selectedIds);
        }

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

    private void initViews() {
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDeliveryFee = findViewById(R.id.tv_delivery_fee);
        tvTotal = findViewById(R.id.tv_total);
        tvDiscount = findViewById(R.id.tv_discount);
        layoutDiscount = findViewById(R.id.layout_discount);
        tvAddress = findViewById(R.id.tv_address);
        etNote = findViewById(R.id.et_note);
        etPromoCode = findViewById(R.id.et_promo_code);
        btnApplyPromo = findViewById(R.id.btn_apply_promo);
        tvPromoError = findViewById(R.id.tv_promo_error);
        tvVnpayNote = findViewById(R.id.tv_vnpay_note);
        rgPaymentMethod = findViewById(R.id.rg_payment_method);
        btnPlaceOrder = findViewById(R.id.btn_place_order);

        // Mock address for now
        tvAddress.setText("123 Phố Chùa Láng, Đống Đa, Hà Nội");
        ((TextView) findViewById(R.id.tv_address_label)).setText("Nhà riêng");
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
}
