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
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class SendMoney : Fragment() {

    private lateinit var etPhoneNumber: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnSendMoney: Button
    private lateinit var btnScanQR: Button

    private val auth = FirebaseAuth.getInstance()
    private val usersDb = FirebaseDatabase.getInstance().getReference("users")
    private val transactionsDb = FirebaseDatabase.getInstance().getReference("transactions")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sendmoney, container, false)

        etPhoneNumber = view.findViewById(R.id.etSearch)
        etAmount = view.findViewById(R.id.etAmount)
        btnSendMoney = view.findViewById(R.id.btnSend)
        btnScanQR = view.findViewById(R.id.btnScanQR)

        btnSendMoney.setOnClickListener {
            val phone = etPhoneNumber.text.toString()
            val amountText = etAmount.text.toString()
            if (phone.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(requireContext(), "Enter phone and amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDouble()
            openPinEntry(phone, amount)
        }

        btnScanQR.setOnClickListener {
            startQRScanner()
        }

        parentFragmentManager.setFragmentResultListener("pin_result", this) { _, bundle ->
            val phone = bundle.getString("phone") ?: return@setFragmentResultListener
            val amount = bundle.getDouble("amount")
            searchReceiverAndSend(phone, amount)
        }

        return view
    }

    private fun openPinEntry(phone: String, amount: Double) {
        val fragment = PinEntryFragment().apply {
            arguments = Bundle().apply {
                putString("phone", phone)
                putDouble("amount", amount)
            }
        }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun performTransactionFromPin(phone: String, amount: Double) {
        searchReceiverAndSend(phone, amount)
    }


    private fun startQRScanner() {
        val options = ScanOptions()
        options.setPrompt("Scan a request QR code")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        qrLauncher.launch(options)
    }

    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val qrData = result.contents
            val parts = qrData.split("|")

            if (parts.size == 2) {
                val receiverUserId = parts[0]
                val amount = parts[1].toDouble()

                performTransactionRealtime(receiverUserId, amount)
            } else {
                Toast.makeText(requireContext(), "Invalid QR Code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun searchReceiverAndSend(phone: String, amount: Double) {
        usersDb.orderByChild("phone").equalTo(phone).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    for (userSnap in snapshot.children) {
                        val receiverId = userSnap.key ?: continue
                        performTransactionRealtime(receiverId, amount)
                    }
                } else {
                    Toast.makeText(requireContext(), "Receiver not found", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Search failed: ${it.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun performTransactionRealtime(receiverId: String, amount: Double) {
        val currentUid = auth.currentUser?.uid ?: return

        usersDb.orderByChild("authUid").equalTo(currentUid).get()
            .addOnSuccessListener { senderSnapshot ->
                if (!senderSnapshot.exists()) {
                    Toast.makeText(requireContext(), "Sender not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val senderSnap = senderSnapshot.children.first()
                val senderId = senderSnap.key ?: return@addOnSuccessListener

                val senderBalance = senderSnap.child("balance").getValue(Double::class.java) ?: 0.0

                // 🔍 Get receiver by receiverId
                val receiverRef = usersDb.child(receiverId)
                receiverRef.get().addOnSuccessListener { receiverSnap ->
                    val receiverBalance = receiverSnap.child("balance").getValue(Double::class.java) ?: 0.0
                    val senderName = senderSnap.child("name").getValue(String::class.java) ?: "Unknown"
                    val receiverName = receiverSnap.child("name").getValue(String::class.java) ?: "Unknown"

                    if (senderBalance < amount) {
                        Toast.makeText(requireContext(), "Insufficient balance", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val newSenderBalance = senderBalance - amount
                    val newReceiverBalance = receiverBalance + amount

                    val updates = mapOf(
                        "/$senderId/balance" to newSenderBalance,
                        "/$receiverId/balance" to newReceiverBalance
                    )

                    usersDb.updateChildren(updates).addOnSuccessListener {
                        saveTransaction(senderId, receiverId, senderName, receiverName, amount)
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), "Balance update failed: ${it.message}", Toast.LENGTH_SHORT).show()
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
        val transactionId1 = transactionsDb.push().key!!
        val transactionId2 = transactionsDb.push().key!!

        val senderTransaction = Transaction(
            userId = senderId,
            receiver = receiverId,
            amount = amount,
            type = "send",
            timestamp = System.currentTimeMillis(),
        )
        transactionsDb.child(transactionId1).setValue(senderTransaction)

        val receiverTransaction = Transaction(
            userId = receiverId,  // Receiver UID
            receiver = senderId,
            amount = amount,
            type = "receive",
            timestamp = System.currentTimeMillis(),
        )
        transactionsDb.child(transactionId2).setValue(receiverTransaction)

        Toast.makeText(requireContext(), "Transaction successful", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack() // Go back
    }


}
