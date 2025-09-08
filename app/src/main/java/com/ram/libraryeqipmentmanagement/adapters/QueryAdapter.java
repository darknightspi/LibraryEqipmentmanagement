package com.ram.libraryeqipmentmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ram.libraryeqipmentmanagement.R;
import com.ram.libraryeqipmentmanagement.models.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QueryAdapter extends RecyclerView.Adapter<QueryAdapter.QueryViewHolder> {
    private List<Query> queries = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
    private final boolean isClosedQueries;

    public QueryAdapter(List<Query> queries, boolean isClosedQueries) {
        this.queries = queries;
        this.isClosedQueries = isClosedQueries;
    }

    @NonNull
    @Override
    public QueryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_query, parent, false);
        return new QueryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueryViewHolder holder, int position) {
        Query query = queries.get(position);
        holder.bind(query);
    }

    @Override
    public int getItemCount() {
        return queries.size();
    }

    public void setQueries(List<Query> queries) {
        this.queries = queries;
        notifyDataSetChanged();
    }

    class QueryViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView descriptionText;
        private final TextView statusText;
        private final TextView dateText;
        private final TextView resolutionText;

        QueryViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.textQueryTitle);
            descriptionText = itemView.findViewById(R.id.textQueryDescription);
            statusText = itemView.findViewById(R.id.textQueryStatus);
            dateText = itemView.findViewById(R.id.textQueryDate);
            resolutionText = itemView.findViewById(R.id.textQueryResolution);
        }

        void bind(Query query) {
            // Set equipment name as title
            titleText.setText(query.getEquipmentName());
            
            // Set query text as description
            descriptionText.setText(query.getQueryText());
            
            // Set status with appropriate color
            String status = query.getStatus().substring(0, 1).toUpperCase() + query.getStatus().substring(1);
            statusText.setText(status);
            statusText.setTextColor(itemView.getContext().getResources().getColor(
                "open".equals(query.getStatus()) ? android.R.color.holo_red_dark : android.R.color.holo_green_dark
            ));
            
            // Set date
            if (query.getTimestamp() != null) {
                dateText.setText(dateFormat.format(query.getTimestamp()));
            }
            
            // Show resolution for closed queries
            if (isClosedQueries && query.getResolution() != null && !query.getResolution().isEmpty()) {
                resolutionText.setVisibility(View.VISIBLE);
                resolutionText.setText("Resolution: " + query.getResolution());
            } else {
                resolutionText.setVisibility(View.GONE);
            }
        }
    }
} 