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
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.Address;
import com.utt.foodcouriers_client.data.remote.AddressClient;
import com.utt.foodcouriers_client.data.remote.AuthClient;
import com.utt.foodcouriers_client.databinding.FragmentProfileBinding;
import com.utt.foodcouriers_client.ui.auth.LoginActivity;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;

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
            startActivity(new Intent(requireContext(), EditProfileActivity.class));
        });

        binding.ivAvatar.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), EditProfileActivity.class));
        });

        binding.btnManageAddresses.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddressListActivity.class));
        });

        binding.btnNotifications.setOnClickListener(v -> {
            ToastBanner.showSuccess(getString(R.string.notifications_title));
        });
    }

    private void setupLogoutButton() {
        binding.btnLogout.setOnClickListener(v -> {
            AuthClient.getInstance().signOut(new AuthClient.ApiCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    sessionManager.clearSession();
                    requireActivity().runOnUiThread(() -> {
                        ToastBanner.showSuccess(getString(R.string.auth_login_success));
                        startActivity(new Intent(requireContext(), LoginActivity.class));
                        requireActivity().finish();
                    });
                }

                @Override
                public void onError(String error) {
                    sessionManager.clearSession();
                    requireActivity().runOnUiThread(() -> {
                        startActivity(new Intent(requireContext(), LoginActivity.class));
                        requireActivity().finish();
                    });
                }
            });
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
