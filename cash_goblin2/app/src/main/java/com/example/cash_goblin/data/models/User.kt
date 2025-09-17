package com.example.cash_goblin.data.models

data class User(
    val userId: String = "",
    val authUid: String = "",
    val email: String = "",
    val phone: String = "",
    val balance: Double = 0.0,
    val pin: String = ""
)
