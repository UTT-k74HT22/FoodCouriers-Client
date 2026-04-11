package com.utt.foodcouriers_client.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class UserProfile implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("auth_id")
    private String authId;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("role")
    private String role;

    @SerializedName("is_active")
    private Boolean active;

    public UserProfile() {
    }

    public UserProfile(String id, String fullName, String email, String phone) {
        this(id, null, fullName, email, phone, "customer", true);
    }

    public UserProfile(String id, String authId, String fullName, String email, String phone, String role, Boolean active) {
        this.id = id;
        this.authId = authId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getAuthId() {
        return authId;
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

    public String getRole() {
        return role;
    }

    public boolean isCustomer() {
        return "customer".equalsIgnoreCase(role);
    }

    public boolean isStaff() {
        return "staff".equalsIgnoreCase(role);
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
