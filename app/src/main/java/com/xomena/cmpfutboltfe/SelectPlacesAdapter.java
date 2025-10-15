package com.xomena.cmpfutboltfe;
import com.xomena.cmpfutboltfe.model.*;
import com.xomena.cmpfutboltfe.util.*;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
public class SelectPlacesAdapter extends
        RecyclerView.Adapter<SelectPlacesAdapter.ViewHolder> {

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView nameTextView;
        public TextView addressTextView;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            nameTextView = (TextView) itemView.findViewById(R.id.select_place_name);
            addressTextView = (TextView) itemView.findViewById(R.id.select_place_address);

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
    private List<MyPlace> mPlaces;
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
    public SelectPlacesAdapter(List<MyPlace> places) {
        mPlaces = places;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public SelectPlacesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View placeView = inflater.inflate(R.layout.item_select_place, parent, false);

        // Return a new holder instance
        return new ViewHolder(placeView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(SelectPlacesAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        MyPlace place = mPlaces.get(position);

        // Set item views based on the data model
        TextView textView = viewHolder.nameTextView;
        TextView addrView = viewHolder.addressTextView;
        textView.setText(place.getName());
        addrView.setText(place.getAddress());
    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return mPlaces.size();
    }

    public MyPlace getItem(int position) {
        if (position >= 0 && position < mPlaces.size()) {
            return mPlaces.get(position);
        }
        return null;
    }
}
