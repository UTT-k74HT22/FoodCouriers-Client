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
import com.utt.foodcouriers_client.data.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public static final Category CATEGORY_ALL = new Category(null, "Tất cả", null);
    
    private final List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;
    private final Context context;
    private int selectedPosition = 0;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
    }

    public CategoryAdapter(Context context, OnCategoryClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setCategories(List<Category> newCategories) {
        categories.clear();
        categories.add(CATEGORY_ALL);
        if (newCategories != null) {
            categories.addAll(newCategories);
        }
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_chip, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        boolean isSelected = position == selectedPosition;
        holder.bind(category, isSelected);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvCategoryName;
        private final ImageView ivCategoryIcon;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCategoryClick(categories.get(position), position);
                }
            });
        }

        void bind(Category category, boolean isSelected) {
            tvCategoryName.setText(category.getName());

            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.category_chip_selected_background);
                tvCategoryName.setTextColor(context.getColor(R.color.white));
            } else {
                itemView.setBackgroundResource(R.drawable.category_chip_background);
                tvCategoryName.setTextColor(context.getColor(R.color.text_primary));
            }

            if (category.getId() == null) {
                ivCategoryIcon.setVisibility(View.GONE);
            } else {
                String imageUrl = category.getImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_food_bowl)
                            .error(R.drawable.ic_food_bowl)
                            .circleCrop()
                            .into(ivCategoryIcon);
                    ivCategoryIcon.setVisibility(View.VISIBLE);
                } else {
                    ivCategoryIcon.setVisibility(View.GONE);
                }
            }
        }
    }
}