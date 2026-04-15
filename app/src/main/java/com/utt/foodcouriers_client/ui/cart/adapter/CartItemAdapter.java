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
import androidx.recyclerview.widget.DiffUtil;
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
        if (newGroups == null || newGroups.isEmpty()) {
            boolean wasEmpty = groups.isEmpty();
            groups.clear();
            if (!wasEmpty) {
                notifyDataSetChanged();
            }
            selectedCartItemIds.clear();
            notifySelectionChanged();
            return;
        }

        groups.clear();
        groups.addAll(newGroups);

        Set<String> availableIds = new LinkedHashSet<>();
        for (CartRestaurantGroup group : groups) {
            for (CartItem item : group.getItems()) {
                if (item.getId() != null) {
                    availableIds.add(item.getId());
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

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= groups.size()) {
            return -1;
        }
        CartRestaurantGroup group = groups.get(position);
        return group.getRestaurantId() != null ? group.getRestaurantId().hashCode() : position;
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

    private void handleItemCheckboxClick(CartItem item, boolean isChecked) {
        if (item == null || item.getId() == null) {
            return;
        }

        if (isChecked) {
            selectedCartItemIds.add(item.getId());
        } else {
            selectedCartItemIds.remove(item.getId());
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    private CartRestaurantGroup findParentGroup(CartItem item) {
        if (item == null || item.getId() == null) {
            return null;
        }
        for (CartRestaurantGroup group : groups) {
            for (CartItem groupItem : group.getItems()) {
                if (item.getId().equals(groupItem.getId())) {
                    return group;
                }
            }
        }
        return null;
    }

    private void notifySelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(getSelectionState());
        }
    }

    private int getRestaurantSelectionState(CartRestaurantGroup group) {
        if (group == null || group.getItems().isEmpty()) {
            return 0;
        }
        int selectedCount = 0;
        int totalCount = 0;
        for (CartItem item : group.getItems()) {
            if (item.getId() != null) {
                totalCount++;
                if (selectedCartItemIds.contains(item.getId())) {
                    selectedCount++;
                }
            }
        }
        if (selectedCount == 0) {
            return 0;
        } else if (selectedCount == totalCount) {
            return 2;
        } else {
            return 1;
        }
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
        private final CheckBox cbSelectRestaurant;
        private final TextView tvRestaurantName;
        private final TextView tvRestaurantMeta;
        private final LinearLayout llItemsContainer;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
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

            int selectionState = getRestaurantSelectionState(group);

            cbSelectRestaurant.setOnCheckedChangeListener(null);
            cbSelectRestaurant.setChecked(selectionState != 0);
            cbSelectRestaurant.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    toggleRestaurant(group, true);
                } else {
                    toggleRestaurant(group, false);
                }
            });

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
                ivFoodImage.setImageTintList(null);
                Glide.with(context)
                        .load(item.getImageUrl())
                        .apply(IMAGE_REQUEST_OPTIONS)
                        .into(ivFoodImage);
            } else {
                ivFoodImage.setImageResource(R.drawable.ic_food_bowl);
                ivFoodImage.setImageTintList(android.content.res.ColorStateList.valueOf(context.getColor(R.color.primary_dark)));
            }

            boolean isSelected = item.getId() != null && selectedCartItemIds.contains(item.getId());
            cbSelectItem.setOnCheckedChangeListener(null);
            cbSelectItem.setChecked(isSelected);
            cbSelectItem.setOnCheckedChangeListener((buttonView, isChecked) -> handleItemCheckboxClick(item, isChecked));

            btnIncrease.setOnClickListener(v -> listener.onIncrease(item.getId()));
            btnDecrease.setOnClickListener(v -> listener.onDecrease(item.getId()));

            return itemView;
        }
    }

    static class GroupDiffCallback extends DiffUtil.Callback {
        private final List<CartRestaurantGroup> oldList;
        private final List<CartRestaurantGroup> newList;

        GroupDiffCallback(List<CartRestaurantGroup> oldList, List<CartRestaurantGroup> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            if (oldItemPosition >= oldList.size() || newItemPosition >= newList.size()) {
                return false;
            }
            String oldId = oldList.get(oldItemPosition).getRestaurantId();
            String newId = newList.get(newItemPosition).getRestaurantId();
            if (oldId == null || newId == null) {
                return oldItemPosition == newItemPosition;
            }
            return oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            if (oldItemPosition >= oldList.size() || newItemPosition >= newList.size()) {
                return false;
            }
            CartRestaurantGroup oldGroup = oldList.get(oldItemPosition);
            CartRestaurantGroup newGroup = newList.get(newItemPosition);
            return oldGroup.getRestaurantId().equals(newGroup.getRestaurantId())
                    && oldGroup.getItems().size() == newGroup.getItems().size();
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