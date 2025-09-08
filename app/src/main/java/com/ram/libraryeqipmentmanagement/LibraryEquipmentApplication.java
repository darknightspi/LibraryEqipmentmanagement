package com.ram.libraryeqipmentmanagement;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseReference;

public class LibraryEquipmentApplication extends Application {
    private static final String TAG = "LibraryEquipmentApp";
    private static Context context;
    private FirebaseDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        // Initialize Google Play Services first
        initializeGooglePlayServices();

        // Initialize Firebase
        initializeFirebase();
    }

    private void initializeGooglePlayServices() {
        try {
            // Update Android security provider
            ProviderInstaller.installIfNeeded(this);

            // Check Google Play Services availability
            GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
            int result = googleAPI.isGooglePlayServicesAvailable(this);
            if (result != ConnectionResult.SUCCESS) {
                Log.e(TAG, "Google Play Services is not available: " + result);
                // Handle the error appropriately in your app
                if (googleAPI.isUserResolvableError(result)) {
                    Log.w(TAG, "Google Play Services error is resolvable");
                }
            } else {
                Log.d(TAG, "Google Play Services initialized successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Google Play Services: " + e.getMessage());
        }
    }

    private void initializeFirebase() {
        try {
            // Initialize Firebase if not already initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }

            // Get database instance and enable persistence
            database = FirebaseDatabase.getInstance();
            database.setPersistenceEnabled(true);

            // Set keep sync
            DatabaseReference librariesRef = database.getReference("libraries");
            librariesRef.keepSynced(true);

            // Test database connection
            librariesRef.child(".info/connected").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    boolean connected = snapshot.getValue(Boolean.class);
                    Log.d(TAG, "Firebase connection state changed. Connected: " + connected);
                    if (!connected) {
                        // Try to reconnect
                        database.goOnline();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Firebase connection listener cancelled: " + error.getMessage());
                }
            });

            // Initialize App Check with debug token
            FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            );

            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
        }
    }

    public static Context getAppContext() {
        return context;
    }

    public FirebaseDatabase getDatabase() {
        return database;
    }
} 