package com.utt.foodcouriers_client.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.repository.CartRepository;
import com.utt.foodcouriers_client.databinding.ActivityMainBinding;
import com.utt.foodcouriers_client.ui.auth.LoginActivity;
import com.utt.foodcouriers_client.ui.cart.CartFragment;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.discover.DiscoverFragment;
import com.utt.foodcouriers_client.ui.home.HomeFragment;
import com.utt.foodcouriers_client.ui.notification.NotificationsFragment;
import com.utt.foodcouriers_client.ui.order.OrdersFragment;
import com.utt.foodcouriers_client.ui.profile.ProfileFragment;
import com.utt.foodcouriers_client.utils.SessionManager;

public class MainActivity extends BaseActivity {

    public static final String EXTRA_OPEN_CART = "open_cart";
    private ActivityMainBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sessionManager = SessionManager.getInstance(this);

        applyTopWindowInset(binding.topToolbar);
        applyBottomWindowInset(binding.bottomNavigation);
        setSupportActionBar(binding.topToolbar);
        binding.topToolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getSupportFragmentManager().addOnBackStackChangedListener(this::syncChrome);

        binding.btnCart.setOnClickListener(v -> openCartScreen());

        binding.btnNotification.setOnClickListener(v ->
                openSecondaryFragment(new NotificationsFragment(), getString(R.string.notifications_title)));

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                showPrimaryFragment(new HomeFragment(), getString(R.string.nav_home));
                return true;
            }
            if (item.getItemId() == R.id.navigation_discover) {
                showPrimaryFragment(new DiscoverFragment(), getString(R.string.nav_discover));
                return true;
            }
            if (item.getItemId() == R.id.navigation_orders) {
                showPrimaryFragment(new OrdersFragment(), getString(R.string.nav_orders));
                return true;
            }
            if (item.getItemId() == R.id.navigation_profile) {
                showPrimaryFragment(new ProfileFragment(), getString(R.string.nav_profile));
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            binding.bottomNavigation.setSelectedItemId(R.id.navigation_home);
            showPrimaryFragment(new HomeFragment(), getString(R.string.nav_home));
            if (getIntent().getBooleanExtra(EXTRA_OPEN_CART, false)) {
                openCartScreen();
            }
        } else {
            syncChrome();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartBadge();
    }

    private void loadCartBadge() {
        if (!sessionManager.isLoggedIn()) {
            binding.tvCartBadge.setVisibility(View.GONE);
            return;
        }
        CartRepository.getInstance().getCart(this, new RepositoryCallback<CartRepository.CartState>() {
            @Override
            public void onSuccess(CartRepository.CartState result) {
                updateCartBadge(result.getSummary().getItemCount());
            }

            @Override
            public void onError(String error) {
                binding.tvCartBadge.setVisibility(View.GONE);
            }
        });
    }

    private void updateCartBadge(int count) {
        if (count > 0) {
            binding.tvCartBadge.setVisibility(View.VISIBLE);
            binding.tvCartBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            binding.tvCartBadge.setVisibility(View.GONE);
        }
    }

    public void openCartScreen() {
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        openSecondaryFragment(new CartFragment(), getString(R.string.cart_title));
    }

    public void openSecondaryFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(title)
                .commit();
        binding.topToolbar.setTitle(title);
        syncChrome();
    }

    private void showPrimaryFragment(Fragment fragment, String title) {
        getSupportFragmentManager().popBackStack();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        binding.topToolbar.setTitle(title);
        syncChrome();
    }

    private void syncChrome() {
        boolean isSecondaryScreen = getSupportFragmentManager().getBackStackEntryCount() > 0;
        binding.bottomNavigation.setVisibility(isSecondaryScreen ? View.GONE : View.VISIBLE);
        if (isSecondaryScreen) {
            binding.topToolbar.setNavigationIcon(R.drawable.ic_back);
        } else {
            binding.topToolbar.setNavigationIcon(null);
            binding.topToolbar.setTitle(resolvePrimaryTitle());
        }
    }

    private String resolvePrimaryTitle() {
        int selectedItemId = binding.bottomNavigation.getSelectedItemId();
        if (selectedItemId == R.id.navigation_discover) {
            return getString(R.string.nav_discover);
        }
        if (selectedItemId == R.id.navigation_orders) {
            return getString(R.string.nav_orders);
        }
        if (selectedItemId == R.id.navigation_profile) {
            return getString(R.string.nav_profile);
        }
        return getString(R.string.nav_home);
    }
}
