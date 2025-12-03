// domain/model/SimpleRoll.kt
package com.example.wardice.domain.model

data class SimpleRollConfig(
    val diceCount: Int,
    val dieType: DieType
)

data class SimpleRollResult(
    val rolls: List<Int>,
    val total: Int,
    val average: Double,
    val faceCounts: Map<Int, Int>
)
