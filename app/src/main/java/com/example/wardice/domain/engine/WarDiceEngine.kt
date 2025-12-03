package com.example.wardice.domain.engine

//All dice behaviour is now in one reusable engine file, independent of Compose or UI.

import kotlin.random.Random

/* ---------- Shared helpers ---------- */

fun defaultTN(sides: Int): Int = sides / 2

fun countsMap(sorted: List<Int>): Map<Int, Int> =
    sorted.groupingBy { it }.eachCount()

/* ---------- Simple Roll ---------- */

data class SimpleRollConfig(
    val diceCount: Int,
    val sides: Int,
    val successTN: Int? = null
)

data class SimpleRollResult(
    val sides: Int,
    val diceCount: Int,
    val rolls: List<Int>,
    val countsByFace: Map<Int, Int>,
    val successes: Int
)

fun simpleRoll(
    config: SimpleRollConfig,
    rng: Random = Random(System.currentTimeMillis())
): SimpleRollResult {
    val n = config.diceCount.coerceAtLeast(1)
    val sides = config.sides.coerceAtLeast(2)

    val rolls = List(n) { rng.nextInt(1, sides + 1) }.sorted()
    val counts = countsMap(rolls)

    val tn = config.successTN?.coerceIn(1, sides) ?: defaultTN(sides)
    val successes = rolls.count { it >= tn }

    return SimpleRollResult(
        sides = sides,
        diceCount = n,
        rolls = rolls,
        countsByFace = counts,
        successes = successes
    )
}

/* ---------- Sequence Roll (1 run) ---------- */

data class SequenceOutcome(
    val sides: Int,
    val hitTN: Int,
    val woundTN: Int,
    val saveTN: Int,
    val critHitTN: Int,
    val critWoundTN: Int,
    val attacks: Int,
    val torrent: Boolean,
    val hitRolls: List<Int>,
    val hitBaseSuccesses: Int,
    val hitCrits: Int,
    val extraHitsFromCrit: Int,
    val totalHitsAfterCrit: Int,
    val autoWoundsFromHitCrit: Int,
    val woundRolls: List<Int>,
    val woundCrits: Int,
    val woundSuccessesFromRoll: Int,
    val totalWounds: Int,
    val autoSavesFromWoundCrit: Int,
    val saveRolls: List<Int>,
    val saveSuccessesFromRoll: Int,
    val totalSaves: Int,
    val unsaved: Int
)

private fun rollXd6Sum(x: Int, rng: Random): Int {
    if (x <= 0) return 0
    var sum = 0
    repeat(x) { sum += rng.nextInt(1, 7) }
    return sum
}

/**
 * Exact port of your old runOnce() from Compose app.
 */
fun runSequenceOnce(
    baseAttacks: Int,
    attackXd6: Boolean,
    sides: Int,
    torrent: Boolean,
    hitTN: Int,
    woundTN: Int,
    saveTN: Int,
    critHitTN: Int,
    critWoundTN: Int,
    sustainEnabled: Boolean,
    sustainX: Int,
    lethalHit: Boolean,
    devastatingWound: Boolean,
    rerollHitFails: Boolean,
    rerollHitOnes: Boolean,
    rerollWoundFails: Boolean,
    rerollWoundOnes: Boolean,
    rng: Random = Random(System.currentTimeMillis())
): SequenceOutcome {

    val attacks = if (attackXd6) rollXd6Sum(baseAttacks, rng) else baseAttacks

    // HIT
    val hitFinal: List<Int>
    val hitCrits: Int
    val hitBase: Int
    if (torrent) {
        hitFinal = emptyList()
        hitCrits = 0
        hitBase = attacks
    } else {
        val first = MutableList(attacks) { rng.nextInt(1, sides + 1) }
        val second = first.toMutableList()
        if (rerollHitFails && rerollHitOnes) {
            for (i in second.indices) if (second[i] < hitTN) second[i] = rng.nextInt(1, sides + 1)
        } else if (rerollHitFails) {
            for (i in second.indices) if (second[i] < hitTN) second[i] = rng.nextInt(1, sides + 1)
        } else if (rerollHitOnes) {
            for (i in second.indices) if (second[i] == 1) second[i] = rng.nextInt(1, sides + 1)
        }
        second.sort()
        hitFinal = second
        hitCrits = hitFinal.count { it >= critHitTN }
        hitBase = hitFinal.count { it >= hitTN }
    }
    val extraHits = if (!torrent && sustainEnabled) hitCrits * sustainX else 0
    val totalHitsAfterCrit = hitBase + extraHits

    // WOUND
    val autoWounds = if (!torrent && lethalHit) hitCrits else 0
    val woundDice = (totalHitsAfterCrit - autoWounds).coerceAtLeast(0)
    val woundFirst = MutableList(woundDice) { rng.nextInt(1, sides + 1) }
    val woundSecond = woundFirst.toMutableList()
    if (rerollWoundFails) {
        for (i in woundSecond.indices) if (woundSecond[i] < woundTN) woundSecond[i] = rng.nextInt(1, sides + 1)
    } else if (rerollWoundOnes) {
        for (i in woundSecond.indices) if (woundSecond[i] == 1) woundSecond[i] = rng.nextInt(1, sides + 1)
    }
    woundSecond.sort()
    val woundFinal = woundSecond
    val woundCrits = woundFinal.count { it >= critWoundTN }
    val woundFromRoll = woundFinal.count { it >= woundTN }
    val totalWounds = autoWounds + woundFromRoll

    // SAVE
    // Devastating Wound: crit wounds are UNSAVABLE (bypass saves)
    val unsavableFromDW = if (devastatingWound) woundCrits else 0
    val saveDice = (totalWounds - unsavableFromDW).coerceAtLeast(0)
    val saveRolls =
        if (saveDice > 0) List(saveDice) { rng.nextInt(1, sides + 1) }.sorted() else emptyList()
    val saveFromRoll = saveRolls.count { it >= saveTN }
    val totalSaves = saveFromRoll
    val unsaved = unsavableFromDW + (saveDice - saveFromRoll)

    return SequenceOutcome(
        sides, hitTN, woundTN, saveTN, critHitTN, critWoundTN, attacks, torrent,
        hitFinal, hitBase, hitCrits, extraHits, totalHitsAfterCrit, autoWounds,
        woundFinal, woundCrits, woundFromRoll, totalWounds,
        /* autoSavesFromWoundCrit = */ 0,
        saveRolls, saveFromRoll, totalSaves, unsaved
    )
}

/* ---------- Sequence Roll simulation (many runs) ---------- */

class SimDistributions(
    val hits: IntArray,
    val wounds: IntArray,
    val unsaved: IntArray,
    val sims: Long
)

/**
 * Exact port of simulateMany() from your old app.
 */
fun simulateSequenceMany(
    sims: Long,
    baseAttacks: Int,
    attackXd6: Boolean,
    sides: Int,
    torrent: Boolean,
    hitTN: Int,
    woundTN: Int,
    saveTN: Int,
    critHitTN: Int,
    critWoundTN: Int,
    sustainEnabled: Boolean,
    sustainX: Int,
    lethalHit: Boolean,
    devastatingWound: Boolean,
    rerollHitFails: Boolean,
    rerollHitOnes: Boolean,
    rerollWoundFails: Boolean,
    rerollWoundOnes: Boolean
): SimDistributions {
    val rng = Random(System.currentTimeMillis())

    fun bump(b: MutableList<Int>, idx: Int) {
        if (idx < 0) return
        while (b.size <= idx) b.add(0)
        b[idx] = b[idx] + 1
    }

    val hitsL = mutableListOf<Int>()
    val woundsL = mutableListOf<Int>()
    val unsavedL = mutableListOf<Int>()

    repeat(sims.toInt()) {
        val attacks = if (attackXd6) rollXd6Sum(baseAttacks, rng) else baseAttacks

        // HIT
        var hitsBase = 0
        var hitCrits = 0
        if (torrent) {
            hitsBase = attacks
        } else {
            repeat(attacks) {
                var r = rng.nextInt(1, sides + 1)
                val doRerollFail = rerollHitFails && r < hitTN
                val doRerollOne = !rerollHitFails && rerollHitOnes && r == 1
                if (doRerollFail || doRerollOne) r = rng.nextInt(1, sides + 1)
                if (r >= hitTN) hitsBase++
                if (r >= critHitTN) hitCrits++
            }
        }
        val extraHits = if (!torrent && sustainEnabled) hitCrits * sustainX else 0
        val hitsTotal = hitsBase + extraHits
        bump(hitsL, hitsTotal)

        // WOUND
        val autoWounds = if (!torrent && lethalHit) hitCrits else 0
        val woundDice = (hitsTotal - autoWounds).coerceAtLeast(0)
        var woundSucc = autoWounds
        var woundCrits = 0
        repeat(woundDice) {
            var r = rng.nextInt(1, sides + 1)
            val doRerollFail = rerollWoundFails && r < woundTN
            val doRerollOne = !rerollWoundFails && rerollWoundOnes && r == 1
            if (doRerollFail || doRerollOne) r = rng.nextInt(1, sides + 1)
            if (r >= woundTN) woundSucc++
            if (r >= critWoundTN) woundCrits++
        }
        bump(woundsL, woundSucc)

        // SAVE
        val unsavable = if (devastatingWound) woundCrits else 0
        val saveDice = (woundSucc - unsavable).coerceAtLeast(0)
        var saves = 0
        repeat(saveDice) {
            val r = rng.nextInt(1, sides + 1)
            if (r >= saveTN) saves++
        }
        val uns = unsavable + (saveDice - saves)
        bump(unsavedL, uns)
    }

    fun finalizeBuckets(src: MutableList<Int>): IntArray {
        var last = src.lastIndex
        while (last >= 0 && src[last] == 0) last--
        val want = (last + 3).coerceAtLeast(0)
        while (src.size <= want) src.add(0)
        return src.toIntArray()
    }

    return SimDistributions(
        hits = finalizeBuckets(hitsL),
        wounds = finalizeBuckets(woundsL),
        unsaved = finalizeBuckets(unsavedL),
        sims = sims
    )
}
