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

public class PopularRestaurantAdapter extends RecyclerView.Adapter<PopularRestaurantAdapter.ViewHolder> {

    private final List<Restaurant> restaurants = new ArrayList<>();
    private final OnRestaurantClickListener listener;
    private final Context context;

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    public PopularRestaurantAdapter(Context context, OnRestaurantClickListener listener) {
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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_restaurant_card_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(restaurants.get(position));
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivRestaurantImage;
        private final TextView tvRestaurantName;
        private final TextView tvRestaurantInfo;
        private final TextView tvRating;
        private final TextView tvReviewCount;
        private final TextView tvDeliveryFee;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRestaurantImage = itemView.findViewById(R.id.ivRestaurantImage);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvRestaurantInfo = itemView.findViewById(R.id.tvRestaurantInfo);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            tvDeliveryFee = itemView.findViewById(R.id.tvDeliveryFee);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRestaurantClick(restaurants.get(pos));
                }
            });
        }

        void bind(Restaurant restaurant) {
            tvRestaurantName.setText(restaurant.getName());
            tvRestaurantInfo.setText(restaurant.getDescription() != null ? restaurant.getDescription() : "Restaurant");
            tvRating.setText(restaurant.getFormattedRating());
            tvReviewCount.setText("(" + restaurant.getReviewCount() + ")");
            tvDeliveryFee.setText(restaurant.getFormattedDeliveryFee());

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
        }
    }
}