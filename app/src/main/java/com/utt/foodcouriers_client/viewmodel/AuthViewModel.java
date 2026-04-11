package com.utt.foodcouriers_client.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.utt.foodcouriers_client.data.common.RepositoryCallback;
import com.utt.foodcouriers_client.data.model.UserProfile;
import com.utt.foodcouriers_client.data.repository.AuthRepository;

public class AuthViewModel extends BaseViewModel {

    private final AuthRepository repository = AuthRepository.getInstance();
    private final MutableLiveData<UserProfile> user = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    public MutableLiveData<UserProfile> getUser() {
        return user;
    }

    public MutableLiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public void login(String email, String password) {
        setLoading(true);
        repository.login(email, password, new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile result) {
                setLoading(false);
                user.setValue(result);
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }

    public void register(String fullName, String email, String phone, String password) {
        setLoading(true);
        repository.register(fullName, email, phone, password, new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile result) {
                setLoading(false);
                successMessage.setValue("registered");
                user.setValue(result);
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }

    public void resetPassword(String email) {
        setLoading(true);
        repository.resetPassword(email, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                setLoading(false);
                successMessage.setValue("reset_password");
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                postError(error);
            }
        });
    }
}
