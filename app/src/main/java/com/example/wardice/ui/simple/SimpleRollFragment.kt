package com.example.wardice.ui.simple

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.wardice.R
import com.example.wardice.databinding.FragmentSimpleRollBinding
import com.example.wardice.domain.engine.SimpleRollConfig
import com.example.wardice.domain.engine.simpleRoll

class SimpleRollFragment : Fragment(R.layout.fragment_simple_roll) {

    private var _binding: FragmentSimpleRollBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSimpleRollBinding.bind(view)

        // Populate die types d2..d100
        val diceOptions = (2..100).map { "d$it" }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            diceOptions
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerDieSides.adapter = adapter
        binding.spinnerDieSides.setSelection(diceOptions.indexOf("d6").coerceAtLeast(0))

        binding.buttonRoll.setOnClickListener {
            val count = binding.editDiceCount.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
            val selectedDie = binding.spinnerDieSides.selectedItem as String
            val sides = selectedDie.removePrefix("d").toInt()
            val tn = binding.editSuccessTN.text.toString().toIntOrNull()

            val result = simpleRoll(SimpleRollConfig(count, sides, tn))

            val total = result.rolls.sum()
            val avg = if (result.rolls.isNotEmpty()) result.rolls.average() else 0.0

            binding.textSummary.text =
                "d${result.sides} x ${result.diceCount} | Total=$total | Avg=${"%.2f".format(avg)} | Successes=${result.successes}"

            val faceCounts = result.countsByFace.entries
                .sortedBy { it.key }
                .joinToString("\n") { (face, cnt) -> "$face \u2192 $cnt" }

            binding.textResults.text =
                "Rolls: ${result.rolls.joinToString(", ")}\n\nFace → count:\n$faceCounts"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
