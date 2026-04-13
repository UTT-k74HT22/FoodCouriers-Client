package com.utt.foodcouriers_client.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.Address;
import com.utt.foodcouriers_client.data.remote.AddressClient;
import com.utt.foodcouriers_client.databinding.ActivityAddressFormBinding;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;

public class AddressFormActivity extends BaseActivity {

    private ActivityAddressFormBinding binding;
    private SessionManager sessionManager;
    private Address editingAddress;
    private boolean isEditMode = false;
    private int selectedLabelPosition = 0;

    private final String[] labelOptions = {"Nhà", "Công ty", "Khác"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddressFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = SessionManager.getInstance(this);

        setupLabelSpinner();
        setupClickListeners();
        loadExistingAddress();
    }

    private void setupLabelSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                labelOptions
        );
        binding.spinnerLabel.setAdapter(adapter);
        binding.spinnerLabel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLabelPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        binding.btnCancel.setOnClickListener(v -> finish());

        binding.btnSave.setOnClickListener(v -> saveAddress());
    }

    private void loadExistingAddress() {
        if (getIntent().hasExtra("address")) {
            editingAddress = (Address) getIntent().getSerializableExtra("address");
            if (editingAddress != null) {
                isEditMode = true;
                binding.tvTitle.setText(R.string.address_edit_title);
                bindAddressData(editingAddress);
            }
        }
    }

    private void bindAddressData(Address address) {
        for (int i = 0; i < labelOptions.length; i++) {
            if (labelOptions[i].equals(address.getLabel())) {
                binding.spinnerLabel.setSelection(i);
                break;
            }
        }
        binding.etAddress.setText(address.getFullAddress());
        binding.etDistrict.setText(address.getDistrict());
        binding.etCity.setText(address.getCity());
        binding.cbDefault.setChecked(address.isDefault());
    }

    private void saveAddress() {
        String label = labelOptions[selectedLabelPosition];
        String fullAddress = binding.etAddress.getText().toString().trim();
        String district = binding.etDistrict.getText().toString().trim();
        String city = binding.etCity.getText().toString().trim();
        boolean isDefault = binding.cbDefault.isChecked();

        if (TextUtils.isEmpty(fullAddress)) {
            binding.etAddress.setHint(getString(R.string.auth_validation_required));
            binding.etAddress.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(city)) {
            city = "HCM";
        }

        showLoading(true);

        String userId = sessionManager.getUserId();
        if (userId == null) {
            showLoading(false);
            ToastBanner.showError(getString(R.string.error_title));
            return;
        }

        if (isEditMode && editingAddress != null) {
            AddressClient.getInstance().updateAddress(
                    editingAddress.getId(),
                    label, fullAddress, district, city, isDefault,
                    new AddressClient.ApiCallback<Address>() {
                        @Override
                        public void onSuccess(Address result) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                ToastBanner.showSuccess(getString(R.string.address_update_success));
                                binding.getRoot().postDelayed(() -> {
                                    setResult(RESULT_OK);
                                    finish();
                                }, 800);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                ToastBanner.showError(error);
                            });
                        }
                    }
            );
        } else {
            AddressClient.getInstance().createAddress(
                    userId, label, fullAddress, district, city, isDefault,
                    new AddressClient.ApiCallback<Address>() {
                        @Override
                        public void onSuccess(Address result) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                ToastBanner.showSuccess(getString(R.string.address_create_success));
                                binding.getRoot().postDelayed(() -> {
                                    setResult(RESULT_OK);
                                    finish();
                                }, 800);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                ToastBanner.showError(error);
                            });
                        }
                    }
            );
        }
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
