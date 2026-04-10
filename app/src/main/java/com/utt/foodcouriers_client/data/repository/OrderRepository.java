package com.utt.foodcouriers_client.data.repository;

public class OrderRepository {

    private static OrderRepository instance;

    public static synchronized OrderRepository getInstance() {
        if (instance == null) {
            instance = new OrderRepository();
        }
        return instance;
    }
}
