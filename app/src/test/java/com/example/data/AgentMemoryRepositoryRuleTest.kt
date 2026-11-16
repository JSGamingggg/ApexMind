package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AgentMemoryRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveGameProfile_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = AgentMemoryRepository(firestore)

        val profile = GameProfile(
            userId = aliceUid,
            gameId = "neural_2048",
            difficultyLevel = 4L,
            autoAdjustDifficulty = true,
            targetWinRate = 65L,
            totalWins = 8L,
            totalLosses = 3L,
            recentOutcomes = listOf("win", "win", "loss", "win"),
            bestScore = 2048L,
            aggressionWeight = 60L,
            defenseWeight = 75L,
            lookaheadWeight = 80L,
            trapAwarenessWeight = 70L,
            lastAdjustmentReason = "Promoted to Tier 4 after 72% win rate."
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveOrUpdateGameProfile(profile)
        }
        assertTrue("Expected saveOrUpdateGameProfile to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val emittedProfiles = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGameProfiles().first { list -> list.any { it.gameId == "neural_2048" } }
        }
        val fetched = emittedProfiles.first { it.gameId == "neural_2048" }
        assertEquals(4L, fetched.difficultyLevel)
        assertEquals(8L, fetched.totalWins)

        val toggledResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleStrategicObjective(
                gameId = "neural_2048",
                objectiveText = "Maintain corner lock",
                baseProfile = fetched
            )
        }
        assertTrue("Expected toggleStrategicObjective to succeed: ${toggledResult.exceptionOrNull()}", toggledResult.isSuccess)
        val updatedProfiles = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGameProfiles().first { list ->
                list.firstOrNull { it.gameId == "neural_2048" }?.completedObjectives?.contains("Maintain corner lock") == true
            }
        }
        assertTrue(updatedProfiles.first { it.gameId == "neural_2048" }.completedObjectives.contains("Maintain corner lock"))
    }

    @Test
    fun upsertStateMemoryAndAddResearch_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = AgentMemoryRepository(firestore)

        val memory = StateMemory(
            userId = aliceUid,
            gameId = "tactical_grid",
            stateSignature = "GRID_CENTER_OPEN_TRAP_NE",
            stateDescription = "Center energy node open, enemy trap suspected at (1,3)",
            actionTaken = "CAPTURE_2_2",
            scoreDelta = 25L,
            outcome = "optimal",
            qValueScore = 90L,
            lessonLearned = "Securing (2,2) controls 4 adjacent synergy lanes.",
            difficultyLevel = 3L,
            timesRecalled = 1L
        )

        val memIdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertStateMemory(memory)
        }
        assertTrue(memIdResult.isSuccess)
        val memId = memIdResult.getOrThrow()

        val memories = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStateMemories().first { list -> list.any { it.id == memId } }
        }
        assertTrue(memories.any { it.stateSignature == "GRID_CENTER_OPEN_TRAP_NE" })
    }

    @Test
    fun getGameProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = AgentMemoryRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveOrUpdateGameProfile(
                GameProfile(userId = aliceUid, gameId = "cyber_snake", difficultyLevel = 2L)
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = AgentMemoryRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getGameProfileByUser(aliceUid, "cyber_snake")
        }
        assertTrue("Expected cross-user read to fail", crossResult.isFailure)
        val firestoreEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun getGameProfile_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = AgentMemoryRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveOrUpdateGameProfile(
                GameProfile(userId = aliceUid, gameId = "tactical_grid", difficultyLevel = 1L)
            ).getOrThrow()
        }

        auth.signOut()
        val unauthResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getGameProfileByUser(aliceUid, "tactical_grid")
        }
        assertTrue("Expected unauthenticated read to fail", unauthResult.isFailure)
        val firestoreEx = unauthResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@apexmind.test"
        const val BOB_EMAIL = "bob@apexmind.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
