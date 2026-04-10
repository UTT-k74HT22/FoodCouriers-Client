package com.utt.foodcouriers_client.data.model;

public class BannerItem {
    private final String title;
    private final String subtitle;

    public BannerItem(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }
}
