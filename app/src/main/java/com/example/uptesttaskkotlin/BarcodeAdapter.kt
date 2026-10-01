package com.example.uptesttaskkotlin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BarcodeAdapter : ListAdapter<BarcodeItem, BarcodeAdapter.ViewHolder>(BarcodeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text1: TextView = view.findViewById(android.R.id.text1)
        private val text2: TextView = view.findViewById(android.R.id.text2)
        private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun bind(item: BarcodeItem) {
            text1.text = item.displayValue
            text2.text = dateFormat.format(Date(item.timestamp))
        }
    }

    class BarcodeDiffCallback : DiffUtil.ItemCallback<BarcodeItem>() {
        override fun areItemsTheSame(oldItem: BarcodeItem, newItem: BarcodeItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BarcodeItem, newItem: BarcodeItem): Boolean {
            return oldItem == newItem
        }
    }
}
