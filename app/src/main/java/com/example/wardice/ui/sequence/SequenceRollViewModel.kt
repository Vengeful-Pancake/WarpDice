// ui/sequence/SequenceRollViewModel.kt
package com.example.wardice.ui.sequence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardice.domain.engine.WarDiceEngine
import com.example.wardice.domain.model.SequenceRollConfig
import com.example.wardice.domain.model.SequenceRollResult
import com.example.wardice.domain.model.SequenceSimulationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SequenceUiState(
    val lastResult: SequenceRollResult? = null,
    val simulation: SequenceSimulationResult? = null
)

class SequenceRollViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SequenceUiState())
    val uiState: StateFlow<SequenceUiState> = _uiState

    fun runOnce(config: SequenceRollConfig) {
        val result = WarDiceEngine.runSequence(config)
        _uiState.value = SequenceUiState(lastResult = result)
    }

    fun simulate(config: SequenceRollConfig, runs: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val simResult = WarDiceEngine.simulateSequence(config, runs)
            _uiState.value = SequenceUiState(
                lastResult = WarDiceEngine.runSequence(config),
                simulation = simResult
            )
        }
    }
}
