package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiStrategyService
import com.example.ai.LiveTacticalAdvice
import com.example.data.AgentMemoryRepository
import com.example.data.GameProfile
import com.example.data.GameType
import com.example.data.MatchEpisode
import com.example.data.ResearchDoc
import com.example.data.StateMemory
import com.example.engine.AdaptiveDifficultyEngine
import com.example.engine.AiDecisionTelemetry
import com.example.engine.CyberSnakeEngine
import com.example.engine.CyberSnakeState
import com.example.engine.Matrix2048State
import com.example.engine.Neural2048Engine
import com.example.engine.TacticalGridEngine
import com.example.engine.TacticalGridState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val TAG = "ApexMindVM"

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data class Running(val label: String) : AiTaskState
    data class Success(val message: String) : AiTaskState
    data class Error(val errorMessage: String) : AiTaskState
}

sealed interface ResearchModalState {
    data object Idle : ResearchModalState
    data class Fetching(val statusMessage: String) : ResearchModalState
    data class Success(
        val sourceTitle: String,
        val summary: String,
        val extractedObjectives: List<String>,
        val fetchedFromWeb: Boolean
    ) : ResearchModalState
    data class Error(val message: String) : ResearchModalState
}

class ApexMindViewModel(
    private val repository: AgentMemoryRepository,
    private val aiService: AiStrategyService = AiStrategyService(),
    val currentUserId: String
) : ViewModel() {

    val gameProfiles: StateFlow<List<GameProfile>> = repository.observeGameProfiles()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing gameProfiles", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val researchDocs: StateFlow<List<ResearchDoc>> = repository.observeResearchDocs()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing researchDocs", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val stateMemories: StateFlow<List<StateMemory>> = repository.observeStateMemories()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing stateMemories", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val matchEpisodes: StateFlow<List<MatchEpisode>> = repository.observeMatchEpisodes()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing matchEpisodes", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedGame = MutableStateFlow(GameType.TACTICAL_GRID)
    val selectedGame: StateFlow<GameType> = _selectedGame.asStateFlow()

    private val _tacticalGridState = MutableStateFlow(TacticalGridEngine.createInitialState(1))
    val tacticalGridState: StateFlow<TacticalGridState> = _tacticalGridState.asStateFlow()

    private val _matrix2048State = MutableStateFlow(Neural2048Engine.createInitialState(1))
    val matrix2048State: StateFlow<Matrix2048State> = _matrix2048State.asStateFlow()

    private val _cyberSnakeState = MutableStateFlow(CyberSnakeEngine.createInitialState(1))
    val cyberSnakeState: StateFlow<CyberSnakeState> = _cyberSnakeState.asStateFlow()

    private val _isAutoPlaying = MutableStateFlow(false)
    val isAutoPlaying: StateFlow<Boolean> = _isAutoPlaying.asStateFlow()

    private val _playbackSpeedMultiplier = MutableStateFlow(1) // 1x, 2x, 4x
    val playbackSpeedMultiplier: StateFlow<Int> = _playbackSpeedMultiplier.asStateFlow()

    private val _decisionLog = MutableStateFlow<List<AiDecisionTelemetry>>(emptyList())
    val decisionLog: StateFlow<List<AiDecisionTelemetry>> = _decisionLog.asStateFlow()

    private val _liveAdvice = MutableStateFlow<LiveTacticalAdvice?>(null)
    val liveAdvice: StateFlow<LiveTacticalAdvice?> = _liveAdvice.asStateFlow()

    private val _aiTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val aiTaskState: StateFlow<AiTaskState> = _aiTaskState.asStateFlow()

    private val _researchModalState = MutableStateFlow<ResearchModalState>(ResearchModalState.Idle)
    val researchModalState: StateFlow<ResearchModalState> = _researchModalState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private var autoPlayJob: Job? = null
    private var hasSeededInitialProfiles = false

    // Session metrics for current match
    private var currentMatchMemoriesRecalled = 0
    private var currentMatchMistakesAvoided = 0
    private var currentMatchMistakesCommitted = 0
    private var criticalTurnSnapshot: AiDecisionTelemetry? = null

    init {
        seedInitialDataIfNeeded()
    }

    private fun seedInitialDataIfNeeded() {
        viewModelScope.launch {
            delay(800L)
            if (hasSeededInitialProfiles) return@launch
            hasSeededInitialProfiles = true
            val currentProfiles = gameProfiles.value
            if (currentProfiles.isEmpty()) {
                GameType.entries.forEach { game ->
                    val defaultProfile = GameProfile(
                        userId = currentUserId,
                        gameId = game.id,
                        difficultyLevel = 2L,
                        autoAdjustDifficulty = true,
                        targetWinRate = 65L,
                        totalWins = 1L,
                        totalLosses = 1L,
                        recentOutcomes = listOf("loss", "win"),
                        bestScore = 120L,
                        aggressionWeight = 58L,
                        defenseWeight = 64L,
                        lookaheadWeight = 66L,
                        trapAwarenessWeight = 70L,
                        lastAdjustmentReason = "Calibrated at Tier 2 (50% baseline win/loss ratio)."
                    )
                    repository.saveOrUpdateGameProfile(defaultProfile)
                }

                // Seed 3 starter state-action memories so the user can immediately see Memory Hits and Mistake Avoidance in action
                val starterMemories = listOf(
                    StateMemory(
                        userId = currentUserId,
                        gameId = GameType.TACTICAL_GRID.id,
                        stateSignature = "TG_OPENING_C_NONE_HIGH_2_2_AI0_BOT0",
                        stateDescription = "Opening Turn 1: Center sector (2,2) uncaptured with high adjacency synergy potential",
                        actionTaken = "SECTOR_2_2",
                        scoreDelta = 28L,
                        outcome = "optimal",
                        qValueScore = 85L,
                        lessonLearned = "Always anchor center (2,2) on Turn 1 to unlock +12 synergy across all 4 cardinal directions.",
                        difficultyLevel = 2L,
                        timesRecalled = 3L
                    ),
                    StateMemory(
                        userId = currentUserId,
                        gameId = GameType.NEURAL_2048.id,
                        stateSignature = "N2048_TL_CORNER_MAX64_OPEN_R0_OPEN",
                        stateDescription = "Max Tile 64 in top-left corner, top row not locked",
                        actionTaken = "DOWN",
                        scoreDelta = -20L,
                        outcome = "mistake",
                        qValueScore = -78L,
                        lessonLearned = "Never slide DOWN when top-left anchor row is unlocked—spawns a 2-tile in (0,0) and dislodges the max tile.",
                        difficultyLevel = 2L,
                        timesRecalled = 4L
                    ),
                    StateMemory(
                        userId = currentUserId,
                        gameId = GameType.CYBER_SNAKE.id,
                        stateSignature = "SNAKE_CORE_NE_U1D0L1R1",
                        stateDescription = "Head approaching NE Data Core with southern wall blocked",
                        actionTaken = "RIGHT",
                        scoreDelta = 35L,
                        outcome = "win",
                        qValueScore = 82L,
                        lessonLearned = "Approach NE core horizontally first to preserve vertical escape lanes toward center grid.",
                        difficultyLevel = 2L,
                        timesRecalled = 2L
                    )
                )
                starterMemories.forEach { repository.upsertStateMemory(it) }
            }
        }
    }

    fun getProfileForGame(game: GameType): GameProfile {
        return gameProfiles.value.firstOrNull { it.gameId == game.id } ?: GameProfile(
            userId = currentUserId,
            gameId = game.id,
            difficultyLevel = 2L
        )
    }

    fun selectGame(game: GameType) {
        pauseAutoPlay()
        _selectedGame.value = game
        _liveAdvice.value = null
        resetCurrentMatch()
    }

    fun setPlaybackSpeed(multiplier: Int) {
        _playbackSpeedMultiplier.value = multiplier.coerceIn(1, 4)
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    fun clearAiTaskState() {
        _aiTaskState.value = AiTaskState.Idle
    }

    fun resetCurrentMatch() {
        pauseAutoPlay()
        val game = _selectedGame.value
        val level = getProfileForGame(game).difficultyLevel.toInt().coerceIn(1, 10)
        currentMatchMemoriesRecalled = 0
        currentMatchMistakesAvoided = 0
        currentMatchMistakesCommitted = 0
        criticalTurnSnapshot = null
        _decisionLog.value = emptyList()
        _liveAdvice.value = null

        when (game) {
            GameType.TACTICAL_GRID -> _tacticalGridState.value = TacticalGridEngine.createInitialState(level)
            GameType.NEURAL_2048 -> _matrix2048State.value = Neural2048Engine.createInitialState(level)
            GameType.CYBER_SNAKE -> _cyberSnakeState.value = CyberSnakeEngine.createInitialState(level)
        }
    }

    fun toggleAutoPlay() {
        if (_isAutoPlaying.value) {
            pauseAutoPlay()
        } else {
            startAutoPlay()
        }
    }

    private fun pauseAutoPlay() {
        _isAutoPlaying.value = false
        autoPlayJob?.cancel()
        autoPlayJob = null
    }

    private fun startAutoPlay() {
        if (isCurrentGameOver()) {
            resetCurrentMatch()
        }
        _isAutoPlaying.value = true
        autoPlayJob?.cancel()
        autoPlayJob = viewModelScope.launch {
            while (_isAutoPlaying.value && !isCurrentGameOver()) {
                executeSingleAiStep()
                val delayMs = when (_playbackSpeedMultiplier.value) {
                    4 -> 220L
                    2 -> 450L
                    else -> 850L
                }
                delay(delayMs)
            }
            _isAutoPlaying.value = false
        }
    }

    private fun isCurrentGameOver(): Boolean = when (_selectedGame.value) {
        GameType.TACTICAL_GRID -> _tacticalGridState.value.isGameOver
        GameType.NEURAL_2048 -> _matrix2048State.value.isGameOver
        GameType.CYBER_SNAKE -> _cyberSnakeState.value.isGameOver
    }

    fun stepSingleTurn(forcedAction: String? = null) {
        pauseAutoPlay()
        viewModelScope.launch {
            if (isCurrentGameOver()) {
                resetCurrentMatch()
            } else {
                executeSingleAiStep(forcedAction)
            }
        }
    }

    private suspend fun executeSingleAiStep(forcedAction: String? = null) {
        val game = _selectedGame.value
        val profile = getProfileForGame(game)
        val memories = stateMemories.value
        val docs = researchDocs.value

        val telemetry: AiDecisionTelemetry = when (game) {
            GameType.TACTICAL_GRID -> {
                val (nextState, stepTelemetry) = TacticalGridEngine.stepAiTurn(
                    state = _tacticalGridState.value,
                    profile = profile,
                    memories = memories,
                    researchDocs = docs,
                    forcedAction = forcedAction
                )
                _tacticalGridState.value = nextState
                stepTelemetry
            }
            GameType.NEURAL_2048 -> {
                val (nextState, stepTelemetry) = Neural2048Engine.stepAiTurn(
                    state = _matrix2048State.value,
                    profile = profile,
                    memories = memories,
                    researchDocs = docs,
                    forcedAction = forcedAction
                )
                _matrix2048State.value = nextState
                stepTelemetry
            }
            GameType.CYBER_SNAKE -> {
                val (nextState, stepTelemetry) = CyberSnakeEngine.stepAiTurn(
                    state = _cyberSnakeState.value,
                    profile = profile,
                    memories = memories,
                    researchDocs = docs,
                    forcedAction = forcedAction
                )
                _cyberSnakeState.value = nextState
                stepTelemetry
            }
        }

        if (telemetry.chosenAction == "NONE") return

        _decisionLog.value = (listOf(telemetry) + _decisionLog.value).take(30)

        // Track memory recall & mistake stats
        if (telemetry.recalledMemory != null) {
            currentMatchMemoriesRecalled++
            val mem = telemetry.recalledMemory
            repository.upsertStateMemory(mem.copy(timesRecalled = mem.timesRecalled + 1L))
        }
        if (telemetry.avoidedMistakeMemory != null) {
            currentMatchMistakesAvoided++
            val mem = telemetry.avoidedMistakeMemory
            repository.upsertStateMemory(mem.copy(timesRecalled = mem.timesRecalled + 1L))
        }
        if (telemetry.wasMistakeCommitted) {
            currentMatchMistakesCommitted++
            criticalTurnSnapshot = telemetry
            // Immediately record the mistake in State-Action Memory so the AI never repeats it!
            val mistakeMem = StateMemory(
                userId = currentUserId,
                gameId = game.id,
                stateSignature = telemetry.stateSignature,
                stateDescription = telemetry.stateDescription,
                actionTaken = telemetry.chosenAction,
                scoreDelta = telemetry.immediateScoreDelta.toLong(),
                outcome = "mistake",
                qValueScore = -75L,
                lessonLearned = "Avoid ${telemetry.chosenAction} in [${telemetry.stateSignature}]: caused ${telemetry.immediateScoreDelta} pt penalty/hazard.",
                difficultyLevel = profile.difficultyLevel,
                timesRecalled = 0L
            )
            repository.upsertStateMemory(mistakeMem)
        } else if (criticalTurnSnapshot == null || abs(telemetry.immediateScoreDelta) > abs(criticalTurnSnapshot?.immediateScoreDelta ?: 0)) {
            criticalTurnSnapshot = telemetry
        }

        if (isCurrentGameOver()) {
            finalizeCompletedMatch()
        }
    }

    private suspend fun finalizeCompletedMatch() {
        val game = _selectedGame.value
        val profile = getProfileForGame(game)

        val (isWin, finalScore, movesCount) = when (game) {
            GameType.TACTICAL_GRID -> {
                val st = _tacticalGridState.value
                Triple(st.aiScore >= st.botScore, st.aiScore, st.turn)
            }
            GameType.NEURAL_2048 -> {
                val st = _matrix2048State.value
                Triple(st.isWin || st.maxTile >= st.targetTile, st.score, st.turn)
            }
            GameType.CYBER_SNAKE -> {
                val st = _cyberSnakeState.value
                Triple(st.isWin, st.score, st.turn)
            }
        }

        val outcomeStr = if (isWin) "win" else "loss"
        val calibration = AdaptiveDifficultyEngine.evaluatePostMatchAdjustment(
            profile = profile,
            outcome = outcomeStr,
            mistakesCommitted = currentMatchMistakesCommitted,
            memoriesRecalled = currentMatchMemoriesRecalled + currentMatchMistakesAvoided
        )

        val turnSnap = criticalTurnSnapshot ?: _decisionLog.value.firstOrNull()
        if (turnSnap != null && !turnSnap.wasMistakeCommitted && isWin) {
            // Retain winning key state-action memory
            val existingMem = stateMemories.value.firstOrNull {
                it.gameId == game.id && it.stateSignature == turnSnap.stateSignature && it.actionTaken == turnSnap.chosenAction
            }
            val winMem = StateMemory(
                id = existingMem?.id ?: "",
                userId = currentUserId,
                gameId = game.id,
                stateSignature = turnSnap.stateSignature,
                stateDescription = turnSnap.stateDescription,
                actionTaken = turnSnap.chosenAction,
                scoreDelta = turnSnap.immediateScoreDelta.toLong(),
                outcome = "win",
                qValueScore = ((existingMem?.qValueScore ?: 65L) + 12L).coerceIn(40L, 98L),
                lessonLearned = "Action ${turnSnap.chosenAction} in [${turnSnap.stateSignature}] yielded +${turnSnap.immediateScoreDelta} pts and contributed to Tier ${profile.difficultyLevel} victory.",
                difficultyLevel = profile.difficultyLevel,
                timesRecalled = existingMem?.timesRecalled ?: 0L
            )
            repository.upsertStateMemory(winMem)
        }

        // Update GameProfile with new wins/losses, heuristic refinement, and adaptive difficulty level
        val updatedProfile = profile.copy(
            difficultyLevel = calibration.newLevel.toLong(),
            totalWins = if (isWin) profile.totalWins + 1L else profile.totalWins,
            totalLosses = if (!isWin) profile.totalLosses + 1L else profile.totalLosses,
            recentOutcomes = calibration.updatedRecentOutcomes,
            bestScore = maxOf(profile.bestScore, finalScore.toLong()),
            trapAwarenessWeight = (profile.trapAwarenessWeight + if (currentMatchMistakesCommitted > 0) 4L else 1L).coerceIn(0L, 100L),
            lookaheadWeight = (profile.lookaheadWeight + 2L).coerceIn(0L, 100L),
            lastAdjustmentReason = calibration.reason
        )
        repository.saveOrUpdateGameProfile(updatedProfile)

        val fallbackReflection = buildString {
            append(if (isWin) "VICTORY" else "DEFEAT")
            append(" at Tier ${profile.difficultyLevel} ($finalScore pts in $movesCount turns). ")
            append("Recalled $currentMatchMemoriesRecalled state memories, avoided $currentMatchMistakesAvoided past mistakes, committed $currentMatchMistakesCommitted new mistakes. ")
            append(calibration.reason)
        }

        val episode = MatchEpisode(
            userId = currentUserId,
            gameId = game.id,
            difficultyLevel = profile.difficultyLevel,
            outcome = outcomeStr,
            finalScore = finalScore.toLong(),
            movesCount = movesCount.toLong(),
            memoriesRecalled = currentMatchMemoriesRecalled.toLong(),
            mistakesAvoided = currentMatchMistakesAvoided.toLong(),
            mistakesCommitted = currentMatchMistakesCommitted.toLong(),
            difficultyDelta = calibration.delta.toLong(),
            aiReflection = fallbackReflection
        )
        repository.addMatchEpisode(episode)
        _statusBanner.value = fallbackReflection
    }

    /**
     * Invokes Gemini 3.8 Flash to conduct pre-game research from a wiki, strategy guide, or video transcript,
     * saving the ResearchDoc and permanently boosting the AI's heuristic weights and state memory bank.
     */
    fun runPreGameResearch(
        game: GameType,
        sourceTitle: String,
        sourceType: String,
        rawContent: String
    ) {
        if (rawContent.isBlank()) {
            _aiTaskState.value = AiTaskState.Error("Please select a research source or paste strategy text.")
            return
        }
        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Running("Synthesizing pre-game research via Gemini...")
            val profile = getProfileForGame(game)
            val result = aiService.synthesizePreGameResearch(
                game = game,
                sourceTitle = sourceTitle.ifBlank { "${game.title} Strategy Guide" },
                sourceType = sourceType,
                rawContent = rawContent,
                currentProfile = profile
            )

            result.fold(
                onSuccess = { synthesis ->
                    persistResearchSynthesis(
                        game = game,
                        sourceTitle = sourceTitle.ifBlank { "${game.title} Strategy Briefing" }.take(150),
                        sourceType = sourceType,
                        rawContent = rawContent.take(4900),
                        synthesis = synthesis,
                        profile = profile
                    )
                    _aiTaskState.value = AiTaskState.Success(
                        "Research complete! Extracted ${synthesis.tacticalRules.size} tactical rules and updated ${game.title} AI weights."
                    )
                },
                onFailure = { err ->
                    _aiTaskState.value = AiTaskState.Error(err.message ?: "Research synthesis failed.")
                }
            )
        }
    }

    /**
     * Triggered from the Research Lab modal inside GameStrategyDashboard:
     * fetches strategy material from an external source for the user-entered game title,
     * extracts strategic objectives via Gemini 3.8 Flash, and persists them to Firestore.
     */
    fun fetchExternalStrategyViaGemini(
        game: GameType,
        gameTitle: String,
        externalSource: String,
        sourceType: String
    ) {
        val trimmedTitle = gameTitle.trim()
        if (trimmedTitle.isBlank()) {
            _researchModalState.value = ResearchModalState.Error("Please enter a game title to research.")
            return
        }
        viewModelScope.launch {
            _researchModalState.value = ResearchModalState.Fetching(
                "Fetching external strategy for $trimmedTitle via Gemini..."
            )
            val profile = getProfileForGame(game)
            val result = aiService.fetchAndSynthesizeGameTitleStrategy(
                targetGame = game,
                gameTitleInput = trimmedTitle,
                externalSourceInput = externalSource,
                sourceType = sourceType,
                currentProfile = profile
            )

            result.fold(
                onSuccess = { fetched ->
                    persistResearchSynthesis(
                        game = game,
                        sourceTitle = fetched.resolvedSourceTitle,
                        sourceType = fetched.resolvedSourceType,
                        rawContent = fetched.fetchedExcerpt,
                        synthesis = fetched.synthesis,
                        profile = profile
                    )
                    _researchModalState.value = ResearchModalState.Success(
                        sourceTitle = fetched.resolvedSourceTitle,
                        summary = fetched.synthesis.synthesizedSummary,
                        extractedObjectives = fetched.synthesis.keyObjectives,
                        fetchedFromWeb = fetched.fetchedFromWeb
                    )
                },
                onFailure = { err ->
                    _researchModalState.value = ResearchModalState.Error(
                        err.message ?: "Failed to fetch strategy objectives via Gemini."
                    )
                }
            )
        }
    }

    fun resetResearchModalState() {
        _researchModalState.value = ResearchModalState.Idle
    }

    private suspend fun persistResearchSynthesis(
        game: GameType,
        sourceTitle: String,
        sourceType: String,
        rawContent: String,
        synthesis: com.example.ai.ResearchSynthesisResult,
        profile: GameProfile
    ) {
        val doc = ResearchDoc(
            userId = currentUserId,
            gameId = game.id,
            sourceTitle = sourceTitle,
            sourceType = sourceType,
            rawContent = rawContent,
            synthesizedSummary = synthesis.synthesizedSummary,
            keyObjectives = synthesis.keyObjectives,
            tacticalRules = synthesis.tacticalRules,
            aggressionBoost = synthesis.aggressionBoost.toLong(),
            defenseBoost = synthesis.defenseBoost.toLong(),
            lookaheadBoost = synthesis.lookaheadBoost.toLong(),
            trapAwarenessBoost = synthesis.trapAwarenessBoost.toLong()
        )
        repository.addResearchDoc(doc)

        // Apply learned heuristic boosts to the AI's GameProfile
        val updatedProfile = profile.copy(
            aggressionWeight = (profile.aggressionWeight + synthesis.aggressionBoost).coerceIn(15L, 100L),
            defenseWeight = (profile.defenseWeight + synthesis.defenseBoost).coerceIn(15L, 100L),
            lookaheadWeight = (profile.lookaheadWeight + synthesis.lookaheadBoost).coerceIn(15L, 100L),
            trapAwarenessWeight = (profile.trapAwarenessWeight + synthesis.trapAwarenessBoost).coerceIn(15L, 100L),
            lastAdjustmentReason = "Pre-game research applied from '${doc.sourceTitle}': updated neural heuristic weights."
        )
        repository.saveOrUpdateGameProfile(updatedProfile)

        // Also seed a proactive StateMemory rule from the top tactical heuristic
        val topRule = synthesis.tacticalRules.firstOrNull()
        if (topRule != null) {
            val ruleSig = when (game) {
                GameType.TACTICAL_GRID -> "TG_RESEARCH_PROACTIVE_RULE"
                GameType.NEURAL_2048 -> "N2048_RESEARCH_PROACTIVE_RULE"
                GameType.CYBER_SNAKE -> "SNAKE_RESEARCH_PROACTIVE_RULE"
            }
            repository.upsertStateMemory(
                StateMemory(
                    userId = currentUserId,
                    gameId = game.id,
                    stateSignature = ruleSig,
                    stateDescription = "Pre-Game Research Rule (${doc.sourceTitle})",
                    actionTaken = "APPLY_RESEARCH_HEURISTIC",
                    scoreDelta = 30L,
                    outcome = "optimal",
                    qValueScore = 88L,
                    lessonLearned = topRule,
                    difficultyLevel = profile.difficultyLevel,
                    timesRecalled = 1L
                )
            )
        }
    }

    /**
     * Calls Gemini 3.8 Flash during a live match to analyze the exact board state,
     * active research rules, and episodic memories, then optionally executes the recommended move.
     */
    fun consultGeminiOnCurrentBoard(executeMoveImmediately: Boolean = false) {
        pauseAutoPlay()
        viewModelScope.launch {
            val game = _selectedGame.value
            val profile = getProfileForGame(game)
            _aiTaskState.value = AiTaskState.Running("Consulting Gemini on live ${game.title} board state...")

            val (sig, desc, validActions) = when (game) {
                GameType.TACTICAL_GRID -> {
                    val st = _tacticalGridState.value
                    val (s, d) = TacticalGridEngine.encodeStateSignature(st)
                    val actions = st.cells.filter { it.owner == com.example.engine.CellOwner.NONE }
                        .map { "SECTOR_${it.row}_${it.col}" }
                    Triple(s, d, actions)
                }
                GameType.NEURAL_2048 -> {
                    val st = _matrix2048State.value
                    val (s, d) = Neural2048Engine.encodeStateSignature(st.board)
                    val actions = Neural2048Engine.DIRECTIONS.filter { dir ->
                        Neural2048Engine.simulateSlide(st.board, dir).first != st.board
                    }
                    Triple(s, d, actions)
                }
                GameType.CYBER_SNAKE -> {
                    val st = _cyberSnakeState.value
                    val (s, d) = CyberSnakeEngine.encodeStateSignature(st)
                    Triple(s, d, listOf("UP", "DOWN", "LEFT", "RIGHT"))
                }
            }

            if (validActions.isEmpty()) {
                _aiTaskState.value = AiTaskState.Error("Match is already finished. Reset match to consult.")
                return@launch
            }

            val gameMemories = stateMemories.value.filter { it.gameId == game.id }
            val gameResearch = researchDocs.value.filter { it.gameId == game.id }

            val consultResult = aiService.consultOnLiveBoardState(
                game = game,
                stateSignature = sig,
                stateDescription = desc,
                validActions = validActions,
                matchedMemories = gameMemories,
                recentResearch = gameResearch,
                profile = profile
            )

            consultResult.fold(
                onSuccess = { advice ->
                    _liveAdvice.value = advice
                    _aiTaskState.value = AiTaskState.Success(
                        "Gemini recommends ${advice.recommendedAction} (${advice.confidencePercent}% confidence)."
                    )
                    if (executeMoveImmediately) {
                        executeSingleAiStep(forcedAction = advice.recommendedAction)
                    }
                },
                onFailure = { err ->
                    _aiTaskState.value = AiTaskState.Error(err.message ?: "Live board consultation failed.")
                }
            )
        }
    }

    /**
     * Runs a deep Gemini Post-Match Autopsy on the most recent critical state transition
     * and stores the permanent lesson in Firestore.
     */
    fun runDeepAiAutopsyOnLastMatch() {
        val game = _selectedGame.value
        val profile = getProfileForGame(game)
        val snap = criticalTurnSnapshot ?: _decisionLog.value.firstOrNull()
        if (snap == null) {
            _aiTaskState.value = AiTaskState.Error("Play at least one turn first so the AI has state telemetry to analyze.")
            return
        }

        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Running("Running Gemini deep mistake & strategy autopsy...")
            val outcome = if (snap.wasMistakeCommitted || snap.immediateScoreDelta < 0) "loss" else "win"
            val score = when (game) {
                GameType.TACTICAL_GRID -> _tacticalGridState.value.aiScore
                GameType.NEURAL_2048 -> _matrix2048State.value.score
                GameType.CYBER_SNAKE -> _cyberSnakeState.value.score
            }

            val result = aiService.runPostMatchMistakeAutopsy(
                game = game,
                outcome = outcome,
                finalScore = score,
                difficultyLevel = profile.difficultyLevel.toInt(),
                criticalStateSignature = snap.stateSignature,
                criticalStateDescription = snap.stateDescription,
                criticalAction = snap.chosenAction,
                scoreDelta = snap.immediateScoreDelta,
                mistakesCommitted = currentMatchMistakesCommitted
            )

            result.fold(
                onSuccess = { autopsy ->
                    val newMemory = StateMemory(
                        userId = currentUserId,
                        gameId = game.id,
                        stateSignature = snap.stateSignature,
                        stateDescription = snap.stateDescription,
                        actionTaken = snap.chosenAction,
                        scoreDelta = snap.immediateScoreDelta.toLong(),
                        outcome = if (autopsy.recommendedQDelta < 0) "mistake" else "optimal",
                        qValueScore = autopsy.recommendedQDelta.toLong(),
                        lessonLearned = autopsy.criticalLesson,
                        difficultyLevel = profile.difficultyLevel,
                        timesRecalled = 1L
                    )
                    repository.upsertStateMemory(newMemory)

                    val updatedProfile = profile.copy(
                        aggressionWeight = (profile.aggressionWeight + autopsy.aggressionAdjustment).coerceIn(15L, 100L),
                        defenseWeight = (profile.defenseWeight + autopsy.defenseAdjustment).coerceIn(15L, 100L),
                        lookaheadWeight = (profile.lookaheadWeight + autopsy.lookaheadAdjustment).coerceIn(15L, 100L),
                        trapAwarenessWeight = (profile.trapAwarenessWeight + autopsy.trapAwarenessAdjustment).coerceIn(15L, 100L),
                        lastAdjustmentReason = autopsy.reflectionSummary.take(480)
                    )
                    repository.saveOrUpdateGameProfile(updatedProfile)
                    _aiTaskState.value = AiTaskState.Success("Autopsy Lesson Retained: ${autopsy.criticalLesson}")
                },
                onFailure = { err ->
                    _aiTaskState.value = AiTaskState.Error(err.message ?: "Autopsy failed.")
                }
            )
        }
    }

    fun addManualStateMemory(
        game: GameType,
        stateSignature: String,
        stateDescription: String,
        actionTaken: String,
        outcome: String,
        qValueScore: Int,
        lessonLearned: String
    ) {
        viewModelScope.launch {
            val profile = getProfileForGame(game)
            val mem = StateMemory(
                userId = currentUserId,
                gameId = game.id,
                stateSignature = stateSignature.trim().uppercase().replace(" ", "_"),
                stateDescription = stateDescription.trim(),
                actionTaken = actionTaken.trim().uppercase(),
                scoreDelta = if (qValueScore >= 0) 25L else -25L,
                outcome = outcome,
                qValueScore = qValueScore.toLong().coerceIn(-100L, 100L),
                lessonLearned = lessonLearned.trim(),
                difficultyLevel = profile.difficultyLevel,
                timesRecalled = 0L
            )
            repository.upsertStateMemory(mem)
            _statusBanner.value = "Saved custom state-action memory [${mem.stateSignature}] to vault."
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            repository.deleteStateMemory(memoryId)
        }
    }

    fun setManualDifficultyLevel(game: GameType, level: Int) {
        viewModelScope.launch {
            val profile = getProfileForGame(game)
            val clamped = level.coerceIn(1, 10)
            val updated = profile.copy(
                difficultyLevel = clamped.toLong(),
                lastAdjustmentReason = "Manual calibration to Tier $clamped by operator."
            )
            repository.saveOrUpdateGameProfile(updated)
            if (_selectedGame.value == game) {
                resetCurrentMatch()
            }
        }
    }

    fun toggleAutoAdjustDifficulty(game: GameType, enabled: Boolean) {
        viewModelScope.launch {
            val profile = getProfileForGame(game)
            val updated = profile.copy(
                autoAdjustDifficulty = enabled,
                lastAdjustmentReason = if (enabled) {
                    "Auto-Adaptive Difficulty enabled (Target Win Rate: ${profile.targetWinRate}%)."
                } else {
                    "Locked at Manual Tier ${profile.difficultyLevel}."
                }
            )
            repository.saveOrUpdateGameProfile(updated)
        }
    }

    fun updateTargetWinRate(game: GameType, targetPercent: Int) {
        viewModelScope.launch {
            val profile = getProfileForGame(game)
            val clamped = targetPercent.coerceIn(35, 85)
            val updated = profile.copy(
                targetWinRate = clamped.toLong(),
                lastAdjustmentReason = "Updated optimal learning target win-rate band around $clamped%."
            )
            repository.saveOrUpdateGameProfile(updated)
        }
    }

    fun toggleStrategicObjective(game: GameType, objective: String) {
        viewModelScope.launch {
            val profile = getProfileForGame(game)
            val result = repository.toggleStrategicObjective(
                gameId = game.id,
                objectiveText = objective,
                baseProfile = profile
            )
            result.onFailure { err ->
                _statusBanner.value = "Could not sync objective state: ${err.localizedMessage ?: "Unknown error"}"
            }
        }
    }
}
