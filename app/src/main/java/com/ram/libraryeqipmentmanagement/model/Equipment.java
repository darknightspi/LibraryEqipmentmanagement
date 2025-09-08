package com.ram.libraryeqipmentmanagement.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Equipment {
    private String id;
    private String name;
    private String description;
    private String specification;
    private String status;
    private String libraryId;
    private String departmentId;
    private int position;
    private double price;
    private long purchaseDate;
    private long createdAt;
    private long updatedAt;

    // Default constructor required for Firebase
    public Equipment() {
    }

    public Equipment(String name, String description, String specification, String status, 
                    String libraryId, String departmentId, int position, long purchaseDate) {
        this.name = name;
        this.description = description;
        this.specification = specification;
        this.status = status;
        this.libraryId = libraryId;
        this.departmentId = departmentId;
        this.position = position;
        this.purchaseDate = purchaseDate;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(String libraryId) {
        this.libraryId = libraryId;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(long purchaseDate) {
        this.purchaseDate = purchaseDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Exclude
    public boolean isAvailable() {
        return "available".equalsIgnoreCase(status);
    }

    @Exclude
    public boolean isInUse() {
        return "in_use".equalsIgnoreCase(status);
    }

    @Exclude
    public boolean isInMaintenance() {
        return "maintenance".equalsIgnoreCase(status);
    }
} 