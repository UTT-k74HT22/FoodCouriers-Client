package com.utt.foodcouriers_client.data.repository;

public class AuthRepository {

    private static AuthRepository instance;

    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }
}
