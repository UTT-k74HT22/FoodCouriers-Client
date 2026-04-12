package com.utt.foodcouriers_client.ui.common;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.utils.ToastBanner;

public abstract class BaseActivity extends AppCompatActivity {

    protected ProgressBar progressBar;

    protected void configureToolbar(@Nullable Toolbar toolbar, boolean showBack) {
        if (toolbar == null) {
            return;
        }
        applyTopWindowInset(toolbar);
        setSupportActionBar(toolbar);
        if (showBack) {
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
            toolbar.setNavigationContentDescription(R.string.toolbar_back);
        }
    }

    protected void setToolbarTitle(@Nullable String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
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

    protected void applyTopWindowInset(@Nullable View view) {
        if (view == null) {
            return;
        }
        final int initialPaddingLeft = view.getPaddingLeft();
        final int initialPaddingTop = view.getPaddingTop();
        final int initialPaddingRight = view.getPaddingRight();
        final int initialPaddingBottom = view.getPaddingBottom();
        final ViewGroup.LayoutParams initialLayoutParams = view.getLayoutParams();
        final int initialHeight = initialLayoutParams != null ? initialLayoutParams.height : ViewGroup.LayoutParams.WRAP_CONTENT;

        ViewCompat.setOnApplyWindowInsetsListener(view, (target, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            target.setPadding(
                    initialPaddingLeft,
                    initialPaddingTop + systemBars.top,
                    initialPaddingRight,
                    initialPaddingBottom
            );

            ViewGroup.LayoutParams layoutParams = target.getLayoutParams();
            if (layoutParams != null && initialHeight > 0) {
                layoutParams.height = initialHeight + systemBars.top;
                target.setLayoutParams(layoutParams);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    protected void applyBottomWindowInset(@Nullable View view) {
        if (view == null) {
            return;
        }
        final int initialPaddingLeft = view.getPaddingLeft();
        final int initialPaddingTop = view.getPaddingTop();
        final int initialPaddingRight = view.getPaddingRight();
        final int initialPaddingBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (target, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            target.setPadding(
                    initialPaddingLeft,
                    initialPaddingTop,
                    initialPaddingRight,
                    initialPaddingBottom + systemBars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
