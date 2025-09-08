package com.ram.libraryeqipmentmanagement.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.ram.libraryeqipmentmanagement.R;
import com.ram.libraryeqipmentmanagement.model.Department;

import java.util.List;
import java.util.Map;

public class DepartmentAdapter extends RecyclerView.Adapter<DepartmentAdapter.ViewHolder> {
    private List<Department> departments;
    private final Context context;
    private final OnDepartmentClickListener listener;
    private final FirebaseFirestore db;
    private static final String TAG = "DepartmentAdapter";

    public interface OnDepartmentClickListener {
        void onDepartmentClick(Department department);
        void onDepartmentLongClick(Department department);
    }

    public DepartmentAdapter(Context context, List<Department> departments, OnDepartmentClickListener listener) {
        this.context = context;
        this.departments = departments;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_department, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Department department = departments.get(position);
        holder.bind(department);
    }

    @Override
    public int getItemCount() {
        return departments.size();
    }

    public void updateData(List<Department> newDepartments) {
        this.departments = newDepartments;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final ImageButton btnDepartmentStats;

        ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.departmentName);
            btnDepartmentStats = itemView.findViewById(R.id.btnDepartmentStats);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDepartmentClick(departments.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Department department = departments.get(position);
                    listener.onDepartmentLongClick(department);
                    return true;
                }
                return false;
            });

            btnDepartmentStats.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showDepartmentStatistics(departments.get(position));
                }
            });
        }

        void bind(Department department) {
            nameTextView.setText(department.getName());
            itemView.setTag(department.getId());
        }
    }

    private void showDepartmentStatistics(Department department) {
        Log.d(TAG, "Showing statistics for department: " + department.getName());
        
        if (department.getId() == null) {
            Log.e(TAG, "Department ID is null");
            Toast.makeText(context, "Error: Department ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        // First get all libraries in this department
        db.collection("libraries")
            .whereEqualTo("departmentId", department.getId())
            .get()
            .addOnSuccessListener(librarySnapshots -> {
                Log.d(TAG, "Found " + librarySnapshots.size() + " libraries");
                final int[] counters = new int[2]; // [totalEquipment, workingEquipment]
                final int[] processedLibraries = new int[1];

                if (librarySnapshots.isEmpty()) {
                    showStatisticsDialog(department.getName(), 0, 0);
                    return;
                }

                for (QueryDocumentSnapshot libraryDoc : librarySnapshots) {
                    String libraryId = libraryDoc.getId();
                    String libraryName = libraryDoc.getString("name");
                    Log.d(TAG, "Processing library: " + libraryName + " (ID: " + libraryId + ")");

                    // Get the library structure data
                    db.collection("libraries")
                        .document(libraryId)
                        .get()
                        .addOnSuccessListener(libraryData -> {
                            // Get rows and columns from library data
                            Long rows = libraryData.getLong("rows");
                            Long cols = libraryData.getLong("columns");
                            
                            if (rows != null && cols != null) {
                                // Now get the equipment data for each cell
                                db.collection("libraries")
                                    .document(libraryId)
                                    .collection("equipment")
                                    .get()
                                    .addOnSuccessListener(equipmentSnapshots -> {
                                        Log.d(TAG, "Found " + equipmentSnapshots.size() + " equipment in library: " + libraryName);
                                        
                                        for (QueryDocumentSnapshot equipmentDoc : equipmentSnapshots) {
                                            counters[0]++; // totalEquipment
                                            Boolean isFunctional = equipmentDoc.getBoolean("functional");
                                            if (Boolean.TRUE.equals(isFunctional)) {
                                                counters[1]++; // workingEquipment
                                            }
                                        }

                                        processedLibraries[0]++;
                                        Log.d(TAG, "Processed library " + libraryName + 
                                            ". Current totals - Total: " + counters[0] + 
                                            ", Working: " + counters[1]);

                                        // Show dialog when all libraries are processed
                                        if (processedLibraries[0] == librarySnapshots.size()) {
                                            Log.d(TAG, "All libraries processed. Final totals - Total: " + 
                                                counters[0] + ", Working: " + counters[1]);
                                            
                                            if (context instanceof android.app.Activity) {
                                                ((android.app.Activity) context).runOnUiThread(() -> {
                                                    showStatisticsDialog(
                                                        department.getName(),
                                                        counters[0], // totalEquipment
                                                        counters[1]  // workingEquipment
                                                    );
                                                });
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error loading equipment for library: " + libraryName, e);
                                        processedLibraries[0]++;
                                        handleError();
                                    });
                            } else {
                                Log.e(TAG, "Invalid rows/columns for library: " + libraryName);
                                processedLibraries[0]++;
                                handleError();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error loading library data: " + libraryName, e);
                            processedLibraries[0]++;
                            handleError();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading libraries for department: " + department.getName(), e);
                handleError();
            });
    }

    private void handleError() {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> 
                Toast.makeText(context, "Error loading statistics", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void showStatisticsDialog(String departmentName, int totalEquipment, int workingEquipment) {
        Log.d(TAG, "Showing statistics dialog for " + departmentName);
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_statistics, null);

            TextView titleText = dialogView.findViewById(R.id.textTitle);
            TextView totalEquipmentText = dialogView.findViewById(R.id.textTotalEquipment);
            TextView workingText = dialogView.findViewById(R.id.textAvailableStats);
            TextView notWorkingText = dialogView.findViewById(R.id.textInUseStats);

            int notWorkingEquipment = totalEquipment - workingEquipment;
            float workingPercent = totalEquipment > 0 ? (workingEquipment * 100f / totalEquipment) : 0;
            float notWorkingPercent = totalEquipment > 0 ? (notWorkingEquipment * 100f / totalEquipment) : 0;

            titleText.setText(departmentName + " Statistics");
            totalEquipmentText.setText("Total Equipment: " + totalEquipment);
            workingText.setText(String.format("Working: %d (%.1f%%)", workingEquipment, workingPercent));
            notWorkingText.setText(String.format("Not Working: %d (%.1f%%)", notWorkingEquipment, notWorkingPercent));

            // Hide the maintenance stats as we don't need it
            dialogView.findViewById(R.id.textMaintenanceStats).setVisibility(View.GONE);

            AlertDialog dialog = builder.setView(dialogView)
                .setPositiveButton("OK", null)
                .create();
            dialog.show();
            Log.d(TAG, "Statistics dialog shown successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error showing statistics dialog", e);
            handleError();
        }
    }

    private void showRenameDialog(Department department) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Rename Department");

        // Set up the input
        final EditText input = new EditText(context);
        input.setText(department.getName());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                renameDepartment(department, newName);
            } else {
                Toast.makeText(context, "Department name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteConfirmationDialog(Department department) {
        new AlertDialog.Builder(context)
            .setTitle("Delete Department")
            .setMessage("Are you sure you want to delete this department? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteDepartment(department))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void renameDepartment(Department department, String newName) {
        FirebaseFirestore.getInstance()
            .collection("departments")
            .document(department.getId())
            .update("name", newName)
            .addOnSuccessListener(aVoid -> {
                department.setName(newName);
                notifyDataSetChanged();
                Toast.makeText(context, "Department renamed successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Error renaming department", Toast.LENGTH_SHORT).show();
            });
    }

    private void deleteDepartment(Department department) {
        FirebaseFirestore.getInstance()
            .collection("departments")
            .document(department.getId())
            .delete()
            .addOnSuccessListener(aVoid -> {
                int position = departments.indexOf(department);
                departments.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(context, "Department deleted successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Error deleting department", Toast.LENGTH_SHORT).show();
            });
    }
} 