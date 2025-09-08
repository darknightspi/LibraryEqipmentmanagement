package com.ram.libraryeqipmentmanagement;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ram.libraryeqipmentmanagement.adapters.QueryAdapter;
import com.ram.libraryeqipmentmanagement.models.Query;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;
import com.ram.libraryeqipmentmanagement.databinding.ActivityClosedQueriesBinding;
import android.util.Log;
import java.util.Collections;

public class ClosedQueriesActivity extends AppCompatActivity {
    private static final String TAG = "ClosedQueriesActivity";
    private ActivityClosedQueriesBinding binding;
    private FirebaseService firebaseService;
    private QueryAdapter queryAdapter;
    private List<Query> closedQueries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClosedQueriesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            // Initialize Firebase Service
            firebaseService = FirebaseService.getInstance(this);

            // Set up toolbar
            setSupportActionBar(binding.topAppBar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Closed Queries");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            // Initialize RecyclerView
            closedQueries = new ArrayList<>();
            queryAdapter = new QueryAdapter(closedQueries, true); // true because these are closed queries
            binding.recyclerViewClosedQueries.setLayoutManager(new LinearLayoutManager(this));
            binding.recyclerViewClosedQueries.setAdapter(queryAdapter);

            // Load closed queries
            loadClosedQueries();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadClosedQueries() {
        if (binding == null || firebaseService == null) {
            Log.e(TAG, "Binding or FirebaseService is null");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyStateLayout.setVisibility(View.GONE);
        binding.recyclerViewClosedQueries.setVisibility(View.GONE);

        try {
            firebaseService.getFirestore()
                .collection("queries")
                .whereEqualTo("status", "closed")
                .addSnapshotListener((snapshots, error) -> {
                    if (binding == null) {
                        Log.e(TAG, "Binding is null in snapshot listener");
                        return;
                    }

                    binding.progressBar.setVisibility(View.GONE);
                    
                    if (error != null) {
                        Log.e(TAG, "Error loading queries", error);
                        Toast.makeText(this, "Error loading queries: " + error.getMessage(), 
                                     Toast.LENGTH_SHORT).show();
                        binding.emptyStateLayout.setVisibility(View.VISIBLE);
                        binding.recyclerViewClosedQueries.setVisibility(View.GONE);
                        return;
                    }

                    closedQueries.clear();
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (com.google.firebase.firestore.DocumentSnapshot document : snapshots) {
                            try {
                                Query query = new Query();
                                query.setId(document.getId());
                                query.setEquipmentId(document.getString("equipmentId"));
                                query.setEquipmentName(document.getString("equipmentName"));
                                query.setPosition(document.getLong("position") != null ? document.getLong("position") : 0);
                                query.setQueryText(document.getString("queryText"));
                                query.setStatus(document.getString("status"));
                                query.setUserId(document.getString("userId"));
                                query.setResolution(document.getString("resolution"));
                                
                                // Try to get resolvedAt timestamp first, fall back to timestamp if not available
                                Timestamp timestamp = document.getTimestamp("resolvedAt");
                                if (timestamp == null) {
                                    timestamp = document.getTimestamp("timestamp");
                                }
                                if (timestamp != null) {
                                    query.setTimestamp(timestamp.toDate());
                                }
                                
                                closedQueries.add(query);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing query document: " + document.getId(), e);
                            }
                        }
                        
                        // Sort queries by timestamp (most recent first)
                        Collections.sort(closedQueries, (q1, q2) -> {
                            if (q1 == null || q2 == null || q1.getTimestamp() == null || q2.getTimestamp() == null) {
                                return 0;
                            }
                            return q2.getTimestamp().compareTo(q1.getTimestamp());
                        });
                        
                        queryAdapter.notifyDataSetChanged();
                        binding.emptyStateLayout.setVisibility(View.GONE);
                        binding.recyclerViewClosedQueries.setVisibility(View.VISIBLE);
                    } else {
                        binding.emptyStateLayout.setVisibility(View.VISIBLE);
                        binding.recyclerViewClosedQueries.setVisibility(View.GONE);
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadClosedQueries", e);
            Toast.makeText(this, "Error loading queries: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.recyclerViewClosedQueries.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 