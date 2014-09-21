package com.xomena.cmpfutboltfe;

/**
 * Created by andriy on 9/17/14.
 */
import java.util.ArrayList;
import android.widget.TextView;
import android.widget.BaseAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

public class FootballFieldAdapter extends BaseAdapter {

    ArrayList<FootballFieldItem> data;
    Context context;
    private static LayoutInflater inflater = null;

    public FootballFieldAdapter(Context context, ArrayList<FootballFieldItem> data) {
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }
    @Override
    public Object getItem(int position) {
        return data.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // See if the view needs to be inflated
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.ff_list_item, null);
        }
        // Extract the desired views
        TextView nameText = (TextView) view.findViewById(R.id.ffNameValue);
        TextView addrText = (TextView) view.findViewById(R.id.ffAddressValue);
        TextView phoneText = (TextView) view.findViewById(R.id.ffPhoneValue);
        // Get the data item
        FootballFieldItem item = data.get(position);
        // Display the data item's properties
        nameText.setText(item.name);
        addrText.setText(item.address);
        phoneText.setText(item.phone);

        return view;
    }
}