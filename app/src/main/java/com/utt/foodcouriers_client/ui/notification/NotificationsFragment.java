package com.utt.foodcouriers_client.ui.notification;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.NotificationItem;
import com.utt.foodcouriers_client.data.remote.SupabaseRealtimeClient;
import com.utt.foodcouriers_client.databinding.FragmentNotificationsBinding;
import com.utt.foodcouriers_client.ui.common.BaseFragment;
import com.utt.foodcouriers_client.ui.main.MainActivity;
import com.utt.foodcouriers_client.ui.notification.adapter.NotificationAdapter;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;
import com.utt.foodcouriers_client.utils.websocket.RealtimeChannel;
import com.utt.foodcouriers_client.utils.websocket.RealtimeListener;
import com.utt.foodcouriers_client.viewmodel.NotificationViewModel;

import java.util.Collections;
import java.util.List;

public class NotificationsFragment extends BaseFragment {

    private static final String TAG_REAL_TIME = "RealTimeWebSocket";

    private FragmentNotificationsBinding binding;
    private NotificationViewModel viewModel;
    private NotificationAdapter adapter;
    private SessionManager sessionManager;
    private RealtimeChannel notificationChannel;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        sessionManager = SessionManager.getInstance(requireContext());
        
        setupRecyclerView();
        setupSwipeRefresh();
        setupActions();
        observeViewModel();
        loadNotifications();
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeToNotifications();
    }

    @Override
    public void onStop() {
        super.onStop();
        unsubscribeFromNotifications();
    }

    private void subscribeToNotifications() {
        currentUserId = sessionManager.getUserId();
        if (currentUserId == null || currentUserId.isBlank()) {
            return;
        }
        
        Log.d(TAG_REAL_TIME, "Subscribing to notifications for user: " + currentUserId);
        
        RealtimeListener listener = new RealtimeListener() {
            @Override
            public void onInsert(JsonObject record) {
                Log.d(TAG_REAL_TIME, "New notification received");
                refreshNotifications();
            }

            @Override
            public void onUpdate(JsonObject record, JsonObject oldRecord) {
                Log.d(TAG_REAL_TIME, "Notification updated");
                refreshNotifications();
            }

            @Override
            public void onDelete(JsonObject oldRecord) {
                Log.d(TAG_REAL_TIME, "Notification deleted");
                refreshNotifications();
            }

            @Override
            public void onConnected() {
                Log.d(TAG_REAL_TIME, "Connected to notifications realtime");
            }

            @Override
            public void onDisconnected() {
                Log.w(TAG_REAL_TIME, "Disconnected from notifications realtime");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG_REAL_TIME, "Notifications realtime error: " + error);
            }
        };
        
        notificationChannel = SupabaseRealtimeClient.getInstance()
                .subscribe("public:notifications", "user_id=eq." + currentUserId, listener);
        Log.d(TAG_REAL_TIME, "Subscribed to notifications channel: " + (notificationChannel != null ? "success" : "failed"));
    }

    private void unsubscribeFromNotifications() {
        if (notificationChannel != null) {
            SupabaseRealtimeClient.getInstance().unsubscribe(notificationChannel);
            notificationChannel = null;
            Log.d(TAG_REAL_TIME, "Unsubscribed from notifications");
        }
    }

    private void refreshNotifications() {
        if (getContext() != null) {
            requireActivity().runOnUiThread(() -> {
                viewModel.refreshNotifications(requireContext());
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter();
        adapter.setOnNotificationClickListener(new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(NotificationItem notification) {
                if (notification.isUnread()) {
                    viewModel.markAsRead(requireContext(), notification.getId());
                }
            }
        });
        
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifications.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refreshNotifications(requireContext());
        });
    }

    private void setupActions() {
        binding.btnMarkAllRead.setOnClickListener(v -> {
            viewModel.markAllAsRead(requireContext());
        });

        binding.btnDeleteAll.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.notifications_delete_all))
                .setMessage(getString(R.string.notifications_delete_confirm_message))
                .setNegativeButton(getString(R.string.address_cancel), null)
                .setPositiveButton(getString(R.string.address_delete), (dialog, which) -> {
                    viewModel.deleteAll(requireContext());
                })
                .show());
    }

    private void observeViewModel() {
        viewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            binding.swipeRefresh.setRefreshing(false);

            updateActionState(notifications);
            if (notifications == null || notifications.isEmpty()) {
                adapter.submitList(Collections.emptyList());
                binding.emptyState.getRoot().setVisibility(View.VISIBLE);
                binding.rvNotifications.setVisibility(View.GONE);
            } else {
                binding.emptyState.getRoot().setVisibility(View.GONE);
                binding.rvNotifications.setVisibility(View.VISIBLE);
                adapter.submitList(notifications);
            }
        });

        viewModel.getUnreadCount().observe(getViewLifecycleOwner(), unreadCount -> {
            int count = unreadCount != null ? unreadCount : 0;
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateNotificationBadge(count);
            }
        });

        viewModel.getSuccessAction().observe(getViewLifecycleOwner(), action -> {
            if (action == null) {
                return;
            }
            if (action == NotificationViewModel.ACTION_MARK_ALL_READ) {
                ToastBanner.showSuccess(getString(R.string.notifications_mark_all_read_success));
            } else if (action == NotificationViewModel.ACTION_DELETE_ALL) {
                ToastBanner.showSuccess(getString(R.string.notifications_delete_success));
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.swipeRefresh.setRefreshing(isLoading != null && isLoading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isBlank()) {
                ToastBanner.showError(error);
            }
        });
    }

    private void updateActionState(List<NotificationItem> notifications) {
        int totalCount = notifications != null ? notifications.size() : 0;
        int unreadCount = 0;
        if (notifications != null) {
            for (NotificationItem notification : notifications) {
                if (notification != null && notification.isUnread()) {
                    unreadCount++;
                }
            }
        }

        binding.tvNotificationCount.setText(totalCount > 0
                ? getString(R.string.notifications_count_summary, totalCount, unreadCount)
                : getString(R.string.notifications_empty_count));
        binding.btnMarkAllRead.setEnabled(unreadCount > 0);
        binding.btnDeleteAll.setEnabled(totalCount > 0);
    }

    private void loadNotifications() {
        String userId = sessionManager.getUserId();
        if (userId != null && !userId.isBlank()) {
            viewModel.loadNotifications(requireContext());
        } else {
            ToastBanner.showWarning(getString(R.string.notifications_login_required));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity().getActionBar() != null) {
            getActivity().getActionBar().setTitle(getString(R.string.notifications_title));
        }
        loadNotifications();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unsubscribeFromNotifications();
        binding = null;
    }
}
