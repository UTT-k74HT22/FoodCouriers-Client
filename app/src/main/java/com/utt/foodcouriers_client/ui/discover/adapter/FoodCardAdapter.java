package com.utt.foodcouriers_client.ui.discover.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.MenuItem;
import com.utt.foodcouriers_client.data.model.Restaurant;
import com.utt.foodcouriers_client.ui.restaurant.FoodDetailActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodCardAdapter extends RecyclerView.Adapter<FoodCardAdapter.ViewHolder> {

    private final List<MenuItem> items = new ArrayList<>();
    private final OnFoodAddClickListener listener;
    private final NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final Context context;
    private String restaurantId;
    private String restaurantName;
    private Restaurant restaurant;

    public interface OnFoodAddClickListener {
        void onFoodAddClick(MenuItem menuItem);
    }

    public FoodCardAdapter(Context context, OnFoodAddClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setRestaurantInfo(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public void setItems(List<MenuItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivFoodImage;
        private final TextView tvFoodName;
        private final TextView tvFoodPrice;
        private final ImageView btnAddToCart;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoodImage = itemView.findViewById(R.id.ivFoodImage);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvFoodPrice = itemView.findViewById(R.id.tvFoodPrice);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
        }

        void bind(MenuItem item) {
            tvFoodName.setText(item.getName());
            tvFoodPrice.setText(formatter.format(item.getPrice()));

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.food_placeholder_burger)
                        .error(R.drawable.food_placeholder_burger)
                        .centerCrop()
                        .into(ivFoodImage);
            } else {
                ivFoodImage.setImageResource(R.drawable.food_placeholder_burger);
            }

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, FoodDetailActivity.class);
                intent.putExtra(FoodDetailActivity.EXTRA_MENU_ITEM, item);
                if (restaurant != null) {
                    intent.putExtra(FoodDetailActivity.EXTRA_RESTAURANT, restaurant);
                }
                context.startActivity(intent);
            });

            btnAddToCart.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFoodAddClick(item);
                }
            });
        }
    }
}