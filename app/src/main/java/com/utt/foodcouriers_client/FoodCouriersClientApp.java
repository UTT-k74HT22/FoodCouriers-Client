package com.utt.foodcouriers_client;

import android.app.Application;

import com.utt.foodcouriers_client.utils.ToastBanner;

public class FoodCouriersClientApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ToastBanner.init(this);
    }
}
