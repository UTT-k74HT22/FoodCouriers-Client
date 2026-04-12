package com.utt.foodcouriers_client.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.databinding.FragmentCartBinding;
import com.utt.foodcouriers_client.ui.auth.LoginActivity;
import com.utt.foodcouriers_client.ui.cart.adapter.CartItemAdapter;
import com.utt.foodcouriers_client.ui.checkout.CheckoutActivity;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.viewmodel.CartViewModel;

import java.text.NumberFormat;
import java.util.Locale;

public class CartFragment extends BaseFragment {

    private FragmentCartBinding binding;
    private CartViewModel viewModel;
    private CartItemAdapter adapter;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CartViewModel.class);
        if (!viewModel.isLoggedIn(requireContext())) {
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }
        setupRecyclerView();
        setupActions();
        observeViewModel();
        viewModel.loadCart(requireContext());
    }

    private void setupRecyclerView() {
        adapter = new CartItemAdapter(requireContext(), new CartItemAdapter.CartItemListener() {
            @Override
            public void onIncrease(String cartItemId) {
                int currentQuantity = adapter.getQuantityForItem(cartItemId);
                viewModel.updateCartItemQuantity(requireContext(), cartItemId, currentQuantity + 1);
            }

            @Override
            public void onDecrease(String cartItemId) {
                int currentQuantity = adapter.getQuantityForItem(cartItemId);
                viewModel.updateCartItemQuantity(requireContext(), cartItemId, currentQuantity - 1);
            }
        });

        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCartItems.setNestedScrollingEnabled(false);
        binding.rvCartItems.setAdapter(adapter);
    }

    private void setupActions() {
        binding.btnCheckout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CheckoutActivity.class);
            startActivity(intent);
        });

        binding.btnExploreMenus.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void observeViewModel() {
        viewModel.getCartItems().observe(getViewLifecycleOwner(), items -> adapter.submitList(items));

        viewModel.getCartSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary == null) {
                return;
            }

            binding.tvCartCount.setText(getString(R.string.cart_item_count, summary.getItemCount()));
            binding.tvEtaValue.setText(summary.getSubtotal() >= 200_000
                    ? getString(R.string.cart_free_delivery)
                    : getString(R.string.cart_delivery_window));
            binding.tvRestaurantValue.setText(summary.getRestaurantName().isEmpty()
                    ? getString(R.string.cart_restaurant_fallback)
                    : summary.getRestaurantName());
            binding.tvSubtotalValue.setText(currencyFormatter.format(summary.getSubtotal()));
            binding.tvDeliveryValue.setText(currencyFormatter.format(summary.getDeliveryFee()));
            binding.tvServiceValue.setText(currencyFormatter.format(summary.getServiceFee()));
            binding.tvSavingsValue.setText(currencyFormatter.format(summary.getSavings()));
            binding.tvTotalPrice.setText(currencyFormatter.format(summary.getTotal()));
        });

        viewModel.getEmptyState().observe(getViewLifecycleOwner(), isEmpty -> {
            boolean empty = Boolean.TRUE.equals(isEmpty);
            binding.layoutCartContent.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.cardCheckout.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.emptyState.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.btnExploreMenus.setVisibility(empty ? View.VISIBLE : View.GONE);

            if (empty) {
                binding.emptyState.tvEmptyTitle.setText(R.string.cart_empty_title);
                binding.emptyState.tvEmptyMessage.setText(R.string.cart_empty_message);
                binding.tvTitle.setText(R.string.cart_title);
                binding.tvSubtitle.setText(R.string.cart_empty_header_subtitle);
            } else {
                binding.tvTitle.setText(R.string.cart_header_title);
                binding.tvSubtitle.setText(R.string.cart_header_subtitle);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
