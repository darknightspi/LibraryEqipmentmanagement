package com.ram.libraryeqipmentmanagement.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ram.libraryeqipmentmanagement.R;
import com.ram.libraryeqipmentmanagement.model.Library;
import com.ram.libraryeqipmentmanagement.service.FirebaseService;

import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {
    private static final String TAG = "LibraryAdapter";
    private List<Library> libraries;
    private final Context context;
    private final OnLibraryClickListener listener;
    private final FirebaseService firebaseService;
    private final boolean isAdmin;

    public interface OnLibraryClickListener {
        void onLibraryClick(Library library);
        void onLibraryLongClick(Library library);
    }

    public LibraryAdapter(Context context, List<Library> libraries, OnLibraryClickListener listener) {
        this(context, libraries, listener, false);
    }

    public LibraryAdapter(Context context, List<Library> libraries, OnLibraryClickListener listener, boolean isAdmin) {
        this.context = context;
        this.libraries = libraries;
        this.listener = listener;
        this.firebaseService = FirebaseService.getInstance(context);
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_library, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Library library = libraries.get(position);
        holder.bind(library);
    }

    @Override
    public int getItemCount() {
        return libraries.size();
    }

    public void updateData(List<Library> newLibraries) {
        this.libraries = newLibraries;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;

        ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textViewLibraryName);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onLibraryClick(libraries.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onLibraryLongClick(libraries.get(position));
                    return true;
                }
                return false;
            });
        }

        void bind(Library library) {
            nameTextView.setText(library.getName());
        }
    }
} 