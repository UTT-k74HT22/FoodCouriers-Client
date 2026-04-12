package com.utt.foodcouriers_client.ui.cart.adapter;

import android.content.res.ColorStateList;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.CartItem;
import com.utt.foodcouriers_client.databinding.ComponentCartItemBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder> {

    private static final RequestOptions IMAGE_REQUEST_OPTIONS = new RequestOptions()
            .placeholder(R.drawable.ic_food_bowl)
            .error(R.drawable.ic_food_bowl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop();

    private final List<CartItem> items = new ArrayList<>();
    private final CartItemListener listener;
    private final Context context;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public interface CartItemListener {
        void onIncrease(String cartItemId);

        void onDecrease(String cartItemId);
    }

    public CartItemAdapter(Context context, CartItemListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void submitList(List<CartItem> cartItems) {
        items.clear();
        if (cartItems != null) {
            items.addAll(cartItems);
        }
        notifyDataSetChanged();
    }

    public int getQuantityForItem(String cartItemId) {
        for (CartItem item : items) {
            if (item.getId() != null && item.getId().equals(cartItemId)) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    @NonNull
    @Override
    public CartItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ComponentCartItemBinding binding = ComponentCartItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new CartItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CartItemViewHolder extends RecyclerView.ViewHolder {
        private final ComponentCartItemBinding binding;

        CartItemViewHolder(ComponentCartItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CartItem item) {
            binding.tvRestaurantName.setText(item.getRestaurantName());
            binding.tvFoodName.setText(item.getName());
            binding.tvFoodPrice.setText(currencyFormatter.format(item.getPrice()));
            binding.tvLineTotal.setText(currencyFormatter.format(item.getPrice() * item.getQuantity()));
            binding.tvQuantity.setText(String.valueOf(item.getQuantity()));
            binding.tvFoodNote.setVisibility(item.getNote() == null || item.getNote().isBlank() ? View.GONE : View.VISIBLE);
            binding.tvFoodNote.setText(item.getNote());
            binding.tvRemoveHint.setVisibility(item.getQuantity() == 1 ? View.VISIBLE : View.GONE);
            binding.ivFoodImage.setContentDescription(context.getString(R.string.food_image_description, item.getName()));

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                binding.ivFoodImage.setImageTintList(null);
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .apply(IMAGE_REQUEST_OPTIONS)
                        .into(binding.ivFoodImage);
            } else {
                binding.ivFoodImage.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.primary_dark)
                ));
                binding.ivFoodImage.setImageResource(R.drawable.ic_food_bowl);
            }

            binding.btnIncrease.setOnClickListener(v -> listener.onIncrease(item.getId()));
            binding.btnDecrease.setOnClickListener(v -> listener.onDecrease(item.getId()));
        }
    }
}
