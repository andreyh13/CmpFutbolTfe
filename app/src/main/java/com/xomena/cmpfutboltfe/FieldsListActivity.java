package com.xomena.cmpfutboltfe;
import com.xomena.cmpfutboltfe.model.*;
import com.xomena.cmpfutboltfe.util.*;
import com.xomena.cmpfutboltfe.ui.adapter.*;

import android.graphics.drawable.Drawable;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class FieldsListActivity extends AppCompatActivity implements FootballFieldAdapter.OnItemClickListener {

    private List<FootballField> ff_data;
    private static final String TAG = "FieldsListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fields_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarPitches);
        setSupportActionBar(toolbar);
        try {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayShowTitleEnabled(false);
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Exception", e);
        }

        final Drawable upArrow = ResourcesCompat.getDrawable(getResources(), R.drawable.abc_ic_ab_back_material,
                getApplicationContext().getTheme());
        toolbar.setNavigationIcon(upArrow);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Intent i = getIntent();
        String county = i.getStringExtra(MainActivity.EXTRA_COUNTY);
        this.ff_data = i.getParcelableArrayListExtra(MainActivity.EXTRA_FIELDS);

        TextView title = (TextView)findViewById(R.id.toolbar_title_pitches);
        title.setText(county);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rvPitches);

        FootballFieldAdapter adapter = new FootballFieldAdapter(FootballFieldItem.createPitchesList(this.ff_data));
        adapter.setOnItemClickListener(this);
        // Attach the adapter to the recyclerview to populate items
        recyclerView.setAdapter(adapter);
        // Set layout manager to position the items
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);
    }

    public void onItemClick(View itemView, int position) {
        TextView nameTextView = (TextView) itemView.findViewById(R.id.ffNameValue);
        if (nameTextView != null) {
            String mName = nameTextView.getText().toString();
            for(FootballField f : this.ff_data){
                if(f.getName().equals(mName)){
                    Intent intent = new Intent(this, FieldDetailActivity.class);
                    intent.putExtra(MainActivity.EXTRA_ITEM, f);
                    startActivity(intent);
                }
            }
        }
    }
}
