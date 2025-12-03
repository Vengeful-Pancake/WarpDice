// ui/custom/CustomRollViewModel.kt
package com.example.wardice.ui.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardice.domain.engine.WarDiceEngine
import com.example.wardice.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class CustomUiState(
    val steps: List<CustomStep> = emptyList(),
    val lastResult: CustomSequenceResult? = null
)

class CustomRollViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CustomUiState())
    val uiState: StateFlow<CustomUiState> = _uiState

    private var idCounter = 0L

    fun addStep(template: ThresholdRollConfig) {
        val step = CustomStep(
            id = idCounter++,
            name = "Step ${idCounter}",
            config = template
        )
        _uiState.value = _uiState.value.copy(
            steps = _uiState.value.steps + step
        )
    }

    fun updateStep(updated: CustomStep) {
        _uiState.value = _uiState.value.copy(
            steps = _uiState.value.steps.map { if (it.id == updated.id) updated else it }
        )
    }

    fun removeStep(id: Long) {
        _uiState.value = _uiState.value.copy(
            steps = _uiState.value.steps.filterNot { it.id == id }
        )
    }

    fun moveStep(from: Int, to: Int) {
        val list = _uiState.value.steps.toMutableList()
        val step = list.removeAt(from)
        list.add(to, step)
        _uiState.value = _uiState.value.copy(steps = list)
    }

    fun runOnce(simulateRuns: Int? = null) {
        val steps = _uiState.value.steps
        if (steps.isEmpty()) return

        viewModelScope.launch(Dispatchers.Default) {
            val rng = Random.Default
            val results = mutableListOf<ThresholdRollResult>()
            steps.forEach { step ->
                results += WarDiceEngine.thresholdRoll(step.config, rng)
            }
            _uiState.value = _uiState.value.copy(
                lastResult = CustomSequenceResult(results)
            )
        }
    }
}
