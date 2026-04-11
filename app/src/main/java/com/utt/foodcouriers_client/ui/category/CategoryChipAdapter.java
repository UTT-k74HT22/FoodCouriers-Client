package com.utt.foodcouriers_client.ui.category;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.FoodCategory;
import com.utt.foodcouriers_client.databinding.TemplateCategoryChipBinding;

import java.util.ArrayList;
import java.util.List;

public class CategoryChipAdapter extends RecyclerView.Adapter<CategoryChipAdapter.CategoryChipViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClicked(FoodCategory category);
    }

    private final OnCategoryClickListener listener;
    private final List<FoodCategory> items = new ArrayList<>();
    private String selectedCategoryId;

    public CategoryChipAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<FoodCategory> categories, String selectedCategoryId) {
        items.clear();
        if (categories != null) {
            items.addAll(categories);
        }
        this.selectedCategoryId = selectedCategoryId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        TemplateCategoryChipBinding binding = TemplateCategoryChipBinding.inflate(inflater, parent, false);
        return new CategoryChipViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryChipViewHolder holder, int position) {
        holder.bind(items.get(position), position == items.size() - 1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class CategoryChipViewHolder extends RecyclerView.ViewHolder {
        private final TemplateCategoryChipBinding binding;

        private CategoryChipViewHolder(TemplateCategoryChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(FoodCategory category, boolean isLast) {
            boolean selected = category.getId().equals(selectedCategoryId);
            binding.tvCategoryChip.setText(category.getName());
            binding.tvCategoryChip.setSelected(selected);
            binding.tvCategoryChip.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    selected ? R.color.white : R.color.text_secondary
            ));

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.getRoot().getLayoutParams();
            if (params == null) {
                params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            params.setMarginEnd(isLast ? 0 : dpToPx(10));
            binding.getRoot().setLayoutParams(params);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClicked(category);
                }
            });
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
