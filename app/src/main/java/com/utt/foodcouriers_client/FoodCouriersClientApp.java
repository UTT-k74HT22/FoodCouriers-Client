package com.utt.foodcouriers_client;

import android.app.Application;
import android.util.Log;

import com.utt.foodcouriers_client.data.remote.AuthClient;
import com.utt.foodcouriers_client.utils.SessionManager;
import com.utt.foodcouriers_client.utils.ToastBanner;

public class FoodCouriersClientApp extends Application {

    private static final String TAG = "FoodCouriersApp";

    @Override
    public void onCreate() {
        super.onCreate();
        ToastBanner.init(this);

        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn()) {
            if (sessionManager.isTokenExpired()) {
                Log.d(TAG, "Token expired, attempting refresh...");
                if (sessionManager.hasRefreshToken()) {
                    AuthClient.getInstance().setSession(
                            sessionManager.getAccessToken(),
                            sessionManager.getRefreshToken()
                    );
                    AuthClient.getInstance().refreshSession(new AuthClient.ApiCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            Log.d(TAG, "Token refresh successful");
                            SessionManager.getInstance(FoodCouriersClientApp.this).updateSession(
                                    AuthClient.getInstance().getAccessToken(),
                                    AuthClient.getInstance().getRefreshToken()
                            );
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Token refresh failed: " + error);
                            AuthClient.getInstance().clearSession();
                            sessionManager.clearSession();
                        }
                    });
                } else {
                    Log.d(TAG, "No refresh token, clearing session");
                    AuthClient.getInstance().clearSession();
                    sessionManager.clearSession();
                }
            } else {
                AuthClient.getInstance().setSession(
                        sessionManager.getAccessToken(),
                        sessionManager.getRefreshToken()
                );
            }
        }
    }
}
