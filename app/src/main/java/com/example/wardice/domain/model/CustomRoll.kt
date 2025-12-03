// domain/model/CustomRoll.kt
package com.example.wardice.domain.model

data class CustomStep(
    val id: Long,
    val name: String,
    val config: ThresholdRollConfig
)

data class CustomSequenceResult(
    val stepResults: List<ThresholdRollResult>
)
