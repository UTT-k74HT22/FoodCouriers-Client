package com.utt.foodcouriers_client.data.remote;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.utt.foodcouriers_client.data.model.UserProfile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AuthClient extends BaseSupabaseClient {

    private static final String CUSTOMER_ROLE = "customer";
    private static AuthClient instance;

    private AuthClient() {
        super();
    }

    public static synchronized AuthClient getInstance() {
        if (instance == null) {
            instance = new AuthClient();
        }
        return instance;
    }

    public void signIn(String email, String password, ApiCallback<UserProfile> callback) {
        Log.d(TAG, "signIn called: email=" + email);
        
        if (!SupabaseConfig.isConfigured()) {
            Log.e(TAG, "signIn: Supabase is not configured");
            postError(callback, "Supabase is not configured");
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/token?grant_type=password")
                .post(RequestBody.create(gson.toJson(body), MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
                .build();

        Log.d(TAG, "signIn request URL: " + request.url());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                Log.e(TAG, "signIn failure", exception);
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "signIn response code: " + response.code());
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    Log.d(TAG, "signIn response body: " + json);
                    if (!response.isSuccessful()) {
                        postError(callback, parseAuthError(json));
                        return;
                    }

                    AuthResponse authResponse = gson.fromJson(json, AuthResponse.class);
                    if (authResponse == null || authResponse.getAccessToken() == null || authResponse.getUser() == null) {
                        postError(callback, "Invalid response from server");
                        return;
                    }

                    Log.d(TAG, "signIn success - setting session, userId: " + authResponse.getUser().getId());
                    setSession(authResponse.getAccessToken(), authResponse.getRefreshToken());
                    fetchUserProfile(authResponse.getUser().getId(), callback);
                }
            }
        });
    }

    public void signUp(String email, String password, String fullName, String phone, ApiCallback<UserProfile> callback) {
        if (!SupabaseConfig.isConfigured()) {
            postError(callback, "Supabase is not configured");
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("full_name", fullName);
        metadata.put("phone", phone != null ? phone : "");
        metadata.put("role", CUSTOMER_ROLE);

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("data", metadata);

        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/signup")
                .post(RequestBody.create(gson.toJson(body), MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                Log.e(TAG, "signUp failure", exception);
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (!response.isSuccessful()) {
                        postError(callback, parseAuthError(json));
                        return;
                    }

                    AuthResponse authResponse = gson.fromJson(json, AuthResponse.class);
                    if (authResponse == null || authResponse.getAccessToken() == null || authResponse.getUser() == null) {
                        postError(callback, "Invalid response from server");
                        return;
                    }

                    setSession(authResponse.getAccessToken(), authResponse.getRefreshToken());
                    String authUserId = authResponse.getUser().getId();
                    fetchUserProfile(authUserId, new ApiCallback<UserProfile>() {
                        @Override
                            public void onSuccess(UserProfile result) {
                            postSuccess(callback, result);
                        }

                        @Override
                        public void onError(String error) {
                            createUserProfile(authUserId, fullName, phone, email, null, callback);
                        }
                    });
                }
            }
        });
    }

    public void resetPasswordForEmail(String email, ApiCallback<Boolean> callback) {
        if (!SupabaseConfig.isConfigured()) {
            postError(callback, "Supabase is not configured");
            return;
        }

        if (email == null || email.trim().isEmpty()) {
            postError(callback, "Email is required");
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("email", email.trim());

        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/recover")
                .post(RequestBody.create(gson.toJson(body), MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
                .build();

        Log.d(TAG, "resetPasswordForEmail request: " + request.url() + " email=" + email);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                Log.e(TAG, "resetPasswordForEmail failure", exception);
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    Log.d(TAG, "resetPasswordForEmail response code=" + response.code() + " body=" + json);
                    if (response.isSuccessful()) {
                        postSuccess(callback, true);
                    } else {
                        postError(callback, parseAuthError(json));
                    }
                }
            }
        });
    }

    public void signOut(ApiCallback<Void> callback) {
        if (!isAuthenticated()) {
            clearSession();
            postSuccess(callback, null);
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/logout")
                .post(RequestBody.create("", MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                clearSession();
                postSuccess(callback, null);
            }

            @Override
            public void onResponse(Call call, Response response) {
                clearSession();
                postSuccess(callback, null);
            }
        });
    }

    public void bootstrapSocialSession(String accessToken, String refreshToken, ApiCallback<UserProfile> callback) {
        if (isNullOrBlank(accessToken) || isNullOrBlank(refreshToken)) {
            postError(callback, "Missing social auth session tokens");
            return;
        }

        setSession(accessToken, refreshToken);
        fetchCurrentUserProfile(true, callback);
    }

    public void getCurrentUser(ApiCallback<UserProfile> callback) {
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }

        fetchCurrentUserProfile(false, callback);
    }

    public void refreshSession(ApiCallback<Boolean> callback) {
        if (isNullOrBlank(refreshToken)) {
            postError(callback, "Missing refresh token");
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("refresh_token", refreshToken);

        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/token?grant_type=refresh_token")
                .post(RequestBody.create(gson.toJson(body), MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
                .build();

        Log.d(TAG, "refreshSession: requesting new access token");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                Log.e(TAG, "refreshSession failure", exception);
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    Log.d(TAG, "refreshSession response code=" + response.code());
                    if (!response.isSuccessful()) {
                        postError(callback, parseAuthError(json));
                        return;
                    }

                    AuthResponse authResponse = gson.fromJson(json, AuthResponse.class);
                    if (authResponse == null || isNullOrBlank(authResponse.getAccessToken())) {
                        postError(callback, "Invalid refresh response");
                        return;
                    }

                    setSession(
                            authResponse.getAccessToken(),
                            authResponse.getRefreshToken() != null ? authResponse.getRefreshToken() : refreshToken
                    );
                    postSuccess(callback, true);
                }
            }
        });
    }

    private void fetchCurrentUserProfile(boolean createMissingProfile, ApiCallback<UserProfile> callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.AUTH_URL + "/user")
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (!response.isSuccessful()) {
                        postError(callback, parseRestError("Failed to get user", response.code(), json));
                        return;
                    }

                    AuthUser authUser = gson.fromJson(json, AuthUser.class);
                    if (authUser == null || authUser.getId() == null) {
                        postError(callback, "Failed to parse user details");
                        return;
                    }

                    Log.d(
                            TAG,
                            "Step 5: Loaded auth user from Supabase | authId=" + authUser.getId()
                                    + ", email=" + safeValue(authUser.resolveEmail())
                                    + ", fullName=" + safeValue(authUser.resolveDisplayName())
                                    + ", avatarUrl=" + safeValue(authUser.resolveAvatarUrl())
                    );

                    fetchUserProfile(authUser.getId(), new ApiCallback<UserProfile>() {
                        @Override
                        public void onSuccess(UserProfile result) {
                            maybeBackfillSocialAvatar(authUser, result, callback);
                        }

                        @Override
                        public void onError(String error) {
                            if (!createMissingProfile || !isProfileMissingError(error)) {
                                postError(callback, error);
                                return;
                            }

                            createUserProfile(
                                    authUser.getId(),
                                    authUser.resolveDisplayName(),
                                    authUser.resolvePhone(),
                                    authUser.resolveEmail(),
                                    authUser.resolveAvatarUrl(),
                                    callback
                            );
                        }
                    });
                }
            }
        });
    }

    private void fetchUserProfile(String authId, ApiCallback<UserProfile> callback) {
        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/users?auth_id=eq." + authId + "&select=*")
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (!response.isSuccessful()) {
                        postError(callback, parseRestError("Failed to fetch profile", response.code(), json));
                        return;
                    }

                    UserProfile[] profiles = gson.fromJson(json, UserProfile[].class);
                    if (profiles == null || profiles.length == 0) {
                        postError(callback, "User profile not found");
                        return;
                    }

                    postSuccess(callback, profiles[0]);
                }
            }
        });
    }

    private void createUserProfile(
            String authId,
            String fullName,
            String phone,
            String email,
            String avatarUrl,
            ApiCallback<UserProfile> callback
    ) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("auth_id", authId);
        profile.put("full_name", fullName);
        profile.put("phone", phone);
        profile.put("email", email);
        if (!isNullOrBlank(avatarUrl)) {
            profile.put("avatar_url", avatarUrl);
        }
        profile.put("role", CUSTOMER_ROLE);
        profile.put("is_active", true);

        Log.d(
                TAG,
                "Step 6: Creating missing public.users profile | authId=" + safeValue(authId)
                        + ", email=" + safeValue(email)
                        + ", fullName=" + safeValue(fullName)
                        + ", phone=" + safeValue(phone)
                        + ", avatarUrl=" + safeValue(avatarUrl)
        );

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/users")
                .post(RequestBody.create(gson.toJson(profile), MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_PREFER, SupabaseConfig.PREF_RETURN_REPRESENTATION)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Step 6: Failed to create public.users profile | code=" + response.code() + ", body=" + json);
                        postError(callback, parseRestError("Failed to create profile", response.code(), json));
                        return;
                    }

                    UserProfile[] profiles = gson.fromJson(json, UserProfile[].class);
                    if (profiles == null || profiles.length == 0) {
                        postError(callback, "Failed to create profile");
                        return;
                    }

                    Log.d(
                            TAG,
                            "Step 6: Created public.users profile successfully | userId=" + safeValue(profiles[0].getId())
                                    + ", avatarUrl=" + safeValue(profiles[0].getAvatarUrl())
                    );
                    postSuccess(callback, profiles[0]);
                }
            }
        });
    }

    private void maybeBackfillSocialAvatar(
            AuthUser authUser,
            UserProfile profile,
            ApiCallback<UserProfile> callback
    ) {
        String authAvatarUrl = authUser.resolveAvatarUrl();
        if (profile == null) {
            postError(callback, "User profile is missing");
            return;
        }

        if (isNullOrBlank(profile.getAvatarUrl()) && !isNullOrBlank(authAvatarUrl)) {
            Log.d(
                    TAG,
                    "Step 6: Backfilling avatar from auth metadata | userId=" + safeValue(profile.getId())
                            + ", avatarUrl=" + safeValue(authAvatarUrl)
            );

            updateProfile(profile.getId(), null, null, authAvatarUrl, new ApiCallback<UserProfile>() {
                @Override
                public void onSuccess(UserProfile updatedProfile) {
                    Log.d(
                            TAG,
                            "Step 6: Avatar backfill completed | userId=" + safeValue(updatedProfile.getId())
                                    + ", avatarUrl=" + safeValue(updatedProfile.getAvatarUrl())
                    );
                    postSuccess(callback, updatedProfile);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Step 6: Avatar backfill failed | error=" + error);
                    postError(callback, error);
                }
            });
            return;
        }

        postSuccess(callback, profile);
    }

    private boolean isProfileMissingError(String error) {
        return error != null && error.toLowerCase().contains("profile not found");
    }

    private static boolean isNullOrBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public void updateProfile(String userDbId, String fullName, String phone, String avatarUrl, ApiCallback<UserProfile> callback) {
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }

        Map<String, Object> profile = new HashMap<>();
        if (fullName != null) profile.put("full_name", fullName);
        if (phone != null) profile.put("phone", phone);
        if (avatarUrl != null && !avatarUrl.isEmpty()) profile.put("avatar_url", avatarUrl);

        if (profile.isEmpty()) {
            postError(callback, "No data to update");
            return;
        }

        Log.d(TAG, "updateProfile payload: " + profile);

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/users?id=eq." + userDbId)
                .patch(RequestBody.create(gson.toJson(profile), MediaType.parse(SupabaseConfig.CONTENT_TYPE_JSON)))
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .addHeader(SupabaseConfig.HEADER_PREFER, "return=representation")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (!response.isSuccessful()) {
                        postError(callback, parseRestError("Failed to update profile", response.code(), json));
                        return;
                    }

                    UserProfile[] profiles = gson.fromJson(json, UserProfile[].class);
                    if (profiles == null || profiles.length == 0) {
                        postError(callback, "Failed to update profile");
                        return;
                    }

                    postSuccess(callback, profiles[0]);
                }
            }
        });
    }

    public void getUserById(String userDbId, ApiCallback<UserProfile> callback) {
        if (!isAuthenticated()) {
            postError(callback, "Not authenticated");
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "/users?id=eq." + userDbId + "&select=*")
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException exception) {
                postError(callback, "Network error: " + exception.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String json = responseBody != null ? responseBody.string() : "";
                    if (!response.isSuccessful()) {
                        postError(callback, parseRestError("Failed to get user", response.code(), json));
                        return;
                    }

                    UserProfile[] profiles = gson.fromJson(json, UserProfile[].class);
                    if (profiles == null || profiles.length == 0) {
                        postError(callback, "User not found");
                        return;
                    }

                    postSuccess(callback, profiles[0]);
                }
            }
        });
    }

    private static class AuthResponse {
        @SerializedName("access_token")
        private String accessToken;

        @SerializedName("refresh_token")
        private String refreshToken;

        private AuthUser user;

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public AuthUser getUser() {
            return user;
        }
    }

    private static class AuthUser {
        private String id;
        private String email;
        @SerializedName("phone")
        private String phone;
        @SerializedName("user_metadata")
        private UserMetadata userMetadata;

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String resolveEmail() {
            if (!isNullOrBlank(email)) {
                return email;
            }
            return userMetadata != null ? userMetadata.email : null;
        }

        public String resolvePhone() {
            if (!isNullOrBlank(phone)) {
                return phone;
            }
            return userMetadata != null ? userMetadata.phone : null;
        }

        public String resolveDisplayName() {
            if (userMetadata != null) {
                if (!isNullOrBlank(userMetadata.fullName)) {
                    return userMetadata.fullName;
                }
                if (!isNullOrBlank(userMetadata.name)) {
                    return userMetadata.name;
                }
            }

            String resolvedEmail = resolveEmail();
            if (!isNullOrBlank(resolvedEmail) && resolvedEmail.contains("@")) {
                return resolvedEmail.substring(0, resolvedEmail.indexOf('@'));
            }

            return "Customer";
        }

        public String resolveAvatarUrl() {
            return userMetadata != null ? userMetadata.avatarUrl : null;
        }
    }

    private static class UserMetadata {
        @SerializedName("full_name")
        private String fullName;
        @SerializedName("name")
        private String name;
        @SerializedName("phone")
        private String phone;
        @SerializedName("email")
        private String email;
        @SerializedName("avatar_url")
        private String avatarUrl;
    }

    private String safeValue(String value) {
        return !isNullOrBlank(value) ? value : "n/a";
    }
}
