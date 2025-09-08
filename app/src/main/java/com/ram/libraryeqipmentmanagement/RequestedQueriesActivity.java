package com.ram.libraryeqipmentmanagement;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.ram.libraryeqipmentmanagement.databinding.ActivityRequestedQueriesBinding;

public class RequestedQueriesActivity extends AppCompatActivity {
    private ActivityRequestedQueriesBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRequestedQueriesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up toolbar
        setSupportActionBar(binding.topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Requested Queries");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 