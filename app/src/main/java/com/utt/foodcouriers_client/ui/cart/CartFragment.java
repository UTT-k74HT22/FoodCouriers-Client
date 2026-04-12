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
import com.utt.foodcouriers_client.utils.ToastBanner;
import com.utt.foodcouriers_client.viewmodel.CartViewModel;

import java.text.NumberFormat;
import java.util.Locale;

public class CartFragment extends BaseFragment {

    public static final String EXTRA_SELECTED_CART_ITEM_IDS = "selected_cart_item_ids";
    private FragmentCartBinding binding;
    private CartViewModel viewModel;
    private CartItemAdapter adapter;
    private CartItemAdapter.SelectionState currentSelection = new CartItemAdapter.SelectionState(new java.util.ArrayList<>(), 0, 0, 0, 0, 0);
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

            @Override
            public void onSelectionChanged(CartItemAdapter.SelectionState selectionState) {
                currentSelection = selectionState;
                renderSelectionSummary(selectionState);
            }
        });

        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCartItems.setNestedScrollingEnabled(false);
        binding.rvCartItems.setAdapter(adapter);
    }

    private void setupActions() {
        binding.btnCheckout.setOnClickListener(v -> {
            if (currentSelection.getSelectedCartItemIds().isEmpty()) {
                showErrorSnackbar("Hay chon it nhat mot mon de tiep tuc.");
                return;
            }
            Intent intent = new Intent(requireContext(), CheckoutActivity.class);
            intent.putStringArrayListExtra(EXTRA_SELECTED_CART_ITEM_IDS, new java.util.ArrayList<>(currentSelection.getSelectedCartItemIds()));
            startActivity(intent);
        });

        binding.btnExploreMenus.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void observeViewModel() {
        viewModel.getRestaurantGroups().observe(getViewLifecycleOwner(), groups -> adapter.submitGroups(groups));

        viewModel.getCartSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary == null) {
                return;
            }

            binding.tvCartCount.setText(getString(R.string.cart_item_count, summary.getItemCount()));
            binding.tvEtaValue.setText(getString(R.string.cart_grouped_eta_hint));
            binding.tvSubtotalValue.setText(currencyFormatter.format(summary.getSubtotal()));
            binding.tvDeliveryValue.setText(currencyFormatter.format(summary.getDeliveryFee()));
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
                binding.tvSubtitle.setText(R.string.cart_grouped_subtitle);
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.isEmpty()) {
                return;
            }
            ToastBanner.showSuccess(message);
        });
    }

    private void renderSelectionSummary(CartItemAdapter.SelectionState selectionState) {
        binding.tvRestaurantValue.setText(getString(
                R.string.cart_restaurant_selected_count,
                selectionState.getSelectedRestaurantCount()
        ));
        binding.tvSubtotalValue.setText(currencyFormatter.format(selectionState.getSubtotal()));
        binding.tvDeliveryValue.setText(currencyFormatter.format(selectionState.getDeliveryFee()));
        binding.tvTotalPrice.setText(currencyFormatter.format(selectionState.getTotal()));
        binding.btnCheckout.setEnabled(!selectionState.getSelectedCartItemIds().isEmpty());
        binding.btnCheckout.setAlpha(selectionState.getSelectedCartItemIds().isEmpty() ? 0.5f : 1f);
        binding.tvCartCount.setText(getString(R.string.cart_item_count, selectionState.getSelectedItemCount()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
