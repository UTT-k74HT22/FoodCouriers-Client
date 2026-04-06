package com.utt.foodcouriers_client.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utt.foodcouriers_client.R;

import java.util.List;

class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder> {
    private final List<OnboardingPage> pages;

    OnboardingPagerAdapter(List<OnboardingPage> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        OnboardingPage page = pages.get(position);
        holder.eyebrow.setText(page.eyebrowResId);
        holder.title.setText(page.titleResId);
        holder.subtitle.setText(page.subtitleResId);
        holder.chipOne.setText(page.chipOneResId);
        holder.chipTwo.setText(page.chipTwoResId);
        holder.chipThree.setText(page.chipThreeResId);
        holder.mainIcon.setImageResource(page.mainIconResId);
        holder.topAccentIcon.setImageResource(page.accentTopResId);
        holder.bottomAccentIcon.setImageResource(page.accentBottomResId);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        final TextView eyebrow;
        final TextView title;
        final TextView subtitle;
        final TextView chipOne;
        final TextView chipTwo;
        final TextView chipThree;
        final ImageView mainIcon;
        final ImageView topAccentIcon;
        final ImageView bottomAccentIcon;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            eyebrow = itemView.findViewById(R.id.tvOnboardingEyebrow);
            title = itemView.findViewById(R.id.tvOnboardingTitle);
            subtitle = itemView.findViewById(R.id.tvOnboardingSubtitle);
            chipOne = itemView.findViewById(R.id.tvChipOne);
            chipTwo = itemView.findViewById(R.id.tvChipTwo);
            chipThree = itemView.findViewById(R.id.tvChipThree);
            mainIcon = itemView.findViewById(R.id.ivMainIllustration);
            topAccentIcon = itemView.findViewById(R.id.ivTopAccent);
            bottomAccentIcon = itemView.findViewById(R.id.ivBottomAccent);
        }
    }
}
