package com.utt.foodcouriers_client.data.repository;

public class SearchRepository {

    private static SearchRepository instance;

    public static synchronized SearchRepository getInstance() {
        if (instance == null) {
            instance = new SearchRepository();
        }
        return instance;
    }
}
