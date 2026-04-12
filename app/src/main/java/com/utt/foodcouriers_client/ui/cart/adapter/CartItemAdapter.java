package com.utt.foodcouriers_client.ui.cart.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
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
import com.utt.foodcouriers_client.data.model.CartRestaurantGroup;
import com.utt.foodcouriers_client.databinding.ComponentCartItemBinding;
import com.utt.foodcouriers_client.databinding.ComponentCartRestaurantGroupBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CartItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_RESTAURANT = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private static final RequestOptions IMAGE_REQUEST_OPTIONS = new RequestOptions()
            .placeholder(R.drawable.ic_food_bowl)
            .error(R.drawable.ic_food_bowl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop();

    private final Context context;
    private final CartItemListener listener;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final List<Row> rows = new ArrayList<>();
    private final Set<String> selectedCartItemIds = new LinkedHashSet<>();

    public interface CartItemListener {
        void onIncrease(String cartItemId);

        void onDecrease(String cartItemId);

        void onSelectionChanged(SelectionState selectionState);
    }

    public CartItemAdapter(Context context, CartItemListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void submitGroups(List<CartRestaurantGroup> groups) {
        rows.clear();
        Set<String> availableIds = new LinkedHashSet<>();

        if (groups != null) {
            for (CartRestaurantGroup group : groups) {
                rows.add(Row.forRestaurant(group));
                for (CartItem item : group.getItems()) {
                    rows.add(Row.forItem(group, item));
                    if (item.getId() != null) {
                        availableIds.add(item.getId());
                    }
                }
            }
        }

        if (selectedCartItemIds.isEmpty()) {
            selectedCartItemIds.addAll(availableIds);
        } else {
            selectedCartItemIds.retainAll(availableIds);
            if (selectedCartItemIds.isEmpty()) {
                selectedCartItemIds.addAll(availableIds);
            }
        }

        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public int getQuantityForItem(String cartItemId) {
        for (Row row : rows) {
            if (row.type == VIEW_TYPE_ITEM && row.cartItem != null && cartItemId.equals(row.cartItem.getId())) {
                return row.cartItem.getQuantity();
            }
        }
        return 0;
    }

    public SelectionState getSelectionState() {
        int selectedItemCount = 0;
        int selectedSubtotal = 0;
        int selectedDeliveryFee = 0;
        int selectedRestaurantCount = 0;
        List<String> selectedIds = new ArrayList<>(selectedCartItemIds);

        for (Row row : rows) {
            if (row.type != VIEW_TYPE_RESTAURANT || row.group == null) {
                continue;
            }
            boolean hasSelection = false;
            for (CartItem item : row.group.getItems()) {
                if (item.getId() != null && selectedCartItemIds.contains(item.getId())) {
                    hasSelection = true;
                    selectedItemCount += item.getQuantity();
                    selectedSubtotal += item.getQuantity() * item.getPrice();
                }
            }
            if (hasSelection) {
                selectedRestaurantCount += 1;
                selectedDeliveryFee += row.group.getDeliveryFee();
            }
        }

        return new SelectionState(
                selectedIds,
                selectedItemCount,
                selectedRestaurantCount,
                selectedSubtotal,
                selectedDeliveryFee,
                selectedSubtotal + selectedDeliveryFee
        );
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_RESTAURANT) {
            ComponentCartRestaurantGroupBinding binding = ComponentCartRestaurantGroupBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new RestaurantGroupViewHolder(binding);
        }

        ComponentCartItemBinding binding = ComponentCartItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new CartItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof RestaurantGroupViewHolder) {
            ((RestaurantGroupViewHolder) holder).bind(row.group);
        } else if (holder instanceof CartItemViewHolder) {
            ((CartItemViewHolder) holder).bind(row.cartItem);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private void toggleRestaurant(CartRestaurantGroup group, boolean checked) {
        if (group == null) {
            return;
        }
        for (CartItem item : group.getItems()) {
            if (item.getId() == null) {
                continue;
            }
            if (checked) {
                selectedCartItemIds.add(item.getId());
            } else {
                selectedCartItemIds.remove(item.getId());
            }
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    private void toggleItem(CartItem item, boolean checked) {
        if (item == null || item.getId() == null) {
            return;
        }
        if (checked) {
            selectedCartItemIds.add(item.getId());
        } else {
            selectedCartItemIds.remove(item.getId());
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(getSelectionState());
        }
    }

    private boolean isRestaurantFullySelected(CartRestaurantGroup group) {
        if (group == null || group.getItems().isEmpty()) {
            return false;
        }
        for (CartItem item : group.getItems()) {
            if (item.getId() == null || !selectedCartItemIds.contains(item.getId())) {
                return false;
            }
        }
        return true;
    }

    private boolean isRestaurantPartiallySelected(CartRestaurantGroup group) {
        if (group == null || group.getItems().isEmpty()) {
            return false;
        }
        boolean hasSelected = false;
        boolean hasUnselected = false;
        for (CartItem item : group.getItems()) {
            boolean selected = item.getId() != null && selectedCartItemIds.contains(item.getId());
            hasSelected |= selected;
            hasUnselected |= !selected;
        }
        return hasSelected && hasUnselected;
    }

    private class RestaurantGroupViewHolder extends RecyclerView.ViewHolder {
        private final ComponentCartRestaurantGroupBinding binding;

        private RestaurantGroupViewHolder(ComponentCartRestaurantGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(CartRestaurantGroup group) {
            if (group == null) {
                return;
            }
            binding.tvRestaurantName.setText(group.getRestaurantName());
            binding.tvRestaurantMeta.setText(
                    group.getItems().size() + " mon • Phi giao " + currencyFormatter.format(group.getDeliveryFee())
            );

            binding.cbSelectRestaurant.setOnCheckedChangeListener(null);
            binding.cbSelectRestaurant.setChecked(isRestaurantFullySelected(group));
            binding.cbSelectRestaurant.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary)));
            binding.cbSelectRestaurant.setEnabled(true);
            binding.cbSelectRestaurant.setOnCheckedChangeListener((buttonView, isChecked) -> toggleRestaurant(group, isChecked));
            binding.cbSelectRestaurant.setAlpha(isRestaurantPartiallySelected(group) ? 0.7f : 1f);

            binding.getRoot().setOnClickListener(v -> toggleRestaurant(group, !isRestaurantFullySelected(group)));
        }
    }

    private class CartItemViewHolder extends RecyclerView.ViewHolder {
        private final ComponentCartItemBinding binding;

        private CartItemViewHolder(ComponentCartItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(CartItem item) {
            if (item == null) {
                return;
            }
            binding.tvFoodName.setText(item.getName());
            binding.tvFoodPrice.setText(currencyFormatter.format(item.getPrice()));
            binding.tvLineTotal.setText(currencyFormatter.format(item.getPrice() * item.getQuantity()));
            binding.tvQuantity.setText(String.valueOf(item.getQuantity()));
            binding.tvFoodNote.setVisibility(item.getNote() == null || item.getNote().trim().isEmpty() ? View.GONE : View.VISIBLE);
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

            binding.cbSelectItem.setOnCheckedChangeListener(null);
            binding.cbSelectItem.setChecked(item.getId() != null && selectedCartItemIds.contains(item.getId()));
            binding.cbSelectItem.setOnCheckedChangeListener((buttonView, isChecked) -> toggleItem(item, isChecked));
            binding.btnIncrease.setOnClickListener(v -> listener.onIncrease(item.getId()));
            binding.btnDecrease.setOnClickListener(v -> listener.onDecrease(item.getId()));
            binding.getRoot().setOnClickListener(v -> toggleItem(item, !(item.getId() != null && selectedCartItemIds.contains(item.getId()))));
        }
    }

    private static class Row {
        private final int type;
        private final CartRestaurantGroup group;
        private final CartItem cartItem;

        private Row(int type, CartRestaurantGroup group, CartItem cartItem) {
            this.type = type;
            this.group = group;
            this.cartItem = cartItem;
        }

        private static Row forRestaurant(CartRestaurantGroup group) {
            return new Row(VIEW_TYPE_RESTAURANT, group, null);
        }

        private static Row forItem(CartRestaurantGroup group, CartItem item) {
            return new Row(VIEW_TYPE_ITEM, group, item);
        }
    }

    public static class SelectionState {
        private final List<String> selectedCartItemIds;
        private final int selectedItemCount;
        private final int selectedRestaurantCount;
        private final int subtotal;
        private final int deliveryFee;
        private final int total;

        public SelectionState(List<String> selectedCartItemIds, int selectedItemCount, int selectedRestaurantCount, int subtotal, int deliveryFee, int total) {
            this.selectedCartItemIds = selectedCartItemIds;
            this.selectedItemCount = selectedItemCount;
            this.selectedRestaurantCount = selectedRestaurantCount;
            this.subtotal = subtotal;
            this.deliveryFee = deliveryFee;
            this.total = total;
        }

        public List<String> getSelectedCartItemIds() {
            return selectedCartItemIds;
        }

        public int getSelectedItemCount() {
            return selectedItemCount;
        }

        public int getSelectedRestaurantCount() {
            return selectedRestaurantCount;
        }

        public int getSubtotal() {
            return subtotal;
        }

        public int getDeliveryFee() {
            return deliveryFee;
        }

        public int getTotal() {
            return total;
        }
    }
}
