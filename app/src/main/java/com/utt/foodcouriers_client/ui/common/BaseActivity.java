package com.utt.foodcouriers_client.ui.common;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.utils.ToastBanner;

public abstract class BaseActivity extends AppCompatActivity {

    protected ProgressBar progressBar;

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

    protected void showLoading() {
        if (progressBar == null) {
            progressBar = findViewById(R.id.progress_bar);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    protected void hideLoading() {
        if (progressBar == null) {
            progressBar = findViewById(R.id.progress_bar);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void showErrorSnackbar(String message) {
        showErrorBanner(message);
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
