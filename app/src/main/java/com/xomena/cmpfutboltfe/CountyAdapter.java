package com.xomena.cmpfutboltfe;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
public class CountyAdapter extends
        RecyclerView.Adapter<CountyAdapter.ViewHolder> {

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView nameTextView;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            nameTextView = (TextView) itemView.findViewById(R.id.county_name);
        }
    }

    // Store a member variable for the contacts
    private List<County> mCounties;

    // Pass in the contact array into the constructor
    public CountyAdapter(List<County> counties) {
        mCounties = counties;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public CountyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View countyView = inflater.inflate(R.layout.item_county, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(countyView);
        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(CountyAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        County county = mCounties.get(position);

        // Set item views based on the data model
        TextView textView = viewHolder.nameTextView;
        textView.setText(county.getName());
    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return mCounties.size();
    }
}