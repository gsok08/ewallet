package com.example.cash_goblin.data.repositories

import com.example.cash_goblin.data.models.User  // ✅ Use this one
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthR(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    suspend fun signUp(
        userId: String,
        email: String,
        password: String,
        phone: String,
        pin: String
    ): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val authUid = result.user?.uid ?: throw Exception("Auth UID not found")

            val user = User(
                userId = userId,
                authUid = authUid,
                email = email,
                phone = phone,
                balance = 0.0,
                pin = pin
            )

            db.getReference("users").child(userId).setValue(user).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun currentUserId(): String? = auth.currentUser?.uid
}
