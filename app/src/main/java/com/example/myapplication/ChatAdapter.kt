package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val chatMessages: List<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_ME = 1
    private val VIEW_TYPE_OPPONENT = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ME) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_me, parent, false)
            MyMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_opponent, parent, false)
            OpponentMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = chatMessages[position]
        if (holder is MyMessageViewHolder) {
            holder.messageText.text = message.content
        } else if (holder is OpponentMessageViewHolder) {
            holder.messageText.text = message.content
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatMessages[position].role == "user") VIEW_TYPE_ME else VIEW_TYPE_OPPONENT
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }

    inner class MyMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
    }

    inner class OpponentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
    }
}