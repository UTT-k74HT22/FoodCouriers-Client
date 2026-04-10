package com.utt.foodcouriers_client.utils;

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.utt.foodcouriers_client.R;

public class ToastBanner implements Application.ActivityLifecycleCallbacks {

    public enum ToastBannerType {
        SUCCESS,
        ERROR,
        WARNING
    }

    private static final int DURATION_MS = 3000;
    private static Activity currentActivity;
    private static ToastBanner instance;

    public static void init(Application app) {
        if (instance == null) {
            instance = new ToastBanner();
            app.registerActivityLifecycleCallbacks(instance);
        }
    }

    public static void showSuccess(String message) {
        show(message, ToastBannerType.SUCCESS);
    }

    public static void showError(String message) {
        show(message, ToastBannerType.ERROR);
    }

    public static void showWarning(String message) {
        show(message, ToastBannerType.WARNING);
    }

    private static void show(String message, ToastBannerType type) {
        if (currentActivity == null || message == null || message.isBlank()) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            int backgroundColor;
            switch (type) {
                case SUCCESS:
                    backgroundColor = currentActivity.getColor(R.color.success);
                    break;
                case ERROR:
                    backgroundColor = currentActivity.getColor(R.color.error);
                    break;
                case WARNING:
                    backgroundColor = currentActivity.getColor(R.color.warning);
                    break;
                default:
                    backgroundColor = currentActivity.getColor(R.color.primary);
            }
            showBannerView(currentActivity, message, backgroundColor);
        });
    }

    private static void showBannerView(Activity activity, String message, int backgroundColor) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        View rootView = activity.findViewById(android.R.id.content);
        if (!(rootView instanceof ViewGroup)) {
            return;
        }

        ViewGroup viewGroup = (ViewGroup) rootView;
        int bannerId = activity.getResources().getIdentifier("banner_container", "id", activity.getPackageName());
        if (bannerId == 0) {
            bannerId = 12346;
        }

        for (int index = 0; index < viewGroup.getChildCount(); index++) {
            View child = viewGroup.getChildAt(index);
            if (child.getId() == bannerId) {
                viewGroup.removeView(child);
                break;
            }
        }

        TextView banner = new TextView(activity);
        banner.setId(bannerId);
        banner.setText(message);
        banner.setTextColor(Color.WHITE);
        banner.setTextSize(14f);
        banner.setGravity(Gravity.CENTER);
        banner.setPadding(48, 28, 48, 28);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(18f);
        drawable.setColor(backgroundColor);
        banner.setBackground(drawable);

        ViewGroup.LayoutParams params;
        if (rootView instanceof CoordinatorLayout) {
            CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            layoutParams.topMargin = getStatusBarHeight(activity) + 16;
            params = layoutParams;
        } else {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            layoutParams.topMargin = getStatusBarHeight(activity) + 16;
            params = layoutParams;
        }

        viewGroup.addView(banner, params);
        banner.setAlpha(0f);
        banner.setTranslationY(-72f);
        banner.animate().alpha(1f).translationY(0f).setDuration(220).start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (banner.getParent() != null) {
                banner.animate()
                        .alpha(0f)
                        .translationY(-72f)
                        .setDuration(220)
                        .withEndAction(() -> viewGroup.removeView(banner))
                        .start();
            }
        }, DURATION_MS);
    }

    private static int getStatusBarHeight(Activity activity) {
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? activity.getResources().getDimensionPixelSize(resourceId) : 0;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}
