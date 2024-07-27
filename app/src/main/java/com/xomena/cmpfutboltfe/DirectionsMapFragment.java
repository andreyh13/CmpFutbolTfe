package com.xomena.cmpfutboltfe;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DirectionsMapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DirectionsMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DirectionsMapFragment extends Fragment {

    private GoogleMap mMap;
    private JSONObject jsonObject;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DirectionsMapFragment.
     */
    public static DirectionsMapFragment newInstance() {
        DirectionsMapFragment fragment = new DirectionsMapFragment();
        return fragment;
    }

    public DirectionsMapFragment() {
        // Required empty public constructor
    }

    public void setJSON(JSONObject json){
        this.jsonObject = json;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_directions_map, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity a;
        if (context instanceof Activity){
            a= (Activity) context;
        } else {
            a = getActivity();
        }

        try {
            mListener = (OnFragmentInteractionListener) a;
        } catch (ClassCastException e) {
            throw new ClassCastException(a.toString()
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
        void onDirectionsMapClick();
        void onRouteAnimationClick();
    }
}
