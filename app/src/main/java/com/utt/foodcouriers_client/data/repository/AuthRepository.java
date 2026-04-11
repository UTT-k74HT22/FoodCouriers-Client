package com.utt.foodcouriers_client.data.repository;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.UserProfile;
import com.utt.foodcouriers_client.data.remote.AuthClient;

public class AuthRepository {

    private static AuthRepository instance;
    private final AuthClient authClient;

    private AuthRepository() {
        authClient = AuthClient.getInstance();
    }

    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }

    public void login(String email, String password, RepositoryCallback<UserProfile> callback) {
        if (email == null || email.trim().isEmpty()) {
            callback.onError("Email is required");
            return;
        }
        if (password == null || password.isEmpty()) {
            callback.onError("Password is required");
            return;
        }

        authClient.signIn(email.trim(), password, new AuthClient.ApiCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void register(String fullName, String email, String phone, String password, RepositoryCallback<UserProfile> callback) {
        if (fullName == null || fullName.trim().isEmpty()) {
            callback.onError("Full name is required");
            return;
        }
        if (email == null || email.trim().isEmpty()) {
            callback.onError("Email is required");
            return;
        }
        if (password == null || password.isEmpty()) {
            callback.onError("Password is required");
            return;
        }
        if (password.length() < 6) {
            callback.onError("Password must be at least 6 characters");
            return;
        }

        authClient.signUp(email.trim(), password, fullName.trim(), phone != null ? phone.trim() : "", new AuthClient.ApiCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void resetPassword(String email, RepositoryCallback<Boolean> callback) {
        if (email == null || email.trim().isEmpty()) {
            callback.onError("Email is required");
            return;
        }

        authClient.resetPasswordForEmail(email.trim(), new AuthClient.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void logout(RepositoryCallback<Boolean> callback) {
        authClient.signOut(new AuthClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                callback.onSuccess(true);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getCurrentUser(RepositoryCallback<UserProfile> callback) {
        authClient.getCurrentUser(new AuthClient.ApiCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public boolean isAuthenticated() {
        return authClient.isAuthenticated();
    }
}
