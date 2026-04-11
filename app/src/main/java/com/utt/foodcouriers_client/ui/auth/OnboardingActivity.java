package com.utt.foodcouriers_client.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.card.MaterialCardView;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.main.MainActivity;
import com.utt.foodcouriers_client.utils.SessionManager;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends BaseActivity {
    private static final String PREFS_NAME = "foodcouriers_prefs";
    private static final String KEY_ONBOARDING_SEEN = "onboarding_seen";

    private ViewPager2 viewPager;
    private MaterialCardView indicatorOne;
    private MaterialCardView indicatorTwo;
    private TextView skipAction;
    private Button primaryButton;
    private Button secondaryButton;
    private List<OnboardingPage> pages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn() && !sessionManager.isTokenExpired()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        if (hasSeenOnboarding()) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_onboarding);
        bindViews();
        setUpPages();
        setUpPager();
        bindActions();
        updateFooter(0);
    }

    private void bindViews() {
        viewPager = findViewById(R.id.viewPagerOnboarding);
        indicatorOne = findViewById(R.id.indicatorOne);
        indicatorTwo = findViewById(R.id.indicatorTwo);
        skipAction = findViewById(R.id.tvSkip);
        primaryButton = findViewById(R.id.btnPrimaryAction);
        secondaryButton = findViewById(R.id.btnSecondaryAction);
    }

    private void setUpPages() {
        pages = Arrays.asList(
                new OnboardingPage(
                        R.string.onboarding_page_one_eyebrow,
                        R.string.onboarding_page_one_title,
                        R.string.onboarding_page_one_subtitle,
                        R.string.onboarding_page_one_chip_one,
                        R.string.onboarding_page_one_chip_two,
                        R.string.onboarding_page_one_chip_three,
                        R.drawable.ic_delivery_bag,
                        R.drawable.ic_food_bowl,
                        R.drawable.ic_burger_line
                ),
                new OnboardingPage(
                        R.string.onboarding_page_two_eyebrow,
                        R.string.onboarding_page_two_title,
                        R.string.onboarding_page_two_subtitle,
                        R.string.onboarding_page_two_chip_one,
                        R.string.onboarding_page_two_chip_two,
                        R.string.onboarding_page_two_chip_three,
                        R.drawable.ic_route_path,
                        R.drawable.ic_fresh_leaf,
                        R.drawable.ic_receipt
                )
        );
    }

    private void setUpPager() {
        viewPager.setAdapter(new OnboardingPagerAdapter(pages));
        viewPager.setPageTransformer((page, position) -> {
            float abs = Math.abs(position);
            page.setAlpha(1f - Math.min(abs * 0.25f, 0.25f));
            page.setTranslationX(-position * 48f);
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateFooter(position);
            }
        });
    }

    private void bindActions() {
        skipAction.setOnClickListener(v -> openLogin());
        primaryButton.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < pages.size() - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                openLogin();
            }
        });
        secondaryButton.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() == 0) {
                openLogin();
            } else {
                openRegister();
            }
        });
    }

    private void updateFooter(int position) {
        setIndicatorState(indicatorOne, position == 0);
        setIndicatorState(indicatorTwo, position == 1);
        if (position == 0) {
            primaryButton.setText(R.string.onboarding_continue);
            secondaryButton.setText(R.string.onboarding_sign_in);
            skipAction.setVisibility(View.VISIBLE);
        } else {
            primaryButton.setText(R.string.onboarding_start);
            secondaryButton.setText(R.string.onboarding_create_account);
            skipAction.setVisibility(View.GONE);
        }
    }

    private void setIndicatorState(@NonNull MaterialCardView indicator, boolean active) {
        indicator.setCardBackgroundColor(getColor(active ? R.color.primary : R.color.auth_indicator_inactive));
    }

    private boolean hasSeenOnboarding() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_ONBOARDING_SEEN, false);
    }

    private void markOnboardingSeen() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply();
    }

    private void openLogin() {
        markOnboardingSeen();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void openRegister() {
        markOnboardingSeen();
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }
}
