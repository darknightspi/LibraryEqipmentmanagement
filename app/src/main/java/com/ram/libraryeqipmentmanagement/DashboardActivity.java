package com.ram.libraryeqipmentmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.EditText;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ram.libraryeqipmentmanagement.databinding.ActivityDashboardBinding;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import com.ram.libraryeqipmentmanagement.LibraryEquipmentApplication;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.card.MaterialCardView;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "DashboardActivity";
    private ActivityDashboardBinding binding;
    private FirebaseService firebaseService;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private String userRole;
    private static final int ACCOUNT_ACTIVITY_REQUEST_CODE = 100;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Toolbar topAppBar;
    private ActivityResultLauncher<Intent> accountActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Activity Result Launcher
        accountActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshUserProfile();
                }
            }
        );

        // Initialize Firebase Service
        firebaseService = FirebaseService.getInstance(this);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        drawerLayout = binding.drawerLayout;
        topAppBar = binding.topAppBar;

        // Set up toolbar
        setSupportActionBar(topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Dashboard");
        }

        // Set up navigation drawer
        setupNavigationDrawer();

        // Set up back press handling
        setupBackPressHandling();

        // Get user info from Firebase and update UI
        loadUserRole();

        // Set up button click listeners with animations and role-based access
        setupClickListeners();

        // Load user profile data
        loadUserProfile();
    }

    private void setupNavigationDrawer() {
        actionBarDrawerToggle = new ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.nav_open,
            R.string.nav_close
        );
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // Set up navigation view
        binding.navView.setNavigationItemSelectedListener(this);
    }

    private void setupBackPressHandling() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void loadUserRole() {
        String userId = firebaseService.getCurrentUserId();
        if (userId != null) {
            firebaseService.getUserRole(userId)
                .addOnSuccessListener(role -> {
                    userRole = role;
                    runOnUiThread(() -> {
                        // Update UI based on role
                        NavigationView navigationView = binding.navView;
                        if (isAdmin()) {
                            // Show admin-only menu items
                            navigationView.getMenu().findItem(R.id.nav_reports).setVisible(true);
                            navigationView.getMenu().findItem(R.id.nav_analytics).setVisible(true);
                            navigationView.getMenu().findItem(R.id.nav_admin_requests).setVisible(true);
                        } else {
                            // Hide admin-only menu items
                            navigationView.getMenu().findItem(R.id.nav_reports).setVisible(false);
                            navigationView.getMenu().findItem(R.id.nav_analytics).setVisible(false);
                            navigationView.getMenu().findItem(R.id.nav_admin_requests).setVisible(false);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Error loading user role", Toast.LENGTH_SHORT).show());
                });
        }
    }

    private void loadUserProfile() {
        String userId = firebaseService.getCurrentUserId();
        if (userId == null) {
            Log.e("DashboardActivity", "User not authenticated - userId is null");
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DashboardActivity", "Loading user profile for userId: " + userId);
        Log.d("DashboardActivity", "Current user: " + (firebaseService.getCurrentUser() != null ? 
            firebaseService.getCurrentUser().getEmail() : "null"));
        
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Log.d("DashboardActivity", "User document exists");
                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    String fullName = "";
                    
                    Log.d("DashboardActivity", "First name: " + firstName + ", Last name: " + lastName);
                    
                    if (firstName != null && !firstName.isEmpty()) {
                        fullName = firstName;
                        if (lastName != null && !lastName.isEmpty()) {
                            fullName += " " + lastName;
                        }
                    } else {
                        // Fallback to email if name not found
                        FirebaseUser user = firebaseService.getCurrentUser();
                        if (user != null && user.getEmail() != null) {
                            fullName = user.getEmail().split("@")[0];
                        } else {
                            fullName = "User";
                        }
                    }
                    
                    final String displayName = fullName;
                    Log.d("DashboardActivity", "Display name: " + displayName);
                    
                    runOnUiThread(() -> {
                        // Update dashboard profile
                        binding.userNameText.setText(displayName);
                        
                        // Update navigation header
                        View headerView = binding.navView.getHeaderView(0);
                        TextView nameText = headerView.findViewById(R.id.nav_header_name);
                        TextView emailText = headerView.findViewById(R.id.nav_header_email);
                        
                        nameText.setText(displayName);
                        FirebaseUser user = firebaseService.getCurrentUser();
                        if (user != null && user.getEmail() != null) {
                            emailText.setText(user.getEmail());
                        }
                    });
                } else {
                    Log.e("DashboardActivity", "User document does not exist");
                    Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("DashboardActivity", "Error loading user profile: " + e.getMessage());
                Log.e("DashboardActivity", "Error details: " + e.toString());
                Toast.makeText(this, "Error loading user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void setupRealtimeListeners() {
        // Listen for department count changes
        firebaseService.getDepartmentsCount()
            .addOnSuccessListener(dataSnapshot -> {
                runOnUiThread(() -> {
                    // Update department count in analytics if needed
                });
            })
            .addOnFailureListener(e -> {
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error loading department count", Toast.LENGTH_SHORT).show());
            });

        // Listen for equipment status changes
        firebaseService.getEquipmentStatusCount()
            .addOnSuccessListener(dataSnapshot -> {
                runOnUiThread(() -> {
                    // Update equipment statistics in analytics if needed
                });
            })
            .addOnFailureListener(e -> {
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error loading equipment status", Toast.LENGTH_SHORT).show());
            });
    }

    private void setupClickListeners() {
        // Departments Button
        binding.departmentsButton.setOnClickListener(v -> {
            animateButtonClick(binding.departmentsButton);
            Intent intent = new Intent(this, DepartmentActivity.class);
            startActivity(intent);
        });

        // Statistics Button
        binding.statisticsButton.setOnClickListener(v -> {
            animateButtonClick(binding.statisticsButton);
            if (isAdmin()) {
                showOverallStatistics();
            } else {
                Toast.makeText(this, "Only administrators can access statistics", Toast.LENGTH_SHORT).show();
            }
        });

        // Open Queries Button
        binding.openQueriesButton.setOnClickListener(v -> {
            animateButtonClick(binding.openQueriesButton);
            Intent intent = new Intent(this, OpenQueriesActivity.class);
            startActivity(intent);
        });

        // Closed Queries Button
        binding.closedQueriesButton.setOnClickListener(v -> {
            animateButtonClick(binding.closedQueriesButton);
            Intent intent = new Intent(this, ClosedQueriesActivity.class);
            startActivity(intent);
        });
    }

    private void showRenameDepartmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Department");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                // Get selected department and update its name
                // This will be implemented when a department is selected
                Toast.makeText(this, "Please select a department first", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Department name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteDepartmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Department")
               .setMessage("Are you sure you want to delete this department? This action cannot be undone.")
               .setPositiveButton("Delete", (dialog, which) -> {
                   // Get selected department and delete it
                   // This will be implemented when a department is selected
                   Toast.makeText(this, "Please select a department first", Toast.LENGTH_SHORT).show();
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
               .show();
    }

    private boolean isAdmin() {
        return "admin".equalsIgnoreCase(userRole);
    }

    private void animateButtonClick(View view) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction(() ->
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start())
            .start();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Already in home
        } else if (id == R.id.nav_dashboard) {
            // Already in dashboard
        } else if (id == R.id.nav_reports) {
            if (isAdmin()) {
                // TODO: Show reports
                Toast.makeText(this, "Reports clicked", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Only administrators can access reports", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_analytics) {
            if (isAdmin()) {
                // TODO: Show analytics
                Toast.makeText(this, "Analytics clicked", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Only administrators can access analytics", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_settings) {
            // TODO: Show settings
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_account) {
            Intent intent = new Intent(this, AccountActivity.class);
            intent.putExtra("userRole", userRole);
            accountActivityLauncher.launch(intent);
        } else if (id == R.id.nav_admin_requests) {
            // Open Admin Requests Activity
            Intent intent = new Intent(this, AdminRequestActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            firebaseService.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void refreshUserProfile() {
        // Force refresh from Firestore
        db.collection("users")
            .document(firebaseService.getCurrentUserId())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    String fullName = "";
                    
                    if (firstName != null && !firstName.isEmpty()) {
                        fullName = firstName;
                        if (lastName != null && !lastName.isEmpty()) {
                            fullName += " " + lastName;
                        }
                    }
                    
                    final String displayName = fullName;
                    // Update dashboard profile
                    binding.userNameText.setText(displayName);
                    
                    // Update navigation header
                    View headerView = binding.navView.getHeaderView(0);
                    TextView nameText = headerView.findViewById(R.id.nav_header_name);
                    TextView emailText = headerView.findViewById(R.id.nav_header_email);
                    
                    nameText.setText(displayName);
                    FirebaseUser user = firebaseService.getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        emailText.setText(user.getEmail());
                    }
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Error refreshing profile", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile data when activity resumes
        loadUserProfile();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showOverallStatistics() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Create and show dialog with loading state
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_overall_statistics, null);
        AlertDialog dialog = builder.setView(dialogView)
            .setPositiveButton("OK", null)
            .create();
        dialog.show();

        // Initialize views
        TextView textTotalDepartments = dialogView.findViewById(R.id.textTotalDepartments);
        TextView textTotalLibraries = dialogView.findViewById(R.id.textTotalLibraries);
        TextView textTotalEquipment = dialogView.findViewById(R.id.textTotalEquipment);
        TextView textWorkingEquipment = dialogView.findViewById(R.id.textWorkingEquipment);
        TextView textNotWorkingEquipment = dialogView.findViewById(R.id.textNotWorkingEquipment);
        LinearLayout departmentStatsContainer = dialogView.findViewById(R.id.departmentStatsContainer);

        // Get all departments
        db.collection("departments")
            .get()
            .addOnSuccessListener(departmentSnapshots -> {
                int totalDepartments = departmentSnapshots.size();
                final int[] totalLibraries = {0};
                final int[] totalEquipment = {0};
                final int[] workingEquipment = {0};

                // Process each department
                for (QueryDocumentSnapshot departmentDoc : departmentSnapshots) {
                    String departmentId = departmentDoc.getId();
                    String departmentName = departmentDoc.getString("name");

                    // Create department stats view
                    View departmentStatsView = getLayoutInflater().inflate(R.layout.item_department_stats, departmentStatsContainer, false);
                    TextView textDepartmentName = departmentStatsView.findViewById(R.id.textDepartmentName);
                    TextView textLibraryCount = departmentStatsView.findViewById(R.id.textLibraryCount);
                    TextView textEquipmentCount = departmentStatsView.findViewById(R.id.textEquipmentCount);
                    TextView textWorkingCount = departmentStatsView.findViewById(R.id.textWorkingCount);
                    TextView textNotWorkingCount = departmentStatsView.findViewById(R.id.textNotWorkingCount);

                    textDepartmentName.setText(departmentName);
                    departmentStatsContainer.addView(departmentStatsView);

                    // Get libraries for this department
                    db.collection("libraries")
                        .whereEqualTo("departmentId", departmentId)
                        .get()
                        .addOnSuccessListener(librarySnapshots -> {
                            int departmentLibraries = librarySnapshots.size();
                            totalLibraries[0] += departmentLibraries;
                            final int[] departmentEquipment = {0};
                            final int[] departmentWorking = {0};

                            // Process each library
                            for (QueryDocumentSnapshot libraryDoc : librarySnapshots) {
                                String libraryId = libraryDoc.getId();

                                // Get equipment for this library
                                db.collection("libraries")
                                    .document(libraryId)
                                    .collection("equipment")
                                    .get()
                                    .addOnSuccessListener(equipmentSnapshots -> {
                                        int libraryEquipment = equipmentSnapshots.size();
                                        departmentEquipment[0] += libraryEquipment;
                                        totalEquipment[0] += libraryEquipment;

                                        for (QueryDocumentSnapshot equipmentDoc : equipmentSnapshots) {
                                            Boolean isFunctional = equipmentDoc.getBoolean("functional");
                                            if (Boolean.TRUE.equals(isFunctional)) {
                                                departmentWorking[0]++;
                                                workingEquipment[0]++;
                                            }
                                        }

                                        // Update department stats
                                        textLibraryCount.setText("Libraries: " + departmentLibraries);
                                        textEquipmentCount.setText("Total Equipment: " + departmentEquipment[0]);
                                        textWorkingCount.setText("Working: " + departmentWorking[0]);
                                        textNotWorkingCount.setText("Not Working: " + (departmentEquipment[0] - departmentWorking[0]));

                                        // Update overall stats
                                        textTotalDepartments.setText("Total Departments: " + totalDepartments);
                                        textTotalLibraries.setText("Total Libraries: " + totalLibraries[0]);
                                        textTotalEquipment.setText("Total Equipment: " + totalEquipment[0]);
                                        textWorkingEquipment.setText("Working Equipment: " + workingEquipment[0]);
                                        textNotWorkingEquipment.setText("Not Working Equipment: " + (totalEquipment[0] - workingEquipment[0]));
                                    });
                            }

                            if (librarySnapshots.isEmpty()) {
                                // Update department stats for departments with no libraries
                                textLibraryCount.setText("Libraries: 0");
                                textEquipmentCount.setText("Total Equipment: 0");
                                textWorkingCount.setText("Working: 0");
                                textNotWorkingCount.setText("Not Working: 0");
                            }
                        });
                }

                if (departmentSnapshots.isEmpty()) {
                    // Show empty state
                    textTotalDepartments.setText("Total Departments: 0");
                    textTotalLibraries.setText("Total Libraries: 0");
                    textTotalEquipment.setText("Total Equipment: 0");
                    textWorkingEquipment.setText("Working Equipment: 0");
                    textNotWorkingEquipment.setText("Not Working Equipment: 0");
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading statistics", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
    }
} 