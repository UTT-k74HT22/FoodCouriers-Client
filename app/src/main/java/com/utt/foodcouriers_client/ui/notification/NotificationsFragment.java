package com.utt.foodcouriers_client.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.databinding.FragmentNotificationsBinding;
import com.utt.foodcouriers_client.ui.common.BaseFragment;

public class NotificationsFragment extends BaseFragment {

    private FragmentNotificationsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity().getActionBar() != null) {
            getActivity().getActionBar().setTitle(getString(R.string.notifications_title));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
