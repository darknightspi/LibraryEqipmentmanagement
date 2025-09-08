package com.ram.libraryeqipmentmanagement.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ram.libraryeqipmentmanagement.R;
import com.ram.libraryeqipmentmanagement.model.User;

import java.util.List;

public class AdminRequestAdapter extends RecyclerView.Adapter<AdminRequestAdapter.ViewHolder> {

    private final List<User> requests;
    private final OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onApprove(User user);
        void onReject(User user);
    }

    public AdminRequestAdapter(List<User> requests, OnRequestActionListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = requests.get(position);
        
        // Set user details
        String displayName = user.getFullName();
        String email = user.getEmail();
        
        holder.nameTextView.setText(displayName);
        holder.emailTextView.setText(email);
        
        // Set button click listeners
        holder.approveButton.setOnClickListener(v -> listener.onApprove(user));
        holder.rejectButton.setOnClickListener(v -> listener.onReject(user));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nameTextView;
        final TextView emailTextView;
        final MaterialButton approveButton;
        final MaterialButton rejectButton;

        ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            approveButton = itemView.findViewById(R.id.approveButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
    }
} 