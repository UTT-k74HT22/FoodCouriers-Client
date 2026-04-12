package com.utt.foodcouriers_client.ui.cart.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.CartItem;
import com.utt.foodcouriers_client.data.model.CartRestaurantGroup;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.GroupViewHolder> {

    private static final RequestOptions IMAGE_REQUEST_OPTIONS = new RequestOptions()
            .placeholder(R.drawable.ic_food_bowl)
            .error(R.drawable.ic_food_bowl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop();

    private final Context context;
    private final CartItemListener listener;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final List<CartRestaurantGroup> groups = new ArrayList<>();
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

    public void submitGroups(List<CartRestaurantGroup> newGroups) {
        groups.clear();
        Set<String> availableIds = new LinkedHashSet<>();

        if (newGroups != null) {
            groups.addAll(newGroups);
            for (CartRestaurantGroup group : newGroups) {
                for (CartItem item : group.getItems()) {
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
        for (CartRestaurantGroup group : groups) {
            for (CartItem item : group.getItems()) {
                if (item.getId() != null && cartItemId.equals(item.getId())) {
                    return item.getQuantity();
                }
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

        for (CartRestaurantGroup group : groups) {
            boolean hasSelection = false;
            for (CartItem item : group.getItems()) {
                if (item.getId() != null && selectedCartItemIds.contains(item.getId())) {
                    hasSelection = true;
                    selectedItemCount += item.getQuantity();
                    selectedSubtotal += item.getQuantity() * item.getPrice();
                }
            }
            if (hasSelection) {
                selectedRestaurantCount += 1;
                selectedDeliveryFee += group.getDeliveryFee();
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
    public int getItemCount() {
        return groups.size();
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

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_cart_group_section, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.bind(groups.get(position));
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        private final View rootView;
        private final CheckBox cbSelectRestaurant;
        private final TextView tvRestaurantName;
        private final TextView tvRestaurantMeta;
        private final LinearLayout llItemsContainer;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = itemView;
            cbSelectRestaurant = itemView.findViewById(R.id.cb_select_restaurant);
            tvRestaurantName = itemView.findViewById(R.id.tv_restaurant_name);
            tvRestaurantMeta = itemView.findViewById(R.id.tv_restaurant_meta);
            llItemsContainer = itemView.findViewById(R.id.ll_items_container);
        }

        void bind(CartRestaurantGroup group) {
            if (group == null) {
                return;
            }

            tvRestaurantName.setText(group.getRestaurantName());
            tvRestaurantMeta.setText(
                    group.getItems().size() + " mon • Phi giao " + currencyFormatter.format(group.getDeliveryFee())
            );

            cbSelectRestaurant.setOnCheckedChangeListener(null);
            cbSelectRestaurant.setChecked(isRestaurantFullySelected(group));
            cbSelectRestaurant.setOnCheckedChangeListener((buttonView, isChecked) -> toggleRestaurant(group, isChecked));

            llItemsContainer.removeAllViews();
            for (CartItem item : group.getItems()) {
                View itemView = createItemView(item);
                llItemsContainer.addView(itemView);
            }
        }

        private View createItemView(CartItem item) {
            View itemView = LayoutInflater.from(context)
                    .inflate(R.layout.component_cart_item, llItemsContainer, false);

            ImageView ivFoodImage = itemView.findViewById(R.id.iv_food_image);
            TextView tvFoodName = itemView.findViewById(R.id.tv_food_name);
            TextView tvFoodPrice = itemView.findViewById(R.id.tv_food_price);
            TextView tvLineTotal = itemView.findViewById(R.id.tv_line_total);
            TextView tvQuantity = itemView.findViewById(R.id.tv_quantity);
            CheckBox cbSelectItem = itemView.findViewById(R.id.cb_select_item);
            TextView btnIncrease = itemView.findViewById(R.id.btn_increase);
            TextView btnDecrease = itemView.findViewById(R.id.btn_decrease);

            tvFoodName.setText(item.getName());
            tvFoodPrice.setText(currencyFormatter.format(item.getPrice()));
            tvLineTotal.setText(currencyFormatter.format(item.getPrice() * item.getQuantity()));
            tvQuantity.setText(String.valueOf(item.getQuantity()));

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(item.getImageUrl())
                        .apply(IMAGE_REQUEST_OPTIONS)
                        .into(ivFoodImage);
            } else {
                ivFoodImage.setImageResource(R.drawable.ic_food_bowl);
            }

            cbSelectItem.setOnCheckedChangeListener(null);
            cbSelectItem.setChecked(item.getId() != null && selectedCartItemIds.contains(item.getId()));
            cbSelectItem.setOnCheckedChangeListener((buttonView, isChecked) -> toggleItem(item, isChecked));

            btnIncrease.setOnClickListener(v -> listener.onIncrease(item.getId()));
            btnDecrease.setOnClickListener(v -> listener.onDecrease(item.getId()));

            return itemView;
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
