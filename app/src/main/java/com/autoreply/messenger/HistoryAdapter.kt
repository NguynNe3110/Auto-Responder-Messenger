package com.autoreply.messenger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var items = listOf<ReplyHistory>()

    fun setData(list: List<ReplyHistory>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderText: TextView = itemView.findViewById(R.id.senderText)
        private val receivedText: TextView = itemView.findViewById(R.id.receivedText)
        private val replyText: TextView = itemView.findViewById(R.id.replyText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)

        fun bind(item: ReplyHistory) {
            senderText.text = item.sender
            receivedText.text = "Nhận: ${item.receivedMessage}"
            replyText.text = "Phản hồi: ${item.replyMessage}"

            val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            timeText.text = sdf.format(Date(item.timestamp))
        }
    }
}