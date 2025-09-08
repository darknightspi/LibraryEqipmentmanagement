package com.ram.libraryeqipmentmanagement;

public class GridCell {
    private int position;
    private int row;
    private int column;
    private String equipmentName;
    private String specifications;
    private boolean isFunctional;

    public GridCell() {
        // Required empty constructor for Firestore
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
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

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public boolean isFunctional() {
        return isFunctional;
    }

    public void setFunctional(boolean functional) {
        isFunctional = functional;
    }

    public boolean hasEquipment() {
        return equipmentName != null && !equipmentName.isEmpty();
    }
} 