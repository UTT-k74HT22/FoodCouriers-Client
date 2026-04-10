package com.utt.foodcouriers_client.ui.common;

import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.utils.ToastBanner;

public abstract class BaseActivity extends AppCompatActivity {

    protected void configureToolbar(@Nullable Toolbar toolbar, boolean showBack) {
        if (toolbar == null) {
            return;
        }
        setSupportActionBar(toolbar);
        if (showBack) {
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
            toolbar.setNavigationContentDescription(R.string.toolbar_back);
        }
    }

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void showSuccessBanner(String message) {
        ToastBanner.showSuccess(message);
    }

    protected void showErrorBanner(String message) {
        ToastBanner.showError(message);
    }

    protected void showWarningBanner(String message) {
        ToastBanner.showWarning(message);
    }
}
