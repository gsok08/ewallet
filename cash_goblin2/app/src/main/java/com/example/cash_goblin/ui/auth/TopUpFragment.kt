package com.example.cash_goblin.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cash_goblin.R
import com.example.cash_goblin.data.models.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TopUpFragment : Fragment() {

    private lateinit var etAmount: EditText
    private lateinit var btnTopUp: Button

    private val auth = FirebaseAuth.getInstance()
    private val usersDb = FirebaseDatabase.getInstance().getReference("users")
    private val transactionsDb = FirebaseDatabase.getInstance().getReference("transactions")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_topup, container, false)

        etAmount = view.findViewById(R.id.etTopUpAmount)
        btnTopUp = view.findViewById(R.id.btnTopUp)

        btnTopUp.setOnClickListener {
            val amount = etAmount.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
            } else {
                topUp(amount)
            }
        }

        return view
    }

    private fun topUp(amount: Double) {
        val uid = auth.currentUser?.uid ?: return

        usersDb.orderByChild("authUid").equalTo(uid).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val userSnap = snapshot.children.first()
                val userId = userSnap.key ?: return@addOnSuccessListener
                val currentBalance = userSnap.child("balance").getValue(Double::class.java) ?: 0.0
                val newBalance = currentBalance + amount

                usersDb.child(userId).child("balance").setValue(newBalance)
                    .addOnSuccessListener {
                        saveTopUpTransaction(userId, amount) // 🔹 Log the transaction
                        Toast.makeText(requireContext(), "Top-up successful!", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun saveTopUpTransaction(userId: String, amount: Double) {
        val txId = transactionsDb.push().key!!
        val transaction = Transaction(
            userId = userId,
            receiver = userId,
            amount = amount,
            type = "topup",
            timestamp = System.currentTimeMillis()
        )
        transactionsDb.child(txId).setValue(transaction)
    }

}
