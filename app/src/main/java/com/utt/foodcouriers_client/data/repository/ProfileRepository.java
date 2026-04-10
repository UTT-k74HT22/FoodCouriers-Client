package com.utt.foodcouriers_client.data.repository;

public class ProfileRepository {

    private static ProfileRepository instance;

    public static synchronized ProfileRepository getInstance() {
        if (instance == null) {
            instance = new ProfileRepository();
        }
        return instance;
    }
}
