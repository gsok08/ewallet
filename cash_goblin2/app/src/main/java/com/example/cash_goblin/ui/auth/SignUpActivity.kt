package com.example.cash_goblin.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cash_goblin.Home
import com.example.cash_goblin.R
import com.example.cash_goblin.data.repositories.AuthR
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private val authRepo = AuthR()
    private val usersDb = FirebaseDatabase.getInstance().getReference("users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up)

        val username = findViewById<EditText>(R.id.etUsername)
        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val phone = findViewById<EditText>(R.id.etPhone)
        val pin = findViewById<EditText>(R.id.etPin)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        btnSignUp.setOnClickListener {
            val uName = username.text.toString().trim()
            val uEmail = email.text.toString().trim()
            val uPass = password.text.toString().trim()
            val uPhone = phone.text.toString().trim()
            val uPin = pin.text.toString().trim()

            if (uPin.length != 6) {
                Toast.makeText(this, "PIN must be 6 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkDuplicateAndSignUp(uName, uEmail, uPass, uPhone, uPin)
        }
    }

    private fun checkDuplicateAndSignUp(
        username: String,
        email: String,
        password: String,
        phone: String,
        pin: String
    ) {
        usersDb.get().addOnSuccessListener { snapshot ->
            var emailExists = false
            var phoneExists = false

            for (child in snapshot.children) {
                val userEmail = child.child("email").value?.toString()
                val userPhone = child.child("phone").value?.toString()

                if (userEmail == email) emailExists = true
                if (userPhone == phone) phoneExists = true
            }

            when {
                emailExists -> Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show()
                phoneExists -> Toast.makeText(this, "Phone number already exists", Toast.LENGTH_SHORT).show()
                else -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val result = authRepo.signUp(username, email, password, phone, pin)
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(this@SignUpActivity, "Sign up successful", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@SignUpActivity, Home::class.java)
                                    startActivity(intent)
                                    finish()
                                },
                                onFailure = {
                                    Toast.makeText(this@SignUpActivity, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this@SignUpActivity, "Sign up failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

        }.addOnFailureListener {
            Toast.makeText(this, "Error checking duplicates: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
