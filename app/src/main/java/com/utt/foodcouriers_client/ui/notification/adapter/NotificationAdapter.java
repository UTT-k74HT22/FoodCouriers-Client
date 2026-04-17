package com.utt.foodcouriers_client.ui.notification.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.NotificationItem;
import com.utt.foodcouriers_client.databinding.ItemNotificationBinding;

/**
 * Adapter render danh sách thông báo bằng {@link ListAdapter}.
 *
 * <p>Adapter chỉ chịu trách nhiệm hiển thị item và bắn sự kiện click ra ngoài.
 * Việc mark read, reload list hoặc cập nhật badge thuộc về Fragment/ViewModel.</p>
 */
public class NotificationAdapter extends ListAdapter<NotificationItem, NotificationAdapter.NotificationViewHolder> {

    private OnNotificationClickListener clickListener;

    /**
     * Callback khi user bấm một thông báo trong danh sách.
     */
    public interface OnNotificationClickListener {
        /**
         * @param notification item vừa được user chọn
         */
        void onNotificationClick(NotificationItem notification);
    }

    public NotificationAdapter() {
        super(DIFF_CALLBACK);
    }

    /**
     * Gắn listener để Fragment xử lý thao tác click.
     *
     * @param listener listener nhận item được click
     */
    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.clickListener = listener;
    }

    private static final DiffUtil.ItemCallback<NotificationItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<NotificationItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull NotificationItem oldItem, @NonNull NotificationItem newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull NotificationItem oldItem, @NonNull NotificationItem newItem) {
            return oldItem.getTitle() != null && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.isUnread() == newItem.isUnread();
        }
    };

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = getItem(position);
        holder.bind(item);
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;

        NotificationViewHolder(@NonNull ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Bind dữ liệu thông báo vào layout item.
         *
         * @param item thông báo cần render
         */
        void bind(NotificationItem item) {
            binding.tvTitle.setText(item.getTitle());
            binding.tvMessage.setText(item.getMessage());
            binding.tvTime.setText(item.getTimeLabel());

            binding.indicatorUnread.setVisibility(item.isUnread() ? View.VISIBLE : View.GONE);

            binding.getRoot().setAlpha(item.isUnread() ? 1.0f : 0.7f);

            int iconRes = getIconForType(item.getType());
            binding.ivIcon.setImageResource(iconRes);

            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onNotificationClick(item);
                }
            });
        }

        /**
         * Chọn icon theo loại thông báo từ backend.
         *
         * @param type loại thông báo, ví dụ {@code order}, {@code promotion}, {@code system}
         * @return drawable resource phù hợp
         */
        private int getIconForType(String type) {
            if (type == null) {
                return R.drawable.ic_notification;
            }
            switch (type.toLowerCase()) {
                case "order":
                    return R.drawable.ic_orders;
                case "promotion":
                case "promo":
                    return R.drawable.ic_fresh_leaf;
                case "system":
                    return R.drawable.ic_delivery;
                default:
                    return R.drawable.ic_notification;
            }
        }
    }
}
