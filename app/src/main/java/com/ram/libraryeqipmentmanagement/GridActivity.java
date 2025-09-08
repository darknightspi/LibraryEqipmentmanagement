package com.ram.libraryeqipmentmanagement;

import android.os.Bundle;
import android.view.View;
import androidx.gridlayout.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class GridActivity extends AppCompatActivity {

    private GridLayout gridLayout;
    private int libraryId, rows, cols;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid);

        // Get values from Intent
        libraryId = getIntent().getIntExtra("LIBRARY_ID", -1);
        rows = getIntent().getIntExtra("ROWS", 0);
        cols = getIntent().getIntExtra("COLUMNS", 0);

        gridLayout = findViewById(R.id.equipmentGrid);
        gridLayout.setColumnCount(cols);
        gridLayout.setRowCount(rows);

        if (libraryId == -1 || rows <= 0 || cols <= 0) {
            Toast.makeText(this, "Invalid data!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Create Grid Dynamically
        createGrid();
    }

    private void createGrid() {
        for (int i = 0; i < rows * cols; i++) {
            TextView textView = new TextView(this);
            textView.setText("Item " + (i + 1));
            textView.setTextSize(16);
            textView.setPadding(20, 20, 20, 20);
            textView.setBackgroundResource(R.drawable.grid_item_bg);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(i / cols);
            params.columnSpec = GridLayout.spec(i % cols);
            textView.setLayoutParams(params);

            textView.setOnClickListener(v -> {
                Toast.makeText(this, "Clicked: " + ((TextView) v).getText(), Toast.LENGTH_SHORT).show();
            });

            gridLayout.addView(textView);
        }
    }
}
