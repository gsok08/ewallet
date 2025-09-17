package com.example.cash_goblin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.example.cash_goblin.ui.auth.LoginActivity
import com.example.cash_goblin.Home // Assuming you have a HomeActivity for logged-in users

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val nextActivity = if (currentUser != null) {
            Home::class.java
        } else {
            LoginActivity::class.java
        }

        startActivity(Intent(this, nextActivity))

        finish()
    }
}
