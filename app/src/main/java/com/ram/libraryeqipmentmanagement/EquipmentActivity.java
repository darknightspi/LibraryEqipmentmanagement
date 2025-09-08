package com.ram.libraryeqipmentmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;
import android.widget.Toast;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.util.Log;
import androidx.appcompat.widget.SearchView;
import java.util.ArrayList;
import java.util.List;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.widget.ImageView;
import android.text.TextUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ram.libraryeqipmentmanagement.adapter.EquipmentAdapter;
import com.ram.libraryeqipmentmanagement.model.Equipment;
import com.ram.libraryeqipmentmanagement.model.Library;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ram.libraryeqipmentmanagement.databinding.ActivityEquipmentBinding;
import androidx.annotation.NonNull;
import android.view.Gravity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.LinearLayout;
import java.util.HashMap;
import java.util.Map;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.view.ViewGroup;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class EquipmentActivity extends AppCompatActivity implements EquipmentAdapter.OnEquipmentClickListener {
    private static final String TAG = "EquipmentActivity";
    private ActivityEquipmentBinding binding;
    private int rows;
    private int columns;
    private String libraryId;
    private String libraryName;
    private FirebaseService firebaseService;
    private static final int CELL_MARGIN = 4;
    private static final int CELL_SIZE = 100; // Fixed cell size in dp
    private SearchView searchView;
    private Button openButton;
    private TextView searchResults;
    private View searchClickOverlay;
    private List<Equipment> currentSearchResults = new ArrayList<>();
    private List<Equipment> equipmentList = new ArrayList<>();
    private Library library;
    private String userRole;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEquipmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Service
        firebaseService = FirebaseService.getInstance(getApplicationContext());
        db = FirebaseFirestore.getInstance();

        // Get intent extras
        libraryId = getIntent().getStringExtra("libraryId");
        userRole = getIntent().getStringExtra("userRole");
        libraryName = getIntent().getStringExtra("library_name");
        rows = getIntent().getIntExtra("rows", -1);
        columns = getIntent().getIntExtra("columns", -1);

        Log.d(TAG, "Received dimensions - rows: " + rows + ", columns: " + columns);

        // Validate library information
        if (libraryId == null || libraryName == null) {
            Log.e(TAG, "Invalid library information - id: " + libraryId + ", name: " + libraryName);
            Toast.makeText(this, "Invalid library information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Validate grid dimensions
        if (rows <= 0 || columns <= 0) {
            Log.e(TAG, "Invalid grid dimensions - rows: " + rows + ", columns: " + columns);
            Toast.makeText(this, "Grid dimensions not set", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set title
        setTitle(libraryName);

        // Initialize views
        initializeViews();
        
        // Setup search view
        setupSearchView();

        // Set up FAB
        if (isAdmin()) {
            binding.fabAddEquipment.setVisibility(View.VISIBLE);
            binding.fabAddEquipment.setOnClickListener(v -> showAddEquipmentDialog());
        } else {
            binding.fabAddEquipment.setVisibility(View.GONE);
        }

        // Create grid layout
        createGridLayout();

        // Load equipment
        loadEquipment();
    }

    private void initializeViews() {
        // Set library title
        binding.libraryTitle.setText(libraryName);
        
        // Initialize grid layout
        binding.gridLayout.setRowCount(rows);
        binding.gridLayout.setColumnCount(columns);
        binding.gridLayout.setClickable(true);
        binding.gridLayout.setFocusable(true);
        
        // Create the grid cells
        createGridLayout();
    }

    private void createGridLayout() {
        Log.d(TAG, "Creating grid layout with " + rows + " rows and " + columns + " columns");
        binding.gridLayout.removeAllViews();

        // Set fixed dimensions for cells
        int cellSize = getResources().getDimensionPixelSize(R.dimen.grid_cell_size);

        for (int i = 0; i < rows * columns; i++) {
            // Create cell view
            View cell = LayoutInflater.from(this).inflate(R.layout.grid_cell_item, binding.gridLayout, false);
            TextView textView = cell.findViewById(R.id.cellText);
            
            // Set layout parameters
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellSize;
            params.height = cellSize;
            params.setMargins(4, 4, 4, 4);
            params.columnSpec = GridLayout.spec(i % columns);
            params.rowSpec = GridLayout.spec(i / columns);
            cell.setLayoutParams(params);
            
            // Store position
            final int position = i;
            cell.setTag(position);
            
            // Set click listener based on user role
            cell.setOnClickListener(v -> {
                Log.d(TAG, "Cell clicked at position: " + position);
                Equipment equipment = findEquipmentByPosition(position);
                
                if (equipment != null) {
                    Log.d(TAG, "Equipment found: " + equipment.getName());
                    // Show equipment options dialog for both admin and user
                    showEquipmentOptionsDialog(equipment);
                } else {
                    Log.d(TAG, "No equipment found at position: " + position);
                    if (isAdmin()) {
                        // Show add equipment dialog for empty cells (admin only)
                        showAddEquipmentDialog(position);
                    } else {
                        Toast.makeText(this, "No equipment at this position", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            
            binding.gridLayout.addView(cell);
        }
        
        updateGridWithEquipment();
    }

    private Equipment findEquipmentByPosition(int position) {
        if (equipmentList == null) {
            Log.e(TAG, "Equipment list is null");
            return null;
        }
        
        Log.d(TAG, "Searching for equipment at position: " + position);
        Log.d(TAG, "Current equipment list size: " + equipmentList.size());
        
        for (Equipment equipment : equipmentList) {
            Log.d(TAG, "Checking equipment: " + equipment.getName() + 
                  " at position: " + equipment.getPosition() + 
                  " with ID: " + equipment.getId());
            if (equipment.getPosition() == position) {
                Log.d(TAG, "Found equipment at position " + position + ": " + equipment.getName());
                return equipment;
            }
        }
        
        Log.d(TAG, "No equipment found at position: " + position);
        return null;
    }
    
    private void updateCellAppearance(MaterialCardView cellView, int position) {
        Equipment equipment = findEquipmentByPosition(position);
        TextView cellText = (TextView) cellView.getChildAt(0);
        
        if (equipment != null) {
            // Set equipment name
            cellText.setText(equipment.getName());
            Log.d(TAG, "Updating cell at position " + position + " with equipment: " + equipment.getName());
            
            // Set background color based on status
            int backgroundColor;
            switch (equipment.getStatus().toLowerCase()) {
                case "available":
                    backgroundColor = Color.parseColor("#E8F5E9"); // Light green
                    break;
                case "in_use":
                    backgroundColor = Color.parseColor("#E3F2FD"); // Light blue
                    break;
                case "maintenance":
                    backgroundColor = Color.parseColor("#FFF3E0"); // Light orange
                    break;
                default:
                    backgroundColor = Color.WHITE;
            }
            cellView.setCardBackgroundColor(backgroundColor);
        } else {
            cellText.setText("");
            cellView.setCardBackgroundColor(Color.WHITE);
        }
    }

    private void loadEquipment() {
        Log.d(TAG, "Loading equipment for library: " + libraryId);
        binding.progressBar.setVisibility(View.VISIBLE);
        
        if (equipmentList == null) {
            equipmentList = new ArrayList<>();
        } else {
            equipmentList.clear();
        }

        firebaseService.getEquipmentByLibrary(libraryId)
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d(TAG, "Successfully retrieved equipment data from Firestore");
                Log.d(TAG, "Number of documents: " + queryDocumentSnapshots.size());
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        String name = document.getString("name");
                        String specifications = document.getString("specifications");
                        Boolean isFunctional = document.getBoolean("functional");
                        Long position = document.getLong("position");
                        String status = document.getString("status");

                        Log.d(TAG, "Processing equipment document - Name: " + name + 
                              ", Position: " + position + 
                              ", Status: " + status);

                        if (name != null && position != null) {
                            Equipment equipment = new Equipment(
                                name,
                                document.getString("description") != null ? document.getString("description") : "",
                                specifications != null ? specifications : "",
                                status != null ? status : (isFunctional != null && isFunctional ? "available" : "maintenance"),
                                libraryId,
                                document.getString("departmentId") != null ? document.getString("departmentId") : "",
                                position.intValue(),
                                document.getLong("purchaseDate") != null ? document.getLong("purchaseDate") : System.currentTimeMillis()
                            );
                            equipment.setId(document.getId());
                            equipmentList.add(equipment);
                            
                            Log.d(TAG, "Added equipment to list: " + equipment.getName() + 
                                  " at position: " + equipment.getPosition() + 
                                  " with ID: " + equipment.getId() +
                                  " status: " + equipment.getStatus());
                        } else {
                            Log.e(TAG, "Missing required fields in document: " + document.getId());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing equipment document: " + document.getId(), e);
                    }
                }
                
                Log.d(TAG, "Total equipment loaded: " + equipmentList.size());
                
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (equipmentList.isEmpty()) {
                        Log.d(TAG, "No equipment found for library");
                        binding.emptyView.setVisibility(View.VISIBLE);
                        binding.gridLayout.setVisibility(View.GONE);
                    } else {
                        Log.d(TAG, "Updating grid with loaded equipment");
                        binding.emptyView.setVisibility(View.GONE);
                        binding.gridLayout.setVisibility(View.VISIBLE);
                        updateGridWithEquipment();
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading equipment", e);
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load equipment", Toast.LENGTH_SHORT).show();
                });
            });
    }

    private void updateGridWithEquipment() {
        Log.d(TAG, "Updating grid with equipment data");
        for (int i = 0; i < binding.gridLayout.getChildCount(); i++) {
            View cell = binding.gridLayout.getChildAt(i);
            TextView textView = cell.findViewById(R.id.cellText);
            int position = (int) cell.getTag();
            
            Equipment equipment = findEquipmentByPosition(position);
            if (equipment != null) {
                Log.d(TAG, "Setting equipment " + equipment.getName() + " at position " + position);
                textView.setText(equipment.getName());
                
                // Set background color based on status
                int backgroundColor;
                switch (equipment.getStatus().toLowerCase()) {
                    case "available":
                        backgroundColor = getResources().getColor(R.color.cell_available);
                        break;
                    case "in_use":
                        backgroundColor = getResources().getColor(R.color.cell_in_use);
                        break;
                    case "maintenance":
                        backgroundColor = getResources().getColor(R.color.cell_maintenance);
                        break;
                    default:
                        backgroundColor = getResources().getColor(R.color.cell_empty);
                }
                cell.setBackgroundColor(backgroundColor);
            } else {
                Log.d(TAG, "No equipment at position " + position + ", clearing cell");
                textView.setText("");
                cell.setBackgroundColor(getResources().getColor(R.color.cell_empty));
            }
        }
    }

    private void showAddEquipmentDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Add Equipment");
        
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_equipment, null);
        
        EditText nameInput = dialogView.findViewById(R.id.editTextEquipmentDescription);
        EditText specInput = dialogView.findViewById(R.id.editTextEquipmentSpecification);
        SwitchMaterial statusSwitch = dialogView.findViewById(R.id.equipmentStatusSwitch);
        
        builder.setView(dialogView);
        
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String specification = specInput.getText().toString().trim();
            boolean isFunctional = statusSwitch.isChecked();
            
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Please enter equipment name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show position selection dialog
            showPositionSelectionDialog(name, specification, isFunctional);
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showPositionSelectionDialog(String name, String specification, boolean isFunctional) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Select Position");
        builder.setMessage("Click on a cell in the grid to place the equipment");

        // Create a temporary dialog to show while selecting position
        androidx.appcompat.app.AlertDialog positionDialog = builder.create();
        positionDialog.show();

        // Set up a one-time click listener for position selection
        View.OnClickListener positionSelector = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (int) v.getTag();
                if (findEquipmentByPosition(position) != null) {
                    Toast.makeText(EquipmentActivity.this, "Position already occupied", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create and save equipment with selected position
                Equipment equipment = new Equipment(
                    name,
                    "", // Empty description since we don't use it
                    specification,
                    isFunctional ? "available" : "maintenance",
                    libraryId,
                    "",
                    position,
                    System.currentTimeMillis()
                );

                firebaseService.addEquipment(libraryId, equipment)
                    .addOnSuccessListener(aVoid -> {
                        runOnUiThread(() -> {
                            Toast.makeText(EquipmentActivity.this, "Equipment added successfully", Toast.LENGTH_SHORT).show();
                            loadEquipment();
                            positionDialog.dismiss();
                        });
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> {
                            Toast.makeText(EquipmentActivity.this, "Failed to add equipment", Toast.LENGTH_SHORT).show();
                            positionDialog.dismiss();
                        });
                    });

                // Remove the temporary click listener
                for (int i = 0; i < binding.gridLayout.getChildCount(); i++) {
                    View child = binding.gridLayout.getChildAt(i);
                    child.setOnClickListener(null);
                }
                // Restore original click listeners
                createGridLayout();
            }
        };

        // Set temporary click listener on all cells
        for (int i = 0; i < binding.gridLayout.getChildCount(); i++) {
            View child = binding.gridLayout.getChildAt(i);
            child.setOnClickListener(positionSelector);
        }
    }

    private boolean isAdmin() {
        return "admin".equals(userRole);
    }

    @Override
    public void onEquipmentClick(Equipment equipment) {
        showEquipmentDialog(equipment);
    }

    @Override
    public void onEquipmentLongClick(Equipment equipment) {
        if (isAdmin()) {
            showEquipmentOptionsDialog(equipment);
        }
    }

    private void showEquipmentOptionsDialog(Equipment equipment) {
        // Create options array based on user role
        String[] options;
        if (isAdmin()) {
            options = new String[]{"View Details", "Edit", "Delete", "Change Status"};
        } else {
            options = new String[]{"View Details"};
        }
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Equipment Options");
        builder.setItems(options, (dialog, which) -> {
            if (isAdmin()) {
                switch (which) {
                    case 0: // View Details
                        showEquipmentDetailsDialog(equipment);
                        break;
                    case 1: // Edit
                        showEditEquipmentDialog(equipment);
                        break;
                    case 2: // Delete
                        showDeleteConfirmationDialog(equipment);
                        break;
                    case 3: // Change Status
                        showChangeStatusDialog(equipment);
                        break;
                }
            } else {
                // For regular users, only show details
                showEquipmentDetailsDialog(equipment);
            }
        });
        
        builder.create().show();
    }
    
    private void showDeleteConfirmationDialog(Equipment equipment) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Equipment")
            .setMessage("Are you sure you want to delete this equipment?")
            .setPositiveButton("Yes", (dialog, which) -> {
                firebaseService.deleteEquipment(equipment.getId(), equipment)
                    .addOnSuccessListener(aVoid -> {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Equipment deleted", Toast.LENGTH_SHORT).show();
                            loadEquipment();
                        });
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Failed to delete equipment", Toast.LENGTH_SHORT).show();
                        });
                    });
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void setupSearchView() {
        // Remove the click overlay when search is active
        binding.searchView.setOnSearchClickListener(v -> {
            binding.searchClickOverlay.setVisibility(View.GONE);
        });

        binding.searchView.setOnCloseListener(() -> {
            binding.searchClickOverlay.setVisibility(View.VISIBLE);
            return false;
        });

        binding.searchClickOverlay.setOnClickListener(v -> {
            binding.searchClickOverlay.setVisibility(View.GONE);
            binding.searchView.setIconified(false);
            binding.searchView.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });

        // Make sure the close button is always visible
        binding.searchView.setIconifiedByDefault(false);

        // Set up close button listener
        ImageView closeButton = binding.searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        if (closeButton != null) {
            closeButton.setVisibility(View.VISIBLE);
            closeButton.setOnClickListener(v -> {
                // Just clear the text without triggering the text change listener
                EditText searchEditText = binding.searchView.findViewById(androidx.appcompat.R.id.search_src_text);
                if (searchEditText != null) {
                    searchEditText.setText("");
                }
            });
        }

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                hideKeyboard();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Only perform search for non-empty text and when not cleared by X button
                if (newText.length() >= 2) {
                    performSearch(newText);
                }
                return true;
            }
        });

        binding.openButton.setOnClickListener(v -> {
            if (!currentSearchResults.isEmpty()) {
                // Get the first found equipment and show its dialog
                Equipment firstFound = currentSearchResults.get(0);
                showEquipmentDialog(firstFound);
            } else {
                Toast.makeText(this, getString(R.string.no_results), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && binding.searchView != null) {
            imm.hideSoftInputFromWindow(binding.searchView.getWindowToken(), 0);
        }
    }

    private void performSearch(String query) {
        if (query == null || query.isEmpty()) {
            binding.searchResults.setText("");
            binding.searchResults.setVisibility(View.GONE);
            binding.openButton.setVisibility(View.GONE);
            return;
        }

        currentSearchResults.clear();
        
        for (Equipment equipment : equipmentList) {
            if (equipment.getName().toLowerCase().contains(query.toLowerCase())) {
                currentSearchResults.add(equipment);
            }
        }

        if (!currentSearchResults.isEmpty()) {
            int count = currentSearchResults.size();
            binding.searchResults.setText(count + " " + getString(count == 1 ? R.string.result_found : R.string.results_found));
            binding.searchResults.setVisibility(View.VISIBLE);
            binding.openButton.setVisibility(View.VISIBLE);
        } else {
            binding.searchResults.setText(R.string.no_results);
            binding.searchResults.setVisibility(View.VISIBLE);
            binding.openButton.setVisibility(View.GONE);
        }
    }

    private void showEquipmentDialog(Equipment equipment) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Equipment Details");

        // Create a custom layout for the dialog
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_equipment_details, null);
        builder.setView(dialogView);

        // Initialize views
        TextView txtName = dialogView.findViewById(R.id.txtEquipmentName);
        TextView txtSpecification = dialogView.findViewById(R.id.txtEquipmentSpecification);
        TextView txtStatus = dialogView.findViewById(R.id.txtEquipmentStatus);
        TextView txtPosition = dialogView.findViewById(R.id.txtEquipmentPosition);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);

        // Set equipment details
        txtName.setText(equipment.getName());
        txtSpecification.setText(equipment.getSpecification());
        txtStatus.setText(equipment.getStatus());
        txtPosition.setText(String.format("Position: %d", equipment.getPosition()));

        // Create the dialog
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // Set up close button
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showChangeStatusDialog(Equipment equipment) {
        String[] statusOptions = {"Available", "In Use", "Maintenance"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Change Status");
        builder.setItems(statusOptions, (dialog, which) -> {
            String newStatus;
            switch (which) {
                case 0:
                    newStatus = "available";
                    break;
                case 1:
                    newStatus = "in_use";
                    break;
                case 2:
                    newStatus = "maintenance";
                    break;
                default:
                    newStatus = "available";
            }
            
            // Update equipment status
            equipment.setStatus(newStatus);
            firebaseService.updateEquipment(equipment.getId(), equipment)
                .addOnSuccessListener(aVoid -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                        loadEquipment(); // Reload data
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                    });
                });
        });
        
        builder.create().show();
    }

    private void showAddEquipmentDialog(int position) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Add Equipment");
        
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_equipment, null);
        
        TextInputEditText nameInput = dialogView.findViewById(R.id.editTextEquipmentDescription);
        TextInputEditText specInput = dialogView.findViewById(R.id.editTextEquipmentSpecification);
        SwitchMaterial statusSwitch = dialogView.findViewById(R.id.equipmentStatusSwitch);
        
        builder.setView(dialogView);
        
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String specification = specInput.getText() != null ? specInput.getText().toString().trim() : "";
            boolean isFunctional = statusSwitch.isChecked();
            
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Please enter equipment name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create and save equipment
            Equipment equipment = new Equipment(
                name,
                "", // Empty description since we don't use it
                specification,
                isFunctional ? "available" : "maintenance",
                libraryId,
                "",
                position,
                System.currentTimeMillis()
            );

            firebaseService.addEquipment(libraryId, equipment)
                .addOnSuccessListener(aVoid -> {
                    runOnUiThread(() -> {
                        Toast.makeText(EquipmentActivity.this, "Equipment added successfully", Toast.LENGTH_SHORT).show();
                        loadEquipment();
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Toast.makeText(EquipmentActivity.this, "Failed to add equipment", Toast.LENGTH_SHORT).show();
                    });
                });
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showEditEquipmentDialog(Equipment equipment) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Edit Equipment");
        
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_equipment, null);
        
        TextInputEditText nameInput = dialogView.findViewById(R.id.editTextEquipmentDescription);
        TextInputEditText specInput = dialogView.findViewById(R.id.editTextEquipmentSpecification);
        SwitchMaterial statusSwitch = dialogView.findViewById(R.id.equipmentStatusSwitch);
        
        // Set current values
        nameInput.setText(equipment.getName());
        specInput.setText(equipment.getSpecification());
        statusSwitch.setChecked("available".equals(equipment.getStatus()));
        
        builder.setView(dialogView);
        
        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String specification = specInput.getText() != null ? specInput.getText().toString().trim() : "";
            boolean isFunctional = statusSwitch.isChecked();
            
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Please enter equipment name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update equipment
            equipment.setName(name);
            equipment.setSpecification(specification);
            equipment.setStatus(isFunctional ? "available" : "maintenance");
            equipment.setUpdatedAt(System.currentTimeMillis());

            firebaseService.updateEquipment(equipment.getId(), equipment)
                .addOnSuccessListener(aVoid -> {
                    runOnUiThread(() -> {
                        Toast.makeText(EquipmentActivity.this, "Equipment updated successfully", Toast.LENGTH_SHORT).show();
                        loadEquipment();
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Toast.makeText(EquipmentActivity.this, "Failed to update equipment", Toast.LENGTH_SHORT).show();
                    });
                });
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showEquipmentDetailsDialog(Equipment equipment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Equipment Details");

        // Create a custom layout for the dialog
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_equipment_details, null);
        builder.setView(dialogView);

        // Initialize views
        TextView txtName = dialogView.findViewById(R.id.txtEquipmentName);
        TextView txtSpecification = dialogView.findViewById(R.id.txtEquipmentSpecification);
        TextView txtStatus = dialogView.findViewById(R.id.txtEquipmentStatus);
        TextView txtPosition = dialogView.findViewById(R.id.txtEquipmentPosition);
        EditText queryInput = dialogView.findViewById(R.id.editTextQuery);
        Button btnSubmitQuery = dialogView.findViewById(R.id.btnSubmitQuery);
        Button btnClose = dialogView.findViewById(R.id.btnClose);

        // Set equipment details
        txtName.setText("Name: " + equipment.getName());
        
        if (equipment.getSpecification() != null && !equipment.getSpecification().isEmpty()) {
            txtSpecification.setText("Specification: " + equipment.getSpecification());
            txtSpecification.setVisibility(View.VISIBLE);
        } else {
            txtSpecification.setVisibility(View.GONE);
        }
        
        // Set status based on functional state
        String status = "available".equals(equipment.getStatus()) ? "ON" : "OFF";
        txtStatus.setText("Status: " + status);
        
        txtPosition.setText("Position: " + (equipment.getPosition() + 1));

        // Set up Submit Query button
        btnSubmitQuery.setOnClickListener(v -> {
            String queryText = queryInput.getText().toString().trim();
            if (queryText.isEmpty()) {
                Toast.makeText(this, "Please enter your query", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create query object
            Map<String, Object> query = new HashMap<>();
            query.put("equipmentId", equipment.getId());
            query.put("equipmentName", equipment.getName());
            query.put("position", equipment.getPosition());
            query.put("queryText", queryText);
            query.put("status", "open");
            query.put("timestamp", com.google.firebase.Timestamp.now());
            query.put("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());

            // Save query to Firestore
            db.collection("queries")
                .add(query)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Query submitted successfully", Toast.LENGTH_SHORT).show();
                    queryInput.setText(""); // Clear the input
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit query", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error submitting query", e);
                });
        });

        btnClose.setOnClickListener(v -> builder.create().dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        firebaseService.cleanup();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideKeyboard();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}