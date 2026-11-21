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
const ADMIN_EMAIL = "albertocassoma200@gmail.com";

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("templates").get());
  await assertFails(unauthDb.collection("projects").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("user_credits").doc(ALICE_UID).get());
});

test("User profile isolation and privilege escalation guard", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 1000);

  // Self-assigning admin role must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      role: "admin",
      blocked: false,
      themeMode: "dark",
      language: "pt",
      notificationsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid user creation succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      role: "user",
      blocked: false,
      themeMode: "dark",
      language: "pt",
      notificationsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Non-admin cannot list all users
  await assertFails(aliceDb.collection("users").get());

  // Bob cannot read Alice's profile
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("User credits wallet prevents arbitrary balance tampering and enforces +2 / -2 / -4 rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Arbitrary starting balance fails
  await assertFails(
    aliceDb.collection("user_credits").doc(ALICE_UID).set({
      userId: ALICE_UID,
      balance: 9999,
      totalEarned: 9999,
      totalSpent: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Fixed welcome balance 50 succeeds
  await assertSucceeds(
    aliceDb.collection("user_credits").doc(ALICE_UID).set({
      userId: ALICE_UID,
      balance: 50,
      totalEarned: 50,
      totalSpent: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reward ad +2 succeeds
  await assertSucceeds(
    aliceDb.collection("user_credits").doc(ALICE_UID).update({
      balance: 52,
      totalEarned: 52,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Image export -2 succeeds
  await assertSucceeds(
    aliceDb.collection("user_credits").doc(ALICE_UID).update({
      balance: 50,
      totalSpent: 2,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Arbitrary +100 update fails
  await assertFails(
    aliceDb.collection("user_credits").doc(ALICE_UID).update({
      balance: 150,
      totalEarned: 152,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Projects owner isolation and query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("projects").doc("proj_1").set({
      userId: ALICE_UID,
      title: "Luanda Banner",
      category: "Instagram",
      formatName: "Instagram Quadrado",
      width: 1080,
      height: 1080,
      backgroundColorHex: "#0A1628",
      layers: [],
      videoClips: [],
      projectType: "design",
      favorite: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Scoped query succeeds
  await assertSucceeds(aliceDb.collection("projects").where("userId", "==", ALICE_UID).get());
  // Unfiltered query fails
  await assertFails(aliceDb.collection("projects").get());
  // Cross-user read fails
  await assertFails(bobDb.collection("projects").doc("proj_1").get());
});

test("Admin email spoofing blocked when email_verified is false; verified admin succeeds", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_1", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    spoofedAdminDb.collection("categories").doc("cat_1").set({
      name: "Instagram",
      iconKey: "instagram",
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  const realAdminDb = testEnv.authenticatedContext("admin_1", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    realAdminDb.collection("categories").doc("cat_1").set({
      name: "Instagram",
      iconKey: "instagram",
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});
