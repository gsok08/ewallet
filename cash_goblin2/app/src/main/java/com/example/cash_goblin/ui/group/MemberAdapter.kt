package com.example.cash_goblin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cash_goblin.data.models.MemberContribution

class MembersAdapter(
    private val items: List<Pair<String, MemberContribution>>,
    private val onQuickAdd: (String, Double) -> Unit // quick add callback (e.g., add default small amount)
) : RecyclerView.Adapter<MembersAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvMemberName)
        val tvAmount: TextView = view.findViewById(R.id.tvMemberAmount)
        val btnAddSmall: Button = view.findViewById(R.id.btnAddSmall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (name, contrib) = items[position]
        holder.tvName.text = name
        holder.tvAmount.text = "RM ${contrib.amount}"
        holder.btnAddSmall.setOnClickListener {
            onQuickAdd(name, 10.0) // example: quick add RM10
        }
    }
    override fun getItemCount(): Int = items.size
}
