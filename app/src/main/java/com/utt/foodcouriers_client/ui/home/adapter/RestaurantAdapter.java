package com.utt.foodcouriers_client.ui.home.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.Restaurant;

import java.util.ArrayList;
import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder> {

    private final List<Restaurant> restaurants = new ArrayList<>();
    private final OnRestaurantClickListener listener;
    private final Context context;

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    public RestaurantAdapter(Context context, OnRestaurantClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setRestaurants(List<Restaurant> newRestaurants) {
        restaurants.clear();
        if (newRestaurants != null) {
            restaurants.addAll(newRestaurants);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_restaurant_card, parent, false);
        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
        Restaurant restaurant = restaurants.get(position);
        holder.bind(restaurant);
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    class RestaurantViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivRestaurantImage;
        private final TextView tvRestaurantName;
        private final TextView tvRestaurantInfo;
        private final TextView tvRating;
        private final TextView tvDeliveryFee;

        RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRestaurantImage = itemView.findViewById(R.id.ivRestaurantImage);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvRestaurantInfo = itemView.findViewById(R.id.tvRestaurantInfo);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvDeliveryFee = itemView.findViewById(R.id.tvDeliveryFee);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRestaurantClick(restaurants.get(position));
                }
            });
        }

        void bind(Restaurant restaurant) {
            tvRestaurantName.setText(restaurant.getName());
            
            String categoryInfo = restaurant.getDescription() != null ? 
                    restaurant.getDescription() : "Restaurant";
            String distance = " · 2.5km";
            tvRestaurantInfo.setText(categoryInfo + distance);
            
            tvRating.setText(restaurant.getFormattedRating());
            tvDeliveryFee.setText(restaurant.getFormattedDeliveryFee());

            String imageUrl = restaurant.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.food_placeholder_burger)
                        .error(R.drawable.food_placeholder_burger)
                        .centerCrop()
                        .into(ivRestaurantImage);
            } else {
                ivRestaurantImage.setImageResource(R.drawable.food_placeholder_burger);
            }
        }
    }
}