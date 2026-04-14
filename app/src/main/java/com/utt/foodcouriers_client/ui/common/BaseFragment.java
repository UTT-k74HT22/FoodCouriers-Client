package com.utt.foodcouriers_client.ui.common;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.utils.ToastBanner;

public abstract class BaseFragment extends Fragment {

    protected void showToast(String message) {
        ToastBanner.showSuccess(message);
    }

    protected void showError(String message) {
        ToastBanner.showError(message);
    }

    protected void showErrorSnackbar(String message) {
        View view = getView();
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(R.color.error, null))
                    .setTextColor(getResources().getColor(R.color.white, null))
                    .show();
        }
    }

    protected void setToolbarTitle(String title) {
        if (getActivity() != null && getActivity() instanceof com.utt.foodcouriers_client.ui.main.MainActivity) {
            ((com.utt.foodcouriers_client.ui.main.MainActivity) getActivity()).setToolbarTitle(title);
        }
    }
}
