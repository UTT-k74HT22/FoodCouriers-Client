package com.utt.foodcouriers_client.data.remote;

import com.utt.foodcouriers_client.BuildConfig;

public class SupabaseConfig {

    public static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    public static final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;
    public static final String SUPABASE_STORAGE_BUCKET = BuildConfig.SUPABASE_STORAGE_BUCKET;

    public static boolean isConfigured() {
        return SUPABASE_URL != null && !SUPABASE_URL.isBlank()
                && SUPABASE_ANON_KEY != null && !SUPABASE_ANON_KEY.isBlank();
    }
}
