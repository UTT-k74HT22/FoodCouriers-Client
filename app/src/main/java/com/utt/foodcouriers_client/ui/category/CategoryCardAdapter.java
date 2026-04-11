package com.utt.foodcouriers_client.ui.category;

import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.FoodCategory;
import com.utt.foodcouriers_client.databinding.ItemCategoryCardBinding;

import java.util.ArrayList;
import java.util.List;

public class CategoryCardAdapter extends RecyclerView.Adapter<CategoryCardAdapter.CategoryCardViewHolder> {

    public static final int MODE_HOME = 1;
    public static final int MODE_SEARCH = 2;

    public interface OnCategoryClickListener {
        void onCategoryClicked(FoodCategory category);
    }

    private final int mode;
    private final OnCategoryClickListener listener;
    private final List<FoodCategory> items = new ArrayList<>();
    private String selectedCategoryId;

    public CategoryCardAdapter(int mode, OnCategoryClickListener listener) {
        this.mode = mode;
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
    public CategoryCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemCategoryCardBinding binding = ItemCategoryCardBinding.inflate(inflater, parent, false);
        if (mode == MODE_HOME) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) binding.getRoot().getLayoutParams();
            if (params == null) {
                params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            params.width = dpToPx(286);
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            binding.getRoot().setLayoutParams(params);
        }
        return new CategoryCardViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryCardViewHolder holder, int position) {
        holder.bind(items.get(position), position == items.size() - 1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class CategoryCardViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryCardBinding binding;

        private CategoryCardViewHolder(ItemCategoryCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(FoodCategory category, boolean isLast) {
            boolean selected = category.getId().equals(selectedCategoryId);
            CategoryVisuals.CategoryStyle style = CategoryVisuals.resolve(binding.getRoot().getContext(), category);

            binding.tvCategoryEyebrow.setText(style.eyebrow);
            binding.tvCategoryName.setText(category.getName());
            binding.tvCategoryDescription.setText(style.description);
            binding.tvCategoryAction.setText(selected ? R.string.category_card_selected : R.string.category_card_action);
            binding.tvCategoryAction.setTextColor(selected ? style.textAccentColor : ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_secondary));
            binding.tvCategoryMeta.setText(selected ? R.string.category_meta_selected : R.string.category_meta_default);
            binding.tvCategoryStatus.setText(selected ? R.string.category_status_live : R.string.category_status_ready);
            binding.ivCategoryIcon.setImageResource(style.iconResId);
            binding.ivCategoryIcon.setColorFilter(style.textAccentColor);

            GradientDrawable heroBackground = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{style.surfaceColor, ContextCompat.getColor(binding.getRoot().getContext(), R.color.white)}
            );
            heroBackground.setCornerRadius(dpToPx(28));
            binding.heroPanel.setBackground(heroBackground);

            GradientDrawable iconBackground = new GradientDrawable();
            iconBackground.setShape(GradientDrawable.RECTANGLE);
            iconBackground.setCornerRadius(dpToPx(18));
            iconBackground.setColor(style.ornamentColor);
            binding.iconContainer.setBackground(iconBackground);

            GradientDrawable orbBackground = new GradientDrawable();
            orbBackground.setShape(GradientDrawable.OVAL);
            orbBackground.setColor(selected ? style.accentColor : style.ornamentColor);
            binding.backgroundOrb.setBackground(orbBackground);
            binding.backgroundOrb.setAlpha(selected ? 0.22f : 0.48f);

            GradientDrawable actionBackground = new GradientDrawable();
            actionBackground.setShape(GradientDrawable.RECTANGLE);
            actionBackground.setCornerRadius(dpToPx(16));
            actionBackground.setColor(selected ? style.accentColor : ContextCompat.getColor(binding.getRoot().getContext(), R.color.white));
            if (!selected) {
                actionBackground.setStroke(dpToPx(1), ContextCompat.getColor(binding.getRoot().getContext(), R.color.border));
            }
            binding.tvCategoryAction.setBackground(actionBackground);

            binding.selectionIndicator.setBackgroundColor(style.accentColor);
            binding.selectionIndicator.setAlpha(selected ? 1f : 0.18f);
            binding.cardRoot.setCardElevation(selected ? dpToPx(4) : dpToPx(1));
            binding.cardRoot.setStrokeColor(selected ? style.accentColor : ContextCompat.getColor(binding.getRoot().getContext(), R.color.border));
            binding.cardRoot.setStrokeWidth(dpToPx(selected ? 2 : 1));

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) binding.getRoot().getLayoutParams();
            if (params == null) {
                params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            if (mode == MODE_HOME) {
                params.setMarginEnd(isLast ? 0 : dpToPx(12));
            }
            params.bottomMargin = mode == MODE_SEARCH ? dpToPx(16) : 0;
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
