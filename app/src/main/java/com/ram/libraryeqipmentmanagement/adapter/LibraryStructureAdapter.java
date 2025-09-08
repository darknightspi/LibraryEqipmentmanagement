package com.ram.libraryeqipmentmanagement.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ram.libraryeqipmentmanagement.R;
import com.ram.libraryeqipmentmanagement.model.LibraryCell;

import java.util.List;

public class LibraryStructureAdapter extends RecyclerView.Adapter<LibraryStructureAdapter.CellViewHolder> {
    private final List<LibraryCell> cells;
    private final OnCellClickListener listener;

    public interface OnCellClickListener {
        void onCellClick(LibraryCell cell);
    }

    public LibraryStructureAdapter(List<LibraryCell> cells, OnCellClickListener listener) {
        this.cells = cells;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_library_cell, parent, false);
        return new CellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CellViewHolder holder, int position) {
        LibraryCell cell = cells.get(position);
        holder.bind(position + 1);
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    class CellViewHolder extends RecyclerView.ViewHolder {
        private final TextView textCellPosition;

        CellViewHolder(@NonNull View itemView) {
            super(itemView);
            textCellPosition = itemView.findViewById(R.id.textCellPosition);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onCellClick(cells.get(position));
                }
            });
        }

        void bind(int serialNumber) {
            textCellPosition.setText(String.valueOf(serialNumber));
        }
    }
} 