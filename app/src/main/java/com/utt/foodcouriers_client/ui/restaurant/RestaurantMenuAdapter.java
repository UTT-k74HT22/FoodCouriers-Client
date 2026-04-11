package com.utt.foodcouriers_client.ui.restaurant;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.MenuItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestaurantMenuAdapter extends RecyclerView.Adapter<RestaurantMenuAdapter.ViewHolder> {

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MenuItem menuItem);
    }

    public interface OnQuantityChangeListener {
        void onQuantityChange(MenuItem menuItem, int newQuantity);
    }

    private final List<MenuSection> sections = new ArrayList<>();
    private final OnMenuItemClickListener listener;
    private final OnQuantityChangeListener quantityListener;
    private final Context context;
    private final Map<String, Integer> quantities = new HashMap<>();

    public static class MenuSection {
        public String categoryName;
        public List<MenuItem> items;

        public MenuSection(String categoryName, List<MenuItem> items) {
            this.categoryName = categoryName;
            this.items = items;
        }
    }

    public RestaurantMenuAdapter(Context context, OnMenuItemClickListener listener, OnQuantityChangeListener quantityListener) {
        this.context = context;
        this.listener = listener;
        this.quantityListener = quantityListener;
    }

    public void setSections(List<MenuSection> newSections) {
        sections.clear();
        if (newSections != null) {
            sections.addAll(newSections);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        int count = 0;
        for (MenuSection section : sections) {
            if (position == count) {
                return 0;
            }
            count += 1 + section.items.size();
            if (position < count) {
                return 1;
            }
        }
        return 1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_menu_category_header, parent, false);
            return new CategoryHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_restaurant_menu_item, parent, false);
            return new MenuItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int[] positions = getSectionPosition(position);
        MenuSection section = sections.get(positions[0]);

        if (holder instanceof CategoryHeaderViewHolder) {
            ((CategoryHeaderViewHolder) holder).bind(section.categoryName);
        } else if (holder instanceof MenuItemViewHolder) {
            MenuItem item = section.items.get(positions[1]);
            ((MenuItemViewHolder) holder).bind(item, listener, quantityListener, quantities);
        }
    }

    private int[] getSectionPosition(int position) {
        int count = 0;
        for (int i = 0; i < sections.size(); i++) {
            if (position == count) {
                return new int[]{i, -1};
            }
            count++;
            if (position < count + sections.get(i).items.size()) {
                return new int[]{i, position - count};
            }
            count += sections.get(i).items.size();
        }
        return new int[]{0, 0};
    }

    @Override
    public int getItemCount() {
        int count = 0;
        for (MenuSection section : sections) {
            count += 1 + section.items.size();
        }
        return count;
    }

    static abstract class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class CategoryHeaderViewHolder extends ViewHolder {
        private final TextView tvCategoryName;

        CategoryHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
        }

        void bind(String categoryName) {
            tvCategoryName.setText(categoryName);
        }
    }

    static class MenuItemViewHolder extends ViewHolder {
        private final ImageView ivFoodImage;
        private final TextView tvFoodName;
        private final TextView tvFoodDescription;
        private final TextView tvFoodPrice;
        private final FrameLayout flQuantity;
        private final LinearLayout llQuantity;
        private final MaterialButton btnAdd;
        private final ImageView btnDecrease;
        private final ImageView btnIncrease;
        private final TextView tvQuantity;

        MenuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoodImage = itemView.findViewById(R.id.iv_food_image);
            tvFoodName = itemView.findViewById(R.id.tv_food_name);
            tvFoodDescription = itemView.findViewById(R.id.tv_food_description);
            tvFoodPrice = itemView.findViewById(R.id.tv_food_price);
            flQuantity = itemView.findViewById(R.id.fl_quantity);
            llQuantity = itemView.findViewById(R.id.ll_quantity);
            btnAdd = itemView.findViewById(R.id.btn_add);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
        }

        void bind(MenuItem item, OnMenuItemClickListener listener, OnQuantityChangeListener quantityListener, Map<String, Integer> quantities) {
            tvFoodName.setText(item.getName());
            tvFoodDescription.setText(item.getDescription());
            tvFoodPrice.setText(String.format("%,dk", item.getPrice()));

            int quantity = quantities.getOrDefault(item.getId(), 0);
            tvQuantity.setText(String.valueOf(quantity));

            if (quantity > 0) {
                llQuantity.setVisibility(View.VISIBLE);
                btnAdd.setVisibility(View.GONE);
            } else {
                llQuantity.setVisibility(View.GONE);
                btnAdd.setVisibility(View.VISIBLE);
            }

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.food_placeholder_burger)
                        .error(R.drawable.food_placeholder_burger)
                        .centerCrop()
                        .into(ivFoodImage);
            } else {
                ivFoodImage.setImageResource(R.drawable.food_placeholder_burger);
            }

            btnAdd.setOnClickListener(v -> {
                quantities.put(item.getId(), 1);
                llQuantity.setVisibility(View.VISIBLE);
                btnAdd.setVisibility(View.GONE);
                tvQuantity.setText("1");
                if (quantityListener != null) {
                    quantityListener.onQuantityChange(item, 1);
                }
            });

            btnDecrease.setOnClickListener(v -> {
                int current = quantities.getOrDefault(item.getId(), 0);
                if (current > 0) {
                    int newQty = current - 1;
                    quantities.put(item.getId(), newQty);
                    tvQuantity.setText(String.valueOf(newQty));
                    if (newQty == 0) {
                        llQuantity.setVisibility(View.GONE);
                        btnAdd.setVisibility(View.VISIBLE);
                    }
                    if (quantityListener != null) {
                        quantityListener.onQuantityChange(item, newQty);
                    }
                }
            });

            btnIncrease.setOnClickListener(v -> {
                int current = quantities.getOrDefault(item.getId(), 0);
                int newQty = current + 1;
                quantities.put(item.getId(), newQty);
                tvQuantity.setText(String.valueOf(newQty));
                if (quantityListener != null) {
                    quantityListener.onQuantityChange(item, newQty);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMenuItemClick(item);
                }
            });
        }
    }
}