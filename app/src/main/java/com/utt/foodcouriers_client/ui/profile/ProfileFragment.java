package com.utt.foodcouriers_client.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.remote.AuthClient;
import com.utt.foodcouriers_client.databinding.FragmentProfileBinding;
import com.utt.foodcouriers_client.ui.auth.LoginActivity;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.SessionStore;

public class ProfileFragment extends BaseFragment {

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
        setupLogoutButton();
    }

    private void loadUserInfo() {
        if (sessionManager.isLoggedIn()) {
            binding.tvUserName.setText(sessionManager.getUserName());
            binding.tvUserEmail.setText(sessionManager.getUserEmail());
        }
    }

    private void setupLogoutButton() {
        binding.btnLogout.setOnClickListener(v -> {
            AuthClient.getInstance().signOut(new AuthClient.ApiCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    sessionManager.clearSession();
                    SessionStore.clearSession(requireContext());
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), R.string.auth_login_success, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(requireContext(), LoginActivity.class));
                        requireActivity().finish();
                    });
                }

                @Override
                public void onError(String error) {
                    sessionManager.clearSession();
                    SessionStore.clearSession(requireContext());
                    requireActivity().runOnUiThread(() -> {
                        startActivity(new Intent(requireContext(), LoginActivity.class));
                        requireActivity().finish();
                    });
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
