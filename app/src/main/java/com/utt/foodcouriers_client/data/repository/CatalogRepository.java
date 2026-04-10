package com.utt.foodcouriers_client.data.repository;

public class CatalogRepository {

    private static CatalogRepository instance;

    public static synchronized CatalogRepository getInstance() {
        if (instance == null) {
            instance = new CatalogRepository();
        }
        return instance;
    }
}
