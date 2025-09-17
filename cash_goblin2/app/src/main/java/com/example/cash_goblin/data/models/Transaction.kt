package com.example.cash_goblin.data.models

data class Transaction(
    var userId: String = "",
    var receiver: String = "",
    var amount: Double = 0.0,
    var type: String = "",
    var timestamp: Long = 0L,

)
