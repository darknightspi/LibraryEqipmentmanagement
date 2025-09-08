package com.ram.libraryeqipmentmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.ram.libraryeqipmentmanagement.databinding.ActivityGoogleSignInBinding;
import com.ram.libraryeqipmentmanagement.model.User;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;

public class GoogleSignInActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;
    private ActivityGoogleSignInBinding binding;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private FirebaseService firebaseService;
    private View signInLayout;
    private View createPasswordLayout;
    private View verificationLayout;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoogleSignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseService = FirebaseService.getInstance(this);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        signInLayout = binding.signInLayout;
        createPasswordLayout = binding.createPasswordLayout;
        verificationLayout = binding.verificationLayout;

        // Set up Google Sign-In button
        binding.signInButton.setOnClickListener(v -> signIn());

        // Set up create password button
        binding.createPasswordButton.setOnClickListener(v -> createPassword());

        // Set up resend verification email button
        binding.resendVerificationButton.setOnClickListener(v -> resendVerificationEmail());
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            userEmail = account.getEmail();
                            
                            // Send verification email
                            user.sendEmailVerification()
                                .addOnCompleteListener(verificationTask -> {
                                    if (verificationTask.isSuccessful()) {
                                        // Show verification layout
                                        signInLayout.setVisibility(View.GONE);
                                        verificationLayout.setVisibility(View.VISIBLE);
                                        createPasswordLayout.setVisibility(View.GONE);
                                        
                                        Toast.makeText(this, "Verification email sent to " + userEmail,
                                                Toast.LENGTH_LONG).show();
                                        
                                        // Create or update user in Firestore
                                        User newUser = new User(
                                            account.getGivenName(),
                                            account.getFamilyName(),
                                            account.getEmail()
                                        );
                                        firebaseService.createOrUpdateUser(newUser);
                                        
                                        // Start verification check
                                        startVerificationCheck();
                                    } else {
                                        Toast.makeText(this, "Failed to send verification email.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                        }
                    } else {
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startVerificationCheck() {
        // Check email verification status every 5 seconds
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        user.reload();
                        if (user.isEmailVerified()) {
                            runOnUiThread(() -> {
                                verificationLayout.setVisibility(View.GONE);
                                createPasswordLayout.setVisibility(View.VISIBLE);
                            });
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void resendVerificationEmail() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Verification email resent to " + userEmail,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to resend verification email.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    private void createPassword() {
        String password = binding.passwordInput.getText().toString();
        String confirmPassword = binding.confirmPasswordInput.getText().toString();

        if (password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && user.isEmailVerified()) {
            // Store the password securely in Firebase Auth
            user.updatePassword(password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password created successfully", Toast.LENGTH_SHORT).show();
                            // Navigate to main activity
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Failed to create password: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Please verify your email first", Toast.LENGTH_SHORT).show();
        }
    }
} 