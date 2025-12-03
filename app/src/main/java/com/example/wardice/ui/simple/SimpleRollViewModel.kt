// ui/simple/SimpleRollViewModel.kt
package com.example.wardice.ui.simple

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardice.domain.engine.WarDiceEngine
import com.example.wardice.domain.model.SimpleRollConfig
import com.example.wardice.domain.model.SimpleRollResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SimpleUiState(
    val lastRoll: SimpleRollResult? = null,
    val totalDistribution: IntArray? = null,
    val runs: Int = 0
)

class SimpleRollViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SimpleUiState())
    val uiState: StateFlow<SimpleUiState> = _uiState

    fun roll(config: SimpleRollConfig, simulateRuns: Int?) {
        val result = WarDiceEngine.simpleRoll(config)

        if (simulateRuns == null || simulateRuns <= 0) {
            _uiState.value = SimpleUiState(lastRoll = result)
        } else {
            viewModelScope.launch(Dispatchers.Default) {
                val dist = WarDiceEngine.simulateSimpleTotals(config, simulateRuns)
                _uiState.value = SimpleUiState(
                    lastRoll = result,
                    totalDistribution = dist,
                    runs = simulateRuns
                )
            }
        }
    }
}
