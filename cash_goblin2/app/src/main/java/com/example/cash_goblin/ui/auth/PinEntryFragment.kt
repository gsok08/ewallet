package com.example.cash_goblin.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.example.cash_goblin.R
import com.example.cash_goblin.data.models.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PinEntryFragment : Fragment() {

    private lateinit var etPin: EditText
    private lateinit var btnConfirmPin: Button

    private val auth = FirebaseAuth.getInstance()
    private val usersDb = FirebaseDatabase.getInstance().getReference("users")
    private val transactionsDb = FirebaseDatabase.getInstance().getReference("transactions")

    private var phone: String? = null
    private var amount: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        phone = arguments?.getString("phone")
        amount = arguments?.getDouble("amount")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pinentry, container, false)
        etPin = view.findViewById(R.id.etPinEntry)
        btnConfirmPin = view.findViewById(R.id.btnConfirmPin)

        btnConfirmPin.setOnClickListener {
            val enteredPin = etPin.text.toString()
            if (enteredPin.isEmpty()) {
                Toast.makeText(requireContext(), "Enter PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            validatePinAndSend(enteredPin)
        }

        return view
    }

    private fun validatePinAndSend(enteredPin: String) {
        val uid = auth.currentUser?.uid ?: return
        usersDb.orderByChild("authUid").equalTo(uid).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (child in snapshot.children) {
                    val correctPin = child.child("pin").getValue(String::class.java)
                    if (enteredPin == correctPin) {
                        Toast.makeText(requireContext(), "PIN correct", Toast.LENGTH_SHORT).show()
                        performTransaction()
                        return@addOnSuccessListener
                    } else {
                        Toast.makeText(requireContext(), "Incorrect PIN", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun performTransaction() {
        val phoneNumber = phone ?: return
        val sendAmount = amount ?: return

        usersDb.orderByChild("phone").equalTo(phoneNumber).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "Receiver not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val receiverSnap = snapshot.children.first()
                val receiverId = receiverSnap.key ?: return@addOnSuccessListener
                val receiverName = receiverSnap.child("name").getValue(String::class.java) ?: "Unknown"
                val receiverBalance = receiverSnap.child("balance").getValue(Double::class.java) ?: 0.0

                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                usersDb.orderByChild("authUid").equalTo(uid).get()
                    .addOnSuccessListener { senderSnapshot ->
                        val senderSnap = senderSnapshot.children.first()
                        val senderId = senderSnap.key ?: return@addOnSuccessListener
                        val senderName = senderSnap.child("name").getValue(String::class.java) ?: "Unknown"
                        val senderBalance = senderSnap.child("balance").getValue(Double::class.java) ?: 0.0

                        if (senderBalance < sendAmount) {
                            Toast.makeText(requireContext(), "Insufficient balance", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val updates = mapOf(
                            "/$senderId/balance" to senderBalance - sendAmount,
                            "/$receiverId/balance" to receiverBalance + sendAmount
                        )

                        usersDb.updateChildren(updates).addOnSuccessListener {
                            saveTransaction(senderId, receiverId, senderName, receiverName, sendAmount)
                            Toast.makeText(requireContext(), "Transaction successful", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }.addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to update balances", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }

    private fun saveTransaction(
        senderId: String,
        receiverId: String,
        senderName: String,
        receiverName: String,
        amount: Double
    ) {
        val t1 = transactionsDb.push().key!!
        val t2 = transactionsDb.push().key!!

        val senderTx = Transaction(
            userId = senderId,
            receiver = receiverId,
            amount = amount,
            type = "send",
            timestamp = System.currentTimeMillis()
        )
        transactionsDb.child(t1).setValue(senderTx)

        val receiverTx = Transaction(
            userId = receiverId,
            receiver = senderId,
            amount = amount,
            type = "receive",
            timestamp = System.currentTimeMillis()
        )
        transactionsDb.child(t2).setValue(receiverTx)
    }



}
