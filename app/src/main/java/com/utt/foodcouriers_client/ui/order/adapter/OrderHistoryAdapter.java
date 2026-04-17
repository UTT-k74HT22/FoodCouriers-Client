package com.utt.foodcouriers_client.ui.order.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.OrderStatus;
import com.utt.foodcouriers_client.data.model.OrderSummary;

import java.util.ArrayList;
import java.util.List;
/** Gán giá trị từ mảng thành các object để hiển thị lên RecyclerView  */
public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(OrderSummary order);
    }

    private final List<OrderSummary> items = new ArrayList<>();
    private OnOrderClickListener listener;

    public void setListener(OnOrderClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<OrderSummary> orders) {
        items.clear();
        if (orders != null) {
            items.addAll(orders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOrderCode;
        private final TextView tvStatus;
        private final TextView tvRestaurantName;
        private final TextView tvOrderDate;
        private final TextView tvTotal;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderCode = itemView.findViewById(R.id.tv_order_code);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvRestaurantName = itemView.findViewById(R.id.tv_restaurant_name);
            tvOrderDate = itemView.findViewById(R.id.tv_order_date);
            tvTotal = itemView.findViewById(R.id.tv_total);
        }

        void bind(OrderSummary order) {
            OrderStatus status = OrderStatus.fromValue(order.getStatus());
            tvOrderCode.setText(order.getOrderCode());
            tvStatus.setText(status.getLabel());
            tvRestaurantName.setText(order.getRestaurantName());
            tvOrderDate.setText(order.getCreatedAtLabel());
            tvTotal.setText(formatCurrency(order.getTotal()));
            tvStatus.setBackgroundResource(resolveBadge(status));
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderClick(order);
                }
            });
        }

        private int resolveBadge(OrderStatus status) {
            switch (status) {
                case DELIVERED:
                    return R.drawable.badge_success;
                case CANCELLED:
                    return R.drawable.badge_error;
                case AWAITING_PAYMENT:
                case PENDING:
                    return R.drawable.badge_warning;
                case DELIVERING:
                    return R.drawable.badge_info;
                case CONFIRMED:
                case PREPARING:
                case READY_FOR_PICKUP:
                default:
                    return R.drawable.badge_primary;
            }
        }

        private String formatCurrency(int amount) {
            return String.format("%,d đ", amount);
        }
    }
}
