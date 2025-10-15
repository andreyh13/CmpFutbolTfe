package com.xomena.cmpfutboltfe.model

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class MarkerItem(
    private val position: LatLng
) : ClusterItem {

    constructor(lat: Double, lng: Double) : this(LatLng(lat, lng))

    private var _name: String = ""
    private var _snippet: String = ""
    var footballField: FootballField? = null

    fun setName(n: String) {
        _name = n
    }

    fun setSnippet(s: String) {
        _snippet = s
    }

    fun getName(): String = _name

    override fun getPosition(): LatLng = position

    override fun getTitle(): String = _name

    override fun getSnippet(): String = _snippet
}
