package com.utt.foodcouriers_client.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.UserProfile;
import com.utt.foodcouriers_client.data.remote.AuthClient;
import com.utt.foodcouriers_client.data.repository.StorageRepository;
import com.utt.foodcouriers_client.databinding.ActivityEditProfileBinding;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;

public class EditProfileActivity extends BaseActivity {

    private static final String TAG = "EditProfileActivity";

    private ActivityEditProfileBinding binding;
    private SessionManager sessionManager;
    private UserProfile currentUser;
    private Uri selectedImageUri;
    private String currentAvatarUrl;
    private boolean isUploading = false;
    private final StorageRepository storageRepository = StorageRepository.getInstance();

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleImageSelected
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = SessionManager.getInstance(this);

        setupClickListeners();
        loadUserProfile();
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        binding.ivAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        
        if (binding.ivEditAvatar != null) {
            binding.ivEditAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadUserProfile() {
        showLoading(true);

        String userId = sessionManager.getUserId();
        if (userId == null) {
            showLoading(false);
            ToastBanner.showError(getString(R.string.error_title));
            finish();
            return;
        }

        AuthClient.getInstance().getUserById(userId, new AuthClient.ApiCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile result) {
                runOnUiThread(() -> {
                    showLoading(false);
                    currentUser = result;
                    bindUserData(result);
                    sessionManager.updateUserInfo(result);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    ToastBanner.showError(error);
                });
            }
        });
    }

    private void handleImageSelected(@Nullable Uri uri) {
        if (uri == null) return;
        
        selectedImageUri = uri;
        
        Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .centerCrop()
                .into(binding.ivAvatar);
        
        uploadSelectedAvatar();
    }

    private void bindUserData(UserProfile user) {
        binding.etName.setText(user.getFullName());
        binding.etPhone.setText(user.getPhone());
        binding.etEmail.setText(user.getEmail());

        currentAvatarUrl = user.getAvatarUrl();
        loadAvatarPreview(currentAvatarUrl);
    }

    private void loadAvatarPreview(@Nullable String url) {
        if (binding.ivAvatar == null) return;
        
        if (TextUtils.isEmpty(url)) {
            binding.ivAvatar.setImageResource(R.drawable.ic_profile);
            return;
        }
        
        Glide.with(EditProfileActivity.this)
                .load(url)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .centerCrop()
                .into(binding.ivAvatar);
    }

    private void uploadSelectedAvatar() {
        if (selectedImageUri == null) return;

        isUploading = true;
        setLoading(false);
        
        if (binding.progressAvatar != null) {
            binding.progressAvatar.setVisibility(View.VISIBLE);
        }
        
        ToastBanner.showWarning(getString(R.string.toast_uploading));

        storageRepository.uploadImage(this, selectedImageUri, "avatars", new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String imageUrl) {
                Log.d(TAG, "Upload success, imageUrl: " + imageUrl);
                runOnUiThread(() -> {
                    isUploading = false;
                    if (binding.progressAvatar != null) {
                        binding.progressAvatar.setVisibility(View.GONE);
                    }
                    
                    currentAvatarUrl = imageUrl;
                    loadAvatarPreview(currentAvatarUrl);
                    ToastBanner.showSuccess(getString(R.string.toast_upload_success));
                    setLoading(false);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Upload error: " + error);
                runOnUiThread(() -> {
                    isUploading = false;
                    if (binding.progressAvatar != null) {
                        binding.progressAvatar.setVisibility(View.GONE);
                    }
                    ToastBanner.showError(getString(R.string.toast_upload_failed, error));
                    setLoading(false);
                });
            }
        });
    }

    private void updateProfile(String name, String phone, String avatarUrl) {
        Log.d(TAG, "updateProfile called, avatarUrl: " + avatarUrl);
        
        AuthClient.getInstance().updateProfile(
                currentUser.getId(),
                name,
                phone,
                avatarUrl,
                new AuthClient.ApiCallback<UserProfile>() {
                    @Override
                    public void onSuccess(UserProfile result) {
                        Log.d(TAG, "Update profile success, returned avatarUrl: " + result.getAvatarUrl());
                        
                        AuthClient.getInstance().getUserById(currentUser.getId(), new AuthClient.ApiCallback<UserProfile>() {
                            @Override
                            public void onSuccess(UserProfile freshUser) {
                                Log.d(TAG, "Fetched fresh user, avatarUrl: " + freshUser.getAvatarUrl());
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    sessionManager.updateUserInfo(freshUser);
                                    ToastBanner.showSuccess(getString(R.string.profile_update_success));
                                    binding.getRoot().postDelayed(() -> {
                                        setResult(RESULT_OK);
                                        finish();
                                    }, 800);
                                });
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Failed to fetch fresh user: " + error);
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    sessionManager.updateUserInfo(result);
                                    ToastBanner.showSuccess(getString(R.string.profile_update_success));
                                    binding.getRoot().postDelayed(() -> {
                                        setResult(RESULT_OK);
                                        finish();
                                    }, 800);
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            binding.btnSave.setText(R.string.profile_save);
                            binding.btnSave.setEnabled(true);
                            ToastBanner.showError(error);
                        });
                    }
                }
        );
    }

    private void saveProfile() {
        String name = binding.etName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.etName.setError(getString(R.string.error_name_required));
            binding.etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            binding.etPhone.setError(getString(R.string.error_phone_required));
            binding.etPhone.requestFocus();
            return;
        }

        if (isUploading) {
            ToastBanner.showWarning(getString(R.string.toast_uploading));
            return;
        }

        showLoading(true);
        updateProfile(name, phone, currentAvatarUrl);
    }

    private void setLoading(boolean loading) {
        if (binding.btnSave != null) {
            binding.btnSave.setEnabled(!loading && !isUploading);
            String text = loading ? getString(R.string.profile_uploading) : getString(R.string.profile_save);
            binding.btnSave.setText(text);
        }
        if (binding.progressBar != null) {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void showLoading(boolean show) {
        setLoading(show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
