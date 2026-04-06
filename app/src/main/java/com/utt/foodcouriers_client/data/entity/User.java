package com.utt.foodcouriers_client.data.entity;

import com.google.gson.annotations.SerializedName;

public class User {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("auth_id")
    private String authId;
    
    @SerializedName("full_name")
    private String fullName;
    
    @SerializedName("phone")
    private String phone;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("avatar_url")
    private String avatarUrl;
    
    @SerializedName("role")
    private String role;
    
    @SerializedName("is_active")
    private Boolean isActive;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;

    public User() {}

    public User(String id, String authId, String fullName, String phone, String email, String role) {
        this.id = id;
        this.authId = authId;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.isActive = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getAuthId() { return authId; }
    public void setAuthId(String authId) { this.authId = authId; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
    
    public boolean isStaff() {
        return "staff".equalsIgnoreCase(role);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }
}
