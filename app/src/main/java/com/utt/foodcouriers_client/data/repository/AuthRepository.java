package com.utt.foodcouriers_client.data.repository;

import android.content.Context;

import com.utt.foodcouriers_client.data.remote.AuthRemoteDataSource;
import com.utt.foodcouriers_client.data.remote.AuthSession;
import com.utt.foodcouriers_client.data.remote.BaseResponse;
import com.utt.foodcouriers_client.data.remote.dto.LoginResponse;
import com.utt.foodcouriers_client.utils.SessionManager;

public class AuthRepository {
    public interface AuthResultCallback {
        void onComplete(BaseResponse<AuthSession> result);
    }

    private final AuthRemoteDataSource remoteDataSource;
    private final SessionManager sessionManager;

    public AuthRepository(Context context) {
        this.remoteDataSource = new AuthRemoteDataSource();
        this.sessionManager = new SessionManager(context.getApplicationContext());
    }

    public void login(String email, String password, AuthResultCallback callback) {
        remoteDataSource.login(email, password, result -> {
            if (result instanceof BaseResponse.Error) {
                BaseResponse.Error<LoginResponse> error = (BaseResponse.Error<LoginResponse>) result;
                callback.onComplete(new BaseResponse.Error<>(error.getMessage(), error.getCode()));
                return;
            }

            LoginResponse response = ((BaseResponse.Success<LoginResponse>) result).getData();
            long expiresIn = response.getExpiresIn() != null ? response.getExpiresIn() : 0L;
            long expiresAt = System.currentTimeMillis() + (expiresIn * 1000L);

            AuthSession session = new AuthSession(
                    response.getAccessToken(),
                    response.getRefreshToken(),
                    expiresAt,
                    response.getUser() != null ? response.getUser().getId() : null,
                    response.getUser() != null ? response.getUser().getEmail() : email
            );

            sessionManager.saveSession(session);
            callback.onComplete(new BaseResponse.Success<>(session));
        });
    }

    public AuthSession getCurrentSession() {
        return sessionManager.getSession();
    }

    public boolean isLoggedIn() {
        return sessionManager.hasValidSession();
    }

    public void logout() {
        sessionManager.clearSession();
    }
}
