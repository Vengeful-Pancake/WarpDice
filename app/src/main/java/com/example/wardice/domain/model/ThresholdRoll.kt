// domain/model/ThresholdRoll.kt
package com.example.wardice.domain.model

data class ThresholdRollConfig(
    val diceCount: Int,
    val dieType: DieType,
    val target: Int,                // e.g. 4+ to succeed
    val criticalAt: Int? = null,    // e.g. 6
    val rerollFailures: Boolean = false,
    val rerollOnes: Boolean = false
)

data class ThresholdRollResult(
    val successes: Int,
    val failures: Int,
    val criticals: Int,
    val rolls: List<Int>
)
