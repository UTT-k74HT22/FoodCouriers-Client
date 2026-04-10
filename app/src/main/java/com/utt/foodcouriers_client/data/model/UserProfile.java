package com.utt.foodcouriers_client.data.model;

import java.io.Serializable;

public class UserProfile implements Serializable {
    private final String id;
    private final String fullName;
    private final String email;
    private final String phone;

    public UserProfile(String id, String fullName, String email, String phone) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }
}
