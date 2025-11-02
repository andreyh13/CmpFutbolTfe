package com.xomena.cmpfutboltfe

import com.xomena.cmpfutboltfe.model.*
import com.xomena.cmpfutboltfe.util.*

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.fragment.app.Fragment

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MainMapFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MainMapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainMapFragment : Fragment(),
    ClusterManager.OnClusterItemClickListener<MarkerItem>,
    ClusterManager.OnClusterItemInfoWindowClickListener<MarkerItem>,
    ClusterManager.OnClusterClickListener<MarkerItem>, OnMapReadyCallback {

    private val SAVED_CAMERA_STATE = "state_map_camera"

    private var mListener: OnFragmentInteractionListener? = null
    private var ff_data: Map<String, List<FootballField>>? = null
    private var map: GoogleMap? = null
    private var mClusterManager: ClusterManager<MarkerItem>? = null
    private var camera: CameraPosition? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

        val fragManager = this.fragmentManager
        val fragment = fragManager?.findFragmentById(R.id.main_map)
        if (fragment != null) {
            fragManager.beginTransaction().remove(fragment).commit()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_main_map, container, false)

        try {
            MapsInitializer.initialize(requireActivity())
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Could not initialize google play", e)
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(MainActivity.SAVED_KEYS)) {
            val keys = savedInstanceState.getStringArray(MainActivity.SAVED_KEYS)
            ff_data = LinkedHashMap()
            if (keys != null && keys.isNotEmpty()) {
                for (key in keys) {
                    val al = savedInstanceState.getParcelableArrayList<Parcelable>(key)
                    val af = ArrayList<FootballField>()
                    if (al != null) {
                        for (p in al) {
                            af.add(p as FootballField)
                        }
                    }
                    (ff_data as LinkedHashMap<String, List<FootballField>>)[key] = af
                }
            }
        }
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_CAMERA_STATE)) {
            camera = savedInstanceState.getParcelable(SAVED_CAMERA_STATE)
        }

        when (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext())) {
            ConnectionResult.SUCCESS -> {
                val mapFrag = requireActivity().fragmentManager.findFragmentById(R.id.main_map) as MapFragment
                mapFrag.getMapAsync(this)
            }
            ConnectionResult.SERVICE_MISSING -> Toast.makeText(
                requireActivity(),
                getText(R.string.play_service_missing),
                Toast.LENGTH_SHORT
            ).show()
            ConnectionResult.SERVICE_UPDATING -> Toast.makeText(
                requireActivity(),
                getText(R.string.play_service_updating),
                Toast.LENGTH_SHORT
            ).show()
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> Toast.makeText(
                requireActivity(),
                getText(R.string.play_service_update_required),
                Toast.LENGTH_SHORT
            ).show()
            ConnectionResult.SERVICE_DISABLED -> Toast.makeText(
                requireActivity(),
                getText(R.string.play_service_disabled),
                Toast.LENGTH_SHORT
            ).show()
            ConnectionResult.SERVICE_INVALID -> Toast.makeText(
                requireActivity(),
                getText(R.string.play_service_invalid),
                Toast.LENGTH_SHORT
            ).show()
            else -> {}
        }

        return v
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        ff_data?.let { data ->
            savedInstanceState.putStringArray(MainActivity.SAVED_KEYS, data.keys.toTypedArray())
            for (key in data.keys) {
                val af = ArrayList(data[key] ?: emptyList())
                savedInstanceState.putParcelableArrayList(key, af)
            }
        }

        map?.let {
            savedInstanceState.putParcelable(SAVED_CAMERA_STATE, it.cameraPosition)
        }
    }

    fun onMarkerSelected() {
        mListener?.onSelectMarker()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val a: Activity = if (context is Activity) {
            context
        } else {
            requireActivity()
        }
        try {
            mListener = a as OnFragmentInteractionListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                a.toString() + " must implement OnFragmentInteractionListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun onSelectMarker()
    }

    fun setFootbalFields(ff_data: Map<String, List<FootballField>>?) {
        this.ff_data = ff_data
        if (map != null && ff_data != null) {
            initializeMainMap(map!!, ff_data)
        }
    }

    private fun initializeMainMap(googleMap: GoogleMap, ff_data: Map<String, List<FootballField>>) {
        googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        googleMap.setOnCameraIdleListener(mClusterManager)
        googleMap.setOnMarkerClickListener(mClusterManager)
        googleMap.setOnInfoWindowClickListener(mClusterManager)
        val builder = LatLngBounds.Builder()
        for (county in ff_data.keys) {
            val ff_list = ff_data[county]
            if (ff_list != null && ff_list.isNotEmpty()) {
                for (ff in ff_list) {
                    val coord = LatLng(ff.lat, ff.lng)
                    builder.include(coord)
                    val markerItem = MarkerItem(ff.lat, ff.lng)
                    markerItem.setName(ff.name)
                    markerItem.setSnippet(getString(R.string.phoneLabel) + " " + ff.phone)
                    markerItem.footballField = ff
                    mClusterManager?.addItem(markerItem)
                }
            }
        }
        val m_bounds = builder.build()
        map?.moveCamera(CameraUpdateFactory.newLatLngBounds(m_bounds, 8))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.map = googleMap
        googleMap.uiSettings.isMapToolbarEnabled = false

        mClusterManager = ClusterManager(this.requireActivity(), googleMap)
        mClusterManager?.renderer = MarkerItemRenderer()
        mClusterManager?.setOnClusterItemClickListener(this)
        mClusterManager?.setOnClusterItemInfoWindowClickListener(this)
        mClusterManager?.setOnClusterClickListener(this)

        if (ff_data != null) {
            initializeMainMap(googleMap, ff_data!!)

            camera?.let {
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(it))
            }
        }
    }

    private inner class MarkerItemRenderer : DefaultClusterRenderer<MarkerItem>(
        requireActivity().applicationContext,
        map,
        mClusterManager
    ) {
        override fun onBeforeClusterItemRendered(item: MarkerItem, markerOptions: MarkerOptions) {
            markerOptions.position(item.position)
                .title(item.getName())
                .snippet(item.snippet)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_soccerfield))
        }
    }

    override fun onClusterItemClick(item: MarkerItem): Boolean {
        onMarkerSelected()
        return false
    }

    override fun onClusterItemInfoWindowClick(item: MarkerItem) {
        val intent = Intent(this.requireActivity(), FieldDetailActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_ITEM, item.footballField)
        startActivity(intent)
    }

    override fun onClusterClick(cluster: Cluster<MarkerItem>): Boolean {
        map?.let {
            it.moveCamera(CameraUpdateFactory.newLatLng(cluster.position))
            it.animateCamera(CameraUpdateFactory.zoomIn())
        }
        return true
    }

    companion object {
        private const val LOG_TAG = "MainMapFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MainMapFragment.
         */
        fun newInstance(): MainMapFragment {
            return MainMapFragment()
        }
    }
}
