package com.xomena.cmpfutboltfe.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xomena.cmpfutboltfe.R
import com.xomena.cmpfutboltfe.model.MyPlace

class ManagePlacesAdapter(
    private val places: MutableList<MyPlace>
) : RecyclerView.Adapter<ManagePlacesAdapter.ViewHolder>() {

    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onRemovePlace(itemView: View, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.myplace_name)
        val addressTextView: TextView = itemView.findViewById(R.id.myplace_address)
        val removeButton: ImageButton = itemView.findViewById(R.id.myplace_delete)

        init {
            removeButton.setOnClickListener {
                listener?.onRemovePlace(it, bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_place, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val place = places[position]
        holder.nameTextView.text = place.name
        holder.addressTextView.text = place.address
    }

    override fun getItemCount(): Int = places.size

    fun addItem(place: MyPlace, position: Int) {
        places.add(position, place)
        notifyItemInserted(position)
    }

    fun getItem(position: Int): MyPlace? {
        return places.getOrNull(position)
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < places.size) {
            places.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
