package com.utt.foodcouriers_client.ui.home.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.Target;
import com.utt.foodcouriers_client.R;
import com.utt.foodcouriers_client.data.model.BannerItem;

import java.util.ArrayList;
import java.util.List;

public class BannerAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private final List<BannerItem> banners = new ArrayList<>();
    private final OnBannerClickListener listener;
    private final Context context;

    public interface OnBannerClickListener {
        void onBannerClick(BannerItem banner);
    }

    public BannerAdapter(Context context, OnBannerClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setBanners(List<BannerItem> newBanners) {
        banners.clear();
        if (newBanners != null) {
            banners.addAll(newBanners);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        BannerItem banner = banners.get(position);
        holder.bind(banner);
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    class BannerViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {

        private final ImageView ivBannerImage;
        private final TextView tvBannerTitle;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBannerImage = itemView.findViewById(R.id.ivBannerImage);
            tvBannerTitle = itemView.findViewById(R.id.tvBannerTitle);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION && listener != null) {
                    listener.onBannerClick(banners.get(position));
                }
            });
        }

        void bind(BannerItem banner) {
            tvBannerTitle.setText(banner.getTitle());

            String imageUrl = banner.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.banner_placeholder)
                        .error(R.drawable.banner_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(ivBannerImage);
            } else {
                ivBannerImage.setImageResource(R.drawable.banner_placeholder);
            }
        }
    }

    public static class BannerPageChangeCallback extends ViewPager2.OnPageChangeCallback {

        private final ViewPager2 viewPager;
        private final ViewGroup indicatorContainer;
        private int previousPosition = 0;

        public BannerPageChangeCallback(ViewPager2 viewPager, ViewGroup indicatorContainer) {
            this.viewPager = viewPager;
            this.indicatorContainer = indicatorContainer;
        }

        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            updateIndicator(position);
            previousPosition = position;
        }

        private void updateIndicator(int selectedPosition) {
            if (indicatorContainer == null || indicatorContainer.getChildCount() == 0) {
                return;
            }

            for (int i = 0; i < indicatorContainer.getChildCount(); i++) {
                View dot = indicatorContainer.getChildAt(i);
                if (dot != null) {
                    dot.setSelected(i == selectedPosition);
                }
            }
        }

        public void setupIndicators(int count) {
            if (indicatorContainer == null) return;

            indicatorContainer.removeAllViews();
            if (count <= 1) {
                indicatorContainer.setVisibility(View.GONE);
                return;
            }

            indicatorContainer.setVisibility(View.VISIBLE);

            for (int i = 0; i < count; i++) {
                View dot = new View(indicatorContainer.getContext());
                int size = (int) (8 * indicatorContainer.getContext().getResources().getDisplayMetrics().density);
                int margin = (int) (4 * indicatorContainer.getContext().getResources().getDisplayMetrics().density);

                android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(size, size);
                params.setMargins(margin, 0, margin, 0);
                dot.setLayoutParams(params);

                dot.setBackgroundResource(R.drawable.banner_indicator_dot);
                dot.setSelected(i == 0);

                indicatorContainer.addView(dot);
            }
        }
    }
}