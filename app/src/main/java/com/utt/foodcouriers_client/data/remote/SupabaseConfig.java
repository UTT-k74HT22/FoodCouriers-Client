package com.utt.foodcouriers_client.data.remote;

import com.utt.foodcouriers_client.BuildConfig;

public class SupabaseConfig {

    public static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    public static final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;
    public static final String SUPABASE_STORAGE_BUCKET = BuildConfig.SUPABASE_STORAGE_BUCKET;
    public static final String AUTH_URL = SUPABASE_URL + "/auth/v1";
    public static final String REST_URL = SUPABASE_URL + "/rest/v1";
    public static final String STORAGE_URL = SUPABASE_URL + "/storage/v1";
    public static final String STORAGE_OBJECT_URL = STORAGE_URL + "/object/public/" + SUPABASE_STORAGE_BUCKET;

    public static final String HEADER_AUTH = "apikey";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_PREFER = "Prefer";

    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String PREF_RETURN_REPRESENTATION = "return=representation";

    public static boolean isConfigured() {
        return !isNullOrBlank(SUPABASE_URL)
                && !isNullOrBlank(SUPABASE_ANON_KEY);
    }

    public static String buildPublicImageUrl(String path) {
        if (isNullOrBlank(path)) {
            return null;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return STORAGE_OBJECT_URL + "/" + path.replaceFirst("^/", "");
    }

    public static String debugSummary() {
        return "url=" + SUPABASE_URL + ", anonKeyPrefix=" + maskKey(SUPABASE_ANON_KEY);
    }

    private static String maskKey(String value) {
        if (isNullOrBlank(value)) {
            return "<empty>";
        }
        int visible = Math.min(12, value.length());
        return value.substring(0, visible) + "...";
    }

    private static boolean isNullOrBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
