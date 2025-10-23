package com.xomena.cmpfutboltfe.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xomena.cmpfutboltfe.R
import com.xomena.cmpfutboltfe.model.FootballFieldItem

class FootballFieldAdapter(
    private val pitches: List<FootballFieldItem>
) : RecyclerView.Adapter<FootballFieldAdapter.ViewHolder>() {

    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(itemView: View, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.ffNameValue)
        val addressTextView: TextView = itemView.findViewById(R.id.ffAddressValue)
        val phoneTextView: TextView = itemView.findViewById(R.id.ffPhoneValue)

        init {
            itemView.setOnClickListener {
                listener?.onItemClick(it, bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ff_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pitch = pitches[position]
        holder.nameTextView.text = pitch.name
        holder.addressTextView.text = pitch.address
        holder.phoneTextView.text = pitch.phone
    }

    override fun getItemCount(): Int = pitches.size
}
