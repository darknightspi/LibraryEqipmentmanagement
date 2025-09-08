package com.ram.libraryeqipmentmanagement;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import com.ram.libraryeqipmentmanagement.model.Equipment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class AddEquipmentDialog extends Dialog {
    private TextInputEditText nameInput;
    private TextInputEditText specificationsInput;
    private SwitchMaterial statusSwitch;
    private Button saveButton;
    private Button cancelButton;
    private OnEquipmentAddedListener listener;
    private Equipment existingEquipment;
    private String libraryId;

    public interface OnEquipmentAddedListener {
        void onEquipmentAdded(Equipment equipment);
    }

    public AddEquipmentDialog(Context context, OnEquipmentAddedListener listener, String libraryId) {
        super(context);
        this.listener = listener;
        this.libraryId = libraryId;
    }

    public AddEquipmentDialog(Context context, OnEquipmentAddedListener listener, Equipment existingEquipment, String libraryId) {
        super(context);
        this.listener = listener;
        this.existingEquipment = existingEquipment;
        this.libraryId = libraryId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_equipment);

        nameInput = findViewById(R.id.editTextEquipmentDescription);
        specificationsInput = findViewById(R.id.editTextEquipmentSpecification);
        statusSwitch = findViewById(R.id.equipmentStatusSwitch);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        if (existingEquipment != null) {
            nameInput.setText(existingEquipment.getName());
            specificationsInput.setText(existingEquipment.getSpecification());
            boolean isAvailable = "available".equals(existingEquipment.getStatus());
            Log.d("AddEquipmentDialog", "Setting switch state for existing equipment. Status: " + existingEquipment.getStatus() + ", Switch state: " + isAvailable);
            statusSwitch.setChecked(isAvailable);
        }

        saveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String specifications = specificationsInput.getText().toString().trim();
            boolean isFunctional = statusSwitch.isChecked();

            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Please enter equipment name", Toast.LENGTH_SHORT).show();
                return;
            }

            String status = isFunctional ? "available" : "maintenance";
            Log.d("AddEquipmentDialog", "Creating equipment with status: " + status + " (switch state: " + isFunctional + ")");
            
            Equipment equipment = new Equipment(
                name,
                "", // description
                specifications,
                status,
                libraryId,
                "", // departmentId
                0, // position
                System.currentTimeMillis() // purchaseDate
            );

            if (existingEquipment != null) {
                equipment.setId(existingEquipment.getId());
                equipment.setPosition(existingEquipment.getPosition());
            }

            listener.onEquipmentAdded(equipment);
            dismiss();
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }
} 