package com.utt.foodcouriers_client.ui.order;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.repository.OrderRepository;
import com.utt.foodcouriers_client.databinding.FragmentOrdersBinding;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.ui.order.adapter.OrderHistoryAdapter;
import com.utt.foodcouriers_client.viewmodel.OrdersViewModel;

public class OrdersFragment extends BaseFragment {

    private FragmentOrdersBinding binding;
    private OrdersViewModel viewModel;
    private OrderHistoryAdapter adapter;

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
