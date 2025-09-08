package com.ram.libraryeqipmentmanagement.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ram.libraryeqipmentmanagement.R;
import com.ram.libraryeqipmentmanagement.model.Equipment;

import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.ViewHolder> {
    private final List<Equipment> equipmentList;
    private final Context context;
    private final OnEquipmentClickListener listener;

    public interface OnEquipmentClickListener {
        void onEquipmentClick(Equipment equipment);
        void onEquipmentLongClick(Equipment equipment);
    }

    public EquipmentAdapter(Context context, List<Equipment> equipmentList, OnEquipmentClickListener listener) {
        this.context = context;
        this.equipmentList = equipmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_equipment, parent, false);
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
        private final TextView txtName;
        private final TextView txtStatus;
        private final TextView txtSpecification;
        private final TextView txtPosition;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtEquipmentName);
            txtStatus = itemView.findViewById(R.id.txtEquipmentStatus);
            txtSpecification = itemView.findViewById(R.id.txtEquipmentSpecification);
            txtPosition = itemView.findViewById(R.id.txtEquipmentPosition);
        }

        void bind(final Equipment equipment, final OnEquipmentClickListener listener) {
            txtName.setText(equipment.getName());
            txtStatus.setText(equipment.getStatus());
            txtSpecification.setText(equipment.getSpecification());
            txtPosition.setText(String.format("Position: %d", equipment.getPosition()));

            itemView.setOnClickListener(v -> listener.onEquipmentClick(equipment));
            itemView.setOnLongClickListener(v -> {
                listener.onEquipmentLongClick(equipment);
                return true;
            });
        }
    }
} 