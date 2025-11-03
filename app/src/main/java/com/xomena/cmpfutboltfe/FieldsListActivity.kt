package com.xomena.cmpfutboltfe

import com.xomena.cmpfutboltfe.model.*
import com.xomena.cmpfutboltfe.util.*
import com.xomena.cmpfutboltfe.ui.adapter.*

import android.graphics.drawable.Drawable
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView

import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class FieldsListActivity : AppCompatActivity(), FootballFieldAdapter.OnItemClickListener {

    private var ff_data: List<FootballField>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fields_list)

        val toolbar = findViewById<Toolbar>(R.id.toolbarPitches)
        setSupportActionBar(toolbar)
        try {
            val ab = supportActionBar
            ab?.setDisplayShowTitleEnabled(false)
        } catch (e: NullPointerException) {
            Log.e(TAG, "Exception", e)
        }

        val upArrow = ResourcesCompat.getDrawable(
            resources, R.drawable.abc_ic_ab_back_material,
            applicationContext.theme
        )
        toolbar.navigationIcon = upArrow
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val i = intent
        val county = i.getStringExtra(MainActivity.EXTRA_COUNTY)
        this.ff_data = i.getParcelableArrayListExtra(MainActivity.EXTRA_FIELDS)

        val title = findViewById<TextView>(R.id.toolbar_title_pitches)
        title.text = county

        val recyclerView = findViewById<RecyclerView>(R.id.rvPitches)

        val adapter = FootballFieldAdapter(FootballFieldItem.createPitchesList(this.ff_data ?: emptyList()))
        adapter.setOnItemClickListener(this)
        // Attach the adapter to the recyclerview to populate items
        recyclerView.adapter = adapter
        // Set layout manager to position the items
        recyclerView.layoutManager = LinearLayoutManager(this)
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST)
        recyclerView.addItemDecoration(itemDecoration)
    }

    override fun onItemClick(itemView: View, position: Int) {
        val nameTextView = itemView.findViewById<TextView>(R.id.ffNameValue)
        if (nameTextView != null) {
            val mName = nameTextView.text.toString()
            for (f in this.ff_data ?: emptyList()) {
                if (f.name == mName) {
                    val intent = Intent(this, FieldDetailActivity::class.java)
                    intent.putExtra(MainActivity.EXTRA_ITEM, f)
                    startActivity(intent)
                }
            }
        }
    }

    companion object {
        private const val TAG = "FieldsListActivity"
    }
}
