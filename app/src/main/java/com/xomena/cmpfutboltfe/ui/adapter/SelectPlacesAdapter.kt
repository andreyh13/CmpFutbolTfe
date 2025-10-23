package com.xomena.cmpfutboltfe.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xomena.cmpfutboltfe.R
import com.xomena.cmpfutboltfe.model.MyPlace

class SelectPlacesAdapter(
    private val places: List<MyPlace>
) : RecyclerView.Adapter<SelectPlacesAdapter.ViewHolder>() {

    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(itemView: View, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.select_place_name)
        val addressTextView: TextView = itemView.findViewById(R.id.select_place_address)

        init {
            itemView.setOnClickListener {
                listener?.onItemClick(it, bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_place, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val place = places[position]
        holder.nameTextView.text = place.name
        holder.addressTextView.text = place.address
    }

    override fun getItemCount(): Int = places.size

    fun getItem(position: Int): MyPlace? {
        return places.getOrNull(position)
    }
}
