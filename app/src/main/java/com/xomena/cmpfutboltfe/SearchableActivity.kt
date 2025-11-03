package com.xomena.cmpfutboltfe

import android.app.SearchManager
import android.os.Bundle
import android.content.Intent

import androidx.appcompat.app.AppCompatActivity


class SearchableActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_searchable)

        // Get the intent, verify the action and get the query
        val intent = intent
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            //doMySearch(query)
        }
    }
}
