// ui/sequence/SequenceRollFragment.kt
package com.example.wardice.ui.sequence

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.wardice.databinding.FragmentSequenceRollBinding
import com.example.wardice.domain.model.*
import com.github.mikephil.charting.data.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SequenceRollFragment : Fragment() {

    private var _binding: FragmentSequenceRollBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SequenceRollViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSequenceRollBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupDieDropdown()
        setupListeners()
        observeState()
    }

    private fun setupDieDropdown() {
        val labels = DieType.values().map { it.toString() }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
        binding.dropdownAttackDie.setAdapter(adapter)
        binding.dropdownAttackDie.setText(DieType.D6.toString(), false)
    }

    private fun setupListeners() {
        binding.buttonExecute.setOnClickListener {
            val config = buildConfig() ?: return@setOnClickListener
            val simulate = binding.checkSimulate.isChecked
            val runs = binding.inputSimTimes.text.toString().toIntOrNull() ?: 0

            if (simulate && runs > 0) {
                viewModel.simulate(config, runs)
            } else {
                viewModel.runOnce(config)
            }
        }
    }

    private fun buildConfig(): SequenceRollConfig? {
        val attacks = binding.inputAttacks.text.toString().toIntOrNull() ?: return null
        val dieType = DieType.values().first {
            it.toString() == binding.dropdownAttackDie.text.toString()
        }

        val hitTN = binding.inputHitTN.text.toString().toIntOrNull() ?: return null
        val woundTN = binding.inputWoundTN.text.toString().toIntOrNull() ?: return null
        val saveTN = binding.inputSaveTN.text.toString().toIntOrNull() ?: return null

        val hitConfig = ThresholdRollConfig(
            diceCount = attacks,
            dieType = dieType,
            target = hitTN,
            criticalAt = 6,
            rerollFailures = binding.checkRerollHitFailures.isChecked,
            rerollOnes = binding.checkRerollHitOnes.isChecked
        )

        val woundConfig = ThresholdRollConfig(
            diceCount = attacks,           // overridden in engine to hits
            dieType = dieType,
            target = woundTN,
            criticalAt = 6,
            rerollFailures = binding.checkRerollWoundFailures.isChecked,
            rerollOnes = binding.checkRerollWoundOnes.isChecked
        )

        val saveConfig = ThresholdRollConfig(
            diceCount = attacks,           // overridden in engine to wounds
            dieType = dieType,
            target = saveTN,
            criticalAt = 6
        )

        return SequenceRollConfig(
            attacks = hitConfig,
            wounds = woundConfig,
            saves = saveConfig
        )
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                state.lastResult?.let { showSummary(it) }
                state.simulation?.let { showCharts(it) }
            }
        }
    }

    private fun showSummary(result: SequenceRollResult) {
        val sb = StringBuilder()
        sb.appendLine("d${result.attacks.rolls.maxOrNull() ?: 6} | Hit ${result.attacks.successes}+," +
                " Wound ${result.wounds.successes}+," +
                " Save ${result.saves.successes}+")
        sb.appendLine("Attacks (effective): ${result.attacks.rolls.size}")
        sb.appendLine("Hits: ${result.attacks.successes}")
        sb.appendLine("Wounds: ${result.wounds.successes}")
        sb.appendLine("Saves: ${result.saves.successes}")
        sb.appendLine("Unsaved: ${result.unsaved}")
        binding.textSummary.text = sb.toString()
    }

    private fun showCharts(sim: SequenceSimulationResult) {
        setupBarChart(binding.chartHits, sim.hitDistribution, sim.runs, "Hits % by count")
        setupBarChart(binding.chartWounds, sim.woundDistribution, sim.runs, "Wounds % by count")
        setupBarChart(binding.chartUnsaved, sim.unsavedDistribution, sim.runs, "Unsaved % by count")
    }

    private fun setupBarChart(chart: com.github.mikephil.charting.charts.BarChart,
                              dist: IntArray,
                              runs: Int,
                              label: String) {
        val entries = mutableListOf<BarEntry>()
        dist.forEachIndexed { index, count ->
            val percent = (count.toFloat() / runs) * 100f
            entries += BarEntry(index.toFloat(), percent)
        }

        val dataSet = BarDataSet(entries, label)
        val data = BarData(dataSet)
        data.barWidth = 0.9f

        chart.apply {
            this.data = data
            description.isEnabled = false
            setFitBars(true)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
