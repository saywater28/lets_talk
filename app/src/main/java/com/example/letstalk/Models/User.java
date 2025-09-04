package com.example.letstalk.Models;

public class User {
    private String uid;
    private String name;
    private String phoneNumber;
    private String email;
    private String profileImage;

    // Empty constructor required for Firebase
    public User() {
    }

    // Constructor with all fields
    public User(String uid, String name, String phoneNumber, String email, String profileImage) {
        this.uid = uid;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.profileImage = profileImage;
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
