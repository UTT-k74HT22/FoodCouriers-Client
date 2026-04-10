package com.utt.foodcouriers_client.data.common;

public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(String error);
}
