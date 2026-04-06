package com.utt.foodcouriers_client.data.remote;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.utt.foodcouriers_client.BuildConfig;
import com.utt.foodcouriers_client.data.remote.dto.LoginRequest;
import com.utt.foodcouriers_client.data.remote.dto.LoginResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthRemoteDataSource {
    public interface LoginCallback {
        void onComplete(BaseResponse<LoginResponse> result);
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final Gson gson;

    public AuthRemoteDataSource() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public void login(String email, String password, LoginCallback callback) {
        String url = BuildConfig.SUPABASE_URL + "/auth/v1/token?grant_type=password";
        LoginRequest requestBody = new LoginRequest(email, password);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(gson.toJson(requestBody), JSON))
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onComplete(new BaseResponse.Error<>("Khong the ket noi toi may chu", -500));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response safeResponse = response) {
                    String responseBody = safeResponse.body() != null ? safeResponse.body().string() : "";

                    if (!safeResponse.isSuccessful()) {
                        callback.onComplete(new BaseResponse.Error<>(
                                parseErrorMessage(responseBody),
                                safeResponse.code()
                        ));
                        return;
                    }

                    LoginResponse loginResponse = gson.fromJson(responseBody, LoginResponse.class);
                    if (loginResponse == null
                            || loginResponse.getAccessToken() == null
                            || loginResponse.getRefreshToken() == null) {
                        callback.onComplete(new BaseResponse.Error<>("Phan hoi dang nhap khong hop le", -1));
                        return;
                    }

                    callback.onComplete(new BaseResponse.Success<>(loginResponse));
                }
            }
        });
    }

    private String parseErrorMessage(String responseBody) {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json != null) {
                if (json.has("error_description")) {
                    return json.get("error_description").getAsString();
                }
                if (json.has("msg")) {
                    return json.get("msg").getAsString();
                }
                if (json.has("message")) {
                    return json.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
        }

        return "Sai email hoac mat khau";
    }
}
