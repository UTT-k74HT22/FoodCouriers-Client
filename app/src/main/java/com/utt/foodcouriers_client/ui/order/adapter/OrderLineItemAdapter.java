package com.utt.foodcouriers_client.ui.order.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.OrderLineItem;

import java.util.ArrayList;
import java.util.List;

public class OrderLineItemAdapter extends RecyclerView.Adapter<OrderLineItemAdapter.LineItemViewHolder> {

    private final List<OrderLineItem> items = new ArrayList<>();

    public void submitList(List<OrderLineItem> lineItems) {
        items.clear();
        if (lineItems != null) {
            items.addAll(lineItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LineItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_line, parent, false);
        return new LineItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LineItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LineItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvMeta;
        private final TextView tvSubtotal;

        LineItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            tvSubtotal = itemView.findViewById(R.id.tv_subtotal);
        }

        void bind(OrderLineItem item) {
            tvName.setText(item.getName());
            tvMeta.setText("x" + item.getQuantity() + " · " + formatCurrency(item.getPrice()));
            tvSubtotal.setText(formatCurrency(item.getQuantity() * item.getPrice()));
        }

        private String formatCurrency(int amount) {
            return String.format("%,d đ", amount);
        }
    }
}
