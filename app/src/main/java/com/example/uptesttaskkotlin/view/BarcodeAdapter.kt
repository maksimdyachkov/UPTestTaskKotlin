package com.example.uptesttaskkotlin.view

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.uptesttaskkotlin.model.BarcodeItem
import java.text.DateFormat
import java.util.Date

class BarcodeAdapter : ListAdapter<BarcodeItem, BarcodeAdapter.ViewHolder>(BarcodeDiffCallback()) {

    // The history outlives a single day, so the date is shown along with the time.
    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view, dateFormat)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(view: View, private val dateFormat: DateFormat) : RecyclerView.ViewHolder(view) {
        private val text1: TextView = view.findViewById(android.R.id.text1)
        private val text2: TextView = view.findViewById(android.R.id.text2)

        init {
            // A barcode can hold hundreds of characters and line breaks: show one line with "…".
            text1.isSingleLine = true
            text1.ellipsize = TextUtils.TruncateAt.END
        }

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
