package com.ram.libraryeqipmentmanagement;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.ram.libraryeqipmentmanagement.adapter.EquipmentAdapter;
import com.ram.libraryeqipmentmanagement.adapter.LibraryAdapter;
import com.ram.libraryeqipmentmanagement.databinding.ActivityLibraryBinding;
import com.ram.libraryeqipmentmanagement.model.Equipment;
import com.ram.libraryeqipmentmanagement.model.Library;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LibraryActivity extends AppCompatActivity implements LibraryAdapter.OnLibraryClickListener {
    private static final String TAG = "LibraryActivity";
    private ActivityLibraryBinding binding;
    private RecyclerView recyclerView;
    private LibraryAdapter adapter;
    private List<Library> libraryList;
    private FirebaseService firebaseService;
    private String departmentId;
    private String userRole;
    private String departmentName;
    private int libraryId;
    private String libraryName;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FloatingActionButton fabAddLibrary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLibraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Service
        firebaseService = FirebaseService.getInstance(getApplicationContext());

        // Get intent extras
        departmentId = getIntent().getStringExtra("departmentId");
        String departmentName = getIntent().getStringExtra("departmentName");
        userRole = getIntent().getStringExtra("userRole");

        if (departmentId == null || departmentName == null) {
            Toast.makeText(this, "Invalid department information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up toolbar
        setSupportActionBar(binding.topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(departmentName);
        }

        // Initialize variables
        libraryList = new ArrayList<>();
        recyclerView = binding.recyclerView;
        progressBar = binding.progressBar;
        emptyView = binding.emptyView;
        fabAddLibrary = binding.fabAddLibrary;

        // Set up RecyclerView
        setupRecyclerView();

        // Set up FAB
        if (isAdmin()) {
            binding.fabAddLibrary.setVisibility(View.VISIBLE);
            binding.fabAddLibrary.setOnClickListener(v -> showAddLibraryDialog());
        } else {
            binding.fabAddLibrary.setVisibility(View.GONE);
        }

        // Set up retry button
        binding.retryButton.setOnClickListener(v -> loadLibraries());

        // Load libraries
        loadLibraries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload libraries when returning to this activity
        if (firebaseService != null) {
            loadLibraries();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        firebaseService.cleanup();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void setupRecyclerView() {
        adapter = new LibraryAdapter(this, libraryList, this, isAdmin());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadLibraries() {
        Log.d(TAG, "Loading libraries for department: " + departmentId);
        showLoading();

        firebaseService.getLibrariesByDepartment(departmentId)
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, "Successfully retrieved libraries from Firestore");
                List<Library> libraries = new ArrayList<>();
                
                if (!querySnapshot.isEmpty()) {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Library library = document.toObject(Library.class);
                        library.setId(document.getId());
                        libraries.add(library);
                        Log.d(TAG, "Added library: " + library.getName() + " (ID: " + library.getId() + ")");
                    }
                }

                // Update UI on the main thread
                runOnUiThread(() -> {
                    if (libraries.isEmpty()) {
                        showEmpty("No libraries found");
                    } else {
                        showContent(libraries);
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading libraries: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showError("Error loading libraries. Please check your connection and try again.");
                });
            });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        binding.retryButton.setVisibility(View.GONE);
    }

    private void showContent(List<Library> libraries) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        binding.retryButton.setVisibility(View.GONE);
        
        libraryList.clear();
        libraryList.addAll(libraries);
        adapter.notifyDataSetChanged();
    }

    private void showEmpty(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        binding.retryButton.setVisibility(View.VISIBLE);
        emptyView.setText(message);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        binding.retryButton.setVisibility(View.VISIBLE);
        emptyView.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showAddLibraryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_library, null);
        EditText editTextLibraryName = dialogView.findViewById(R.id.editTextLibraryName);
        EditText editTextRows = dialogView.findViewById(R.id.editTextRows);
        EditText editTextColumns = dialogView.findViewById(R.id.editTextColumns);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Library")
               .setView(dialogView)
               .setPositiveButton("Add", null) // Set to null initially
               .setNegativeButton("Cancel", (dialog, which) -> {
                   hideKeyboard(editTextLibraryName);
                   dialog.dismiss();
               });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String libraryName = editTextLibraryName.getText().toString().trim();
                String rowsStr = editTextRows.getText().toString().trim();
                String columnsStr = editTextColumns.getText().toString().trim();

                if (libraryName.isEmpty()) {
                    editTextLibraryName.setError("Please enter library name");
                    return;
                }

                if (rowsStr.isEmpty()) {
                    editTextRows.setError("Please enter number of rows");
                    return;
                }

                if (columnsStr.isEmpty()) {
                    editTextColumns.setError("Please enter number of columns");
                    return;
                }

                int rows = Integer.parseInt(rowsStr);
                int columns = Integer.parseInt(columnsStr);

                if (rows <= 0) {
                    editTextRows.setError("Number of rows must be greater than 0");
                    return;
                }

                if (columns <= 0) {
                    editTextColumns.setError("Number of columns must be greater than 0");
                    return;
                }

                hideKeyboard(editTextLibraryName);
                addLibrary(libraryName, rows, columns);
                dialog.dismiss();
            });
        });

        dialog.show();
        
        // Show keyboard automatically
        editTextLibraryName.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void addLibrary(String name, int rows, int columns) {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        Library library = new Library();
        library.setName(name);
        library.setRows(rows);
        library.setColumns(columns);
        library.setDepartmentId(departmentId);

        firebaseService.addLibrary(library)
            .addOnSuccessListener(aVoid -> {
                // Refresh the list to get the updated data with IDs
                loadLibraries();
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Library added successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error adding library: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onLibraryClick(Library library) {
        Intent intent = new Intent(this, LibraryStructureActivity.class);
        intent.putExtra("libraryId", library.getId());
        intent.putExtra("libraryName", library.getName());
        intent.putExtra("rows", library.getRows());
        intent.putExtra("columns", library.getColumns());
        intent.putExtra("userRole", userRole);
        startActivity(intent);
    }

    @Override
    public void onLibraryLongClick(Library library) {
        if (isAdmin()) {
            showLibraryOptionsDialog(library);
        }
    }

    private void showLibraryOptionsDialog(Library library) {
        String[] options = {"Rename", "Delete"};
        new MaterialAlertDialogBuilder(this)
            .setTitle("Library Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Rename
                        showEditLibraryDialog(library);
                        break;
                    case 1: // Delete
                        showDeleteConfirmationDialog(library);
                        break;
                }
            })
            .show();
    }

    private void showEditLibraryDialog(Library library) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rename_library, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.editTextLibraryName);

        nameInput.setText(library.getName());
        nameInput.requestFocus();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
            .setTitle("Rename Library")
            .setView(dialogView)
            .setPositiveButton("Rename", null)
            .setNegativeButton("Cancel", (dialogInterface, i) -> {
                hideKeyboard(nameInput);
                dialogInterface.dismiss();
            });

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) {
                    nameInput.setError("Please enter library name");
                    return;
                }

                hideKeyboard(nameInput);
                library.setName(name);
                updateLibrary(library);
                dialog.dismiss();
            });
        });

        dialog.show();

        // Show keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void updateLibrary(Library library) {
        firebaseService.updateLibrary(library)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Library updated successfully", Toast.LENGTH_SHORT).show();
                loadLibraries();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error updating library", Toast.LENGTH_SHORT).show();
            });
    }

    private void showDeleteConfirmationDialog(Library library) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Library")
            .setMessage("Are you sure you want to delete this library? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                if (library != null && library.getId() != null) {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    
                    firebaseService.deleteLibrary(library.getId())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Library deleted successfully", Toast.LENGTH_SHORT).show();
                            loadLibraries(); // Refresh the list
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error deleting library: " + e.getMessage(), e);
                            Toast.makeText(this, "Failed to delete library: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        })
                        .addOnCompleteListener(task -> {
                            binding.progressBar.setVisibility(View.GONE);
                        });
                } else {
                    Toast.makeText(this, "Invalid library data", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteLibrary(Library library) {
        if (library == null || library.getId() == null) {
            Toast.makeText(this, "Invalid library information", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        
        firebaseService.deleteLibrary(library.getId())
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Library deleted successfully", Toast.LENGTH_SHORT).show();
                loadLibraries(); // Refresh the list
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting library: " + e.getMessage(), e);
                Toast.makeText(this, "Failed to delete library: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            })
            .addOnCompleteListener(task -> {
                binding.progressBar.setVisibility(View.GONE);
            });
    }

    private boolean isAdmin() {
        return "admin".equalsIgnoreCase(userRole);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_library, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            // Show loading indicator
            showLoading();
            // Reload libraries
            loadLibraries();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
