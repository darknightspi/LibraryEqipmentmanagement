package com.ram.libraryeqipmentmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class LibraryStatisticsDialog extends BottomSheetDialogFragment {
    private static final String TAG = "LibraryStatisticsDialog";
    private TextView totalEquipmentText;
    private TextView workingEquipmentText;
    private TextView nonWorkingEquipmentText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_library_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        totalEquipmentText = view.findViewById(R.id.totalEquipmentText);
        workingEquipmentText = view.findViewById(R.id.workingEquipmentText);
        nonWorkingEquipmentText = view.findViewById(R.id.nonWorkingEquipmentText);
        MaterialButton closeButton = view.findViewById(R.id.closeButton);

        // Get statistics from arguments
        Bundle args = getArguments();
        if (args != null) {
            int totalEquipment = args.getInt("totalEquipment", 0);
            int workingEquipment = args.getInt("workingEquipment", 0);
            int nonWorkingEquipment = args.getInt("nonWorkingEquipment", 0);

            // Calculate percentages
            float workingPercentage = totalEquipment > 0 ? (workingEquipment * 100f / totalEquipment) : 0;
            float nonWorkingPercentage = totalEquipment > 0 ? (nonWorkingEquipment * 100f / totalEquipment) : 0;

            // Update UI
            totalEquipmentText.setText(String.valueOf(totalEquipment));
            workingEquipmentText.setText(String.format("%d (%.1f%%)", workingEquipment, workingPercentage));
            nonWorkingEquipmentText.setText(String.format("%d (%.1f%%)", nonWorkingEquipment, nonWorkingPercentage));
        }

        closeButton.setOnClickListener(v -> dismiss());
    }

    public static LibraryStatisticsDialog newInstance(int totalEquipment, int workingEquipment, int nonWorkingEquipment) {
        LibraryStatisticsDialog dialog = new LibraryStatisticsDialog();
        Bundle args = new Bundle();
        args.putInt("totalEquipment", totalEquipment);
        args.putInt("workingEquipment", workingEquipment);
        args.putInt("nonWorkingEquipment", nonWorkingEquipment);
        dialog.setArguments(args);
        return dialog;
    }
} 