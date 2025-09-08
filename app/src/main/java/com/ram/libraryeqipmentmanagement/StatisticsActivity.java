package com.ram.libraryeqipmentmanagement;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {
    private static final String TAG = "StatisticsActivity";
    private FirebaseFirestore db;
    private TextView totalEquipmentTextView;
    private TextView availableEquipmentTextView;
    private TextView inUseEquipmentTextView;
    private TextView maintenanceEquipmentTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        totalEquipmentTextView = findViewById(R.id.totalEquipmentTextView);
        availableEquipmentTextView = findViewById(R.id.availableEquipmentTextView);
        inUseEquipmentTextView = findViewById(R.id.inUseEquipmentTextView);
        maintenanceEquipmentTextView = findViewById(R.id.maintenanceEquipmentTextView);

        // Load statistics
        loadStatistics();
    }

    private void loadStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("available", 0);
        stats.put("inUse", 0);
        stats.put("maintenance", 0);

        db.collection("equipment")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Increment total count
                            stats.put("total", stats.get("total") + 1);

                            // Count by status
                            String status = document.getString("status");
                            if (status != null) {
                                switch (status.toLowerCase()) {
                                    case "available":
                                        stats.put("available", stats.get("available") + 1);
                                        break;
                                    case "in use":
                                        stats.put("inUse", stats.get("inUse") + 1);
                                        break;
                                    case "maintenance":
                                        stats.put("maintenance", stats.get("maintenance") + 1);
                                        break;
                                }
                            }
                        }

                        // Update UI
                        updateStatisticsUI(stats);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    private void updateStatisticsUI(Map<String, Integer> stats) {
        totalEquipmentTextView.setText(String.format("Total Equipment: %d", stats.get("total")));
        availableEquipmentTextView.setText(String.format("Available: %d", stats.get("available")));
        inUseEquipmentTextView.setText(String.format("In Use: %d", stats.get("inUse")));
        maintenanceEquipmentTextView.setText(String.format("In Maintenance: %d", stats.get("maintenance")));
    }
} 