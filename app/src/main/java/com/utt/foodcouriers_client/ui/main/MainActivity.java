package com.utt.foodcouriers_client.ui.main;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.databinding.ActivityMainBinding;
import com.utt.foodcouriers_client.ui.cart.CartFragment;
import com.utt.foodcouriers_client.ui.common.BaseActivity;
import com.utt.foodcouriers_client.ui.home.HomeFragment;
import com.utt.foodcouriers_client.ui.notification.NotificationsFragment;
import com.utt.foodcouriers_client.ui.order.OrdersFragment;
import com.utt.foodcouriers_client.ui.profile.ProfileFragment;
import com.utt.foodcouriers_client.ui.search.SearchFragment;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.topToolbar);
        binding.topToolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getSupportFragmentManager().addOnBackStackChangedListener(this::syncChrome);

        binding.btnCart.setOnClickListener(v ->
                openSecondaryFragment(new CartFragment(), getString(R.string.cart_title)));

        binding.btnNotification.setOnClickListener(v ->
                openSecondaryFragment(new NotificationsFragment(), getString(R.string.notifications_title)));

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                showPrimaryFragment(new HomeFragment(), getString(R.string.nav_home));
                return true;
            }
            if (item.getItemId() == R.id.navigation_search) {
                showPrimaryFragment(new SearchFragment(), getString(R.string.nav_search));
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
        } else {
            syncChrome();
        }
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
            binding.topToolbar.setTitle(getString(R.string.nav_home));
        }
    }
}