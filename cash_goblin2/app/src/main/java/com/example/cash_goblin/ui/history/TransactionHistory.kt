package com.example.cash_goblin.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.example.cash_goblin.R
import com.example.cash_goblin.data.models.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TransactionHistory : Fragment() {

    private lateinit var listView: ListView
    private lateinit var adapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()

    private val auth = FirebaseAuth.getInstance()
    private val usersDb = FirebaseDatabase.getInstance().getReference("users")
    private val transactionsDb = FirebaseDatabase.getInstance().getReference("transactions")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transactionhistory, container, false)

        listView = view.findViewById(R.id.lvTransactions)
        adapter = TransactionAdapter(requireContext(), transactions)
        listView.adapter = adapter

        loadCurrentUserIdAndTransactions()

        return view
    }

    private fun loadCurrentUserIdAndTransactions() {
        val uid = auth.currentUser?.uid ?: return

        usersDb.orderByChild("authUid").equalTo(uid).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) return@addOnSuccessListener
                val currentUserId = snapshot.children.first().key ?: return@addOnSuccessListener

                loadTransactions(currentUserId)
            }
    }

    private fun loadTransactions(currentUserId: String) {
        transactionsDb.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactions.clear()
                for (child in snapshot.children) {
                    val tx = child.getValue(Transaction::class.java) ?: continue

                    if (tx.userId == currentUserId || tx.receiver == currentUserId || tx.type == "topup") {
                        transactions.add(tx)
                    }
                }

                transactions.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
