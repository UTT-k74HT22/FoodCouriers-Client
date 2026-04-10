package com.utt.foodcouriers_client.data.repository.base;

import com.utt.foodcouriers_client.data.remote.BaseSupabaseClient;
import com.utt.foodcouriers_client.data.remote.SupabaseConfig;

public abstract class BaseSupabaseRepository extends BaseSupabaseClient {

    protected boolean isRemoteConfigured() {
        return SupabaseConfig.isConfigured();
    }
}
