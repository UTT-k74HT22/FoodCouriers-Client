package com.utt.foodcouriers_client.ui.common;

import androidx.fragment.app.Fragment;

import com.utt.foodcouriers_client.utils.ToastBanner;

public abstract class BaseFragment extends Fragment {

    protected void showToast(String message) {
        ToastBanner.showSuccess(message);
    }

    protected void showError(String message) {
        ToastBanner.showError(message);
    }
}
