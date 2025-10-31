package com.xomena.cmpfutboltfe.ui.fragment

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xomena.cmpfutboltfe.MainActivity
import com.xomena.cmpfutboltfe.R
import com.xomena.cmpfutboltfe.model.County
import com.xomena.cmpfutboltfe.model.FootballField
import com.xomena.cmpfutboltfe.ui.adapter.CountyAdapter
import com.xomena.cmpfutboltfe.util.DividerItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * A Fragment displaying list of counties with football fields.
 * Activities that contain this fragment must implement the
 * [OnFragmentInteractionListener] interface to handle interaction events.
 */
class CountiesFragment : Fragment(), CountyAdapter.OnItemClickListener {

    private var ffData: MutableMap<String, MutableList<FootballField>>? = null
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var adapter: CountyAdapter
    private var inProgressAsync = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val recyclerView = inflater.inflate(
            R.layout.fragment_counties,
            container,
            false
        ) as RecyclerView
        setupRecyclerView(recyclerView, savedInstanceState)
        return recyclerView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        ffData?.let { data ->
            outState.putStringArray(
                MainActivity.SAVED_KEYS,
                data.keys.toTypedArray()
            )
            data.forEach { (key, value) ->
                outState.putParcelableArrayList(key, ArrayList(value))
            }
        }
    }

    private fun setupRecyclerView(recyclerView: RecyclerView, savedInstanceState: Bundle?) {
        // Create adapter
        adapter = CountyAdapter(mutableListOf())
        adapter.setOnItemClickListener(this)
        
        // Attach the adapter to the recyclerview
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.addItemDecoration(
            DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL_LIST)
        )

        if (savedInstanceState?.containsKey(MainActivity.SAVED_KEYS) == true) {
            val keys = savedInstanceState.getStringArray(MainActivity.SAVED_KEYS)
            ffData = LinkedHashMap()
            
            keys?.let { keyArray ->
                if (keyArray.isNotEmpty()) {
                    for (key in keyArray) {
                        val parcelableList = savedInstanceState.getParcelableArrayList<FootballField>(key)
                        ffData!![key] = ArrayList(parcelableList ?: emptyList())
                    }
                    showCounties(keyArray)
                }
            }
        } else {
            // Fetch data from network
            inProgressAsync = true
            fetchFootballFields()
        }
    }

    override fun onItemClick(itemView: View, position: Int) {
        val nameTextView = itemView.findViewById<TextView>(R.id.county_name)
        nameTextView?.text?.toString()?.let { county ->
            ffData?.let { data ->
                onCountyPressed(county, data)
            }
        }
    }

    private fun onCountyPressed(county: String, data: Map<String, MutableList<FootballField>>) {
        listener?.onSelectCounty(county, data)
    }

    private fun onInitializeFootballFields(data: Map<String, MutableList<FootballField>>) {
        listener?.onObtainFootballFields(data)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnFragmentInteractionListener
            ?: throw ClassCastException("$context must implement OnFragmentInteractionListener")
        
        if (inProgressAsync) {
            listener?.onStartAsyncTask()
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun fetchFootballFields() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                getFromUrl(DATA_SERVICE_URL)
            }
            processFootballFieldsData(result)
        }
    }

    private fun getFromUrl(url: String): String {
        var result = ""
        
        // Check if you are connected or not
        if (isConnected()) {
            var conn: HttpURLConnection? = null
            try {
                val urlObj = URL(url)
                conn = urlObj.openConnection() as HttpURLConnection
                conn.connect()
                
                val inputStream = conn.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                
                reader.use { r ->
                    var line: String?
                    while (r.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
                result = stringBuilder.toString()
            } catch (ex: MalformedURLException) {
                Log.e(TAG, "Malformed URL", ex)
            } catch (ex: IOException) {
                Log.e(TAG, "IO error", ex)
            } finally {
                conn?.disconnect()
            }
        } else {
            Log.d(TAG, "Network is not connected")
            activity?.let {
                Toast.makeText(it, "Network is not connected", Toast.LENGTH_SHORT).show()
            }
        }
        return result
    }

    @Suppress("DEPRECATION")
    private fun isConnected(): Boolean {
        val connectivityManager = requireActivity()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }

    private fun processFootballFieldsData(result: String) {
        val res = LinkedHashMap<String, MutableList<FootballField>>()
        
        if (result.length > 2) {
            val json = result.substring(2, result.length - 1)
            
            try {
                val jsonArray = JSONArray(json)
                for (i in 1 until jsonArray.length()) {
                    if (!jsonArray.isNull(i)) {
                        val jsVal = jsonArray.getJSONArray(i)
                        val ff = FootballField.fromJSONArray(jsVal)
                        val key = ff.county
                        if (!res.containsKey(key)) {
                            res[key] = mutableListOf()
                        }
                        res[key]?.add(ff)
                    }
                }
            } catch (ex: JSONException) {
                Log.e(TAG, "JSON Exception", ex)
            }
        }

        ffData = res
        val data = res.keys.toTypedArray()
        showCounties(data)
        inProgressAsync = false
    }

    private fun showCounties(data: Array<String>) {
        if (data.isNotEmpty()) {
            data.forEachIndexed { index, county ->
                adapter.addItem(County(county), index)
            }
        }

        ffData?.let { onInitializeFootballFields(it) }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that activity.
     */
    interface OnFragmentInteractionListener {
        fun onObtainFootballFields(ffData: @JvmSuppressWildcards Map<String, List<FootballField>>)
        fun onSelectCounty(county: String, ffData: @JvmSuppressWildcards Map<String, List<FootballField>>)
        fun onStartAsyncTask()
    }

    companion object {
        private const val TAG = "CountiesFragment"
        private const val DATA_SERVICE_URL = 
            "https://script.google.com/macros/s/AKfycbyxqfsV0zdCKFRxgYYWPVO1PMshyhiuvTbvuKkkHjEGimPcdlpd/exec?jsonp=?"

        /**
         * Use this factory method to create a new instance of this fragment.
         *
         * @return A new instance of fragment CountiesFragment.
         */
        @JvmStatic
        fun newInstance() = CountiesFragment()
    }
}
