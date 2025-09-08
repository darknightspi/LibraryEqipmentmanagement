package com.ram.libraryeqipmentmanagement;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.ram.libraryeqipmentmanagement.adapter.AdminRequestAdapter;
import com.ram.libraryeqipmentmanagement.databinding.ActivityAdminRequestBinding;
import com.ram.libraryeqipmentmanagement.model.User;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;
import java.util.ArrayList;
import java.util.List;

public class AdminRequestActivity extends AppCompatActivity implements AdminRequestAdapter.OnRequestActionListener {
    private ActivityAdminRequestBinding binding;
    private FirebaseService firebaseService;
    private AdminRequestAdapter adapter;
    private List<User> pendingRequests;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Admin Requests");
        }

        firebaseService = FirebaseService.getInstance(this);
        pendingRequests = new ArrayList<>();
        recyclerView = binding.recyclerView;
        emptyView = binding.emptyView;
        progressBar = binding.progressBar;
        setupRecyclerView();
        loadPendingRequests();
    }

    private void setupRecyclerView() {
        adapter = new AdminRequestAdapter(pendingRequests, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadPendingRequests() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        firebaseService.getPendingAdminRequests()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pendingRequests.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setId(document.getId());
                        pendingRequests.add(user);
                    }
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        progressBar.setVisibility(View.GONE);
        if (pendingRequests.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onApprove(User user) {
        if (user.getId() == null) {
            Toast.makeText(this, "Error: User ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        handleAdminRequest(user, true);
    }

    @Override
    public void onReject(User user) {
        if (user.getId() == null) {
            Toast.makeText(this, "Error: User ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        handleAdminRequest(user, false);
    }

    private void handleAdminRequest(User user, boolean isApproved) {
        progressBar.setVisibility(View.VISIBLE);
        
        Task<Void> updateTask;
        if (isApproved) {
            updateTask = firebaseService.approveAdminRequest(user.getId());
        } else {
            updateTask = firebaseService.rejectAdminRequest(user.getId());
        }

        updateTask.addOnSuccessListener(aVoid -> {
            String action = isApproved ? "approved" : "rejected";
            Toast.makeText(this, "Request " + action + " successfully", Toast.LENGTH_SHORT).show();
            loadPendingRequests(); // Refresh the list
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error updating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 