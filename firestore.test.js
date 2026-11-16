const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfilePayload(userId, gameId, now) {
  return {
    userId,
    gameId,
    difficultyLevel: 3,
    autoAdjustDifficulty: true,
    targetWinRate: 65,
    totalWins: 5,
    totalLosses: 2,
    recentOutcomes: ["win", "loss", "win"],
    bestScore: 1450,
    aggressionWeight: 60,
    defenseWeight: 70,
    lookaheadWeight: 75,
    trapAwarenessWeight: 80,
    lastAdjustmentReason: "Promoted to Tier 3 after 71% win rate over last 7 matches.",
    createdAt: now,
    updatedAt: now,
  };
}

function validMemoryPayload(userId, now) {
  return {
    userId,
    gameId: "neural_2048",
    stateSignature: "CORNER_LOCKED_MAX_512_ROW0_FULL",
    stateDescription: "512 anchor in top-left corner, top row full, right merge open",
    actionTaken: "LEFT",
    scoreDelta: 64,
    outcome: "optimal",
    qValueScore: 88,
    lessonLearned: "Sliding LEFT preserves top-left anchor and merges 32 tiles safely.",
    difficultyLevel: 4,
    timesRecalled: 2,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or write game profiles or memories", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("game_profiles").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("state_memories").get()
  );
});

test("Authenticated owner: can create, read, list, and update own GameProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("game_profiles")
    .doc("neural_2048");

  await assertSucceeds(docRef.set(validProfilePayload(ALICE_UID, "neural_2048", now)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("game_profiles").get()
  );
  await assertSucceeds(
    docRef.update({
      difficultyLevel: 4,
      totalWins: 6,
      lastAdjustmentReason: "Win rate reached 75%, promoted to Tier 4.",
      updatedAt: new Date(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's state memories or research", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("state_memories")
      .doc("mem_1")
      .set(validMemoryPayload(ALICE_UID, now));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("state_memories")
      .doc("mem_1")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("state_memories").get()
  );
});

test("Shadow Update & Value Poisoning Guard: rejects extra ghost fields or out-of-range difficulty", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("game_profiles")
    .doc("tactical_grid");

  await assertFails(
    docRef.set({
      ...validProfilePayload(ALICE_UID, "tactical_grid", now),
      isAdminGhostField: true,
    })
  );

  await assertFails(
    docRef.set({
      ...validProfilePayload(ALICE_UID, "tactical_grid", now),
      difficultyLevel: 99,
    })
  );
});
