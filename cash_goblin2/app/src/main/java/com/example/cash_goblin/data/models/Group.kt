package com.example.cash_goblin.data.models

data class Group(
    val groupId: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val createdBy: String = "",
    val date: Long = System.currentTimeMillis(),
    val members: Map<String, MemberContribution> = emptyMap(),
    val messages: Map<String, GroupMessage> = emptyMap() /
)