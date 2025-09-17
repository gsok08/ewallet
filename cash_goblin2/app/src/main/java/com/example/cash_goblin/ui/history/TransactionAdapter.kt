package com.example.cash_goblin.ui.history

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.cash_goblin.R
import com.example.cash_goblin.data.models.Transaction
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val context: Context,
    private val transactions: MutableList<Transaction>
) : BaseAdapter() {

    private val usersDb = FirebaseDatabase.getInstance().getReference("users")
    private var currentUserId: String? = null

    init {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            usersDb.orderByChild("authUid").equalTo(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        currentUserId = snapshot.children.first().key
                    }
                }
        }
    }

    override fun getCount(): Int = transactions.size
    override fun getItem(position: Int): Any = transactions[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val tx = transactions[position]
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_transaction, parent, false)

        val tvUser = view.findViewById<TextView>(R.id.tvUser)
        val tvAmount = view.findViewById<TextView>(R.id.tvAmount)
        val tvType = view.findViewById<TextView>(R.id.tvType)
        val tvDate = view.findViewById<TextView>(R.id.tvDate)

        tvAmount.text = "RM %.2f".format(tx.amount)
        tvDate.text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            .format(Date(tx.timestamp))

        val uid = currentUserId ?: return view

        when {
            tx.type == "topup" && tx.userId == uid -> {
                tvUser.text = "Top-up"
                tvType.text = "Top-up"
            }
            tx.userId == uid -> {
                usersDb.child(tx.receiver).get()
                    .addOnSuccessListener { snap ->
                        val name = snap.child("userId").getValue(String::class.java) ?: tx.receiver
                        tvUser.text = "Sent to: $name"
                        tvType.text = "Sent"
                    }
            }
            tx.receiver == uid -> {
                usersDb.child(tx.userId).get()
                    .addOnSuccessListener { snap ->
                        val name = snap.child("userId").getValue(String::class.java) ?: tx.userId
                        tvUser.text = "Received from: $name"
                        tvType.text = "Received"
                    }
            }
            else -> {
                tvUser.text = "Transaction"
                tvType.text = tx.type.replaceFirstChar { it.uppercase() }
            }
        }

        return view
    }
}
