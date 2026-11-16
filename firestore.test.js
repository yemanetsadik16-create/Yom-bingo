const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_UID = "admin_uid_999";
const ADMIN_EMAIL = "desbelebisrat12@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTs() {
  return new Date(Date.now() - 2000);
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

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("rounds").where("cycle", "==", 1).get());
  await assertFails(unauthDb.collection("payment_requests").where("userId", "==", ALICE_UID).get());
});

test("User creation: owner can create profile with wallet 0, cannot mint initial wallet balance", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Alice",
      phone: "0911223344",
      email: "alice@example.com",
      wallet: 0,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );

  // Attempting to create with wallet = 500 must fail
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "Bob",
      phone: "0922334455",
      email: "bob@example.com",
      wallet: 500,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );
});

test("PII & Shadow Update: Bob cannot read Alice profile or inject ghost fields", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Alice",
      phone: "0911223344",
      email: "alice@example.com",
      wallet: 100,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  // Ghost field injection fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      username: "AliceNew",
      isVerified: true,
      updatedAt: pastTs(),
    })
  );
  // Arbitrary wallet credit fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      wallet: 9999,
      updatedAt: pastTs(),
    })
  );
  // Valid profile update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      username: "AliceNew",
      phone: "0911000000",
      updatedAt: pastTs(),
    })
  );
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Alice",
      phone: "0911223344",
      email: "alice@example.com",
      wallet: 100,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    });
  });

  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(spoofedAdminDb.collection("users").get());
  await assertFails(
    spoofedAdminDb.collection("users").doc(ALICE_UID).update({
      wallet: 500,
      updatedAt: pastTs(),
    })
  );

  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(realAdminDb.collection("users").get());
  await assertSucceeds(
    realAdminDb.collection("users").doc(ALICE_UID).update({
      wallet: 500,
      updatedAt: pastTs(),
    })
  );
});

test("Atomic room join, win payout, and terminal state lock", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Alice",
      phone: "0911223344",
      email: "alice@example.com",
      wallet: 100,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    });
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "Bob",
      phone: "0922334455",
      email: "bob@example.com",
      wallet: 100,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const roundId = "c100_10";

  // Alice creates round + claims ticket #5 + deducts 10 ETB in atomic batch
  const batchA = aliceDb.batch();
  batchA.set(aliceDb.collection("rounds").doc(roundId), {
    cycle: 100,
    stake: 10,
    maxTickets: 30,
    playerCount: 1,
    status: "waiting",
    winnerUid: "",
    winnerName: "",
    winnerTicket: 0,
    winnerCalls: 0,
    winnerPrize: 0,
    createdAt: pastTs(),
    updatedAt: pastTs(),
  });
  batchA.set(aliceDb.collection("rounds").doc(roundId).collection("tickets").doc("5"), {
    roundId,
    ticketNumber: 5,
    userId: ALICE_UID,
    username: "Alice",
    stake: 10,
    refunded: false,
    createdAt: pastTs(),
  });
  batchA.update(aliceDb.collection("users").doc(ALICE_UID), {
    wallet: 90,
    updatedAt: pastTs(),
  });
  await assertSucceeds(batchA.commit());

  // Bob joins round + claims ticket #9 + deducts 10 ETB
  const batchB = bobDb.batch();
  batchB.update(bobDb.collection("rounds").doc(roundId), {
    playerCount: 2,
    updatedAt: pastTs(),
  });
  batchB.set(bobDb.collection("rounds").doc(roundId).collection("tickets").doc("9"), {
    roundId,
    ticketNumber: 9,
    userId: BOB_UID,
    username: "Bob",
    stake: 10,
    refunded: false,
    createdAt: pastTs(),
  });
  batchB.update(bobDb.collection("users").doc(BOB_UID), {
    wallet: 90,
    updatedAt: pastTs(),
  });
  await assertSucceeds(batchB.commit());

  // Query alignment test for rounds and tickets
  await assertSucceeds(aliceDb.collection("rounds").where("cycle", "==", 100).get());
  await assertSucceeds(aliceDb.collection("rounds").doc(roundId).collection("tickets").where("roundId", "==", roundId).get());

  // Alice wins: pool = 2 * 10 = 20, prize = floor(20 * 0.8) = 16 -> wallet 90 + 16 = 106
  const winBatch = aliceDb.batch();
  winBatch.update(aliceDb.collection("rounds").doc(roundId), {
    status: "finished",
    winnerUid: ALICE_UID,
    winnerName: "Alice",
    winnerTicket: 5,
    winnerCalls: 14,
    winnerPrize: 16,
    updatedAt: pastTs(),
  });
  winBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    wallet: 106,
    lastWinRoundId: roundId,
    updatedAt: pastTs(),
  });
  await assertSucceeds(winBatch.commit());

  // Bob cannot overwrite finished round (Terminal State Lock)
  const bobWinBatch = bobDb.batch();
  bobWinBatch.update(bobDb.collection("rounds").doc(roundId), {
    status: "finished",
    winnerUid: BOB_UID,
    winnerName: "Bob",
    winnerTicket: 9,
    winnerCalls: 15,
    winnerPrize: 16,
    updatedAt: pastTs(),
  });
  bobWinBatch.update(bobDb.collection("users").doc(BOB_UID), {
    wallet: 106,
    lastWinRoundId: roundId,
    updatedAt: pastTs(),
  });
  await assertFails(bobWinBatch.commit());
});

test("Solo round refund succeeds only once when playerCount < 2", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Alice",
      phone: "0911223344",
      email: "alice@example.com",
      wallet: 80,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    });
    await db.collection("rounds").doc("c200_20").set({
      cycle: 200,
      stake: 20,
      maxTickets: 60,
      playerCount: 1,
      status: "waiting",
      winnerUid: "",
      winnerName: "",
      winnerTicket: 0,
      winnerCalls: 0,
      winnerPrize: 0,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    });
    await db.collection("rounds").doc("c200_20").collection("tickets").doc("3").set({
      roundId: "c200_20",
      ticketNumber: 3,
      userId: ALICE_UID,
      username: "Alice",
      stake: 20,
      refunded: false,
      createdAt: pastTs(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const refundBatch = aliceDb.batch();
  refundBatch.update(aliceDb.collection("rounds").doc("c200_20"), {
    status: "refunded",
    updatedAt: pastTs(),
  });
  refundBatch.update(aliceDb.collection("rounds").doc("c200_20").collection("tickets").doc("3"), {
    refunded: true,
  });
  refundBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    wallet: 100,
    lastRefundRoundId: "c200_20",
    lastRefundTicketId: "3",
    updatedAt: pastTs(),
  });
  await assertSucceeds(refundBatch.commit());

  // Second refund attempt must fail
  const secondRefund = aliceDb.batch();
  secondRefund.update(aliceDb.collection("users").doc(ALICE_UID), {
    wallet: 120,
    lastRefundRoundId: "c200_20",
    lastRefundTicketId: "3",
    updatedAt: pastTs(),
  });
  await assertFails(secondRefund.commit());
});

test("Payment and withdrawal requests: user isolated, admin approves", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Alice",
      phone: "0911223344",
      email: "alice@example.com",
      wallet: 100,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();

  // Alice submits payment request
  await assertSucceeds(
    aliceDb.collection("payment_requests").doc("pay_1").set({
      userId: ALICE_UID,
      username: "Alice",
      phone: "0911223344",
      amount: 100,
      receiptId: "ABC1234567",
      receiptLink: "https://transactioninfo.ethiotelecom.et/receipt/ABC1234567",
      status: "pending",
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );

  // Bob cannot read or approve Alice's payment request
  await assertFails(bobDb.collection("payment_requests").doc("pay_1").get());
  await assertFails(
    aliceDb.collection("payment_requests").doc("pay_1").update({
      status: "approved",
      updatedAt: pastTs(),
    })
  );

  // Admin approves payment request
  await assertSucceeds(
    adminDb.collection("payment_requests").doc("pay_1").update({
      status: "approved",
      amount: 100,
      updatedAt: pastTs(),
    })
  );

  // Alice submits withdrawal with atomic wallet deduction (100 - 50 = 50)
  const wdBatch = aliceDb.batch();
  wdBatch.set(aliceDb.collection("withdrawal_requests").doc("wd_1"), {
    userId: ALICE_UID,
    username: "Alice",
    phone: "0911223344",
    amount: 50,
    telebirrAccount: "0911223344",
    status: "pending",
    createdAt: pastTs(),
    updatedAt: pastTs(),
  });
  wdBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    wallet: 50,
    updatedAt: pastTs(),
  });
  await assertSucceeds(wdBatch.commit());
});
