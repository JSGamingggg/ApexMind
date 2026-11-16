package com.example.engine

import com.example.data.GameProfile
import com.example.data.GameType

data class DifficultyParameters(
    val level: Int,
    val tierName: String,
    // Tactical Grid parameters
    val tacticalTrapCount: Int,
    val tacticalBotLookahead: Int,
    val tacticalTargetLead: Int,
    // 2048 parameters
    val targetTile2048: Int,
    val fourTileSpawnProbPercent: Int,
    // Cyber Snake parameters
    val snakeFirewallCount: Int,
    val snakeTargetCores: Int
)

data class DifficultyCalibrationResult(
    val newLevel: Int,
    val delta: Int,
    val reason: String,
    val updatedRecentOutcomes: List<String>
)

object AdaptiveDifficultyEngine {

    fun getParametersForLevel(level: Int): DifficultyParameters {
        val clamped = level.coerceIn(1, 10)
        val tierName = when (clamped) {
            1 -> "Tier 1 • Novice Sandbox"
            2 -> "Tier 2 • Cadet Calibration"
            3 -> "Tier 3 • Tactical Contender"
            4 -> "Tier 4 • Heuristic Specialist"
            5 -> "Tier 5 • Adaptive Veteran"
            6 -> "Tier 6 • Neural Strategist"
            7 -> "Tier 7 • Master Matrix"
            8 -> "Tier 8 • Apex Challenger"
            9 -> "Tier 9 • Grandmaster Crucible"
            else -> "Tier 10 • Singularity Overdrive"
        }

        val targetTile = when (clamped) {
            1, 2 -> 128
            3, 4 -> 256
            5, 6 -> 512
            7, 8 -> 1024
            else -> 2048
        }

        return DifficultyParameters(
            level = clamped,
            tierName = tierName,
            tacticalTrapCount = (2 + (clamped * 3) / 4).coerceIn(2, 9),
            tacticalBotLookahead = ((clamped + 1) / 2).coerceIn(1, 5),
            tacticalTargetLead = (10 + clamped * 6),
            targetTile2048 = targetTile,
            fourTileSpawnProbPercent = (10 + clamped * 3).coerceIn(10, 40),
            snakeFirewallCount = (2 + clamped).coerceIn(3, 12),
            snakeTargetCores = (5 + clamped * 2).coerceIn(7, 25)
        )
    }

    /**
     * Evaluates the AI's rolling win/loss ratio and match performance metrics to dynamically
     * adjust the game's difficulty level, keeping the challenge inside the optimal learning zone.
     */
    fun evaluatePostMatchAdjustment(
        profile: GameProfile,
        outcome: String, // "win" or "loss"
        mistakesCommitted: Int,
        memoriesRecalled: Int
    ): DifficultyCalibrationResult {
        val currentLevel = profile.difficultyLevel.toInt().coerceIn(1, 10)
        val updatedOutcomes = (profile.recentOutcomes + outcome).takeLast(10)
        val windowSize = updatedOutcomes.size
        val winsInWindow = updatedOutcomes.count { it == "win" }
        val rollingWinRate = if (windowSize > 0) {
            ((winsInWindow.toDouble() / windowSize.toDouble()) * 100.0).toInt()
        } else {
            50
        }

        if (!profile.autoAdjustDifficulty) {
            return DifficultyCalibrationResult(
                newLevel = currentLevel,
                delta = 0,
                reason = "Auto-Adaptive Difficulty paused (Manual Tier $currentLevel). Rolling win rate: $rollingWinRate% ($winsInWindow/$windowSize).",
                updatedRecentOutcomes = updatedOutcomes
            )
        }

        val targetRate = profile.targetWinRate.toInt().coerceIn(30, 90)
        val upperThreshold = (targetRate + 10).coerceAtMost(95)
        val lowerThreshold = (targetRate - 15).coerceAtLeast(20)

        // Require at least 2 matches in the window before shifting difficulty, unless a flawless win or heavy mistake loss
        var delta = 0
        val reason: String

        if (windowSize >= 2 && rollingWinRate >= upperThreshold && currentLevel < 10) {
            delta = 1
            val nextLevel = (currentLevel + 1).coerceAtMost(10)
            reason = "Promoted Tier $currentLevel → Tier $nextLevel: Rolling win rate ($rollingWinRate% over $windowSize matches) exceeded optimal upper bound ($upperThreshold%). Increasing challenge parameters."
        } else if (outcome == "win" && mistakesCommitted == 0 && memoriesRecalled >= 1 && currentLevel < 10 && rollingWinRate >= targetRate) {
            delta = 1
            val nextLevel = (currentLevel + 1).coerceAtMost(10)
            reason = "Promoted Tier $currentLevel → Tier $nextLevel: Flawless zero-mistake victory with active memory recall at $rollingWinRate% win rate."
        } else if (windowSize >= 2 && rollingWinRate <= lowerThreshold && currentLevel > 1) {
            delta = -1
            val nextLevel = (currentLevel - 1).coerceAtLeast(1)
            reason = "Eased Tier $currentLevel → Tier $nextLevel: Rolling win rate ($rollingWinRate% over $windowSize matches) dropped below learning floor ($lowerThreshold%). Consolidating fundamentals."
        } else if (outcome == "loss" && mistakesCommitted >= 3 && currentLevel > 1 && rollingWinRate < targetRate - 5) {
            delta = -1
            val nextLevel = (currentLevel - 1).coerceAtLeast(1)
            reason = "Eased Tier $currentLevel → Tier $nextLevel: High mistake density ($mistakesCommitted mistakes) detected; stepping down difficulty for targeted mistake recovery."
        } else {
            reason = "Maintained Tier $currentLevel (Sweet Spot): Rolling win rate $rollingWinRate% ($winsInWindow/$windowSize) is within optimal learning band ($lowerThreshold%–$upperThreshold%)."
        }

        val finalLevel = (currentLevel + delta).coerceIn(1, 10)
        return DifficultyCalibrationResult(
            newLevel = finalLevel,
            delta = finalLevel - currentLevel,
            reason = reason,
            updatedRecentOutcomes = updatedOutcomes
        )
    }
}
