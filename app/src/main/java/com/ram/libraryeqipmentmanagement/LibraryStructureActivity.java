package com.ram.libraryeqipmentmanagement;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.gridlayout.widget.GridLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.ram.libraryeqipmentmanagement.model.Equipment;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;
import android.graphics.Typeface;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.ram.libraryeqipmentmanagement.models.Query;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryStructureActivity extends AppCompatActivity {
    private static final String TAG = "LibraryStructureActivity";
    private GridLayout gridLayout;
    private String libraryId;
    private String libraryName;
    private int rows;
    private int columns;
    private String userRole;
    private FirebaseFirestore db;
    private boolean isAdmin = false;
    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private TextView searchResultsText;
    private List<Equipment> allEquipment;
    private FirebaseService firebaseService;
    private ListenerRegistration equipmentListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_structure);

        // Initialize Firebase Service
        firebaseService = FirebaseService.getInstance(getApplicationContext());

        // Get intent extras
        libraryId = getIntent().getStringExtra("libraryId");
        libraryName = getIntent().getStringExtra("libraryName");
        rows = getIntent().getIntExtra("rows", 10);
        columns = getIntent().getIntExtra("columns", 10);
        userRole = getIntent().getStringExtra("userRole");
        isAdmin = "admin".equalsIgnoreCase(userRole);

        // Get query-related extras
        boolean fromQuery = getIntent().getBooleanExtra("fromQuery", false);
        String queryId = getIntent().getStringExtra("queryId");
        int highlightPosition = getIntent().getIntExtra("position", -1);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        allEquipment = new ArrayList<>();

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(libraryName);
        }

        // Initialize views
        gridLayout = findViewById(R.id.gridLayout);
        searchEditText = findViewById(R.id.searchEditText);
        clearSearchButton = findViewById(R.id.clearSearchButton);
        searchResultsText = findViewById(R.id.searchResultsText);

        // Set up search functionality
        setupSearch();

        // Initialize GridLayout
        gridLayout.setRowCount(rows);
        gridLayout.setColumnCount(columns);
        
        // Create grid
        createGrid();
        
        // Load equipment data
        loadEquipment();

        // If coming from a query, highlight the specific equipment and show its details
        if (fromQuery && highlightPosition >= 0) {
            Log.d(TAG, "Loading query details - Position: " + highlightPosition + ", QueryID: " + queryId);
            // Post a delayed action to ensure the grid is loaded
            gridLayout.post(() -> {
                View cellView = gridLayout.getChildAt(highlightPosition);
                if (cellView instanceof MaterialCardView) {
                    MaterialCardView cardView = (MaterialCardView) cellView;
                    // Highlight the cell
                    highlightCell(cardView, true);
                    
                    // Scroll to the position
                    cardView.requestFocus();

                    // If there's a query ID, fetch and show the query details
                    if (queryId != null && !queryId.isEmpty()) {
                        Log.d(TAG, "Fetching query document with ID: " + queryId);
                        db.collection("queries")
                            .document(queryId)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Log.d(TAG, "Query document found, converting to Query object");
                                    try {
                                        Query query = documentSnapshot.toObject(Query.class);
                                        if (query != null) {
                                            query.setId(documentSnapshot.getId());
                                            Equipment equipment = findEquipmentAtPosition(highlightPosition);
                                            if (equipment != null) {
                                                Log.d(TAG, "Found equipment at position " + highlightPosition + ", showing details");
                                                showEquipmentDetailsWithHighlightedQuery(equipment, query);
                                            } else {
                                                Log.e(TAG, "Equipment not found at position: " + highlightPosition);
                                                Toast.makeText(this, "Equipment not found at the specified position", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Log.e(TAG, "Failed to convert document to Query object");
                                            Toast.makeText(this, "Error loading query details", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error processing query document", e);
                                        Toast.makeText(this, "Error processing query details", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.e(TAG, "Query document does not exist for ID: " + queryId);
                                    Toast.makeText(this, "Query not found", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching query document: " + e.getMessage(), e);
                                Toast.makeText(this, "Error loading query: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                    } else {
                        Log.e(TAG, "Invalid query ID provided");
                        Toast.makeText(this, "Invalid query ID", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Cell view not found at position: " + highlightPosition);
                    Toast.makeText(this, "Error finding equipment position", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void createGrid() {
        gridLayout.removeAllViews();
        int cellSize = getResources().getDimensionPixelSize(R.dimen.grid_cell_size);

        for (int i = 0; i < rows * columns; i++) {
            int row = i / columns;
            int col = i % columns;

            MaterialCardView cellView = new MaterialCardView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellSize;
            params.height = cellSize;
            params.setMargins(4, 4, 4, 4);
            params.rowSpec = GridLayout.spec(row);
            params.columnSpec = GridLayout.spec(col);

            TextView textView = new TextView(this);
            textView.setLayoutParams(new MaterialCardView.LayoutParams(
                MaterialCardView.LayoutParams.MATCH_PARENT,
                MaterialCardView.LayoutParams.MATCH_PARENT
            ));
            textView.setGravity(Gravity.CENTER);
            // Show serial number (1-based indexing)
            textView.setText(String.format("%d", i + 1));
            textView.setTag("cell_" + i);

            cellView.addView(textView);
            cellView.setLayoutParams(params);
            cellView.setCardBackgroundColor(getResources().getColor(R.color.cell_empty));
            cellView.setRadius(8);
            cellView.setCardElevation(4);

            final int position = i;
            // Allow all users to click, but handle differently based on role
            cellView.setOnClickListener(v -> {
                if (isAdmin) {
                    showEquipmentDialog(position);
                } else {
                    // For regular users, show equipment details if present
                    Equipment equipment = findEquipmentAtPosition(position);
                    if (equipment != null) {
                        showEquipmentDetails(equipment);
                    } else {
                        Toast.makeText(this, "No equipment at this position", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            gridLayout.addView(cellView);
        }

        // Create library card
        MaterialCardView libraryCard = new MaterialCardView(this);
        GridLayout.LayoutParams libraryParams = new GridLayout.LayoutParams();
        libraryParams.width = GridLayout.LayoutParams.MATCH_PARENT;
        libraryParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        libraryParams.columnSpec = GridLayout.spec(0, columns, 1f);
        libraryParams.setMargins(8, 8, 8, 8);
        libraryCard.setLayoutParams(libraryParams);
        libraryCard.setCardElevation(4);
        libraryCard.setRadius(8);

        // Create library name TextView
        TextView libraryName = new TextView(this);
        libraryName.setLayoutParams(new MaterialCardView.LayoutParams(
            MaterialCardView.LayoutParams.MATCH_PARENT,
            MaterialCardView.LayoutParams.WRAP_CONTENT
        ));
        libraryName.setText(this.libraryName);
        libraryName.setTextSize(16);
        libraryName.setGravity(Gravity.CENTER);
        libraryName.setPadding(16, 16, 16, 16);
        libraryCard.addView(libraryName);

        // Add library card to grid
        gridLayout.addView(libraryCard);
    }

    private void loadEquipment() {
        if (libraryId == null) return;

        Log.d(TAG, "Setting up real-time equipment listener for library: " + libraryId);

        // Remove any existing listeners
        if (equipmentListener != null) {
            equipmentListener.remove();
        }

        // Set up real-time listener
        equipmentListener = db.collection("libraries")
            .document(libraryId)
            .collection("equipment")
            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    Log.e(TAG, "Error listening to equipment updates: " + e.getMessage());
                    return;
                }

                if (queryDocumentSnapshots != null) {
                    Log.d(TAG, "Equipment data changed. Processing updates...");
                    
                    // Clear existing equipment
                    allEquipment.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String name = document.getString("name");
                            String specification = document.getString("specification");
                            String status = document.getString("status");
                            Long position = document.getLong("position");

                            Log.d(TAG, "Processing equipment - Name: " + name + 
                                  ", Status: " + status + 
                                  ", Position: " + position);

                            if (name != null && position != null) {
                                Equipment equipment = new Equipment(
                                    name,
                                    "", // description
                                    specification != null ? specification : "",
                                    status != null ? status : "maintenance",
                                    libraryId,
                                    "", // departmentId
                                    position.intValue(),
                                    System.currentTimeMillis() // purchaseDate
                                );
                                equipment.setId(document.getId());
                                allEquipment.add(equipment);
                                updateCell(equipment.getPosition(), equipment);
                                
                                Log.d(TAG, "Added equipment to list - Name: " + equipment.getName() + 
                                      ", Status: " + equipment.getStatus() + 
                                      ", Position: " + equipment.getPosition());
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error parsing equipment document: " + document.getId(), ex);
                        }
                    }
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the listener when activity is destroyed
        if (equipmentListener != null) {
            equipmentListener.remove();
        }
    }

    private void showEquipmentDialog(int position) {
        // Get existing equipment data if any
        View cellView = gridLayout.getChildAt(position);
        Equipment existingEquipment = null;
        if (cellView instanceof MaterialCardView) {
            existingEquipment = (Equipment) cellView.getTag();
        }

        AddEquipmentDialog dialog = new AddEquipmentDialog(
            this,
            equipment -> {
                equipment.setPosition(position);
                saveEquipment(position, equipment);
            },
            existingEquipment,
            libraryId
        );
        dialog.show();
    }

    private void saveEquipment(int position, Equipment equipment) {
        if (libraryId == null) return;

        Log.d(TAG, "Saving equipment at position: " + position + " with status: " + equipment.getStatus());

        // Create a map of equipment data
        java.util.Map<String, Object> equipmentData = new java.util.HashMap<>();
        equipmentData.put("position", position);
        equipmentData.put("name", equipment.getName());
        equipmentData.put("specification", equipment.getSpecification());
        equipmentData.put("status", equipment.getStatus());
        equipmentData.put("timestamp", com.google.firebase.Timestamp.now());

        // Save to Firestore
        db.collection("libraries")
            .document(libraryId)
            .collection("equipment")
            .document(String.valueOf(position))
            .set(equipmentData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Equipment saved successfully. Status: " + equipment.getStatus());
                
                // Update the local equipment list
                boolean found = false;
                for (int i = 0; i < allEquipment.size(); i++) {
                    if (allEquipment.get(i).getPosition() == position) {
                        allEquipment.set(i, equipment);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    allEquipment.add(equipment);
                }
                
                // Update the UI
                updateCell(position, equipment);
                Toast.makeText(this, "Equipment saved successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error saving equipment: " + e.getMessage());
                Toast.makeText(this, "Error saving equipment", Toast.LENGTH_SHORT).show();
            });
    }

    private void updateCell(int position, Equipment equipment) {
        View cellView = gridLayout.getChildAt(position);
        if (cellView instanceof MaterialCardView) {
            MaterialCardView cardView = (MaterialCardView) cellView;
            TextView textView = (TextView) cardView.getChildAt(0);
            
            // Update text only if equipment has a name
            if (equipment.getName() != null && !equipment.getName().isEmpty()) {
                textView.setText(equipment.getName());
                
                // Set background color based on actual status
                int backgroundColor;
                switch (equipment.getStatus().toLowerCase()) {
                    case "available":
                        backgroundColor = getResources().getColor(R.color.cell_functional);
                        break;
                    case "maintenance":
                        backgroundColor = getResources().getColor(R.color.cell_not_functional);
                        break;
                    case "in_use":
                        backgroundColor = getResources().getColor(R.color.cell_in_use);
                        break;
                    default:
                        backgroundColor = getResources().getColor(R.color.cell_empty);
                }
                cardView.setCardBackgroundColor(backgroundColor);
            } else {
                // Show serial number if no equipment name
                textView.setText(String.format("%d", position + 1));
                cardView.setCardBackgroundColor(getResources().getColor(R.color.cell_empty));
            }
            
            cardView.setTag(equipment);
        }
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString().toLowerCase().trim();
                clearSearchButton.setVisibility(searchText.isEmpty() ? View.GONE : View.VISIBLE);
                if (searchText.isEmpty()) {
                    resetSearch();
                } else {
                    performSearch(searchText);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            resetSearch();
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                    getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
    }

    private void performSearch(String searchText) {
        int matchCount = 0;
        boolean hasMatches = false;

        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View cellView = gridLayout.getChildAt(i);
            if (cellView instanceof MaterialCardView) {
                MaterialCardView cardView = (MaterialCardView) cellView;
                Equipment equipment = (Equipment) cardView.getTag();

                if (equipment != null && equipment.getName() != null) {
                    boolean matches = equipment.getName().toLowerCase().contains(searchText) ||
                                   (equipment.getSpecification() != null && 
                                    equipment.getSpecification().toLowerCase().contains(searchText));

                    if (matches) {
                        highlightCell(cardView, true);
                        matchCount++;
                        hasMatches = true;
                        
                        // Scroll to first match
                        if (matchCount == 1) {
                            cardView.requestFocus();
                        }
                    } else {
                        highlightCell(cardView, false);
                    }
                } else {
                    highlightCell(cardView, false);
                }
            }
        }

        // Update search results message
        if (hasMatches) {
            String message = matchCount == 1 
                ? "Found 1 equipment matching \"" + searchText + "\""
                : "Found " + matchCount + " equipment matching \"" + searchText + "\"";
            showSearchResults(message, true);
        } else {
            showSearchResults("No equipment found matching \"" + searchText + "\"", false);
        }
    }

    private void highlightCell(MaterialCardView cardView, boolean highlight) {
        if (highlight) {
            cardView.setStrokeColor(getResources().getColor(R.color.blue));
            cardView.setStrokeWidth(4);
            cardView.setCardElevation(8);
            // Add a pulsing animation
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 1.1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(cardView, "scaleY", 1f, 1.1f);
            
            scaleX.setRepeatMode(ValueAnimator.REVERSE);
            scaleX.setRepeatCount(1);
            scaleX.setDuration(500);
            
            scaleY.setRepeatMode(ValueAnimator.REVERSE);
            scaleY.setRepeatCount(1);
            scaleY.setDuration(500);
            
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleX, scaleY);
            animatorSet.start();
        } else {
            cardView.setStrokeWidth(0);
            cardView.setCardElevation(4);
            cardView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start();
        }
    }

    private void showSearchResults(String message, boolean found) {
        searchResultsText.setText(message);
        searchResultsText.setTextColor(getResources().getColor(found ? R.color.green : R.color.red));
        searchResultsText.setVisibility(View.VISIBLE);
    }

    private void resetSearch() {
        searchResultsText.setVisibility(View.GONE);
        clearSearchButton.setVisibility(View.GONE);
        
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View cellView = gridLayout.getChildAt(i);
            if (cellView instanceof MaterialCardView) {
                highlightCell((MaterialCardView) cellView, false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_library_structure, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_statistics) {
            showStatistics();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showStatistics() {
        db.collection("libraries")
            .document(libraryId)
            .collection("equipment")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int totalEquipment = 0;
                int workingEquipment = 0;

                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Boolean isFunctional = document.getBoolean("functional");
                    String name = document.getString("name");
                    
                    if (name != null && !name.isEmpty()) {
                        totalEquipment++;
                        if (Boolean.TRUE.equals(isFunctional)) {
                            workingEquipment++;
                        }
                    }
                }

                LibraryStatisticsDialog dialog = LibraryStatisticsDialog.newInstance(
                    totalEquipment,
                    workingEquipment,
                    totalEquipment - workingEquipment
                );
                dialog.show(getSupportFragmentManager(), "LibraryStatisticsDialog");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error calculating statistics", e);
                Toast.makeText(this, "Error calculating statistics", Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private Equipment findEquipmentAtPosition(int position) {
        Log.d(TAG, "Searching for equipment at position: " + position);
        Log.d(TAG, "Current equipment list size: " + (allEquipment != null ? allEquipment.size() : 0));
        
        if (allEquipment == null) {
            Log.e(TAG, "Equipment list is null");
            return null;
        }
        
        for (Equipment equipment : allEquipment) {
            if (equipment.getPosition() == position) {
                Log.d(TAG, "Found equipment: " + equipment.getName() + " at position: " + position);
                return equipment;
            }
        }
        
        Log.d(TAG, "No equipment found at position: " + position);
        return null;
    }

    private void showEquipmentDetails(Equipment equipment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Equipment Details");
        
        // Create a custom layout
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

    private void showEquipmentDetailsWithHighlightedQuery(Equipment equipment, Query query) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Equipment Details");

        // Create a custom layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        // Equipment details
        TextView detailsText = new TextView(this);
        String details = String.format(
            "Description: %s\nSpecifications: %s\nStatus: %s",
            equipment.getName(),
            equipment.getSpecification(),
            "available".equals(equipment.getStatus()) ? "Functional" : "Not Functional"
        );
        detailsText.setText(details);
        detailsText.setTextSize(16);
        layout.addView(detailsText);

        // Add spacing
        Space space = new Space(this);
        space.setMinimumHeight(24);
        layout.addView(space);

        // Active query section
        TextView queryHeader = new TextView(this);
        queryHeader.setText("Active Query:");
        queryHeader.setTextSize(18);
        queryHeader.setTypeface(null, Typeface.BOLD);
        layout.addView(queryHeader);

        // Query details with highlighted background
        TextView queryText = new TextView(this);
        String queryDetails = String.format(
            "Query Text: %s\nStatus: %s\nDate: %s",
            query.getQueryText(),
            query.getStatus(),
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(query.getTimestamp())
        );
        queryText.setText(queryDetails);
        queryText.setTextSize(16);
        queryText.setBackground(ContextCompat.getDrawable(this, R.drawable.highlighted_query_background));
        layout.addView(queryText);

        builder.setView(layout);
        builder.setPositiveButton("OK", null);
        builder.show();
    }
} 