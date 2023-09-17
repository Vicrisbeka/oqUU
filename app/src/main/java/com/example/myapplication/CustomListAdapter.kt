package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CustomListAdapter(context: Context, private val items: List<CustomItem>) :
    ArrayAdapter<CustomItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView
        if (itemView == null) {
            itemView =
                LayoutInflater.from(context).inflate(R.layout.list_item_custom, parent, false)
        }

        val currentItem = items[position]

        val titleTextView = itemView?.findViewById<TextView>(R.id.itemTitle)

        titleTextView?.text = currentItem.title
        // Customize other UI components and set their values
        return itemView!!
    }
}

data class CustomItem(val title: String) {
    // You can include additional properties and methods as needed
}
