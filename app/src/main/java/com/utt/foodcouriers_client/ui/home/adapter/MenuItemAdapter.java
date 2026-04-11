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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.MenuItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {

    private static final RequestOptions MENU_ITEM_REQUEST_OPTIONS = new RequestOptions()
            .placeholder(R.drawable.ic_food_bowl)
            .error(R.drawable.ic_food_bowl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop();

    private List<MenuItem> items = new ArrayList<>();
    private OnMenuItemClickListener listener;
    private final NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final Context context;

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MenuItem item);
    }

    public MenuItemAdapter(Context context, OnMenuItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<MenuItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void filterByCategory(String categoryId) {
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivMenuImage;
        private final TextView tvMenuName;
        private final TextView tvMenuPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMenuImage = itemView.findViewById(R.id.ivMenuImage);
            tvMenuName = itemView.findViewById(R.id.tvMenuName);
            tvMenuPrice = itemView.findViewById(R.id.tvMenuPrice);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMenuItemClick(items.get(pos));
                }
            });
        }

        public void bind(MenuItem item) {
            tvMenuName.setText(item.getName());
            tvMenuPrice.setText(formatter.format(item.getPrice()));
            ivMenuImage.setContentDescription(context.getString(R.string.food_image_description, item.getName()));

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .apply(MENU_ITEM_REQUEST_OPTIONS)
                        .into(ivMenuImage);
            } else {
                ivMenuImage.setImageResource(R.drawable.ic_food_bowl);
            }
        }
    }
}