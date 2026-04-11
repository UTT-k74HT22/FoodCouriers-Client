package com.utt.foodcouriers_client.data.remote;

import com.utt.foodcouriers_client.data.model.BannerItem;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class BannerClient extends BaseSupabaseClient {

    private static final String TAG = "BannerClient";
    private static final String TABLE = "banners";
    private static BannerClient instance;

    private BannerClient() {
        super();
    }

    public static synchronized BannerClient getInstance() {
        if (instance == null) {
            instance = new BannerClient();
        }
        return instance;
    }

    public void fetchActiveBanners(ApiCallback<List<BannerItem>> callback) {
        if (!SupabaseConfig.isConfigured()) {
            postError(callback, "Supabase is not configured");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/" + TABLE +
                "?select=*" +
                "&is_active=eq.true" +
                "&order=sort_order.asc";

        String accessToken = AuthClient.getInstance().getAccessToken();
        String authToken = (accessToken != null && !accessToken.isEmpty()) 
                ? accessToken 
                : SupabaseConfig.SUPABASE_ANON_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + authToken)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
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
                        postError(callback, parseRestError("Failed to fetch banners", response.code(), json));
                        return;
                    }

                    BannerItem[] banners = gson.fromJson(json, BannerItem[].class);
                    List<BannerItem> bannerList = banners != null ? Arrays.asList(banners) : new ArrayList<>();
                    
                    postSuccess(callback, bannerList);
                } catch (Exception e) {
                    postError(callback, "Failed to parse banners");
                }
            }
        });
    }

    public void fetchBannersWithTiming(ApiCallback<List<BannerItem>> callback) {
        if (!SupabaseConfig.isConfigured()) {
            postError(callback, "Supabase is not configured");
            return;
        }

        String url = SupabaseConfig.REST_URL + "/" + TABLE +
                "?select=*" +
                "&is_active=eq.true" +
                "&order=sort_order.asc";

        String accessToken = AuthClient.getInstance().getAccessToken();
        String authToken = (accessToken != null && !accessToken.isEmpty()) 
                ? accessToken 
                : SupabaseConfig.SUPABASE_ANON_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader(SupabaseConfig.HEADER_AUTH, SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + authToken)
                .addHeader(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON)
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
                        postError(callback, parseRestError("Failed to fetch banners", response.code(), json));
                        return;
                    }

                    BannerItem[] banners = gson.fromJson(json, BannerItem[].class);
                    List<BannerItem> allBanners = banners != null ? Arrays.asList(banners) : new ArrayList<>();
                    
                    List<BannerItem> validBanners = filterBannersByTiming(allBanners);
                    
                    postSuccess(callback, validBanners);
                } catch (Exception e) {
                    postError(callback, "Failed to parse banners");
                }
            }
        });
    }

    private List<BannerItem> filterBannersByTiming(List<BannerItem> banners) {
        long now = System.currentTimeMillis();
        
        return banners.stream()
                .filter(banner -> {
                    String startDate = banner.getStartDate();
                    String endDate = banner.getEndDate();
                    
                    boolean valid = true;
                    
                    if (startDate != null && !startDate.isEmpty()) {
                        try {
                            long startTime = parseDate(startDate);
                            if (now < startTime) {
                                valid = false;
                            }
                        } catch (Exception ignored) {}
                    }
                    
                    if (valid && endDate != null && !endDate.isEmpty()) {
                        try {
                            long endTime = parseDate(endDate);
                            if (now > endTime) {
                                valid = false;
                            }
                        } catch (Exception ignored) {}
                    }
                    
                    return valid;
                })
                .collect(Collectors.toList());
    }

    private long parseDate(String dateString) throws Exception {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(dateString).getTime();
        } catch (Exception e) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf.parse(dateString).getTime();
            } catch (Exception e2) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                return sdf.parse(dateString.replace(" ", "T")).getTime();
            }
        }
    }
}