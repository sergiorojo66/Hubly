package com.example.rankup.domain.model

data class RankingUser(
    val id: String = "",
    val userName: String = "",
    val points: Int = 0,
    val level: Int = 1,
    val wins: Int = 0,
    val position: Int = 0
)