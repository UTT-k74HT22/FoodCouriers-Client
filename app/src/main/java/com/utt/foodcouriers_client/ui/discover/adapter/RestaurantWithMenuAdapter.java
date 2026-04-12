package com.utt.foodcouriers_client.ui.discover.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.MenuItem;
import com.utt.foodcouriers_client.data.model.Restaurant;
import com.utt.foodcouriers_client.ui.restaurant.RestaurantDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class RestaurantWithMenuAdapter extends RecyclerView.Adapter<RestaurantWithMenuAdapter.ViewHolder> {

    private final List<RestaurantWithMenu> items = new ArrayList<>();
    private final OnFoodAddClickListener listener;
    private final Context context;

    public static class RestaurantWithMenu {
        public Restaurant restaurant;
        public List<MenuItem> menuItems;

        public RestaurantWithMenu(Restaurant restaurant, List<MenuItem> menuItems) {
            this.restaurant = restaurant;
            this.menuItems = menuItems != null ? menuItems : new ArrayList<>();
        }
    }

    public interface OnFoodAddClickListener {
        void onFoodAddClick(MenuItem menuItem, Restaurant restaurant);
    }

    public RestaurantWithMenuAdapter(Context context, OnFoodAddClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<RestaurantWithMenu> newItems) {
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
                .inflate(R.layout.item_restaurant_with_menu, parent, false);
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
        private final ImageView ivRestaurantImage;
        private final TextView tvRestaurantName;
        private final TextView tvRestaurantInfo;
        private final TextView tvRating;
        private final TextView tvReviewCount;
        private final TextView tvDeliveryTime;
        private final TextView tvDeliveryFee;
        private final TextView tvMinOrder;
        private final ImageView ivOpenStatus;
        private final TextView tvMenuSectionTitle;
        private final RecyclerView rvMenuItems;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRestaurantImage = itemView.findViewById(R.id.ivRestaurantImage);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvRestaurantInfo = itemView.findViewById(R.id.tvRestaurantInfo);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            tvDeliveryTime = itemView.findViewById(R.id.tvDeliveryTime);
            tvDeliveryFee = itemView.findViewById(R.id.tvDeliveryFee);
            tvMinOrder = itemView.findViewById(R.id.tvMinOrder);
            ivOpenStatus = itemView.findViewById(R.id.ivOpenStatus);
            tvMenuSectionTitle = itemView.findViewById(R.id.tvMenuSectionTitle);
            rvMenuItems = itemView.findViewById(R.id.rvMenuItems);
        }

        void bind(RestaurantWithMenu item) {
            Restaurant restaurant = item.restaurant;
            
            tvRestaurantName.setText(restaurant.getName());
            tvRestaurantInfo.setText(restaurant.getAddress() != null ? restaurant.getAddress() : "");
            tvRating.setText(restaurant.getFormattedRating());
            tvReviewCount.setText("(" + restaurant.getReviewCount() + ")");
            tvDeliveryTime.setText("20-30 min");
            tvDeliveryFee.setText(restaurant.getFormattedDeliveryFee());
            tvMinOrder.setText("Min " + restaurant.getMinOrder() + "k");
            tvMenuSectionTitle.setText("Menu");

            if (restaurant.isOpen()) {
                ivOpenStatus.setVisibility(View.VISIBLE);
                ivOpenStatus.setColorFilter(context.getColor(R.color.success));
            } else {
                ivOpenStatus.setVisibility(View.VISIBLE);
                ivOpenStatus.setColorFilter(context.getColor(R.color.error));
            }

            if (restaurant.getImageUrl() != null && !restaurant.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(restaurant.getImageUrl())
                        .placeholder(R.drawable.food_placeholder_burger)
                        .error(R.drawable.food_placeholder_burger)
                        .centerCrop()
                        .into(ivRestaurantImage);
            } else {
                ivRestaurantImage.setImageResource(R.drawable.food_placeholder_burger);
            }

            FoodCardAdapter foodAdapter = new FoodCardAdapter(context, menuItem -> {
                if (listener != null) {
                    listener.onFoodAddClick(menuItem, restaurant);
                }
            });
            rvMenuItems.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            rvMenuItems.setAdapter(foodAdapter);
            foodAdapter.setItems(item.menuItems);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, RestaurantDetailActivity.class);
                intent.putExtra("restaurant_id", restaurant.getId());
                context.startActivity(intent);
            });
        }
    }
}