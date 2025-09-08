package com.ram.libraryeqipmentmanagement;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ram.libraryeqipmentmanagement.databinding.ActivityLoginBinding;
import com.ram.libraryeqipmentmanagement.model.User;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;
import java.util.HashMap;
import java.util.Map;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseService firebaseService;
    private boolean isLoginMode = true;
    private boolean isAnimating = false;
    private float initialX = 0f;
    private float initialY = 0f;
    private static final float SWIPE_THRESHOLD = 100f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set initial background color
        binding.getRoot().setBackgroundColor(Color.parseColor("#3F5EFB"));

        // Hide all containers initially
        binding.loginContainer.setVisibility(View.GONE);
        binding.registrationContainer.setVisibility(View.GONE);
        binding.swipeUpIndicator.setVisibility(View.GONE);
        binding.swipeDownIndicator.setVisibility(View.GONE);
        binding.blueStrip.setVisibility(View.GONE);
        binding.welcomeContainer.setVisibility(View.GONE);

        // Initialize Firebase and setup listeners
        initializeFirebase();

        // Start initial animation after a short delay
        binding.getRoot().postDelayed(new Runnable() {
            @Override
            public void run() {
                startInitialAnimation();
            }
        }, 100);
    }

    private void initializeFirebase() {
        try {
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            firebaseService = FirebaseService.getInstance(this);

            // Check if user is already signed in
            if (auth.getCurrentUser() != null) {
                checkUserRoleAndNavigate();
                return;
            }

            setupClickListeners();
            setupSwipeGesture();
        } catch (Exception e) {
            showToast("Error initializing Firebase: " + e.getMessage());
        }
    }

    private void startInitialAnimation() {
        // Show login container
        binding.loginContainer.setVisibility(View.VISIBLE);
        binding.blueStrip.setVisibility(View.VISIBLE);
        binding.welcomeContainer.setVisibility(View.VISIBLE);

        // Load and start animation
        Animation slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        binding.loginContainer.startAnimation(slideUpAnim);
        binding.blueStrip.startAnimation(slideUpAnim);
        binding.welcomeContainer.startAnimation(slideUpAnim);

        slideUpAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Show swipe indicator with fade animation
                binding.swipeUpIndicator.setVisibility(View.VISIBLE);
                binding.swipeUpIndicator.setAlpha(0f);
                binding.swipeUpIndicator.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .start();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    private void checkGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 9000).show();
            } else {
                showToast("This device is not supported");
            }
        }
    }

    private void setupSwipeGesture() {
        binding.getRoot().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = event.getX();
                        initialY = event.getY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float deltaX = event.getX() - initialX;
                        float deltaY = event.getY() - initialY;

                        // Check if it's a vertical swipe (horizontal movement should be less than threshold)
                        if (Math.abs(deltaX) < SWIPE_THRESHOLD) {
                            if (Math.abs(deltaY) > SWIPE_THRESHOLD) {
                                if (deltaY < 0) { // Swipe up
                                    if (isLoginMode) {
                                        // Swipe up from login - show registration
                                        toggleMode();
                                    }
                                } else if (deltaY > 0) { // Swipe down
                                    if (!isLoginMode) {
                                        // Swipe down from registration - show login
                                        toggleMode();
                                    }
                                }
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void setupSwipeIndicators() {
        // Start with login mode
        binding.registrationContainer.setVisibility(View.GONE);
        binding.swipeUpIndicator.setVisibility(View.VISIBLE);
        binding.swipeDownIndicator.setVisibility(View.GONE);
    }

    private void toggleMode() {
        if (isAnimating) return;
        isAnimating = true;

        // Update UI and position before animation starts
        isLoginMode = !isLoginMode;
        updateUI();

        // Determine animation direction based on current mode
        Animation slideAnimation = isLoginMode ?
                AnimationUtils.loadAnimation(this, R.anim.slide_down) :
                AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Apply animation to both containers
        binding.loginContainer.startAnimation(slideAnimation);
        binding.registrationContainer.startAnimation(slideAnimation);

        // Update visibility of containers and indicators
        binding.loginContainer.setVisibility(isLoginMode ? View.VISIBLE : View.GONE);
        binding.registrationContainer.setVisibility(isLoginMode ? View.GONE : View.VISIBLE);
        binding.swipeUpIndicator.setVisibility(isLoginMode ? View.VISIBLE : View.GONE);
        binding.swipeDownIndicator.setVisibility(isLoginMode ? View.GONE : View.VISIBLE);

        slideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    private void updateBlueStripPosition() {
        // Update blue strip position
        ConstraintLayout.LayoutParams blueStripParams = (ConstraintLayout.LayoutParams) binding.blueStrip.getLayoutParams();
        blueStripParams.topToTop = isLoginMode ? ConstraintLayout.LayoutParams.PARENT_ID : ConstraintLayout.LayoutParams.UNSET;
        blueStripParams.bottomToBottom = isLoginMode ? ConstraintLayout.LayoutParams.UNSET : ConstraintLayout.LayoutParams.PARENT_ID;
        binding.blueStrip.setLayoutParams(blueStripParams);

        // Update welcome container position
        ConstraintLayout.LayoutParams welcomeParams = (ConstraintLayout.LayoutParams) binding.welcomeContainer.getLayoutParams();
        welcomeParams.topToTop = binding.blueStrip.getId();
        welcomeParams.bottomToBottom = binding.blueStrip.getId();
        binding.welcomeContainer.setLayoutParams(welcomeParams);
    }

    private void updateUI() {
        if (isLoginMode) {
            binding.welcomeText.setText("Welcome Back!");
            binding.welcomeSubtext.setText("Please login to your account");
            binding.loginButton.setText("Login");
        } else {
            binding.welcomeText.setText("Create Account");
            binding.welcomeSubtext.setText("Please register to continue");
            binding.loginButton.setText("Register");
        }
    }

    private void setupClickListeners() {
        binding.loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.emailInput.getText().toString();
                String password = binding.passwordInput.getText().toString();

                if (validateInput(email, password)) {
                    if (isLoginMode) {
                        loginUser(email, password);
                    } else {
                        String firstName = binding.nameInput.getText().toString();
                        String lastName = binding.surnameInput.getText().toString();
                        if (validateNames(firstName, lastName)) {
                            registerUser(email, password, firstName, lastName);
                        }
                    }
                }
            }
        });

        binding.registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.registerEmailInput.getText().toString();
                String password = binding.registerPasswordInput.getText().toString();
                String firstName = binding.nameInput.getText().toString();
                String lastName = binding.surnameInput.getText().toString();

                if (validateInput(email, password) && validateNames(firstName, lastName)) {
                    registerUser(email, password, firstName, lastName);
                }
            }
        });
    }

    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        if (email.isEmpty()) {
            binding.emailLayout.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError("Please enter a valid email");
            isValid = false;
        } else {
            binding.emailLayout.setError(null);
        }

        if (password.isEmpty()) {
            binding.passwordLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            binding.passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            binding.passwordLayout.setError(null);
        }

        return isValid;
    }

    private boolean validateNames(String firstName, String lastName) {
        boolean isValid = true;

        if (firstName.trim().isEmpty()) {
            binding.nameLayout.setError("First name is required");
            isValid = false;
        } else {
            binding.nameLayout.setError(null);
        }

        if (lastName.trim().isEmpty()) {
            binding.surnameLayout.setError("Last name is required");
            isValid = false;
        } else {
            binding.surnameLayout.setError(null);
        }

        return isValid;
    }

    private void loginUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    checkUserRoleAndNavigate();
                } else {
                    showToast("Login failed: " + task.getException().getMessage());
                }
            });
    }

    private void registerUser(String email, String password, String firstName, String lastName) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Create user document in Firestore
                    String uid = auth.getCurrentUser().getUid();
                    Map<String, Object> user = new HashMap<>();
                    user.put("email", email);
                    user.put("firstName", firstName);
                    user.put("lastName", lastName);
                    user.put("name", firstName + " " + lastName);
                    
                    // If user selected admin role, set as pending
                    if (binding.adminRadioButton.isChecked()) {
                        user.put("role", "user");  // Default to user role
                        user.put("isAdminRequestPending", true);
                        user.put("adminRequestStatus", "pending");
                        user.put("adminRequestTimestamp", System.currentTimeMillis());
                    } else {
                        user.put("role", "user");
                        user.put("isAdminRequestPending", false);
                        user.put("adminRequestStatus", "");
                    }

                    db.collection("users").document(uid)
                        .set(user)
                        .addOnSuccessListener(aVoid -> {
                            if (binding.adminRadioButton.isChecked()) {
                                showToast("Registration successful! Admin access request is pending approval.");
                            } else {
                                showToast("Registration successful!");
                            }
                            // Always start as regular user until admin is approved
                            startDashboardActivity("user");
                        })
                        .addOnFailureListener(e -> showToast("Error creating user profile"));
                } else {
                    showToast("Registration failed: " + task.getException().getMessage());
                }
            });
    }

    private void checkUserRoleAndNavigate() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            checkUserRole(uid);
        }
    }

    private void checkUserRole(String userId) {
        firebaseService.checkAdminStatus(userId)
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        if ("admin".equals(user.getRole())) {
                            // User is an approved admin
                            startDashboardActivity("admin");
                        } else if (user.isAdminRequestPending()) {
                            // Admin request is pending
                            showAdminRequestPendingDialog();
                        } else if ("rejected".equals(user.getAdminRequestStatus())) {
                            // Admin request was rejected
                            showAdminRequestRejectedDialog();
                        } else {
                            // Regular user
                            startDashboardActivity("user");
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error checking user role: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void showAdminRequestDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Request Admin Access")
            .setMessage("Would you like to request admin access? This requires approval.")
            .setPositiveButton("Request", (dialog, which) -> {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                firebaseService.requestAdminAccess(userId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Admin access requested. Please wait for approval.", 
                            Toast.LENGTH_LONG).show();
                        startDashboardActivity("user");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error requesting admin access: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                startDashboardActivity("user");
            })
            .show();
    }

    private void showAdminRequestPendingDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Admin Request Pending")
            .setMessage("Your admin access request is pending approval. You can continue as a regular user.")
            .setPositiveButton("OK", (dialog, which) -> {
                startDashboardActivity("user");
            })
            .show();
    }

    private void showAdminRequestRejectedDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Admin Request Rejected")
            .setMessage("Your admin access request was rejected. You can continue as a regular user.")
            .setPositiveButton("OK", (dialog, which) -> {
                startDashboardActivity("user");
            })
            .show();
    }

    private void startDashboardActivity(String role) {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        intent.putExtra("userRole", role);
        intent.putExtra("userName", "User");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Close LoginActivity
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
} 