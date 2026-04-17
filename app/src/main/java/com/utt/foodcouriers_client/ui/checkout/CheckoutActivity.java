package com.utt.foodcouriers_client.ui.checkout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.utt.foodcouriers_client.ui.cart.CartFragment;
import com.utt.foodcouriers_client.ui.cart.adapter.CartItemAdapter;
import com.utt.foodcouriers_client.ui.checkout.adapter.AddressSelectAdapter;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.order.OrderSuccessActivity;
import com.utt.foodcouriers_client.ui.profile.AddressFormActivity;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;
import com.utt.foodcouriers_client.utils.payment.PaymentMethodEnum;
import com.utt.foodcouriers_client.viewmodel.CheckoutViewModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends BaseActivity {
    private static final String TAG = "CheckoutFlow";
    private static final int REQUEST_ADD_ADDRESS = 200;

    private CheckoutViewModel viewModel;
    private CartItemAdapter adapter;
    private SessionManager sessionManager;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private TextView tvSubtotal;
    private TextView tvDeliveryFee;
    private TextView tvDeliveryDistance;
    private TextView tvTotal;
    private TextView tvDiscount;
    private TextView tvAddress;
    private TextView tvAddressLabel;
    private TextView tvPromoError;
    private TextView tvVnpayNote;
    private View layoutDiscount;
    private View btnApplyPromo;
    private View cardAddress;
    private EditText etNote;
    private EditText etPromoCode;
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
        sessionManager = SessionManager.getInstance(this);

        initViews();
        setupRecyclerView();
        setupPaymentMethodSelector();
        bindObservers();
        setupActions();
        loadDefaultAddress();
        loadCheckoutForCurrentAddress();
    }

    private void initViews() {
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDeliveryFee = findViewById(R.id.tv_delivery_fee);
        tvDeliveryDistance = findViewById(R.id.tv_delivery_distance);
        tvTotal = findViewById(R.id.tv_total);
        tvDiscount = findViewById(R.id.tv_discount);
        tvAddress = findViewById(R.id.tv_address);
        tvAddressLabel = findViewById(R.id.tv_address_label);
        tvPromoError = findViewById(R.id.tv_promo_error);
        tvVnpayNote = findViewById(R.id.tv_vnpay_note);
        layoutDiscount = findViewById(R.id.layout_discount);
        btnApplyPromo = findViewById(R.id.btn_apply_promo);
        cardAddress = findViewById(R.id.card_address);
        etNote = findViewById(R.id.et_note);
        etPromoCode = findViewById(R.id.et_promo_code);
        btnPlaceOrder = findViewById(R.id.btn_place_order);
        rgPaymentMethod = findViewById(R.id.rg_payment_method);

        tvAddressLabel.setText(R.string.checkout_payment_address_label);
        tvAddress.setText(R.string.checkout_payment_no_address);
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
                btnPlaceOrder.setText(R.string.checkout_payment_vnpay_button);
                tvVnpayNote.setVisibility(View.VISIBLE);
            } else {
                btnPlaceOrder.setText(R.string.checkout_payment_cod_button);
                tvVnpayNote.setVisibility(View.GONE);
            }
        });
    }

    private void setupActions() {
        btnPlaceOrder.setOnClickListener(v -> {
            if (selectedAddress == null) {
                showErrorBanner(getString(R.string.checkout_error_no_address));
                return;
            }
            showOrderConfirmationDialog();
        });

        btnApplyPromo.setOnClickListener(v -> {
            String code = etPromoCode.getText().toString();
            viewModel.validatePromotion(this, code);
        });

        cardAddress.setOnClickListener(v -> showAddressSelectionDialog());
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
            if (distance != null && !distance.isBlank()) {
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
        // Chuyển sang OrderSuccessActivity khi đơn hàng được tạo thành công
        viewModel.getIsOrderSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                List<OrderSummary> orders = viewModel.getCreatedOrders().getValue();
                if (orders != null && !orders.isEmpty()) {
                    Intent intent = new Intent(this, OrderSuccessActivity.class);
                    intent.putExtra(OrderSuccessActivity.EXTRA_ORDER_ID, orders.get(0).getId());
                    startActivity(intent);
                    finish();
                }
            }
        });

        viewModel.getVnpayPaymentResult().observe(this, this::openVnpayBrowser);
    }

    private void loadCheckoutForCurrentAddress() {
        ArrayList<String> selectedIds = getIntent().getStringArrayListExtra(CartFragment.EXTRA_SELECTED_CART_ITEM_IDS);
        if (selectedIds == null) {
            return;
        }

        double deliveryLat = selectedAddress != null && selectedAddress.getLatitude() != null
                ? selectedAddress.getLatitude() : 0d;
        double deliveryLon = selectedAddress != null && selectedAddress.getLongitude() != null
                ? selectedAddress.getLongitude() : 0d;
        viewModel.loadCheckoutData(this, selectedIds, deliveryLat, deliveryLon);
    }

    private void loadDefaultAddress() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            Log.w(TAG, "Step 0: Missing userId, skip default address lookup");
            return;
        }

        Log.d(TAG, "Step 0: Loading default address for userId=" + userId);
        AddressClient.getInstance().getAddressesByUserId(userId, new AddressClient.ApiCallback<Address[]>() {
            @Override
            public void onSuccess(Address[] result) {
                runOnUiThread(() -> {
                    Address address = resolveDefaultAddress(result);
                    if (address != null) {
                        setSelectedAddress(address, true);
                    } else {
                        Log.w(TAG, "Step 0: No saved address found");
                    }
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

    private void setSelectedAddress(@Nullable Address address, boolean reloadCheckout) {
        selectedAddress = address;
        if (address == null) {
            tvAddressLabel.setText(R.string.checkout_payment_address_label);
            tvAddress.setText(R.string.checkout_payment_no_address);
            if (reloadCheckout) {
                loadCheckoutForCurrentAddress();
            }
            return;
        }

        String label = address.getLabel() != null && !address.getLabel().isBlank()
                ? address.getLabel() : getString(R.string.checkout_payment_address_label);
        tvAddressLabel.setText(label);
        tvAddress.setText(address.getDisplayAddress());
        Log.d(TAG, "Step 0: Selected address id=" + address.getId()
                + ", lat=" + address.getLatitude()
                + ", lon=" + address.getLongitude());

        if (reloadCheckout) {
            loadCheckoutForCurrentAddress();
        }
    }

    private String getSelectedPaymentMethod() {
        if (rgPaymentMethod.getCheckedRadioButtonId() == R.id.rb_vnpay) {
            return PaymentMethodEnum.VNPAY.getValue();
        }
        return PaymentMethodEnum.COD.getValue();
    }

    private void showOrderConfirmationDialog() {
        String phone = sessionManager.getUserPhone();
        String address = tvAddress.getText().toString().trim();
        if (phone == null || phone.isEmpty()) {
            ToastBanner.showError(getString(R.string.checkout_error_no_phone));
            return;
        }

        if (selectedAddress == null || address.isEmpty() || getString(R.string.checkout_payment_no_address).equals(address)) {
            ToastBanner.showError(getString(R.string.checkout_error_no_address));
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.checkout_confirm_title)
                .setMessage(R.string.checkout_confirm_message)
                .setPositiveButton(R.string.checkout_confirm_yes, (dialog, which) -> submitOrder())
                .setNegativeButton(R.string.checkout_confirm_no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void submitOrder() {
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
    }

    private void openVnpayBrowser(PaymentInitResult result) {
        if (result == null || result.getPaymentUrl().isEmpty()) {
            showErrorBanner(getString(R.string.payment_url_error));
            return;
        }

        sessionManager.setPendingPaymentOrderId(result.getOrderId());
        Log.d(TAG, "Step 5: Opening VNPAY browser for orderId=" + result.getOrderId()
                + ", txnRef=" + result.getProviderOrderRef());

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getPaymentUrl()));
        startActivity(browserIntent);
        finish();
    }

    private void showAddressSelectionDialog() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_address, null);
        RecyclerView rvAddresses = dialogView.findViewById(R.id.rv_addresses);
        TextView tvEmpty = dialogView.findViewById(R.id.tv_empty);
        Button btnAddAddress = dialogView.findViewById(R.id.btn_add_address);

        AddressSelectAdapter addressAdapter = new AddressSelectAdapter();
        rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        rvAddresses.setAdapter(addressAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        addressAdapter.setOnAddressSelectedListener(address -> {
            setSelectedAddress(address, true);
            dialog.dismiss();
        });

        AddressClient.getInstance().getAddressesByUserId(userId, new AddressClient.ApiCallback<Address[]>() {
            @Override
            public void onSuccess(Address[] result) {
                runOnUiThread(() -> {
                    if (result != null && result.length > 0) {
                        rvAddresses.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                        addressAdapter.setAddresses(java.util.Arrays.asList(result));
                        if (selectedAddress != null) {
                            addressAdapter.setSelectedAddressId(selectedAddress.getId());
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
                    tvEmpty.setText("Loi: " + error);
                });
            }
        });

        btnAddAddress.setOnClickListener(v -> {
            dialog.dismiss();
            startActivityForResult(new Intent(this, AddressFormActivity.class), REQUEST_ADD_ADDRESS);
        });

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_ADDRESS && resultCode == RESULT_OK) {
            loadDefaultAddress();
        }
    }
}
