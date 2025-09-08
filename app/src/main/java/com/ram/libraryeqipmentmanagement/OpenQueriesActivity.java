package com.ram.libraryeqipmentmanagement;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import android.content.Intent;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.Menu;
import android.view.MenuItem;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.Timestamp;
import java.util.Collections;

public class OpenQueriesActivity extends AppCompatActivity {
    private static final String TAG = "OpenQueriesActivity";
    private RecyclerView recyclerView;
    private TextView emptyView;
    private View progressBar;
    private FirebaseFirestore db;
    private QueryAdapter adapter;
    private FirebaseService firebaseService;
    private boolean isAdmin = false;
    private ListenerRegistration queryListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_queries);

        // Initialize Firebase
        firebaseService = FirebaseService.getInstance(this);
        db = firebaseService.getFirestore();

        // Check if user is admin
        String userId = firebaseService.getCurrentUserId();
        if (userId != null) {
            firebaseService.getUserRole(userId).addOnSuccessListener(role -> {
                isAdmin = "admin".equals(role);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            });
        }

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Open Queries");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewQueries);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QueryAdapter();
        recyclerView.setAdapter(adapter);

        // Load queries
        loadOpenQueries();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_open_queries, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            refreshQueries();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshQueries() {
        progressBar.setVisibility(View.VISIBLE);
        if (queryListener != null) {
            queryListener.remove();
        }
        loadOpenQueries();
        Toast.makeText(this, "Refreshing queries...", Toast.LENGTH_SHORT).show();
    }

    private void loadOpenQueries() {
        progressBar.setVisibility(View.VISIBLE);
        
        if (queryListener != null) {
            queryListener.remove();
        }
        
        queryListener = db.collection("queries")
            .whereEqualTo("status", "open")
            .addSnapshotListener((value, error) -> {
                progressBar.setVisibility(View.GONE);
                
                if (error != null) {
                    Log.e(TAG, "Error loading queries", error);
                    Toast.makeText(this, "Error loading queries: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    updateEmptyView(true);
                    return;
                }

                List<com.ram.libraryeqipmentmanagement.models.Query> queries = new ArrayList<>();
                if (value != null) {
                    for (QueryDocumentSnapshot doc : value) {
                        try {
                            com.ram.libraryeqipmentmanagement.models.Query query = new com.ram.libraryeqipmentmanagement.models.Query();
                            query.setId(doc.getId());
                            query.setEquipmentId(doc.getString("equipmentId"));
                            query.setEquipmentName(doc.getString("equipmentName"));
                            query.setPosition(doc.getLong("position") != null ? doc.getLong("position") : 0);
                            query.setQueryText(doc.getString("queryText"));
                            query.setStatus(doc.getString("status"));
                            query.setUserId(doc.getString("userId"));
                            query.setLibraryId(doc.getString("libraryId"));
                            if (doc.getTimestamp("timestamp") != null) {
                                query.setTimestamp(doc.getTimestamp("timestamp").toDate());
                            }
                            queries.add(query);
                            Log.d(TAG, "Loaded query: " + query.getId() + " for equipment: " + query.getEquipmentName());
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing query document: " + doc.getId(), e);
                        }
                    }
                    
                    // Sort the queries by timestamp after loading
                    Collections.sort(queries, (q1, q2) -> {
                        if (q1.getTimestamp() == null || q2.getTimestamp() == null) {
                            return 0;
                        }
                        return q2.getTimestamp().compareTo(q1.getTimestamp());
                    });
                }

                adapter.setQueries(queries);
                updateEmptyView(queries.isEmpty());
            });
    }

    private void updateEmptyView(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queryListener != null) {
            queryListener.remove();
        }
    }

    private void showResolveDialog(com.ram.libraryeqipmentmanagement.models.Query query) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Resolve Query");

        // Create input field for resolution
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint("Enter resolution details");
        input.setMinLines(3);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 0, 20, 0);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Resolve", (dialog, which) -> {
            String resolution = input.getText().toString().trim();
            if (!resolution.isEmpty()) {
                resolveQuery(query, resolution);
            } else {
                Toast.makeText(this, "Please enter resolution details", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void resolveQuery(com.ram.libraryeqipmentmanagement.models.Query query, String resolution) {
        progressBar.setVisibility(View.VISIBLE);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "closed");
        updates.put("resolution", resolution);
        updates.put("resolvedAt", Timestamp.now());
        updates.put("resolvedBy", firebaseService.getCurrentUserId());
        
        db.collection("queries").document(query.getId())
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Query resolved successfully", Toast.LENGTH_SHORT).show();
                // Refresh the queries list
                refreshQueries();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error resolving query", e);
                Toast.makeText(this, "Error resolving query: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private class QueryAdapter extends RecyclerView.Adapter<QueryAdapter.QueryViewHolder> {
        private List<com.ram.libraryeqipmentmanagement.models.Query> queries = new ArrayList<>();

        @NonNull
        @Override
        public QueryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_query, parent, false);
            return new QueryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull QueryViewHolder holder, int position) {
            com.ram.libraryeqipmentmanagement.models.Query query = queries.get(position);
            holder.bind(query);
        }

        @Override
        public int getItemCount() {
            return queries.size();
        }

        void setQueries(List<com.ram.libraryeqipmentmanagement.models.Query> newQueries) {
            queries = newQueries;
            notifyDataSetChanged();
        }

        class QueryViewHolder extends RecyclerView.ViewHolder {
            private final TextView titleText;
            private final TextView descriptionText;
            private final TextView statusText;
            private final TextView dateText;
            private final Button resolveButton;
            private final Button viewEquipmentButton;

            QueryViewHolder(@NonNull View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.textQueryTitle);
                descriptionText = itemView.findViewById(R.id.textQueryDescription);
                statusText = itemView.findViewById(R.id.textQueryStatus);
                dateText = itemView.findViewById(R.id.textQueryDate);
                resolveButton = itemView.findViewById(R.id.buttonResolve);
                viewEquipmentButton = itemView.findViewById(R.id.buttonViewEquipment);
            }

            void bind(com.ram.libraryeqipmentmanagement.models.Query query) {
                titleText.setText("Equipment: " + query.getEquipmentName());
                descriptionText.setText("Query: " + query.getQueryText());
                statusText.setText("Status: " + query.getStatus());
                if (query.getTimestamp() != null) {
                    dateText.setText("Submitted: " + android.text.format.DateFormat.format(
                        "MMM dd, yyyy HH:mm", query.getTimestamp()));
                }

                // Show/hide admin controls
                if (isAdmin) {
                    resolveButton.setVisibility(View.VISIBLE);
                    resolveButton.setOnClickListener(v -> showResolveDialog(query));
                } else {
                    resolveButton.setVisibility(View.GONE);
                }

                viewEquipmentButton.setOnClickListener(v -> {
                    if (query.getLibraryId() == null || query.getLibraryId().isEmpty()) {
                        Toast.makeText(OpenQueriesActivity.this, "Library ID not found for this equipment", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    Intent intent = new Intent(OpenQueriesActivity.this, LibraryStructureActivity.class);
                    intent.putExtra("equipmentId", query.getEquipmentId());
                    intent.putExtra("libraryId", query.getLibraryId());
                    intent.putExtra("position", query.getPosition());
                    intent.putExtra("fromQuery", true);
                    intent.putExtra("queryId", query.getId());
                    
                    // Fetch library name before starting activity
                    db.collection("libraries")
                        .document(query.getLibraryId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String libraryName = documentSnapshot.getString("name");
                                intent.putExtra("libraryName", libraryName != null ? libraryName : "Library");
                                startActivity(intent);
                            } else {
                                Toast.makeText(OpenQueriesActivity.this, "Library not found", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error fetching library details", e);
                            Toast.makeText(OpenQueriesActivity.this, "Error loading library details", Toast.LENGTH_SHORT).show();
                        });
                });
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 