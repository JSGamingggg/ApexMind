package com.example.engine

import com.example.data.GameProfile
import com.example.data.GameType
import com.example.data.ResearchDoc
import com.example.data.StateMemory
import kotlin.math.abs
import kotlin.random.Random

data class CandidateEvaluation(
    val action: String,
    val baseHeuristicScore: Int,
    val memoryBonus: Int,
    val finalUtility: Int,
    val matchedMemory: StateMemory? = null,
    val rationale: String
)

data class AiDecisionTelemetry(
    val turnNumber: Int,
    val stateSignature: String,
    val stateDescription: String,
    val chosenAction: String,
    val immediateScoreDelta: Int,
    val candidates: List<CandidateEvaluation>,
    val recalledMemory: StateMemory? = null,
    val avoidedMistakeMemory: StateMemory? = null,
    val wasMistakeCommitted: Boolean = false,
    val reasoningHeadline: String
)

// ============================================================================
// 1. TACTICAL GRID (5x5 Territory, Adjacency Synergy & EMP Trap Avoidance)
// ============================================================================
enum class CellOwner { NONE, AI, BOT }

data class TacticalCell(
    val row: Int,
    val col: Int,
    val baseEnergy: Int,
    val isEmpTrap: Boolean,
    val isTrapRevealed: Boolean = false,
    val owner: CellOwner = CellOwner.NONE
)

data class TacticalGridState(
    val cells: List<TacticalCell>,
    val aiScore: Int = 0,
    val botScore: Int = 0,
    val turn: Int = 1,
    val maxTurns: Int = 14,
    val difficultyLevel: Int = 1,
    val isGameOver: Boolean = false,
    val lastBotAction: String = "Awaiting opening move"
) {
    fun cellAt(r: Int, c: Int): TacticalCell = cells[r * 5 + c]
}

object TacticalGridEngine {

    fun createInitialState(difficultyLevel: Int, seed: Long = System.currentTimeMillis()): TacticalGridState {
        val rng = Random(seed)
        val params = AdaptiveDifficultyEngine.getParametersForLevel(difficultyLevel)
        val rawCells = mutableListOf<TacticalCell>()
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val isCenter = (r in 1..3 && c in 1..3)
                val baseEnergy = if (isCenter) rng.nextInt(18, 38) else rng.nextInt(10, 26)
                rawCells.add(
                    TacticalCell(
                        row = r,
                        col = c,
                        baseEnergy = baseEnergy,
                        isEmpTrap = false
                    )
                )
            }
        }

        // Place EMP traps according to difficulty parameter (never on exact center (2,2))
        val candidateIndices = (0 until 25).filter { it != 12 }.shuffled(rng).take(params.tacticalTrapCount)
        val finalCells = rawCells.mapIndexed { idx, cell ->
            if (idx in candidateIndices) {
                // Traps disguise themselves as tempting high-energy nodes!
                cell.copy(isEmpTrap = true, baseEnergy = rng.nextInt(28, 42))
            } else {
                cell
            }
        }

        return TacticalGridState(
            cells = finalCells,
            turn = 1,
            maxTurns = 12,
            difficultyLevel = difficultyLevel
        )
    }

    fun encodeStateSignature(state: TacticalGridState): Pair<String, String> {
        val centerControlled = state.cellAt(2, 2).owner.name
        val aiCount = state.cells.count { it.owner == CellOwner.AI }
        val botCount = state.cells.count { it.owner == CellOwner.BOT }
        val uncapturedHighNodes = state.cells.filter { it.owner == CellOwner.NONE && it.baseEnergy >= 28 }
        val topNodeCoord = uncapturedHighNodes.maxByOrNull { it.baseEnergy }?.let { "${it.row}_${it.col}" } ?: "NONE"
        val phase = when {
            state.turn <= 3 -> "OPENING"
            state.turn <= 8 -> "MIDGAME"
            else -> "ENDGAME"
        }
        val sig = "TG_${phase}_C_${centerControlled}_HIGH_${topNodeCoord}_AI${aiCount}_BOT${botCount}"
        val desc = "$phase (Turn ${state.turn}/${state.maxTurns}): Center(2,2)=$centerControlled, AI sectors=$aiCount (${state.aiScore} pts) vs Bot=$botCount (${state.botScore} pts), Peak Node=$topNodeCoord"
        return sig to desc
    }

    private fun countNeighbors(cells: List<TacticalCell>, r: Int, c: Int, owner: CellOwner): Int {
        val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        var count = 0
        for ((dr, dc) in dirs) {
            val nr = r + dr
            val nc = c + dc
            if (nr in 0..4 && nc in 0..4) {
                if (cells[nr * 5 + nc].owner == owner) count++
            }
        }
        return count
    }

    fun stepAiTurn(
        state: TacticalGridState,
        profile: GameProfile,
        memories: List<StateMemory>,
        researchDocs: List<ResearchDoc>,
        forcedAction: String? = null
    ): Pair<TacticalGridState, AiDecisionTelemetry> {
        val (sig, desc) = encodeStateSignature(state)
        val openCells = state.cells.filter { it.owner == CellOwner.NONE }
        if (openCells.isEmpty() || state.isGameOver) {
            return state.copy(isGameOver = true) to AiDecisionTelemetry(
                turnNumber = state.turn,
                stateSignature = sig,
                stateDescription = desc,
                chosenAction = "NONE",
                immediateScoreDelta = 0,
                candidates = emptyList(),
                reasoningHeadline = "Grid complete."
            )
        }

        val gameMemories = memories.filter { it.gameId == GameType.TACTICAL_GRID.id }
        val hasResearch = researchDocs.any { it.gameId == GameType.TACTICAL_GRID.id }

        val evaluations = openCells.map { cell ->
            val actionName = "SECTOR_${cell.row}_${cell.col}"
            val aiAdj = countNeighbors(state.cells, cell.row, cell.col, CellOwner.AI)
            val botAdj = countNeighbors(state.cells, cell.row, cell.col, CellOwner.BOT)
            val isCenterRegion = cell.row in 1..3 && cell.col in 1..3

            // Base heuristic calculation weighted by learned GameProfile + Research
            val aggressionTerm = (cell.baseEnergy * profile.aggressionWeight.toInt()) / 55
            val synergyTerm = (aiAdj * 14 * profile.lookaheadWeight.toInt()) / 60
            val blockTerm = (botAdj * 10 * profile.defenseWeight.toInt()) / 60
            val centerBonus = if (cell.row == 2 && cell.col == 2) 22 else if (isCenterRegion) 8 else 0

            // Suspicion of unrevealed EMP trap if baseEnergy >= 29 and isolated from center/anchors
            val trapSuspicionPenalty = if (cell.baseEnergy >= 29 && aiAdj == 0 && (profile.trapAwarenessWeight >= 68 || hasResearch)) {
                -((profile.trapAwarenessWeight.toInt() - 45).coerceAtLeast(5))
            } else {
                0
            }

            val baseHeuristic = aggressionTerm + synergyTerm + blockTerm + centerBonus + trapSuspicionPenalty

            // Query Episodic Memory Bank for matching state or action-pattern
            val exactOrSimilarMemory = gameMemories.firstOrNull { mem ->
                mem.actionTaken == actionName && (mem.stateSignature == sig || mem.stateSignature.contains("HIGH_${cell.row}_${cell.col}"))
            } ?: gameMemories.firstOrNull { mem ->
                mem.actionTaken == actionName
            }

            val memoryBonus = exactOrSimilarMemory?.let { mem ->
                when (mem.outcome) {
                    "mistake", "loss" -> mem.qValueScore.toInt().coerceAtMost(-45)
                    "win", "optimal" -> mem.qValueScore.toInt().coerceAtLeast(25)
                    else -> mem.qValueScore.toInt()
                }
            } ?: 0

            val totalUtility = baseHeuristic + memoryBonus
            val rationale = buildString {
                append("Energy +${cell.baseEnergy}")
                if (aiAdj > 0) append(", Synergy x$aiAdj (+${aiAdj * 12})")
                if (botAdj > 0) append(", Blocks Bot")
                if (exactOrSimilarMemory != null) {
                    append(" • Memory(${exactOrSimilarMemory.outcome.uppercase()}: ${if (memoryBonus >= 0) "+$memoryBonus" else "$memoryBonus"})")
                }
            }

            CandidateEvaluation(
                action = actionName,
                baseHeuristicScore = baseHeuristic,
                memoryBonus = memoryBonus,
                finalUtility = totalUtility,
                matchedMemory = exactOrSimilarMemory,
                rationale = rationale
            )
        }.sortedByDescending { it.finalUtility }

        // Check if the highest-base-heuristic move was vetoed because of a negative mistake memory!
        val naiveTop = evaluations.maxByOrNull { it.baseHeuristicScore }
        val chosenEval = if (forcedAction != null) {
            evaluations.firstOrNull { it.action == forcedAction } ?: evaluations.first()
        } else {
            evaluations.first()
        }

        val avoidedMistake = if (naiveTop != null && naiveTop.action != chosenEval.action && (naiveTop.matchedMemory?.outcome == "mistake" || naiveTop.matchedMemory?.outcome == "loss")) {
            naiveTop.matchedMemory
        } else {
            null
        }

        val parts = chosenEval.action.split("_")
        val targetRow = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val targetCol = parts.getOrNull(2)?.toIntOrNull() ?: 0
        val targetCell = state.cellAt(targetRow, targetCol)

        val aiNeighbors = countNeighbors(state.cells, targetRow, targetCol, CellOwner.AI)
        val immediateDelta = if (targetCell.isEmpTrap) {
            -25
        } else {
            targetCell.baseEnergy + (aiNeighbors * 12)
        }

        val mistakeCommitted = targetCell.isEmpTrap
        val updatedCellsAfterAi = state.cells.map { c ->
            if (c.row == targetRow && c.col == targetCol) {
                c.copy(owner = CellOwner.AI, isTrapRevealed = true)
            } else {
                c
            }
        }

        // Now Adaptive Opponent Bot takes its turn
        val remainingForBot = updatedCellsAfterAi.filter { it.owner == CellOwner.NONE }
        var botDelta = 0
        var botMoveDesc = "No moves left"
        val params = AdaptiveDifficultyEngine.getParametersForLevel(state.difficultyLevel)

        val finalCells = if (remainingForBot.isNotEmpty()) {
            // Bot intelligence scales with difficultyLevel
            val botChoice = remainingForBot.maxByOrNull { cell ->
                val botAdj = countNeighbors(updatedCellsAfterAi, cell.row, cell.col, CellOwner.BOT)
                val aiAdj = countNeighbors(updatedCellsAfterAi, cell.row, cell.col, CellOwner.AI)
                val trapAware = if (cell.isEmpTrap && state.difficultyLevel >= 4) -60 else 0
                cell.baseEnergy + (botAdj * 4 * params.tacticalBotLookahead) + (aiAdj * 3 * params.tacticalBotLookahead) + trapAware
            } ?: remainingForBot.first()

            val botNeighbors = countNeighbors(updatedCellsAfterAi, botChoice.row, botChoice.col, CellOwner.BOT)
            botDelta = if (botChoice.isEmpTrap) -20 else (botChoice.baseEnergy + botNeighbors * 10)
            botMoveDesc = "Bot captured (${botChoice.row},${botChoice.col}) for ${if (botDelta >= 0) "+$botDelta" else "$botDelta"} pts"

            updatedCellsAfterAi.map { c ->
                if (c.row == botChoice.row && c.col == botChoice.col) {
                    c.copy(owner = CellOwner.BOT, isTrapRevealed = true)
                } else {
                    c
                }
            }
        } else {
            updatedCellsAfterAi
        }

        val nextTurn = state.turn + 1
        val anyOpen = finalCells.any { it.owner == CellOwner.NONE }
        val gameOver = !anyOpen || nextTurn > state.maxTurns

        val headline = when {
            avoidedMistake != null -> "MISTAKE AVOIDED! Recalled memory on ${naiveTop?.action} (${avoidedMistake.lessonLearned.take(65)}...) and pivoted to ${chosenEval.action} (+$immediateDelta pts)."
            mistakeCommitted -> "EMP TRAP TRIGGERED at ($targetRow,$targetCol)! Deducted 25 pts — logging state [$sig] to Mistake Ledger so AI avoids this trap pattern next match."
            chosenEval.matchedMemory != null -> "MEMORY HIT on ${chosenEval.action}: Applied retained heuristic (+$immediateDelta pts)."
            else -> "Selected ${chosenEval.action} (${chosenEval.rationale}) → +$immediateDelta pts."
        }

        val nextState = state.copy(
            cells = finalCells,
            aiScore = (state.aiScore + immediateDelta).coerceAtLeast(0),
            botScore = (state.botScore + botDelta).coerceAtLeast(0),
            turn = if (gameOver) state.turn else nextTurn,
            isGameOver = gameOver,
            lastBotAction = botMoveDesc
        )

        return nextState to AiDecisionTelemetry(
            turnNumber = state.turn,
            stateSignature = sig,
            stateDescription = desc,
            chosenAction = chosenEval.action,
            immediateScoreDelta = immediateDelta,
            candidates = evaluations.take(4),
            recalledMemory = chosenEval.matchedMemory,
            avoidedMistakeMemory = avoidedMistake,
            wasMistakeCommitted = mistakeCommitted,
            reasoningHeadline = headline
        )
    }
}

// ============================================================================
// 2. 2048 NEURAL MATRIX (4x4 Heuristic Tile Synthesis)
// ============================================================================
data class Matrix2048State(
    val board: List<Int>, // 16 cells (4x4)
    val score: Int = 0,
    val maxTile: Int = 2,
    val targetTile: Int = 128,
    val turn: Int = 1,
    val difficultyLevel: Int = 1,
    val isGameOver: Boolean = false,
    val isWin: Boolean = false
)

object Neural2048Engine {

    val DIRECTIONS = listOf("UP", "LEFT", "RIGHT", "DOWN")

    fun createInitialState(difficultyLevel: Int, seed: Long = System.currentTimeMillis()): Matrix2048State {
        val params = AdaptiveDifficultyEngine.getParametersForLevel(difficultyLevel)
        var board = List(16) { 0 }
        board = spawnTile(board, params.fourTileSpawnProbPercent, Random(seed))
        board = spawnTile(board, params.fourTileSpawnProbPercent, Random(seed + 7))
        return Matrix2048State(
            board = board,
            score = 0,
            maxTile = board.maxOrNull() ?: 2,
            targetTile = params.targetTile2048,
            turn = 1,
            difficultyLevel = difficultyLevel
        )
    }

    private fun spawnTile(board: List<Int>, fourProbPercent: Int, rng: Random = Random.Default): List<Int> {
        val emptyIndices = board.indices.filter { board[it] == 0 }
        if (emptyIndices.isEmpty()) return board
        val chosenIdx = emptyIndices[rng.nextInt(emptyIndices.size)]
        val tileValue = if (rng.nextInt(100) < fourProbPercent) 4 else 2
        return board.toMutableList().also { it[chosenIdx] = tileValue }
    }

    private fun slideLineLeft(line: List<Int>): Pair<List<Int>, Int> {
        val nonZero = line.filter { it != 0 }.toMutableList()
        val result = mutableListOf<Int>()
        var gained = 0
        var i = 0
        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val merged = nonZero[i] * 2
                result.add(merged)
                gained += merged
                i += 2
            } else {
                result.add(nonZero[i])
                i += 1
            }
        }
        while (result.size < 4) result.add(0)
        return result to gained
    }

    fun simulateSlide(board: List<Int>, direction: String): Pair<List<Int>, Int> {
        val out = MutableList(16) { 0 }
        var totalGain = 0
        for (rc in 0 until 4) {
            val line = when (direction) {
                "LEFT" -> (0..3).map { c -> board[rc * 4 + c] }
                "RIGHT" -> (3 downTo 0).map { c -> board[rc * 4 + c] }
                "UP" -> (0..3).map { r -> board[r * 4 + rc] }
                else -> (3 downTo 0).map { r -> board[r * 4 + rc] } // DOWN
            }
            val (slid, gain) = slideLineLeft(line)
            totalGain += gain
            for (idx in 0..3) {
                when (direction) {
                    "LEFT" -> out[rc * 4 + idx] = slid[idx]
                    "RIGHT" -> out[rc * 4 + (3 - idx)] = slid[idx]
                    "UP" -> out[idx * 4 + rc] = slid[idx]
                    else -> out[(3 - idx) * 4 + rc] = slid[idx]
                }
            }
        }
        return out to totalGain
    }

    fun encodeStateSignature(board: List<Int>): Pair<String, String> {
        val maxVal = board.maxOrNull() ?: 2
        val maxIdx = board.indexOfFirst { it == maxVal }
        val anchorPos = when (maxIdx) {
            0 -> "TL_CORNER"
            3 -> "TR_CORNER"
            12 -> "BL_CORNER"
            15 -> "BR_CORNER"
            else -> "DISLODGED"
        }
        val emptyCount = board.count { it == 0 }
        val row0Full = (0..3).all { board[it] != 0 }
        val densityBand = when {
            emptyCount >= 7 -> "OPEN"
            emptyCount >= 4 -> "MEDIUM"
            else -> "CRITICAL"
        }
        val sig = "N2048_${anchorPos}_MAX${maxVal}_${densityBand}_R0_${if (row0Full) "LOCKED" else "OPEN"}"
        val desc = "Max Tile $maxVal ($anchorPos), $emptyCount empty cells ($densityBand), Top Row ${if (row0Full) "Locked" else "Open"}"
        return sig to desc
    }

    private fun evaluateAfterstate(
        afterBoard: List<Int>,
        immediateGain: Int,
        profile: GameProfile,
        hasResearch: Boolean
    ): Int {
        val emptyCount = afterBoard.count { it == 0 }
        val maxVal = afterBoard.maxOrNull() ?: 2
        val anchorInTopLeft = afterBoard[0] == maxVal
        val anchorInAnyCorner = afterBoard[0] == maxVal || afterBoard[3] == maxVal

        // Monotonicity along row 0 and col 0
        var monotonicScore = 0
        if (afterBoard[0] >= afterBoard[1] && afterBoard[1] >= afterBoard[2] && afterBoard[2] >= afterBoard[3]) {
            monotonicScore += 28
        }
        if (afterBoard[0] >= afterBoard[4] && afterBoard[4] >= afterBoard[8]) {
            monotonicScore += 18
        }

        val cornerBonus = when {
            anchorInTopLeft -> (45 * profile.trapAwarenessWeight.toInt()) / 60
            anchorInAnyCorner -> 20
            else -> -((40 * profile.trapAwarenessWeight.toInt()) / 60)
        }

        val emptyBonus = (emptyCount * 8 * profile.defenseWeight.toInt()) / 60
        val mergeBonus = (immediateGain * profile.aggressionWeight.toInt()) / 50
        val researchBonus = if (hasResearch && anchorInTopLeft) 15 else 0

        return cornerBonus + emptyBonus + mergeBonus + ((monotonicScore * profile.lookaheadWeight.toInt()) / 60) + researchBonus
    }

    fun stepAiTurn(
        state: Matrix2048State,
        profile: GameProfile,
        memories: List<StateMemory>,
        researchDocs: List<ResearchDoc>,
        forcedAction: String? = null
    ): Pair<Matrix2048State, AiDecisionTelemetry> {
        val (sig, desc) = encodeStateSignature(state.board)
        val validMoves = DIRECTIONS.mapNotNull { dir ->
            val (nextBoard, gain) = simulateSlide(state.board, dir)
            if (nextBoard != state.board) Triple(dir, nextBoard, gain) else null
        }

        if (validMoves.isEmpty()) {
            return state.copy(isGameOver = true, isWin = state.maxTile >= state.targetTile) to AiDecisionTelemetry(
                turnNumber = state.turn,
                stateSignature = sig,
                stateDescription = desc,
                chosenAction = "NONE",
                immediateScoreDelta = 0,
                candidates = emptyList(),
                reasoningHeadline = "No legal slides remaining."
            )
        }

        val gameMemories = memories.filter { it.gameId == GameType.NEURAL_2048.id }
        val hasResearch = researchDocs.any { it.gameId == GameType.NEURAL_2048.id }

        val evaluations = validMoves.map { (dir, afterBoard, gain) ->
            val baseScore = evaluateAfterstate(afterBoard, gain, profile, hasResearch)
            val matchedMem = gameMemories.firstOrNull { it.stateSignature == sig && it.actionTaken == dir }
                ?: gameMemories.firstOrNull {
                    it.actionTaken == dir && it.stateSignature.contains(if (state.board[0] == state.maxTile) "TL_CORNER" else "DISLODGED")
                }

            val memBonus = matchedMem?.let { mem ->
                when (mem.outcome) {
                    "mistake", "loss" -> mem.qValueScore.toInt().coerceAtMost(-40)
                    "win", "optimal" -> mem.qValueScore.toInt().coerceAtLeast(25)
                    else -> mem.qValueScore.toInt()
                }
            } ?: 0

            val anchorKept = afterBoard[0] == (afterBoard.maxOrNull() ?: 2)
            val rationale = buildString {
                append("Merge +$gain pts, ${afterBoard.count { it == 0 }} open cells")
                append(if (anchorKept) ", Anchor Locked" else ", Anchor Shifted!")
                if (matchedMem != null) {
                    append(" • Memory(${matchedMem.outcome.uppercase()}: ${if (memBonus >= 0) "+$memBonus" else "$memBonus"})")
                }
            }

            CandidateEvaluation(
                action = dir,
                baseHeuristicScore = baseScore,
                memoryBonus = memBonus,
                finalUtility = baseScore + memBonus,
                matchedMemory = matchedMem,
                rationale = rationale
            )
        }.sortedByDescending { it.finalUtility }

        val naiveTop = evaluations.maxByOrNull { it.baseHeuristicScore }
        val chosenEval = if (forcedAction != null) {
            evaluations.firstOrNull { it.action == forcedAction } ?: evaluations.first()
        } else {
            evaluations.first()
        }

        val avoidedMistake = if (naiveTop != null && naiveTop.action != chosenEval.action &&
            (naiveTop.matchedMemory?.outcome == "mistake" || naiveTop.matchedMemory?.outcome == "loss")
        ) {
            naiveTop.matchedMemory
        } else {
            null
        }

        val (slidBoard, gain) = simulateSlide(state.board, chosenEval.action)
        val params = AdaptiveDifficultyEngine.getParametersForLevel(state.difficultyLevel)
        val spawnedBoard = spawnTile(slidBoard, params.fourTileSpawnProbPercent)
        val newMax = spawnedBoard.maxOrNull() ?: 2
        val wasAnchorInCorner = state.board[0] == state.maxTile
        val isAnchorStillInCorner = spawnedBoard[0] == newMax
        val mistakeCommitted = wasAnchorInCorner && !isAnchorStillInCorner && validMoves.any { (d, b, _) ->
            d != chosenEval.action && b[0] == (b.maxOrNull() ?: 2)
        }

        val anyMoveLeft = DIRECTIONS.any { d -> simulateSlide(spawnedBoard, d).first != spawnedBoard }
        val reachedTarget = newMax >= state.targetTile
        val gameOver = reachedTarget || !anyMoveLeft || state.turn >= 140

        val headline = when {
            avoidedMistake != null -> "MISTAKE AVOIDED! Suppressed ${naiveTop?.action} using stored lesson (${avoidedMistake.lessonLearned.take(60)}...) → slid ${chosenEval.action}."
            mistakeCommitted -> "MISTAKE DETECTED: Sliding ${chosenEval.action} dislodged the top-left ${state.maxTile} anchor! Recording corrective memory for [$sig]."
            chosenEval.matchedMemory != null -> "MEMORY HIT [${chosenEval.action}]: ${chosenEval.rationale}"
            else -> "Executed ${chosenEval.action}: ${chosenEval.rationale}"
        }

        val nextState = state.copy(
            board = spawnedBoard,
            score = state.score + gain,
            maxTile = newMax,
            turn = state.turn + 1,
            isGameOver = gameOver,
            isWin = reachedTarget
        )

        return nextState to AiDecisionTelemetry(
            turnNumber = state.turn,
            stateSignature = sig,
            stateDescription = desc,
            chosenAction = chosenEval.action,
            immediateScoreDelta = if (mistakeCommitted) -15 else gain,
            candidates = evaluations,
            recalledMemory = chosenEval.matchedMemory,
            avoidedMistakeMemory = avoidedMistake,
            wasMistakeCommitted = mistakeCommitted,
            reasoningHeadline = headline
        )
    }
}

// ============================================================================
// 3. CYBER SNAKE ARENA (8x8 Autonomous Pathfinding, Flood-Fill & Hazard Grid)
// ============================================================================
data class GridPoint(val row: Int, val col: Int)

data class CyberSnakeState(
    val gridSize: Int = 8,
    val snakeBody: List<GridPoint>, // head is index 0
    val dataCore: GridPoint,
    val firewalls: Set<GridPoint>,
    val coresCollected: Int = 0,
    val targetCores: Int = 7,
    val score: Int = 0,
    val turn: Int = 1,
    val maxTurns: Int = 85,
    val difficultyLevel: Int = 1,
    val isGameOver: Boolean = false,
    val isWin: Boolean = false,
    val statusNote: String = "Active"
)

object CyberSnakeEngine {

    private val DIRS = mapOf(
        "UP" to GridPoint(-1, 0),
        "DOWN" to GridPoint(1, 0),
        "LEFT" to GridPoint(0, -1),
        "RIGHT" to GridPoint(0, 1)
    )

    fun createInitialState(difficultyLevel: Int, seed: Long = System.currentTimeMillis()): CyberSnakeState {
        val rng = Random(seed)
        val params = AdaptiveDifficultyEngine.getParametersForLevel(difficultyLevel)
        val initialSnake = listOf(GridPoint(4, 3), GridPoint(4, 2), GridPoint(4, 1))
        val forbidden = initialSnake.toMutableSet()
        // Keep immediate front of snake clear on spawn
        forbidden.add(GridPoint(4, 4))

        val firewalls = mutableSetOf<GridPoint>()
        val allPoints = (0 until 8).flatMap { r -> (0 until 8).map { c -> GridPoint(r, c) } }
            .filter { it !in forbidden }
            .shuffled(rng)

        for (pt in allPoints) {
            if (firewalls.size >= params.snakeFirewallCount) break
            // Avoid placing firewalls that completely box in a corner on turn 1
            if (pt.row in 1..6 && pt.col in 1..6) {
                firewalls.add(pt)
            }
        }

        val coreCandidates = allPoints.filter { it !in firewalls && it !in initialSnake }
        val firstCore = coreCandidates.firstOrNull() ?: GridPoint(2, 6)

        return CyberSnakeState(
            snakeBody = initialSnake,
            dataCore = firstCore,
            firewalls = firewalls,
            coresCollected = 0,
            targetCores = params.snakeTargetCores,
            score = 0,
            turn = 1,
            difficultyLevel = difficultyLevel
        )
    }

    private fun isBlocked(pt: GridPoint, state: CyberSnakeState): Boolean {
        if (pt.row !in 0 until state.gridSize || pt.col !in 0 until state.gridSize) return true
        if (pt in state.firewalls) return true
        // Tail tip vacates on next step unless core is eaten
        val bodyWithoutTail = state.snakeBody.dropLast(1)
        return pt in bodyWithoutTail
    }

    private fun computeReachableSpace(start: GridPoint, state: CyberSnakeState, maxSearch: Int = 36): Int {
        if (isBlocked(start, state)) return 0
        val visited = mutableSetOf(start)
        val queue = ArrayDeque<GridPoint>()
        queue.add(start)
        while (queue.isNotEmpty() && visited.size < maxSearch) {
            val curr = queue.removeFirst()
            for (delta in DIRS.values) {
                val next = GridPoint(curr.row + delta.row, curr.col + delta.col)
                if (next !in visited && !isBlocked(next, state)) {
                    visited.add(next)
                    queue.add(next)
                }
            }
        }
        return visited.size
    }

    fun encodeStateSignature(state: CyberSnakeState): Pair<String, String> {
        val head = state.snakeBody.first()
        val coreDirR = when {
            state.dataCore.row < head.row -> "N"
            state.dataCore.row > head.row -> "S"
            else -> "E"
        }
        val coreDirC = when {
            state.dataCore.col < head.col -> "W"
            state.dataCore.col > head.col -> "E"
            else -> ""
        }
        val openMask = DIRS.entries.joinToString("") { (name, d) ->
            val next = GridPoint(head.row + d.row, head.col + d.col)
            "${name.first()}${if (isBlocked(next, state)) "0" else "1"}"
        }
        val sig = "SNAKE_CORE_${coreDirR}${coreDirC}_${openMask}"
        val desc = "Head(${head.row},${head.col}) → Core(${state.dataCore.row},${state.dataCore.col}) [${coreDirR}${coreDirC}], Exits=$openMask, Cores=${state.coresCollected}/${state.targetCores}"
        return sig to desc
    }

    fun stepAiTurn(
        state: CyberSnakeState,
        profile: GameProfile,
        memories: List<StateMemory>,
        researchDocs: List<ResearchDoc>,
        forcedAction: String? = null
    ): Pair<CyberSnakeState, AiDecisionTelemetry> {
        val (sig, desc) = encodeStateSignature(state)
        val head = state.snakeBody.first()
        val gameMemories = memories.filter { it.gameId == GameType.CYBER_SNAKE.id }
        val hasResearch = researchDocs.any { it.gameId == GameType.CYBER_SNAKE.id }

        val evaluations = DIRS.entries.map { (dirName, delta) ->
            val targetPt = GridPoint(head.row + delta.row, head.col + delta.col)
            val blocked = isBlocked(targetPt, state)

            val baseScore = if (blocked) {
                -250
            } else {
                val oldDist = abs(head.row - state.dataCore.row) + abs(head.col - state.dataCore.col)
                val newDist = abs(targetPt.row - state.dataCore.row) + abs(targetPt.col - state.dataCore.col)
                val distStep = if (newDist < oldDist) 28 else -14
                val reachable = computeReachableSpace(targetPt, state)
                val openExits = DIRS.values.count { d ->
                    !isBlocked(GridPoint(targetPt.row + d.row, targetPt.col + d.col), state)
                }

                val aggressionComponent = (distStep * profile.aggressionWeight.toInt()) / 55
                val spaceComponent = (reachable * profile.lookaheadWeight.toInt()) / 65
                val trapPenalty = if (reachable < state.snakeBody.size) {
                    -((75 * profile.trapAwarenessWeight.toInt()) / 60)
                } else if (openExits <= 1 && targetPt != state.dataCore) {
                    -((35 * profile.defenseWeight.toInt()) / 60)
                } else {
                    openExits * 6
                }
                val researchBonus = if (hasResearch && reachable >= state.snakeBody.size + 4) 14 else 0
                aggressionComponent + spaceComponent + trapPenalty + researchBonus
            }

            val matchedMem = gameMemories.firstOrNull { it.stateSignature == sig && it.actionTaken == dirName }
            val memBonus = matchedMem?.let { mem ->
                when (mem.outcome) {
                    "mistake", "loss" -> mem.qValueScore.toInt().coerceAtMost(-55)
                    "win", "optimal" -> mem.qValueScore.toInt().coerceAtLeast(25)
                    else -> mem.qValueScore.toInt()
                }
            } ?: 0

            val reachable = if (blocked) 0 else computeReachableSpace(targetPt, state)
            val rationale = if (blocked) {
                "COLLISION HAZARD ($dirName blocked)"
            } else {
                buildString {
                    append("Reachable space=$reachable cells")
                    val dist = abs(targetPt.row - state.dataCore.row) + abs(targetPt.col - state.dataCore.col)
                    append(", Core dist=$dist")
                    if (matchedMem != null) {
                        append(" • Memory(${matchedMem.outcome.uppercase()}: ${if (memBonus >= 0) "+$memBonus" else "$memBonus"})")
                    }
                }
            }

            CandidateEvaluation(
                action = dirName,
                baseHeuristicScore = baseScore,
                memoryBonus = memBonus,
                finalUtility = baseScore + memBonus,
                matchedMemory = matchedMem,
                rationale = rationale
            )
        }.sortedByDescending { it.finalUtility }

        val naiveTop = evaluations.maxByOrNull { it.baseHeuristicScore }
        val chosenEval = if (forcedAction != null) {
            evaluations.firstOrNull { it.action == forcedAction } ?: evaluations.first()
        } else {
            evaluations.first()
        }

        val avoidedMistake = if (naiveTop != null && naiveTop.action != chosenEval.action &&
            (naiveTop.matchedMemory?.outcome == "mistake" || naiveTop.matchedMemory?.outcome == "loss")
        ) {
            naiveTop.matchedMemory
        } else {
            null
        }

        val chosenDelta = DIRS[chosenEval.action] ?: GridPoint(0, 1)
        val nextHead = GridPoint(head.row + chosenDelta.row, head.col + chosenDelta.col)
        val crashed = isBlocked(nextHead, state)

        if (crashed) {
            val nextState = state.copy(
                isGameOver = true,
                isWin = false,
                statusNote = "Collision at (${nextHead.row},${nextHead.col})"
            )
            return nextState to AiDecisionTelemetry(
                turnNumber = state.turn,
                stateSignature = sig,
                stateDescription = desc,
                chosenAction = chosenEval.action,
                immediateScoreDelta = -40,
                candidates = evaluations,
                recalledMemory = chosenEval.matchedMemory,
                avoidedMistakeMemory = avoidedMistake,
                wasMistakeCommitted = true,
                reasoningHeadline = "CRITICAL COLLISION on ${chosenEval.action}! Recording state [$sig] to Mistake Ledger."
            )
        }

        val ateCore = nextHead == state.dataCore
        val newBody = if (ateCore) {
            listOf(nextHead) + state.snakeBody
        } else {
            listOf(nextHead) + state.snakeBody.dropLast(1)
        }

        val newCoresCount = if (ateCore) state.coresCollected + 1 else state.coresCollected
        val wonMatch = newCoresCount >= state.targetCores
        val nextCore = if (ateCore && !wonMatch) {
            val openPts = (0 until state.gridSize).flatMap { r ->
                (0 until state.gridSize).map { c -> GridPoint(r, c) }
            }.filter { it !in newBody && it !in state.firewalls }
            openPts.randomOrNull() ?: state.dataCore
        } else {
            state.dataCore
        }

        val reachableAfter = computeReachableSpace(nextHead, state.copy(snakeBody = newBody))
        val enteredDeadEnd = !wonMatch && reachableAfter < newBody.size
        val immediateDelta = when {
            ateCore -> 35
            enteredDeadEnd -> -20
            else -> 2
        }

        val nextTurn = state.turn + 1
        val outOfTurns = nextTurn > state.maxTurns
        val gameOver = wonMatch || outOfTurns

        val headline = when {
            avoidedMistake != null -> "MISTAKE AVOIDED! Memory vetoed ${naiveTop?.action} (${avoidedMistake.lessonLearned.take(55)}...) → routed ${chosenEval.action}."
            ateCore -> "DATA CORE HARVESTED at (${nextHead.row},${nextHead.col})! ($newCoresCount/${state.targetCores} cores) +35 pts."
            enteredDeadEnd -> "WARNING: ${chosenEval.action} entered tight pocket (space=$reachableAfter < body=${newBody.size}). Flagging state [$sig]."
            else -> "Navigated ${chosenEval.action}: ${chosenEval.rationale}"
        }

        val nextState = state.copy(
            snakeBody = newBody,
            dataCore = nextCore,
            coresCollected = newCoresCount,
            score = (state.score + immediateDelta).coerceAtLeast(0),
            turn = nextTurn,
            isGameOver = gameOver,
            isWin = wonMatch
        )

        return nextState to AiDecisionTelemetry(
            turnNumber = state.turn,
            stateSignature = sig,
            stateDescription = desc,
            chosenAction = chosenEval.action,
            immediateScoreDelta = immediateDelta,
            candidates = evaluations,
            recalledMemory = chosenEval.matchedMemory,
            avoidedMistakeMemory = avoidedMistake,
            wasMistakeCommitted = enteredDeadEnd,
            reasoningHeadline = headline
        )
    }
}
