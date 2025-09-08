package com.ram.libraryeqipmentmanagement;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.ram.libraryeqipmentmanagement.adapter.DepartmentAdapter;
import com.ram.libraryeqipmentmanagement.databinding.ActivityDepartmentBinding;
import com.ram.libraryeqipmentmanagement.databinding.DialogAddDepartmentBinding;
import com.ram.libraryeqipmentmanagement.model.Department;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;

import java.util.ArrayList;
import java.util.List;

import android.widget.PopupMenu;

public class DepartmentActivity extends AppCompatActivity implements DepartmentAdapter.OnDepartmentClickListener {
    private ActivityDepartmentBinding binding;
    private DepartmentAdapter departmentAdapter;
    private List<Department> departmentList;
    private FirebaseService firebaseService;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private boolean isAdmin = false;
    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDepartmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize InputMethodManager
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Initialize Firebase services
        firebaseService = FirebaseService.getInstance(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Set up toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Departments");
        }

        // Initialize department list and adapter
        departmentList = new ArrayList<>();
        departmentAdapter = new DepartmentAdapter(this, departmentList, this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(departmentAdapter);

        // Check user role and set up UI accordingly
        checkUserRole();

        // Load departments
        loadDepartments();
    }

    private void checkUserRole() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String role = documentSnapshot.getString("role");
                    isAdmin = "admin".equals(role);
                    setupUIForRole();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error checking user role", Toast.LENGTH_SHORT).show();
            });
    }

    private void setupUIForRole() {
        // Show/hide FAB based on admin status
        if (isAdmin) {
            binding.fabAddDepartment.setVisibility(View.VISIBLE);
            binding.fabAddDepartment.setOnClickListener(v -> showAddDepartmentDialog());
        } else {
            binding.fabAddDepartment.setVisibility(View.GONE);
        }
    }

    private void loadDepartments() {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("departments")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                departmentList.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Department department = document.toObject(Department.class);
                    department.setId(document.getId());
                    departmentList.add(department);
                }
                departmentAdapter.notifyDataSetChanged();
                binding.progressBar.setVisibility(View.GONE);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading departments", Toast.LENGTH_SHORT).show();
                binding.progressBar.setVisibility(View.GONE);
            });
    }

    private void showAddDepartmentDialog() {
        DialogAddDepartmentBinding dialogBinding = DialogAddDepartmentBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Add New Department")
            .setView(dialogBinding.getRoot())
            .setPositiveButton("Add", null) // Set to null initially
            .setNegativeButton("Cancel", (dialogInterface, i) -> {
                hideKeyboard(dialogBinding.editTextDepartmentName);
                dialogInterface.dismiss();
            })
            .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String departmentName = dialogBinding.editTextDepartmentName.getText().toString().trim();
                if (!departmentName.isEmpty()) {
                    hideKeyboard(dialogBinding.editTextDepartmentName);
                    addDepartment(departmentName);
                    dialog.dismiss();
                } else {
                    dialogBinding.editTextDepartmentName.setError("Please enter department name");
                }
            });
        });

        dialog.show();
        
        // Show keyboard automatically
        dialogBinding.editTextDepartmentName.requestFocus();
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void hideKeyboard(View view) {
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private void addDepartment(String name) {
        binding.progressBar.setVisibility(View.VISIBLE);
        Department department = new Department(name);
        db.collection("departments")
            .add(department)
            .addOnSuccessListener(documentReference -> {
                department.setId(documentReference.getId());
                departmentList.add(department);
                departmentAdapter.notifyItemInserted(departmentList.size() - 1);
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Department added successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error adding department", Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onDepartmentClick(Department department) {
        // Navigate to LibraryActivity with department information
        Intent intent = new Intent(this, LibraryActivity.class);
        intent.putExtra("departmentId", department.getId());
        intent.putExtra("departmentName", department.getName());
        // Pass the user role to maintain permissions
        if (isAdmin) {
            intent.putExtra("userRole", "admin");
        } else {
            intent.putExtra("userRole", "user");
        }
        startActivity(intent);
    }

    @Override
    public void onDepartmentLongClick(Department department) {
        if (!isAdmin) {
            Toast.makeText(this, "Only administrators can modify departments", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create popup menu anchored to the clicked department item
        View anchorView = binding.recyclerView.findViewWithTag(department.getId());
        if (anchorView == null) {
            anchorView = binding.recyclerView;
        }
        
        PopupMenu popup = new PopupMenu(this, anchorView);
        Menu menu = popup.getMenu();
        menu.add(0, 1, Menu.NONE, "Rename");
        menu.add(0, 2, Menu.NONE, "Delete");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: // Rename
                    showRenameDialog(department);
                    return true;
                case 2: // Delete
                    showDeleteConfirmation(department);
                    return true;
                default:
                    return false;
            }
        });

        popup.show();
    }

    private void showRenameDialog(Department department) {
        DialogAddDepartmentBinding dialogBinding = DialogAddDepartmentBinding.inflate(getLayoutInflater());
        dialogBinding.editTextDepartmentName.setText(department.getName());
        new AlertDialog.Builder(this)
            .setTitle("Rename Department")
            .setView(dialogBinding.getRoot())
            .setPositiveButton("Rename", (dialog, which) -> {
                String newName = dialogBinding.editTextDepartmentName.getText().toString().trim();
                if (!newName.isEmpty()) {
                    renameDepartment(department, newName);
                } else {
                    Toast.makeText(this, "Please enter department name", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void renameDepartment(Department department, String newName) {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("departments").document(department.getId())
            .update("name", newName)
            .addOnSuccessListener(aVoid -> {
                department.setName(newName);
                departmentAdapter.notifyDataSetChanged();
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Department renamed successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error renaming department", Toast.LENGTH_SHORT).show();
            });
    }

    private void showDeleteConfirmation(Department department) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Department")
            .setMessage("Are you sure you want to delete this department? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteDepartment(department))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteDepartment(Department department) {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("departments").document(department.getId())
            .delete()
            .addOnSuccessListener(aVoid -> {
                int position = departmentList.indexOf(department);
                departmentList.remove(position);
                departmentAdapter.notifyItemRemoved(position);
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Department deleted successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error deleting department", Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_department, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            loadDepartments();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 