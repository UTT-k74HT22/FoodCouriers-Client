package com.utt.foodcouriers_client.data.repository.base;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;

import java.util.List;

public interface CrudRepository<T, R> {

    void getAll(RepositoryCallback<List<T>> callback);

    void getById(String id, RepositoryCallback<T> callback);

    void create(R request, RepositoryCallback<T> callback);

    void update(String id, R request, RepositoryCallback<T> callback);

    void delete(String id, RepositoryCallback<Void> callback);
}
