package com.ram.libraryeqipmentmanagement.model;

public class LibraryCell {
    private String id;
    private String libraryId;
    private int row;
    private int column;
    private int equipmentCount;

    public LibraryCell() {
        // Default constructor required for Firestore
    }

    public LibraryCell(String libraryId, int row, int column) {
        this.libraryId = libraryId;
        this.row = row;
        this.column = column;
        this.equipmentCount = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(String libraryId) {
        this.libraryId = libraryId;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getEquipmentCount() {
        return equipmentCount;
    }

    public void setEquipmentCount(int equipmentCount) {
        this.equipmentCount = equipmentCount;
    }
} 