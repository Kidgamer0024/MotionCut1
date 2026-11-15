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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserProfile(uid, ts) {
  return {
    userId: uid,
    displayName: "Creator " + uid,
    photoUrl: "https://example.com/avatar.png",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validCloudProject(uid, ts, overrides = {}) {
  return {
    userId: uid,
    title: "Cyberpunk Neon Edit",
    aspectRatioName: "PORTRAIT_9_16",
    aspectRatioRatioText: "9:16",
    frameRateFps: 60,
    resolutionLabel: "1080p Full HD",
    durationMs: 12000,
    thumbnailColorHex: 0xff141424,
    clipCount: 3,
    trackCount: 2,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

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

// --- SECURITY BOUNDS: UNAUTHENTICATED ACCESS ---

test("Unauthenticated user: cannot read or write user profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .set({ userId: ALICE_UID, displayName: "Anon" })
  );
});

test("Unauthenticated user: cannot list or read cloud projects", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validCloudProject(ALICE_UID, new Date(Date.now() - 1000)));
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .get()
  );
});

// --- SECURITY BOUNDS: CROSS-USER ISOLATION ---

test("Authenticated user: cannot read, create, update, or delete another user's profile or projects", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .collection("users")
      .doc(BOB_UID)
      .set(validUserProfile(BOB_UID, new Date(Date.now() - 1000)));
    await db
      .collection("users")
      .doc(BOB_UID)
      .collection("projects")
      .doc("bob_proj_1")
      .set(validCloudProject(BOB_UID, new Date(Date.now() - 1000)));
  });

  const aliceCtx = testEnv.authenticatedContext(ALICE_UID);
  const aliceDb = aliceCtx.firestore();

  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("projects").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("projects")
      .doc("bob_proj_1")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("projects")
      .doc("bob_proj_1")
      .update({ title: "Hacked" })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("projects")
      .doc("bob_proj_1")
      .delete()
  );
});

test("Authenticated user: cannot list top-level users collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").get());
});

// --- SCHEMA & ANTI-SPOOFING VALIDATION ---

test("Authenticated owner: can create and update their own UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, ts))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Director",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated owner: rejects UserProfile with mismatched userId or unknown fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validUserProfile(ALICE_UID, ts),
      userId: BOB_UID,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validUserProfile(ALICE_UID, ts),
      isAdmin: true,
    })
  );
});

test("Authenticated owner: can create, list, rename, update, and delete CloudProject", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("projects")
    .doc("proj_cyber_01");

  const ts = new Date(Date.now() - 2000);
  await assertSucceeds(projRef.set(validCloudProject(ALICE_UID, ts)));

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );

  // Rename action
  await assertSucceeds(
    projRef.update({
      title: "Neo-Tokyo Director Cut",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Full metadata sync update
  await assertSucceeds(
    projRef.update({
      durationMs: 18500,
      clipCount: 6,
      trackCount: 3,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(projRef.delete());
});

test("Authenticated owner: rejects CloudProject with immutable field mutation, ghost fields, invalid ID, or invalid bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("projects");
  const ts = new Date(Date.now() - 2000);

  // Invalid document ID containing disallowed characters
  await assertFails(
    projCol.doc("invalid.id$!").set(validCloudProject(ALICE_UID, ts))
  );

  // Missing required field
  const incomplete = validCloudProject(ALICE_UID, ts);
  delete incomplete.title;
  await assertFails(projCol.doc("proj_bad_1").set(incomplete));

  // Ghost field ("id" or extra field)
  await assertFails(
    projCol
      .doc("proj_bad_2")
      .set(validCloudProject(ALICE_UID, ts, { id: "proj_bad_2" }))
  );

  // Out-of-bounds frameRateFps
  await assertFails(
    projCol
      .doc("proj_bad_3")
      .set(validCloudProject(ALICE_UID, ts, { frameRateFps: 999 }))
  );

  // Future timestamp
  await assertFails(
    projCol.doc("proj_bad_4").set(
      validCloudProject(ALICE_UID, ts, {
        createdAt: new Date(Date.now() + 600000),
      })
    )
  );

  // Seed valid project, then attempt mutating createdAt and userId
  const validRef = projCol.doc("proj_valid_1");
  await assertSucceeds(validRef.set(validCloudProject(ALICE_UID, ts)));

  await assertFails(
    validRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    validRef.update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
