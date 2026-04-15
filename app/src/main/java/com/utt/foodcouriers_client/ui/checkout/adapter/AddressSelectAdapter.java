package com.utt.foodcouriers_client.ui.checkout.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.Address;

import java.util.ArrayList;
import java.util.List;

public class AddressSelectAdapter extends RecyclerView.Adapter<AddressSelectAdapter.AddressViewHolder> {

    private List<Address> addresses = new ArrayList<>();
    private OnAddressSelectedListener listener;
    private String selectedAddressId = null;

    public interface OnAddressSelectedListener {
        void onAddressSelected(Address address);
    }

    public void setOnAddressSelectedListener(OnAddressSelectedListener listener) {
        this.listener = listener;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses != null ? addresses : new ArrayList<>();
        if (selectedAddressId == null && !this.addresses.isEmpty()) {
            for (Address addr : this.addresses) {
                if (addr.isDefault()) {
                    selectedAddressId = addr.getId();
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setSelectedAddressId(String selectedAddressId) {
        this.selectedAddressId = selectedAddressId;
        notifyDataSetChanged();
    }

    public String getSelectedAddressId() {
        return selectedAddressId;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_address_select, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        holder.bind(addresses.get(position));
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    class AddressViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvLabel;
        private final TextView tvAddress;
        private final View selectionIndicator;

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvLabel = itemView.findViewById(R.id.tv_label);
            tvAddress = itemView.findViewById(R.id.tv_address);
            selectionIndicator = itemView.findViewById(R.id.selection_indicator);
        }

        public void bind(Address address) {
            tvLabel.setText(address.getLabel());
            tvAddress.setText(address.getDisplayAddress());

            boolean isSelected = address.getId().equals(selectedAddressId);
            if (isSelected) {
                ivIcon.setColorFilter(itemView.getContext().getColor(R.color.primary));
                selectionIndicator.setVisibility(View.VISIBLE);
                itemView.setBackgroundColor(itemView.getContext().getColor(R.color.orange_light));
            } else {
                ivIcon.setColorFilter(itemView.getContext().getColor(R.color.text_secondary));
                selectionIndicator.setVisibility(View.GONE);
                itemView.setBackgroundColor(itemView.getContext().getColor(R.color.surface));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    selectedAddressId = address.getId();
                    notifyDataSetChanged();
                    listener.onAddressSelected(address);
                }
            });
        }
    }
}