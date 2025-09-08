package com.ram.libraryeqipmentmanagement;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ram.libraryeqipmentmanagement.adapter.DepartmentAdapter;
import com.ram.libraryeqipmentmanagement.model.Department;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DepartmentAdapter.OnDepartmentClickListener {
    private static final String TAG = "MainActivity";
    private Button addDepartmentButton;
    private RecyclerView recyclerView;
    private DepartmentAdapter adapter;
    private FirebaseService firebaseService;
    private List<Department> departmentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the title bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        // Initialize UI Components
        addDepartmentButton = findViewById(R.id.btnAddDepartment);
        recyclerView = findViewById(R.id.recyclerViewDepartments);
        firebaseService = FirebaseService.getInstance(this);

        // Initialize department list
        departmentList = new ArrayList<>();

        // Setup RecyclerView
        setupRecyclerView();

        // Set Click Listener for Add Department Button
        addDepartmentButton.setOnClickListener(v -> showAddDepartmentDialog());

        // Load departments
        loadDepartments();
    }

    private void loadDepartments() {
        // Get the database reference and attach a listener
        DatabaseReference deptRef = firebaseService.getDepartmentsReference();
        deptRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                departmentList.clear();
                for (DataSnapshot departmentSnapshot : snapshot.getChildren()) {
                    Department department = departmentSnapshot.getValue(Department.class);
                    if (department != null) {
                        department.setId(departmentSnapshot.getKey());
                        departmentList.add(department);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading departments: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Error loading departments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDepartmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Department");

        final EditText input = new EditText(this);
        input.setHint("Enter Department Name");
        builder.setView(input);

        // Set Positive Button (Add)
        builder.setPositiveButton("Add", (dialog, which) -> {
            String departmentName = input.getText().toString().trim();
            if (!departmentName.isEmpty()) {
                if (!isDepartmentExists(departmentName)) {
                    try {
                        Department department = new Department(departmentName);
                        Task<Void> task = firebaseService.addDepartment(department);
                        task.addOnSuccessListener(aVoid -> {
                                Toast.makeText(MainActivity.this, "Department Added", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error adding department: " + e.getMessage());
                                Toast.makeText(MainActivity.this, "Failed to add department", Toast.LENGTH_SHORT).show();
                            });
                    } catch (Exception e) {
                        Log.e(TAG, "Error adding department: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Error adding department", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Department already exists", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Please enter a name", Toast.LENGTH_SHORT).show();
            }
        });

        // Set Negative Button (Cancel)
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private boolean isDepartmentExists(String name) {
        for (Department department : departmentList) {
            if (department.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DepartmentAdapter(this, departmentList, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDepartmentClick(Department department) {
        Intent intent = new Intent(this, LibraryActivity.class);
        intent.putExtra("departmentId", department.getId());
        intent.putExtra("departmentName", department.getName());
        intent.putExtra("userRole", "admin"); // For testing, set as admin
        startActivity(intent);
    }

    @Override
    public void onDepartmentLongClick(Department department) {
        // Show options menu for department
        showDepartmentOptionsDialog(department);
    }

    private void showDepartmentOptionsDialog(Department department) {
        String[] options = {"Edit", "Delete"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Department Options");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Edit
                    showEditDepartmentDialog(department);
                    break;
                case 1: // Delete
                    showDeleteConfirmationDialog(department);
                    break;
            }
        });
        
        builder.show();
    }

    private void showEditDepartmentDialog(Department department) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Department");

        final EditText input = new EditText(this);
        input.setText(department.getName());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                department.setName(newName);
                firebaseService.updateDepartment(department.getId(), department)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Department updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update department", Toast.LENGTH_SHORT).show();
                    });
            } else {
                Toast.makeText(this, "Department name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteConfirmationDialog(Department department) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Department");
        builder.setMessage("Are you sure you want to delete this department? All libraries and equipment within it will also be deleted.");
        
        builder.setPositiveButton("Delete", (dialog, which) -> {
            firebaseService.deleteDepartment(department.getId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Department deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete department", Toast.LENGTH_SHORT).show();
                });
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
