package com.xomena.cmpfutboltfe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RouteStepAdapter extends
        RecyclerView.Adapter<RouteStepAdapter.ViewHolder> {

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView stepTextView;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            stepTextView = (TextView) itemView.findViewById(R.id.route_step);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Triggers click upwards to the adapter on click
                    if (mListener != null)
                        mListener.onItemClick(v, getLayoutPosition());
                }
            });
        }
    }

    // Store a member variable for the contacts
    private List<RouteStep> mSteps;
    private static OnItemClickListener mListener;

    // Define the listener interface
    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    // Define the method that allows the parent activity or fragment to define the listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    // Pass in the contact array into the constructor
    public RouteStepAdapter(List<RouteStep> steps) {
        mSteps = steps;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public RouteStepAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View stepView = inflater.inflate(R.layout.item_route_step, parent, false);

        // Return a new holder instance
        return new ViewHolder(stepView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(RouteStepAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        RouteStep step = mSteps.get(position);

        // Set item views based on the data model
        TextView textView = viewHolder.stepTextView;
        textView.setText(step.getStepText());
    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return mSteps.size();
    }

    public void addItem(RouteStep mStep, int position) {
        mSteps.add(position, mStep);
        notifyItemInserted(position);
    }
}
