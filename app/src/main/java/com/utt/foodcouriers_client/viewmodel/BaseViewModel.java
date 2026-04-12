package com.utt.foodcouriers_client.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BaseViewModel extends ViewModel {

    protected final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    protected final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    protected final MutableLiveData<String> successMessage = new MutableLiveData<>();

    public MutableLiveData<Boolean> getLoading() {
        return loading;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public MutableLiveData<String> getSuccessMessage() {
        return successMessage;
    }

    protected void setLoading(boolean isLoading) {
        loading.postValue(isLoading);
    }

    protected void postError(String error) {
        errorMessage.postValue(error);
    }

    protected void postSuccess(String message) {
        successMessage.postValue(message);
    }
}
