package com.utt.foodcouriers_client;

import android.app.Application;

import com.utt.foodcouriers_client.data.remote.AuthClient;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;

public class FoodCouriersClientApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ToastBanner.init(this);

        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn()) {
            AuthClient.getInstance().setSession(
                    sessionManager.getAccessToken(),
                    sessionManager.getRefreshToken()
            );
        }
    }
}
