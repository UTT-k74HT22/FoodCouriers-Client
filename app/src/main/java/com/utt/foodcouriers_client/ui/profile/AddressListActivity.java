package com.utt.foodcouriers_client.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.Address;
import com.utt.foodcouriers_client.data.remote.AddressClient;
import com.utt.foodcouriers_client.databinding.ActivityAddressListBinding;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.profile.adapter.AddressAdapter;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;

public class AddressListActivity extends BaseActivity implements AddressAdapter.OnAddressClickListener {

    private ActivityAddressListBinding binding;
    private AddressAdapter adapter;
    private SessionManager sessionManager;
    private View emptyStateView;

    private static final int REQUEST_ADD_ADDRESS = 100;
    private static final int REQUEST_EDIT_ADDRESS = 101;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityAddressListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = SessionManager.getInstance(this);

        setupClickListeners();
        setupRecyclerView();
        setupFab();
        setupEmptyState();
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddresses();
    }

    private void setupRecyclerView() {
        adapter = new AddressAdapter();
        adapter.setOnAddressClickListener(this);
        
        binding.rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAddresses.setAdapter(adapter);
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressFormActivity.class);
            startActivityForResult(intent, REQUEST_ADD_ADDRESS);
        });
    }

    private void setupEmptyState() {
        emptyStateView = findViewById(R.id.empty_state);
        if (emptyStateView != null) {
            TextView tvTitle = emptyStateView.findViewById(R.id.tv_empty_title);
            TextView tvMessage = emptyStateView.findViewById(R.id.tv_empty_message);
            if (tvTitle != null) tvTitle.setText(R.string.address_empty_title);
            if (tvMessage != null) tvMessage.setText(R.string.address_empty_message);
        }
    }

    private void loadAddresses() {
        showLoading(true);

        String userId = sessionManager.getUserId();
        if (userId == null) {
            showLoading(false);
            showEmptyState(true);
            return;
        }

        AddressClient.getInstance().getAddressesByUserId(userId, new AddressClient.ApiCallback<Address[]>() {
            @Override
            public void onSuccess(Address[] result) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (result != null && result.length > 0) {
                        adapter.setAddresses(java.util.Arrays.asList(result));
                        showEmptyState(false);
                    } else {
                        showEmptyState(true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showEmptyState(true);
                    ToastBanner.showError(error);
                });
            }
        });
    }

    private void showEmptyState(boolean show) {
        if (emptyStateView != null) {
            emptyStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        binding.rvAddresses.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean show) {
        if (show) {
            binding.rvAddresses.setVisibility(View.GONE);
            if (emptyStateView != null) {
                emptyStateView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onSetDefault(Address address) {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        AddressClient.getInstance().setDefaultAddress(userId, address.getId(), new AddressClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    ToastBanner.showSuccess(getString(R.string.address_update_success));
                    binding.getRoot().postDelayed(() -> loadAddresses(), 800);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> ToastBanner.showError(error));
            }
        });
    }

    @Override
    public void onEdit(Address address) {
        Intent intent = new Intent(this, AddressFormActivity.class);
        intent.putExtra("address", address);
        startActivityForResult(intent, REQUEST_EDIT_ADDRESS);
    }

    @Override
    public void onDelete(Address address) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.address_delete_confirm)
                .setMessage(R.string.address_delete_message)
                .setPositiveButton(R.string.address_delete, (dialog, which) -> deleteAddress(address))
                .setNegativeButton(R.string.address_cancel, null)
                .show();
    }

    private void deleteAddress(Address address) {
        AddressClient.getInstance().deleteAddress(address.getId(), new AddressClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    ToastBanner.showSuccess(getString(R.string.address_delete_success));
                    binding.getRoot().postDelayed(() -> loadAddresses(), 800);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> ToastBanner.showError(error));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadAddresses();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
