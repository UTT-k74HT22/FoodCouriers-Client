package com.utt.foodcouriers_client.data.repository;

public class CartRepository {

    private static CartRepository instance;

    public static synchronized CartRepository getInstance() {
        if (instance == null) {
            instance = new CartRepository();
        }
        return instance;
    }
}
