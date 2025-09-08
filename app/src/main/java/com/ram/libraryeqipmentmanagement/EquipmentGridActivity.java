package com.ram.libraryeqipmentmanagement;

import android.os.Bundle;
import android.view.View;
import androidx.gridlayout.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class EquipmentGridActivity extends AppCompatActivity {

    private GridLayout gridLayout;
    private int libraryId;
    private int rows;
    private int columns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid);

        libraryId = getIntent().getIntExtra("LIBRARY_ID", -1);
        rows = getIntent().getIntExtra("ROWS", 0);
        columns = getIntent().getIntExtra("COLUMNS", 0);
        
        gridLayout = findViewById(R.id.equipmentGrid);
        
        if (libraryId == -1 || rows <= 0 || columns <= 0) {
            finish();
            return;
        }

        gridLayout.setRowCount(rows);
        gridLayout.setColumnCount(columns);
        createGrid();
    }

    private void createGrid() {
        int totalCells = rows * columns;
        for (int i = 0; i < totalCells; i++) {
            TextView textView = new TextView(this);
            textView.setText("Item " + (i + 1));
            textView.setTextSize(16);
            textView.setPadding(20, 20, 20, 20);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(i / columns);
            params.columnSpec = GridLayout.spec(i % columns);
            textView.setLayoutParams(params);

            gridLayout.addView(textView);
        }
    }
}
