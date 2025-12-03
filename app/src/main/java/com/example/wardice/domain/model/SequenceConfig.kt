// domain/model/SequenceConfig.kt
package com.example.wardice.domain.model

data class SequenceRollConfig(
    val attacks: ThresholdRollConfig,
    val wounds: ThresholdRollConfig,
    val saves: ThresholdRollConfig,
    val simulateRuns: Int? = null
)

data class SequenceRollResult(
    val attacks: ThresholdRollResult,
    val wounds: ThresholdRollResult,
    val saves: ThresholdRollResult,
    val unsaved: Int
)

data class SequenceSimulationResult(
    val hitDistribution: IntArray,      // index = hits
    val woundDistribution: IntArray,    // index = wounds
    val unsavedDistribution: IntArray,  // index = unsaved wounds
    val runs: Int
)
