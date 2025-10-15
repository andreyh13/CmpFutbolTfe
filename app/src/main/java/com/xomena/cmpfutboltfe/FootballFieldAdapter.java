package com.xomena.cmpfutboltfe;
import com.xomena.cmpfutboltfe.model.*;
import com.xomena.cmpfutboltfe.util.*;

import java.util.List;

import android.widget.TextView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;


public class FootballFieldAdapter extends RecyclerView.Adapter<FootballFieldAdapter.ViewHolder> {

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView nameTextView;
        public TextView addressTextView;
        public TextView phoneTextView;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            nameTextView = (TextView) itemView.findViewById(R.id.ffNameValue);
            addressTextView = (TextView) itemView.findViewById(R.id.ffAddressValue);
            phoneTextView = (TextView) itemView.findViewById(R.id.ffPhoneValue);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Triggers click upwards to the adapter on click
                    if (listener != null)
                        listener.onItemClick(v, getLayoutPosition());
                }
            });
        }
    }

    // Store a member variable for the contacts
    private List<FootballFieldItem> mPitches;
    private static OnItemClickListener listener;

    // Define the listener interface
    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    // Define the method that allows the parent activity or fragment to define the listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        FootballFieldAdapter.listener = listener;
    }

    // Pass in the contact array into the constructor
    public FootballFieldAdapter(List<FootballFieldItem> pitches) {
        mPitches = pitches;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public FootballFieldAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View pitchView = inflater.inflate(R.layout.ff_list_item, parent, false);

        // Return a new holder instance
        return new ViewHolder(pitchView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(FootballFieldAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        FootballFieldItem pitch = mPitches.get(position);

        // Set item views based on the data model
        viewHolder.nameTextView.setText(pitch.getName());
        viewHolder.addressTextView.setText(pitch.getAddress());
        viewHolder.phoneTextView.setText(pitch.getPhone());
    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return mPitches.size();
    }
}