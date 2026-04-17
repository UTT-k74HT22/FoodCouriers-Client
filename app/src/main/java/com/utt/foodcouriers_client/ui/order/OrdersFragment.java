package com.utt.foodcouriers_client.ui.order;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.JsonObject;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.remote.SupabaseRealtimeClient;
import com.utt.foodcouriers_client.data.repository.OrderRepository;
import com.utt.foodcouriers_client.databinding.FragmentOrdersBinding;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.ui.order.adapter.OrderHistoryAdapter;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.websocket.RealtimeChannel;
import com.utt.foodcouriers_client.utils.websocket.RealtimeListener;
import com.utt.foodcouriers_client.viewmodel.OrdersViewModel;

public class OrdersFragment extends BaseFragment {

    private static final String TAG_REAL_TIME = "OrdersRealtime";

    private FragmentOrdersBinding binding;
    private OrdersViewModel viewModel;
    private OrderHistoryAdapter adapter;
    private RealtimeChannel ordersChannel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(OrdersViewModel.class);
        setupRecyclerView();
        setupTabs();
        bindObservers();
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh(requireContext()));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.loadOrders(requireContext(), OrderRepository.OrderFilter.ALL);
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeToOrderUpdates();
    }

    @Override
    public void onStop() {
        unsubscribeFromOrderUpdates();
        super.onStop();
    }

    private void setupRecyclerView() {
        adapter = new OrderHistoryAdapter();
        adapter.setListener(order -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), OrderDetailActivity.class);
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.getId());
            startActivity(intent);
        });
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(adapter);
    }

    private void setupTabs() {
        binding.tabLayout.removeAllTabs();
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.orders_filter_all).setTag(OrderRepository.OrderFilter.ALL));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.orders_filter_active).setTag(OrderRepository.OrderFilter.ACTIVE));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.orders_filter_completed).setTag(OrderRepository.OrderFilter.COMPLETED));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.orders_filter_cancelled).setTag(OrderRepository.OrderFilter.CANCELLED));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Object filter = tab.getTag();
                viewModel.loadOrders(requireContext(), filter instanceof OrderRepository.OrderFilter
                        ? (OrderRepository.OrderFilter) filter
                        : OrderRepository.OrderFilter.ALL);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabSelected(tab);
            }
        });
    }

    private void bindObservers() {
        viewModel.getOrders().observe(getViewLifecycleOwner(), orders -> {
            adapter.submitList(orders);
            binding.swipeRefresh.setRefreshing(false);
            boolean isEmpty = orders == null || orders.isEmpty();
            binding.rvOrders.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.emptyState.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            if (isEmpty) {
                binding.emptyState.tvEmptyTitle.setText(R.string.orders_empty_title);
                binding.emptyState.tvEmptyMessage.setText(R.string.orders_empty_message);
            }
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            binding.swipeRefresh.setRefreshing(false);
            if (error != null && !error.isBlank()) {
                showToast(error);
            }
        });
    }

    private void subscribeToOrderUpdates() {
        if (ordersChannel != null || !isAdded()) {
            return;
        }

        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            Log.w(TAG_REAL_TIME, "Skip orders realtime because userId is empty");
            return;
        }

        RealtimeListener listener = new RealtimeListener() {
            @Override
            public void onInsert(JsonObject record) {
                Log.d(TAG_REAL_TIME, "Order inserted, refreshing list");
                refreshOrders();
            }

            @Override
            public void onUpdate(JsonObject record, JsonObject oldRecord) {
                String orderId = record.has("id") && !record.get("id").isJsonNull()
                        ? record.get("id").getAsString()
                        : "unknown";
                String status = record.has("status") && !record.get("status").isJsonNull()
                        ? record.get("status").getAsString()
                        : "unknown";
                Log.d(TAG_REAL_TIME, "Order updated id=" + orderId + ", status=" + status + ", refreshing list");
                refreshOrders();
            }

            @Override
            public void onDelete(JsonObject oldRecord) {
                Log.d(TAG_REAL_TIME, "Order deleted, refreshing list");
                refreshOrders();
            }

            @Override
            public void onConnected() {
                Log.d(TAG_REAL_TIME, "Connected to orders realtime for user: " + userId);
                refreshOrders();
            }

            @Override
            public void onDisconnected() {
                Log.w(TAG_REAL_TIME, "Disconnected from orders realtime");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG_REAL_TIME, "Orders realtime error: " + error);
            }
        };

        ordersChannel = SupabaseRealtimeClient.getInstance()
                .subscribe("public:orders", "user_id=eq." + userId, listener);
        Log.d(TAG_REAL_TIME, "Subscribed to orders channel: " + (ordersChannel != null ? "success" : "failed"));
    }

    private void unsubscribeFromOrderUpdates() {
        if (ordersChannel != null) {
            SupabaseRealtimeClient.getInstance().unsubscribe(ordersChannel);
            ordersChannel = null;
            Log.d(TAG_REAL_TIME, "Unsubscribed from orders realtime");
        }
    }

    private void refreshOrders() {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (binding != null && isAdded()) {
                viewModel.refresh(requireContext());
            }
        });
    }

    @Override
    public void onDestroyView() {
        unsubscribeFromOrderUpdates();
        super.onDestroyView();
        binding = null;
    }
}
