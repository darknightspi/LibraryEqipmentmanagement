package com.ram.libraryeqipmentmanagement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.ram.libraryeqipmentmanagement.model.Library;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;

public class CreateLibraryDialog {
    private final Context context;
    private final OnLibraryCreatedListener listener;
    private final FirebaseService firebaseService;
    private final String departmentId;
    private AlertDialog dialog;
    private EditText editTextName;

    public interface OnLibraryCreatedListener {
        void onLibraryCreated(Library library);
    }

    public CreateLibraryDialog(Context context, String departmentId, OnLibraryCreatedListener listener) {
        this.context = context;
        this.departmentId = departmentId;
        this.listener = listener;
        this.firebaseService = FirebaseService.getInstance(context);
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_library, null);
        
        editTextName = view.findViewById(R.id.editTextLibraryName);

        builder.setTitle("Create Library")
            .setView(view)
            .setPositiveButton("Create", (dialog, which) -> {
                String name = editTextName.getText().toString().trim();

                if (name.isEmpty()) {
                    Toast.makeText(context, "Please enter a library name", Toast.LENGTH_SHORT).show();
                    return;
                }

                Library library = new Library(name, departmentId);
                createLibrary(library);
            })
            .setNegativeButton("Cancel", null);

        dialog = builder.create();
        dialog.show();
    }

    private void createLibrary(Library library) {
        Task<Void> task = firebaseService.addLibrary(library);
        task.addOnSuccessListener(aVoid -> {
            if (listener != null) {
                listener.onLibraryCreated(library);
            }
            Toast.makeText(context, "Library created successfully", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Failed to create library", Toast.LENGTH_SHORT).show();
        });
    }
}
