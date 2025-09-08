package com.ram.libraryeqipmentmanagement;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ram.libraryeqipmentmanagement.model.Equipment;
import java.util.List;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {
    private final Context context;
    private final List<Equipment> equipmentList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Equipment equipment);
        void onItemLongClick(Equipment equipment);
    }

    public GridAdapter(Context context, List<Equipment> equipmentList, OnItemClickListener listener) {
        this.context = context;
        this.equipmentList = equipmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.grid_cell_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        holder.bind(equipment, listener);
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    public void updateData(List<Equipment> newEquipmentList) {
        equipmentList.clear();
        equipmentList.addAll(newEquipmentList);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View itemView;
        private final TextView cellText;

        ViewHolder(View view) {
            super(view);
            itemView = view;
            cellText = view.findViewById(R.id.cellText);
        }

        void bind(final Equipment equipment, final OnItemClickListener listener) {
            cellText.setText(equipment.getName());

            // Set background color based on status
            int backgroundColor;
            switch (equipment.getStatus().toLowerCase()) {
                case "available":
                    backgroundColor = itemView.getContext().getResources().getColor(R.color.cell_available);
                    break;
                case "in_use":
                    backgroundColor = itemView.getContext().getResources().getColor(R.color.cell_in_use);
                    break;
                case "maintenance":
                    backgroundColor = itemView.getContext().getResources().getColor(R.color.cell_maintenance);
                    break;
                default:
                    backgroundColor = itemView.getContext().getResources().getColor(R.color.cell_empty);
            }
            itemView.setBackgroundColor(backgroundColor);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(equipment);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClick(equipment);
                    return true;
                }
                return false;
            });
        }
    }
} 