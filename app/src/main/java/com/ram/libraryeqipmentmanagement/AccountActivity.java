package com.ram.libraryeqipmentmanagement;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ram.libraryeqipmentmanagement.databinding.ActivityAccountBinding;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;

import java.util.HashMap;
import java.util.Map;

public class AccountActivity extends AppCompatActivity {
    private ActivityAccountBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private FirebaseService firebaseService;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();
        firebaseService = FirebaseService.getInstance(this);
        userId = firebaseService.getCurrentUserId();

        // Set up toolbar
        setSupportActionBar(binding.topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Account Settings");
        }

        // Load user data
        loadUserData();

        // Set up click listeners
        setupClickListeners();
    }

    private void loadUserData() {
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("AccountActivity", "Loading user data for userId: " + userId);
        
        db.collection("users").document(userId).get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    Log.d("AccountActivity", "User document exists");
                    String firstName = document.getString("firstName");
                    String lastName = document.getString("lastName");
                    String email = document.getString("email");
                    String role = document.getString("role");

                    Log.d("AccountActivity", "User data - FirstName: " + firstName + 
                        ", LastName: " + lastName + 
                        ", Email: " + email + 
                        ", Role: " + role);

                    if (firstName != null && lastName != null) {
                        binding.nameInput.setText(firstName + " " + lastName);
                    }
                    if (email != null) {
                        binding.emailInput.setText(email);
                    }
                    if (role != null) {
                        binding.roleInput.setText(role);
                    }
                } else {
                    Log.e("AccountActivity", "User document does not exist");
                    Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("AccountActivity", "Error loading user data: " + e.getMessage());
                Toast.makeText(this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void setupClickListeners() {
        // Save changes button
        binding.saveButton.setOnClickListener(v -> saveUserData());

        // Change password button
        binding.changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());

        // Delete account button
        binding.deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());

        // Change photo button
        binding.changePhotoButton.setOnClickListener(v -> {
            // TODO: Implement photo change functionality
            Toast.makeText(this, "Photo change functionality coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveUserData() {
        if (userId == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.saveButton.setEnabled(false);

        // Get updated values
        String fullName = binding.nameInput.getText().toString().trim();
        String[] nameParts = fullName.split(" ");
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Create user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("updatedAt", System.currentTimeMillis());

        // Update Firestore
        db.collection("users").document(userId)
            .update(userData)
            .addOnSuccessListener(aVoid -> {
                // Hide progress
                binding.progressBar.setVisibility(View.GONE);
                binding.saveButton.setEnabled(true);

                // Set result and finish
                Intent resultIntent = new Intent();
                resultIntent.putExtra("firstName", firstName);
                resultIntent.putExtra("lastName", lastName);
                setResult(RESULT_OK, resultIntent);
                
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                // Hide progress
                binding.progressBar.setVisibility(View.GONE);
                binding.saveButton.setEnabled(true);
                Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
            });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText currentPasswordInput = view.findViewById(R.id.currentPasswordInput);
        TextInputEditText newPasswordInput = view.findViewById(R.id.newPasswordInput);
        TextInputEditText confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput);

        builder.setView(view);

        builder.setPositiveButton("Change", (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString();
            String newPassword = newPasswordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();

            if (newPassword.equals(confirmPassword)) {
                changePassword(currentPassword, newPassword);
            } else {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        if (currentUser != null) {
            // Reauthenticate user
            currentUser.reauthenticate(com.google.firebase.auth.EmailAuthProvider
                .getCredential(currentUser.getEmail(), currentPassword))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update password
                        currentUser.updatePassword(newPassword)
                            .addOnCompleteListener(passwordTask -> {
                                if (passwordTask.isSuccessful()) {
                                    Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Error changing password", Toast.LENGTH_SHORT).show();
                                }
                            });
                    } else {
                        Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Account");

        // Inflate the custom layout
        View view = getLayoutInflater().inflate(R.layout.dialog_delete_account, null);
        TextInputEditText passwordInput = view.findViewById(R.id.passwordInput);
        builder.setView(view);

        builder.setPositiveButton("Delete", null); // We'll set the listener later
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            deleteButton.setOnClickListener(v -> {
                String password = passwordInput.getText().toString();
                if (password.isEmpty()) {
                    passwordInput.setError("Password is required");
                    return;
                }
                // Proceed with account deletion
                deleteAccountWithPassword(password, dialog);
            });
        });

        dialog.show();
    }

    private void deleteAccountWithPassword(String password, AlertDialog dialog) {
        if (currentUser != null) {
            // First, re-authenticate the user
            currentUser.reauthenticate(com.google.firebase.auth.EmailAuthProvider
                .getCredential(currentUser.getEmail(), password))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Authentication successful, proceed with deletion
                        // Delete user document from Firestore first
                        db.collection("users").document(currentUser.getUid())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Now delete the user account
                                currentUser.delete()
                                    .addOnCompleteListener(deleteTask -> {
                                        dialog.dismiss();
                                        if (deleteTask.isSuccessful()) {
                                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                            // Navigate to login screen
                                            Intent intent = new Intent(this, LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(this, "Error deleting account: " + deleteTask.getException().getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                            })
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                Toast.makeText(this, "Error deleting account data: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                    } else {
                        Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Send current data back when pressing back button
            String fullName = binding.nameInput.getText().toString().trim();
            String[] nameParts = fullName.split(" ");
            String firstName = nameParts.length > 0 ? nameParts[0] : "";
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            
            Intent resultIntent = new Intent();
            resultIntent.putExtra("firstName", firstName);
            resultIntent.putExtra("lastName", lastName);
            setResult(RESULT_OK, resultIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 