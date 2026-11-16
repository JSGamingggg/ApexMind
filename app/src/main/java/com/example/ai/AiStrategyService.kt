package com.example.ai

import android.util.Log
import com.example.data.GameProfile
import com.example.data.GameType
import com.example.data.ResearchDoc
import com.example.data.StateMemory
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiStrategyService"

data class ResearchSynthesisResult(
    val synthesizedSummary: String,
    val keyObjectives: List<String>,
    val tacticalRules: List<String>,
    val aggressionBoost: Int,
    val defenseBoost: Int,
    val lookaheadBoost: Int,
    val trapAwarenessBoost: Int
)

data class LiveTacticalAdvice(
    val recommendedAction: String,
    val confidencePercent: Int,
    val reasoning: String,
    val riskAssessment: String
)

data class PostMatchAutopsyResult(
    val reflectionSummary: String,
    val criticalLesson: String,
    val recommendedQDelta: Int,
    val aggressionAdjustment: Int,
    val defenseAdjustment: Int,
    val lookaheadAdjustment: Int,
    val trapAwarenessAdjustment: Int
)

class AiStrategyService {

    private val researchSchema = Schema.obj(
        properties = mapOf(
            "synthesizedSummary" to Schema.string(description = "Executive summary of how the AI should play this game based on the research"),
            "keyObjectives" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 primary winning objectives extracted from the source material"
            ),
            "tacticalRules" to Schema.array(
                items = Schema.string(),
                description = "4 to 6 concrete decision heuristics and anti-mistake rules"
            ),
            "aggressionBoost" to Schema.integer(description = "Recommended delta to aggression heuristic weight between -25 and 25"),
            "defenseBoost" to Schema.integer(description = "Recommended delta to defense heuristic weight between -25 and 25"),
            "lookaheadBoost" to Schema.integer(description = "Recommended delta to lookahead heuristic weight between -25 and 25"),
            "trapAwarenessBoost" to Schema.integer(description = "Recommended delta to trap/hazard awareness weight between -25 and 25")
        )
    )

    private val tacticalConsultSchema = Schema.obj(
        properties = mapOf(
            "recommendedAction" to Schema.string(description = "Exact action identifier from the valid actions list"),
            "confidencePercent" to Schema.integer(description = "Confidence level from 50 to 99"),
            "reasoning" to Schema.string(description = "Concise explanation referencing active research rules and past state memories"),
            "riskAssessment" to Schema.string(description = "Potential mistake or hazard to watch out for on the next turn")
        )
    )

    private val autopsySchema = Schema.obj(
        properties = mapOf(
            "reflectionSummary" to Schema.string(description = "Post-match analysis of what worked, where mistakes occurred, and how difficulty affected play"),
            "criticalLesson" to Schema.string(description = "A concise, permanent rule to store in the State-Action Memory Bank so the AI never repeats the key mistake"),
            "recommendedQDelta" to Schema.integer(description = "Q-value adjustment for the critical turning-point state (-90 to +90)"),
            "aggressionAdjustment" to Schema.integer(description = "Heuristic weight adjustment for aggression (-15 to +15)"),
            "defenseAdjustment" to Schema.integer(description = "Heuristic weight adjustment for defense (-15 to +15)"),
            "lookaheadAdjustment" to Schema.integer(description = "Heuristic weight adjustment for lookahead (-15 to +15)"),
            "trapAwarenessAdjustment" to Schema.integer(description = "Heuristic weight adjustment for trap awareness (-15 to +15)")
        )
    )

    private val researchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = researchSchema
            }
        )
    }

    private val consultModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = tacticalConsultSchema
            }
        )
    }

    private val autopsyModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = autopsySchema
            }
        )
    }

    suspend fun synthesizePreGameResearch(
        game: GameType,
        sourceTitle: String,
        sourceType: String,
        rawContent: String,
        currentProfile: GameProfile
    ): Result<ResearchSynthesisResult> {
        val prompt = """
            You are ApexMind, a self-improving mobile game-playing AI agent conducting pre-game research before playing ${game.title}.
            Game Rules Overview: ${game.rulesOverview}
            Current AI Heuristic Weights:
            - Aggression: ${currentProfile.aggressionWeight}/100
            - Defense: ${currentProfile.defenseWeight}/100
            - Lookahead: ${currentProfile.lookaheadWeight}/100
            - Trap/Hazard Awareness: ${currentProfile.trapAwarenessWeight}/100
            - Current Adaptive Difficulty Tier: ${currentProfile.difficultyLevel}/10

            External Research Material ($sourceType — "$sourceTitle"):
            $rawContent

            Analyze this material and extract a structured pre-game strategy briefing:
            1. Write a clear synthesizedSummary explaining the optimal meta-strategy.
            2. Extract 3 to 5 keyObjectives.
            3. Extract 4 to 6 specific, actionable tacticalRules that will guide move selection.
            4. Recommend integer boosts (-25 to +25) for aggressionBoost, defenseBoost, lookaheadBoost, and trapAwarenessBoost.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                researchModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Model returned empty response.")
            val json = JSONObject(jsonText)

            val objectivesArr = json.getJSONArray("keyObjectives")
            val objectives = (0 until objectivesArr.length()).map { objectivesArr.getString(it) }

            val rulesArr = json.getJSONArray("tacticalRules")
            val rules = (0 until rulesArr.length()).map { rulesArr.getString(it) }

            Result.success(
                ResearchSynthesisResult(
                    synthesizedSummary = json.getString("synthesizedSummary"),
                    keyObjectives = objectives,
                    tacticalRules = rules,
                    aggressionBoost = json.optInt("aggressionBoost", 5).coerceIn(-25, 25),
                    defenseBoost = json.optInt("defenseBoost", 5).coerceIn(-25, 25),
                    lookaheadBoost = json.optInt("lookaheadBoost", 10).coerceIn(-25, 25),
                    trapAwarenessBoost = json.optInt("trapAwarenessBoost", 10).coerceIn(-25, 25)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Pre-game research synthesis timed out", e)
            Result.failure(Exception("Research synthesis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Pre-game research synthesis failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to synthesize pre-game research."))
        }
    }

    suspend fun consultOnLiveBoardState(
        game: GameType,
        stateSignature: String,
        stateDescription: String,
        validActions: List<String>,
        matchedMemories: List<StateMemory>,
        recentResearch: List<ResearchDoc>,
        profile: GameProfile
    ): Result<LiveTacticalAdvice> {
        val memoriesContext = if (matchedMemories.isEmpty()) {
            "No exact prior state memories recorded yet."
        } else {
            matchedMemories.take(5).joinToString("\n") {
                "- State [${it.stateSignature}] | Action: ${it.actionTaken} | Outcome: ${it.outcome} (Q=${it.qValueScore}) | Lesson: ${it.lessonLearned}"
            }
        }

        val researchContext = if (recentResearch.isEmpty()) {
            "Standard baseline rules."
        } else {
            recentResearch.take(2).flatMap { it.tacticalRules }.take(6).joinToString("\n") { "- $it" }
        }

        val prompt = """
            You are ApexMind, an autonomous AI playing ${game.title} at Adaptive Difficulty Tier ${profile.difficultyLevel}/10.
            Current Board State Signature: $stateSignature
            Current Board Description: $stateDescription
            Valid Available Actions: ${validActions.joinToString(", ")}

            Learned Heuristic Weights:
            Aggression=${profile.aggressionWeight}, Defense=${profile.defenseWeight}, Lookahead=${profile.lookaheadWeight}, TrapAwareness=${profile.trapAwarenessWeight}

            Active Pre-Game Research Rules:
            $researchContext

            Queried Episodic State-Action Memories:
            $memoriesContext

            Select the single best action from [${validActions.joinToString(", ")}] and explain how your pre-game research and past state memories informed this decision.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                consultModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty tactical response from Gemini.")
            val json = JSONObject(jsonText)
            val rawAction = json.getString("recommendedAction")
            val matchedAction = validActions.firstOrNull {
                it.equals(rawAction, ignoreCase = true) || rawAction.contains(it, ignoreCase = true)
            } ?: validActions.first()

            Result.success(
                LiveTacticalAdvice(
                    recommendedAction = matchedAction,
                    confidencePercent = json.optInt("confidencePercent", 85).coerceIn(50, 99),
                    reasoning = json.getString("reasoning"),
                    riskAssessment = json.getString("riskAssessment")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Live tactical consult timed out", e)
            Result.failure(Exception("Tactical consultation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Live tactical consult failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to consult Gemini on board state."))
        }
    }

    suspend fun runPostMatchMistakeAutopsy(
        game: GameType,
        outcome: String,
        finalScore: Int,
        difficultyLevel: Int,
        criticalStateSignature: String,
        criticalStateDescription: String,
        criticalAction: String,
        scoreDelta: Int,
        mistakesCommitted: Int
    ): Result<PostMatchAutopsyResult> {
        val prompt = """
            You are ApexMind's self-reflection engine analyzing a completed match of ${game.title}.
            Match Outcome: ${outcome.uppercase()}
            Final Score: $finalScore | Difficulty Tier: $difficultyLevel/10 | Mistakes Committed: $mistakesCommitted
            Critical Turning-Point State: [$criticalStateSignature] — $criticalStateDescription
            Action Taken in Turning-Point State: $criticalAction (Immediate Score Delta: $scoreDelta)

            Diagnose why this match ended in a $outcome and formulate a permanent lesson for the State-Action Memory Bank so the AI improves on future encounters with [$criticalStateSignature].
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                autopsyModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty autopsy response.")
            val json = JSONObject(jsonText)
            Result.success(
                PostMatchAutopsyResult(
                    reflectionSummary = json.getString("reflectionSummary"),
                    criticalLesson = json.getString("criticalLesson"),
                    recommendedQDelta = json.optInt("recommendedQDelta", if (outcome == "win") 45 else -55).coerceIn(-95, 95),
                    aggressionAdjustment = json.optInt("aggressionAdjustment", 0).coerceIn(-15, 15),
                    defenseAdjustment = json.optInt("defenseAdjustment", 4).coerceIn(-15, 15),
                    lookaheadAdjustment = json.optInt("lookaheadAdjustment", 5).coerceIn(-15, 15),
                    trapAwarenessAdjustment = json.optInt("trapAwarenessAdjustment", 6).coerceIn(-15, 15)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Post-match autopsy timed out", e)
            Result.failure(Exception("Post-match reflection timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Post-match autopsy failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Post-match AI reflection failed."))
        }
    }
}
