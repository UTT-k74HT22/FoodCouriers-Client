package com.utt.foodcouriers_client.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.utt.foodcouriers_client.FoodCouriersClientApp;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.Address;
import com.utt.foodcouriers_client.data.remote.AddressClient;
import com.utt.foodcouriers_client.data.remote.AuthClient;
import com.utt.foodcouriers_client.databinding.FragmentProfileBinding;
import com.utt.foodcouriers_client.ui.auth.LoginActivity;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;
import com.utt.foodcouriers_client.utils.SessionStore;

public class ProfileFragment extends BaseFragment {

    private static final String TAG = "ProfileFragment";

    private FragmentProfileBinding binding;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = SessionManager.getInstance(requireContext());
        
        loadUserInfo();
        loadAddressCount();
        setupClickListeners();
        setupLogoutButton();
    }

    private void loadUserInfo() {
        if (sessionManager.isLoggedIn()) {
            binding.tvUserName.setText(sessionManager.getUserName());
            binding.tvUserEmail.setText(sessionManager.getUserEmail());

            String phone = sessionManager.getUserPhone();
            if (phone != null && !phone.isEmpty()) {
                binding.tvUserPhone.setText(phone);
                binding.tvUserPhone.setVisibility(View.VISIBLE);
            } else {
                binding.tvUserPhone.setText(R.string.profile_no_phone);
                binding.tvUserPhone.setVisibility(View.VISIBLE);
            }

            String avatarUrl = sessionManager.getUserAvatar();
            Log.d(TAG, "Avatar URL from session: " + avatarUrl);

            if (!TextUtils.isEmpty(avatarUrl)) {
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .centerCrop()
                        .into(binding.ivAvatar);
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_profile);
            }
        } else {
            binding.tvUserName.setText("Người dùng ẩn danh");
            binding.tvUserEmail.setText("Đăng nhập để trải nghiệm đầy đủ");
            binding.tvUserPhone.setVisibility(View.GONE);
            binding.ivAvatar.setImageResource(R.drawable.ic_profile);
        }
        updateAuthUI();
    }

    private void updateAuthUI() {
        if (sessionManager.isLoggedIn()) {
            binding.tvLogoutText.setText(R.string.profile_logout);
            binding.tvLogoutText.setTextColor(getResources().getColor(R.color.error));
            binding.ivLogoutIcon.setImageResource(R.drawable.ic_lock);
            binding.ivLogoutIcon.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.error)));
        } else {
            binding.tvLogoutText.setText(R.string.login_cta);
            binding.tvLogoutText.setTextColor(getResources().getColor(R.color.primary));
            binding.ivLogoutIcon.setImageResource(R.drawable.ic_person);
            binding.ivLogoutIcon.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
        }
    }

    private void loadAddressCount() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            binding.tvAddressCount.setText("0");
            return;
        }

        AddressClient.getInstance().getAddressesByUserId(userId, new AddressClient.ApiCallback<Address[]>() {
            @Override
            public void onSuccess(Address[] result) {
                if (result != null) {
                    requireActivity().runOnUiThread(() ->
                        binding.tvAddressCount.setText(String.valueOf(result.length))
                    );
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load addresses: " + error);
                requireActivity().runOnUiThread(() ->
                    binding.tvAddressCount.setText("0")
                );
            }
        });
    }

    private void setupClickListeners() {
        binding.layoutProfileHeader.setOnClickListener(v -> {
            if (sessionManager.isLoggedIn()) {
                startActivity(new Intent(requireContext(), EditProfileActivity.class));
            } else {
                startActivity(new Intent(requireContext(), LoginActivity.class));
            }
        });

        binding.ivAvatar.setOnClickListener(v -> {
            if (sessionManager.isLoggedIn()) {
                startActivity(new Intent(requireContext(), EditProfileActivity.class));
            } else {
                startActivity(new Intent(requireContext(), LoginActivity.class));
            }
        });

        binding.btnManageAddresses.setOnClickListener(v -> {
            if (sessionManager.isLoggedIn()) {
                startActivity(new Intent(requireContext(), AddressListActivity.class));
            } else {
                ToastBanner.showWarning("Vui lòng đăng nhập để quản lý địa chỉ");
                startActivity(new Intent(requireContext(), LoginActivity.class));
            }
        });

        binding.btnNotifications.setOnClickListener(v -> {
            if (sessionManager.isLoggedIn()) {
                ToastBanner.showSuccess(getString(R.string.notifications_title));
            } else {
                ToastBanner.showWarning("Vui lòng đăng nhập để xem thông báo");
                startActivity(new Intent(requireContext(), LoginActivity.class));
            }
        });
    }

    private void setupLogoutButton() {
        binding.btnLogout.setOnClickListener(v -> {
            if (sessionManager.isLoggedIn()) {
                AuthClient.getInstance().signOut(new AuthClient.ApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        sessionManager.clearSession();
                        SessionStore.clearSession(requireContext());
                        FoodCouriersClientApp.disconnectRealtime();
                        requireActivity().runOnUiThread(() -> {
                            ToastBanner.showSuccess("Đã đăng xuất");
                            loadUserInfo(); // Refresh UI to guest state
                        });
                    }

                    @Override
                    public void onError(String error) {
                        sessionManager.clearSession();
                        SessionStore.clearSession(requireContext());
                        FoodCouriersClientApp.disconnectRealtime();
                        requireActivity().runOnUiThread(() -> {
                            loadUserInfo();
                        });
                    }
                });
            } else {
                startActivity(new Intent(requireContext(), LoginActivity.class));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserInfo();
        loadAddressCount();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
