package com.xomena.cmpfutboltfe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainMapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainMapFragment extends Fragment
        implements ClusterManager.OnClusterItemClickListener<MarkerItem>,
                ClusterManager.OnClusterItemInfoWindowClickListener<MarkerItem>,
                ClusterManager.OnClusterClickListener<MarkerItem> {
    private OnFragmentInteractionListener mListener;
    private Map<String,List<FootballField>> ff_data;
    private GoogleMap map;
    private ClusterManager<MarkerItem> mClusterManager;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MainMapFragment.
     */
    public static MainMapFragment newInstance() {
        return new MainMapFragment();
    }

    public MainMapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_map, container, false);
    }

    public void onMarkerSelected() {
        if (mListener != null) {
            mListener.onSelectMarker();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void onSelectMarker();
    }

    public void initializeMainMap(Map<String,List<FootballField>> ff_data){
        this.ff_data = ff_data;
        MapFragment mapFrag = (MapFragment)
                getActivity().getFragmentManager().findFragmentById(R.id.main_map);
        if(mapFrag!=null && ff_data!=null) {
            map = mapFrag.getMap();
            if (map != null) {
                mClusterManager = new ClusterManager<MarkerItem>(this.getActivity(), map);
                mClusterManager.setRenderer(new MarkerItemRenderer());
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                map.setOnCameraChangeListener(mClusterManager);
                map.setOnMarkerClickListener(mClusterManager);
                map.setOnInfoWindowClickListener(mClusterManager);
                mClusterManager.setOnClusterItemClickListener(this);
                mClusterManager.setOnClusterItemInfoWindowClickListener(this);
                mClusterManager.setOnClusterClickListener(this);
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for(String county : ff_data.keySet()){
                    List<FootballField> ff_list = ff_data.get(county);
                    if(ff_list!=null && ff_list.size()>0){
                        for(FootballField ff: ff_list){
                            LatLng coord = new LatLng(ff.getLat(),ff.getLng());
                            builder.include(coord);
                            /*map.addMarker(new MarkerOptions().position(coord)
                                    .title(ff.getName()).snippet(getString(R.string.phoneLabel)+" "+ff.getPhone())
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_soccerfield)));*/
                            MarkerItem markerItem = new MarkerItem(ff.getLat(),ff.getLng());
                            markerItem.setName(ff.getName());
                            markerItem.setSnippet(getString(R.string.phoneLabel)+" "+ff.getPhone());
                            markerItem.setFootballField(ff);
                            mClusterManager.addItem(markerItem);
                        }
                    }
                }
                LatLngBounds m_bounds = builder.build();
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(m_bounds, 8));

            }
        }
    }

    private class MarkerItemRenderer extends DefaultClusterRenderer<MarkerItem> {
        public MarkerItemRenderer() {
            super(getActivity().getApplicationContext(), map, mClusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(MarkerItem item, MarkerOptions markerOptions) {
            markerOptions.position(item.getPosition())
                    .title(item.getName()).snippet(item.getSnippet())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_soccerfield));
        }
    }

    @Override
    public boolean onClusterItemClick(MarkerItem item) {
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(MarkerItem item) {
        Intent intent = new Intent(this.getActivity(), FieldDetailActivity.class);
        intent.putExtra(MainActivity.EXTRA_ITEM, item.getFootballField());
        startActivity(intent);
    }

    @Override
    public boolean onClusterClick(Cluster<MarkerItem> cluster) {
        if(map!=null) {
            map.moveCamera(CameraUpdateFactory.newLatLng(cluster.getPosition()));
            map.animateCamera(CameraUpdateFactory.zoomIn());
        }
        return true;
    }
}

