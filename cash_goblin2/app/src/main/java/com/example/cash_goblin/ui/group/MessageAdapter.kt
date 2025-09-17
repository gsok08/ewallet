package com.example.cash_goblin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cash_goblin.data.models.GroupMessage
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(private val items: List<GroupMessage>, private val currentUid: String) :
    RecyclerView.Adapter<MessagesAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView = view.findViewById(R.id.tvSender)
        val tvText: TextView = view.findViewById(R.id.tvText)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.tvSender.text = if (m.senderId == currentUid) "You" else m.senderName
        holder.tvText.text = m.text
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.tvTime.text = fmt.format(Date(m.timestamp))
    }

    override fun getItemCount(): Int = items.size
}
