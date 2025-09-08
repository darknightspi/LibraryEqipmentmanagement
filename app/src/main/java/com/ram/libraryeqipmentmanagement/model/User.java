package com.ram.libraryeqipmentmanagement.model;

public class User {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private boolean isAdminRequestPending;
    private String adminRequestStatus; // "pending", "approved", "rejected"
    private long adminRequestTimestamp;

    // Empty constructor required for Firestore
    public User() {}

    public User(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = "user";
        this.isAdminRequestPending = false;
        this.adminRequestStatus = "";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isAdminRequestPending() { return isAdminRequestPending; }
    public void setAdminRequestPending(boolean adminRequestPending) { 
        this.isAdminRequestPending = adminRequestPending; 
    }

    public String getAdminRequestStatus() { return adminRequestStatus; }
    public void setAdminRequestStatus(String adminRequestStatus) { 
        this.adminRequestStatus = adminRequestStatus; 
    }

    public long getAdminRequestTimestamp() { return adminRequestTimestamp; }
    public void setAdminRequestTimestamp(long adminRequestTimestamp) { 
        this.adminRequestTimestamp = adminRequestTimestamp; 
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return email != null ? email : "Unknown User";
        }
    }
} 