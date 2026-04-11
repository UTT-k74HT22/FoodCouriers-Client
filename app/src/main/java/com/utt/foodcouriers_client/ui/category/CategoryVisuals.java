package com.utt.foodcouriers_client.ui.category;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.FoodCategory;

public final class CategoryVisuals {

    private CategoryVisuals() {
    }

    public static CategoryStyle resolve(Context context, FoodCategory category) {
        String key = category.getName() == null ? "" : category.getName().toLowerCase();

        if (containsAny(key, "uống", "drink", "beverage")) {
            return new CategoryStyle(
                    color(context, R.color.mint_light),
                    color(context, R.color.secondary),
                    color(context, R.color.auth_pill_mint_text),
                    color(context, R.color.decor_green),
                    R.drawable.ic_delivery_bag,
                    "Cool picks",
                    "Fresh drinks, teas, and quick sips for every order."
            );
        }
        if (containsAny(key, "tráng", "dessert", "sweet")) {
            return new CategoryStyle(
                    color(context, R.color.yellow_light),
                    color(context, R.color.accent),
                    color(context, R.color.auth_pill_yellow_text),
                    color(context, R.color.orange_light),
                    R.drawable.ic_food_bowl,
                    "Sweet finish",
                    "Desserts and comfort bites that round out the meal."
            );
        }
        if (containsAny(key, "phụ", "side", "snack")) {
            return new CategoryStyle(
                    color(context, R.color.orange_light),
                    color(context, R.color.primary_light),
                    color(context, R.color.primary_dark),
                    color(context, R.color.decor_orange),
                    R.drawable.ic_burger_line,
                    "Quick bites",
                    "Add-on dishes, snacks, and the small plates people repeat."
            );
        }
        if (containsAny(key, "combo", "set")) {
            return new CategoryStyle(
                    color(context, R.color.auth_surface_alt),
                    color(context, R.color.primary),
                    color(context, R.color.primary_dark),
                    color(context, R.color.decor_green),
                    R.drawable.ic_receipt,
                    "Best value",
                    "Bundle-friendly sets built for faster picks and clearer choices."
            );
        }

        return new CategoryStyle(
                color(context, R.color.orange_light),
                color(context, R.color.primary),
                color(context, R.color.primary_dark),
                color(context, R.color.decor_orange),
                R.drawable.ic_fresh_leaf,
                "Kitchen core",
                "Main dishes and signature plates with the strongest demand."
        );
    }

    public static final class CategoryStyle {
        @ColorInt
        public final int surfaceColor;
        @ColorInt
        public final int accentColor;
        @ColorInt
        public final int textAccentColor;
        @ColorInt
        public final int ornamentColor;
        @DrawableRes
        public final int iconResId;
        public final String eyebrow;
        public final String description;

        public CategoryStyle(
                @ColorInt int surfaceColor,
                @ColorInt int accentColor,
                @ColorInt int textAccentColor,
                @ColorInt int ornamentColor,
                @DrawableRes int iconResId,
                String eyebrow,
                String description
        ) {
            this.surfaceColor = surfaceColor;
            this.accentColor = accentColor;
            this.textAccentColor = textAccentColor;
            this.ornamentColor = ornamentColor;
            this.iconResId = iconResId;
            this.eyebrow = eyebrow;
            this.description = description;
        }
    }

    private static int color(Context context, int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
