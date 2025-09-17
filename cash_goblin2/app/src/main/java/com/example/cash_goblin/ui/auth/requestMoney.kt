package com.example.cash_goblin.ui.auth

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cash_goblin.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

class RequestMoney : Fragment() {

    private lateinit var etAmount: EditText
    private lateinit var btnGenerateQR: Button
    private lateinit var ivQRCode: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val requestsDb = FirebaseDatabase.getInstance().getReference("requests")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_requestmoney, container, false)

        etAmount = view.findViewById(R.id.etRequestAmount)
        btnGenerateQR = view.findViewById(R.id.btnGenerateQR)
        ivQRCode = view.findViewById(R.id.ivQRCode)

        btnGenerateQR.setOnClickListener {
            val amountText = etAmount.text.toString()
            if (amountText.isEmpty()) {
                Toast.makeText(requireContext(), "Enter amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDouble()
            generateAndSaveRequest(amount)
        }

        return view
    }

    private fun generateAndSaveRequest(amount: Double) {
        val userId = auth.currentUser?.uid ?: return

        // Store request in Firebase
        val requestId = requestsDb.push().key!!
        val requestData = mapOf(
            "userId" to userId,
            "amount" to amount,
            "timestamp" to System.currentTimeMillis()
        )
        requestsDb.child(requestId).setValue(requestData)

        val qrContent = "$userId|$amount"
        generateQRCode(qrContent)
    }

    private fun generateQRCode(content: String) {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            ivQRCode.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}
