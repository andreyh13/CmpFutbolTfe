package com.xomena.cmpfutboltfe;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class FieldsListActivity extends ActionBarActivity {

    private List<FootballField> ff_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fields_list);

        TextView title = (TextView)findViewById(R.id.textFieldsView);
        ListView listView = (ListView)findViewById(R.id.listFieldsView);

        Intent i = getIntent();
        String county = i.getStringExtra(MainActivity.EXTRA_COUNTY);
        this.ff_data = i.getParcelableArrayListExtra(MainActivity.EXTRA_FIELDS);

        title.setText(getString(R.string.select_field_in)+" "+county);

        ArrayList<FootballFieldItem> data = new ArrayList<FootballFieldItem>(ff_data.size());
        for(FootballField f : ff_data){
            data.add(new FootballFieldItem(f.getName(),f.getAddress(),f.getPhone()));
        }

        FootballFieldAdapter adapter = new FootballFieldAdapter(this, data);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                FootballFieldItem item = (FootballFieldItem)parent.getItemAtPosition(position);
                gotoFieldDetails(item);
            }
        });
    }

    private void gotoFieldDetails(FootballFieldItem item){
        for(FootballField f : this.ff_data){
            if(f.getName().equals(item.name)){
                Intent intent = new Intent(this, FieldDetailActivity.class);
                intent.putExtra(MainActivity.EXTRA_ITEM, f);
                startActivity(intent);
            }
        }
    }
}
