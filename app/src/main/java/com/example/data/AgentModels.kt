package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GameType(
    val id: String,
    val title: String,
    val subtitle: String,
    val genreTag: String,
    val rulesOverview: String
) {
    TACTICAL_GRID(
        id = "tactical_grid",
        title = "Tactical Grid",
        subtitle = "5x5 Territory & Hazard Control",
        genreTag = "TURN-BASED STRATEGY",
        rulesOverview = "Capture high-energy nodes, fortify adjacent sectors for +2x synergy, and detect/avoid redEMP traps while outscoring the adaptive Tactical Bot within 16 turns."
    ),
    NEURAL_2048(
        id = "neural_2048",
        title = "2048 Neural Matrix",
        subtitle = "4x4 Heuristic Tile Synthesis",
        genreTag = "COMBINATORIAL PUZZLE",
        rulesOverview = "Slide and merge matching power-of-two tiles across a 4x4 grid. Maintain corner-locking, row monotonicity, and open merge chains to reach the target milestone tile before the grid locks."
    ),
    CYBER_SNAKE(
        id = "cyber_snake",
        title = "Cyber Snake Arena",
        subtitle = "8x8 Autonomous Pathfinding & Survival",
        genreTag = "REAL-TIME NAVIGATION",
        rulesOverview = "Navigate an 8x8 cybernetic grid to harvest data cores while avoiding self-collision and dynamic firewall hazards that scale with difficulty."
    );

    companion object {
        fun fromId(id: String): GameType = entries.firstOrNull { it.id == id } ?: TACTICAL_GRID
    }
}

@IgnoreExtraProperties
data class GameProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameId: String = "tactical_grid",
    val difficultyLevel: Long = 1L,
    val autoAdjustDifficulty: Boolean = true,
    val targetWinRate: Long = 65L,
    val totalWins: Long = 0L,
    val totalLosses: Long = 0L,
    val recentOutcomes: List<String> = emptyList(),
    val bestScore: Long = 0L,
    val aggressionWeight: Long = 55L,
    val defenseWeight: Long = 60L,
    val lookaheadWeight: Long = 60L,
    val trapAwarenessWeight: Long = 65L,
    val lastAdjustmentReason: String = "Initial calibration at Tier 1 — awaiting match telemetry.",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val winRatePercent: Int
        get() {
            val total = totalWins + totalLosses
            return if (total <= 0L) 0 else ((totalWins.toDouble() / total.toDouble()) * 100.0).toInt()
        }

    val recentWinRatePercent: Int
        get() {
            if (recentOutcomes.isEmpty()) return winRatePercent
            val wins = recentOutcomes.count { it == "win" }
            return ((wins.toDouble() / recentOutcomes.size.toDouble()) * 100.0).toInt()
        }

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "gameId" to gameId,
        "difficultyLevel" to difficultyLevel.coerceIn(1L, 10L),
        "autoAdjustDifficulty" to autoAdjustDifficulty,
        "targetWinRate" to targetWinRate.coerceIn(30L, 90L),
        "totalWins" to totalWins.coerceAtLeast(0L),
        "totalLosses" to totalLosses.coerceAtLeast(0L),
        "recentOutcomes" to recentOutcomes.takeLast(10),
        "bestScore" to bestScore.coerceAtLeast(0L),
        "aggressionWeight" to aggressionWeight.coerceIn(0L, 100L),
        "defenseWeight" to defenseWeight.coerceIn(0L, 100L),
        "lookaheadWeight" to lookaheadWeight.coerceIn(0L, 100L),
        "trapAwarenessWeight" to trapAwarenessWeight.coerceIn(0L, 100L),
        "lastAdjustmentReason" to lastAdjustmentReason.take(500).ifBlank { "Calibrated." },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "difficultyLevel" to difficultyLevel.coerceIn(1L, 10L),
        "autoAdjustDifficulty" to autoAdjustDifficulty,
        "targetWinRate" to targetWinRate.coerceIn(30L, 90L),
        "totalWins" to totalWins.coerceAtLeast(0L),
        "totalLosses" to totalLosses.coerceAtLeast(0L),
        "recentOutcomes" to recentOutcomes.takeLast(10),
        "bestScore" to bestScore.coerceAtLeast(0L),
        "aggressionWeight" to aggressionWeight.coerceIn(0L, 100L),
        "defenseWeight" to defenseWeight.coerceIn(0L, 100L),
        "lookaheadWeight" to lookaheadWeight.coerceIn(0L, 100L),
        "trapAwarenessWeight" to trapAwarenessWeight.coerceIn(0L, 100L),
        "lastAdjustmentReason" to lastAdjustmentReason.take(500).ifBlank { "Calibrated." },
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ResearchDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameId: String = "tactical_grid",
    val sourceTitle: String = "",
    val sourceType: String = "wiki",
    val rawContent: String = "",
    val synthesizedSummary: String = "",
    val keyObjectives: List<String> = emptyList(),
    val tacticalRules: List<String> = emptyList(),
    val aggressionBoost: Long = 0L,
    val defenseBoost: Long = 0L,
    val lookaheadBoost: Long = 0L,
    val trapAwarenessBoost: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "gameId" to gameId,
        "sourceTitle" to sourceTitle.take(150).ifBlank { "Strategy Dossier" },
        "sourceType" to sourceType.ifBlank { "wiki" },
        "rawContent" to rawContent.take(5000).ifBlank { "Game strategy notes." },
        "synthesizedSummary" to synthesizedSummary.take(2000).ifBlank { "Synthesized game heuristics." },
        "keyObjectives" to (keyObjectives.map { it.take(300) }.take(10).ifEmpty { listOf("Maximize board utility") }),
        "tacticalRules" to (tacticalRules.map { it.take(300) }.take(10).ifEmpty { listOf("Prioritize safe high-reward moves") }),
        "aggressionBoost" to aggressionBoost.coerceIn(-50L, 50L),
        "defenseBoost" to defenseBoost.coerceIn(-50L, 50L),
        "lookaheadBoost" to lookaheadBoost.coerceIn(-50L, 50L),
        "trapAwarenessBoost" to trapAwarenessBoost.coerceIn(-50L, 50L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class StateMemory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameId: String = "tactical_grid",
    val stateSignature: String = "",
    val stateDescription: String = "",
    val actionTaken: String = "",
    val scoreDelta: Long = 0L,
    val outcome: String = "optimal", // "win", "loss", "mistake", "optimal"
    val qValueScore: Long = 0L, // -100 to 100
    val lessonLearned: String = "",
    val difficultyLevel: Long = 1L,
    val timesRecalled: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "gameId" to gameId,
        "stateSignature" to stateSignature.take(200).ifBlank { "STATE_DEFAULT" },
        "stateDescription" to stateDescription.take(500).ifBlank { "Game board state snapshot" },
        "actionTaken" to actionTaken.take(100).ifBlank { "MOVE" },
        "scoreDelta" to scoreDelta.coerceIn(-100000L, 100000L),
        "outcome" to if (outcome in listOf("win", "loss", "mistake", "optimal")) outcome else "optimal",
        "qValueScore" to qValueScore.coerceIn(-100L, 100L),
        "lessonLearned" to lessonLearned.take(1000).ifBlank { "Observed state-action outcome." },
        "difficultyLevel" to difficultyLevel.coerceIn(1L, 10L),
        "timesRecalled" to timesRecalled.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "scoreDelta" to scoreDelta.coerceIn(-100000L, 100000L),
        "outcome" to if (outcome in listOf("win", "loss", "mistake", "optimal")) outcome else "optimal",
        "qValueScore" to qValueScore.coerceIn(-100L, 100L),
        "lessonLearned" to lessonLearned.take(1000).ifBlank { "Observed state-action outcome." },
        "difficultyLevel" to difficultyLevel.coerceIn(1L, 10L),
        "timesRecalled" to timesRecalled.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class MatchEpisode(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameId: String = "tactical_grid",
    val difficultyLevel: Long = 1L,
    val outcome: String = "win", // "win" or "loss"
    val finalScore: Long = 0L,
    val movesCount: Long = 0L,
    val memoriesRecalled: Long = 0L,
    val mistakesAvoided: Long = 0L,
    val mistakesCommitted: Long = 0L,
    val difficultyDelta: Long = 0L,
    val aiReflection: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "gameId" to gameId,
        "difficultyLevel" to difficultyLevel.coerceIn(1L, 10L),
        "outcome" to if (outcome == "loss") "loss" else "win",
        "finalScore" to finalScore.coerceAtLeast(0L),
        "movesCount" to movesCount.coerceAtLeast(0L),
        "memoriesRecalled" to memoriesRecalled.coerceAtLeast(0L),
        "mistakesAvoided" to mistakesAvoided.coerceAtLeast(0L),
        "mistakesCommitted" to mistakesCommitted.coerceAtLeast(0L),
        "difficultyDelta" to difficultyDelta.coerceIn(-2L, 2L),
        "aiReflection" to aiReflection.take(1500).ifBlank { "Match episode analyzed." },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
