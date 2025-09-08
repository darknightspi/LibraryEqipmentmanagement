package com.ram.libraryeqipmentmanagement.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Library {
    private String id;
    private String name;
    private String departmentId;
    private int availableEquipment;
    private int inUseEquipment;
    private int maintenanceEquipment;
    private long createdAt;
    private long updatedAt;
    private int rows;
    private int columns;

    // Default constructor required for Firebase
    public Library() {
    }

    // Constructor for creating new library
    public Library(String name, String departmentId) {
        this.name = name;
        this.departmentId = departmentId;
        this.availableEquipment = 0;
        this.inUseEquipment = 0;
        this.maintenanceEquipment = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

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

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getAvailableEquipment() {
        return availableEquipment;
    }

    public void setAvailableEquipment(int availableEquipment) {
        this.availableEquipment = availableEquipment;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getInUseEquipment() {
        return inUseEquipment;
    }

    public void setInUseEquipment(int inUseEquipment) {
        this.inUseEquipment = inUseEquipment;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getMaintenanceEquipment() {
        return maintenanceEquipment;
    }

    public void setMaintenanceEquipment(int maintenanceEquipment) {
        this.maintenanceEquipment = maintenanceEquipment;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setEquipmentCounts(int available, int inUse, int maintenance) {
        this.availableEquipment = available;
        this.inUseEquipment = inUse;
        this.maintenanceEquipment = maintenance;
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

    public int getTotalEquipment() {
        return availableEquipment + inUseEquipment + maintenanceEquipment;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    @Override
    public String toString() {
        return "Library{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", departmentId='" + departmentId + '\'' +
               ", availableEquipment=" + availableEquipment +
               ", inUseEquipment=" + inUseEquipment +
               ", maintenanceEquipment=" + maintenanceEquipment +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               ", rows=" + rows +
               ", columns=" + columns +
               '}';
    }
} 