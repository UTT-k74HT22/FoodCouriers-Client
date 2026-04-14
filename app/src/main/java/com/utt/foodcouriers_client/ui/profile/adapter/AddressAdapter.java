package com.utt.foodcouriers_client.ui.profile.adapter;

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

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    private List<Address> addresses = new ArrayList<>();
    private OnAddressClickListener listener;

    public interface OnAddressClickListener {
        void onSetDefault(Address address);
        void onEdit(Address address);
        void onDelete(Address address);
    }

    public void setOnAddressClickListener(OnAddressClickListener listener) {
        this.listener = listener;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses != null ? addresses : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_address, parent, false);
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
        private final TextView tvDefault;
        private final TextView tvAddress;
        private final TextView btnSetDefault;
        private final TextView btnEdit;
        private final TextView btnDelete;

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvLabel = itemView.findViewById(R.id.tv_label);
            tvDefault = itemView.findViewById(R.id.tv_default);
            tvAddress = itemView.findViewById(R.id.tv_address);
            btnSetDefault = itemView.findViewById(R.id.btn_set_default);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(Address address) {
            tvLabel.setText(address.getLabel());
            tvAddress.setText(address.getDisplayAddress());

            if (address.isDefault()) {
                tvDefault.setVisibility(View.VISIBLE);
                btnSetDefault.setVisibility(View.GONE);
                ivIcon.setColorFilter(itemView.getContext().getColor(R.color.primary));
            } else {
                tvDefault.setVisibility(View.GONE);
                btnSetDefault.setVisibility(View.VISIBLE);
                ivIcon.setColorFilter(itemView.getContext().getColor(R.color.text_secondary));
            }

            btnSetDefault.setOnClickListener(v -> {
                if (listener != null) listener.onSetDefault(address);
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(address);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(address);
            });
        }
    }
}
