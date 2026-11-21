const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_trader_01";
const BOB_UID = "bob_trader_02";
const ADMIN_UID = "admin_patricia_01";
const ADMIN_EMAIL = "patriciamiglioranza4@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(ms = 5000) {
  return new Date(Date.now() - ms);
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

function buildUserProfilePayload(uid, email = "alice@example.com") {
  const now = pastDate();
  return {
    userId: uid,
    email,
    displayName: "Alice Investor",
    accountTier: "LIVE_UNVERIFIED",
    activeMode: "LIVE",
    kycStatus: "NOT_SUBMITTED",
    liveAvailableBalance: 0,
    liveLockedBalance: 0,
    demoAvailableBalance: 10000,
    accountFrozen: false,
    twoFactorEnabled: false,
    biometricEnabled: false,
    dripEnabled: true,
    whitelistedAddresses: [],
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated requests are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("deposit_wallets").get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("kyc_submissions").doc(ALICE_UID).set({ userId: ALICE_UID }));
});

test("User provisioning & profile security bounds (cannot self-credit live balance or fake KYC)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  // Valid Kotlin create payload
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(buildUserProfilePayload(ALICE_UID))
  );

  // Cannot create profile with non-zero liveAvailableBalance
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      ...buildUserProfilePayload(BOB_UID),
      userId: BOB_UID,
      liveAvailableBalance: 5000,
    })
  );

  // Unverified user cannot mutate liveAvailableBalance directly
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      liveAvailableBalance: 99999,
      updatedAt: pastDate(1000),
    })
  );

  // Unverified user cannot self-approve KYC
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      kycStatus: "APPROVED",
      accountTier: "LIVE_VERIFIED",
      updatedAt: pastDate(1000),
    })
  );

  // Allowed mode switch + demo balance trade update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeMode: "DEMO",
      demoAvailableBalance: 8500.5,
      updatedAt: pastDate(500),
    })
  );
});

test("Cross-user isolation on profiles, watchlists, KYC, deposits, orders, positions, ledger", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const t = pastDate(10000);
    await db.collection("users").doc(BOB_UID).set(buildUserProfilePayload(BOB_UID, "bob@example.com"));
    await db.collection("users").doc(BOB_UID).collection("watchlists").doc("core").set({
      userId: BOB_UID,
      name: "Bob WL",
      symbols: ["BTC"],
      createdAt: t,
      updatedAt: t,
    });
    await db.collection("orders").doc("bob_order_1").set({
      userId: BOB_UID,
      symbol: "BTC",
      assetName: "Bitcoin",
      assetClass: "CRYPTO",
      side: "BUY",
      orderType: "MARKET",
      timeInForce: "GTC",
      tradeMode: "DEMO",
      quantity: 0.1,
      notionalUsd: 9000,
      executedPrice: 90000,
      feeUsd: 9,
      realizedPnl: 0,
      status: "FILLED",
      createdAt: t,
      updatedAt: t,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("watchlists").get());
  await assertFails(aliceDb.collection("orders").doc("bob_order_1").get());
  await assertFails(aliceDb.collection("orders").get()); // unfiltered list denied
});

test("Bootstrapped Admin email_verified vs spoofed email security check", async () => {
  // Spoofed admin with email_verified: false MUST fail
  const spoofedAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(spoofedAdminDb.collection("users").get());
  await assertFails(
    spoofedAdminDb.collection("deposit_wallets").doc("BTC_BTC").set({
      symbol: "BTC",
      network: "BTC",
      address: "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
      minDeposit: 25,
      confirmationsRequired: 3,
      active: true,
      qrPayload: "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
      updatedBy: ADMIN_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Verified bootstrapped admin succeeds
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(realAdminDb.collection("users").get());
  await assertSucceeds(
    realAdminDb.collection("deposit_wallets").doc("BTC_BTC").set({
      symbol: "BTC",
      network: "BTC",
      address: "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
      minDeposit: 25,
      confirmationsRequired: 3,
      active: true,
      qrPayload: "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
      updatedBy: ADMIN_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Non-admin user can read deposit_wallets but cannot write/delete them
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("deposit_wallets").get());
  await assertFails(aliceDb.collection("deposit_wallets").doc("BTC_BTC").delete());
});

test("End-to-end KYC, Deposit, Withdrawal Lock, Orders, Positions & Ledger parity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const t0 = pastDate(10000);

  // 1. Alice creates profile & watchlist
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(buildUserProfilePayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("watchlists").doc("core_watchlist").set({
      userId: ALICE_UID,
      name: "Core Watchlist",
      symbols: ["BTC", "NVDA"],
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // 2. Alice submits KYC + updates user status to PENDING
  await assertSucceeds(
    aliceDb.collection("kyc_submissions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Investor",
      dateOfBirth: "1994-05-18",
      nationality: "United States",
      taxResidency: "United States",
      taxIdLast4: "4821",
      addressLine: "450 Lexington Ave, New York, NY",
      riskTolerance: "MODERATE",
      employmentStatus: "Employed",
      annualIncomeRange: "$100,000 - $250,000",
      idType: "PASSPORT",
      status: "PENDING",
      createdAt: t0,
      updatedAt: t0,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      kycStatus: "PENDING",
      accountTier: "LIVE_PENDING_KYC",
      updatedAt: pastDate(4000),
    })
  );

  // 3. Admin approves KYC
  await assertSucceeds(
    adminDb.collection("kyc_submissions").doc(ALICE_UID).update({
      status: "APPROVED",
      reviewedBy: ADMIN_UID,
      updatedAt: pastDate(3000),
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      kycStatus: "APPROVED",
      accountTier: "LIVE_VERIFIED",
      updatedAt: pastDate(3000),
    })
  );

  // 4. Alice submits deposit request & Admin approves + credits $1,000.10
  await assertSucceeds(
    aliceDb.collection("deposit_requests").doc("dep_1").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      assetSymbol: "USDT",
      network: "TRC20",
      walletAddress: "TX8nK9pLmQ2vRz7wJ4sYdF6hBc3aE9mN5u",
      amountUsd: 1000.1,
      cryptoAmount: 1000.1,
      txHash: "0xabc123def456",
      status: "PENDING",
      createdAt: t0,
      updatedAt: t0,
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      liveAvailableBalance: 1000.1,
      updatedAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    adminDb.collection("deposit_requests").doc("dep_1").update({
      status: "APPROVED",
      adminNote: "Verified",
      reviewedBy: ADMIN_UID,
      updatedAt: pastDate(2000),
    })
  );

  // 5. Alice locks $200.20 for withdrawal (checks IEEE-754 conservation rule)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      liveAvailableBalance: 799.9,
      liveLockedBalance: 200.2,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("withdrawal_requests").doc("wd_1").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      assetSymbol: "USDT",
      network: "TRC20",
      destinationAddress: "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb",
      amountUsd: 200.2,
      feeUsd: 2.0,
      netAmountUsd: 198.2,
      status: "PENDING",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // 6. Alice places order, opens position, and queries her own filtered collections
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1").set({
      userId: ALICE_UID,
      symbol: "AAPL",
      assetName: "Apple Inc.",
      assetClass: "STOCK",
      side: "BUY",
      orderType: "LIMIT",
      timeInForce: "GTC",
      tradeMode: "LIVE",
      quantity: 2,
      notionalUsd: 450,
      limitPrice: 225,
      executedPrice: 0,
      feeUsd: 0,
      realizedPnl: 0,
      status: "OPEN",
      createdAt: t0,
      updatedAt: t0,
    })
  );
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1").update({
      limitPrice: 226,
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("positions").doc(`${ALICE_UID}_LIVE_AAPL`).set({
      userId: ALICE_UID,
      tradeMode: "LIVE",
      symbol: "AAPL",
      assetName: "Apple Inc.",
      assetClass: "STOCK",
      quantity: 1,
      avgEntryPrice: 225,
      costBasis: 225,
      realizedPnl: 0,
      dripEnabled: true,
      createdAt: t0,
      updatedAt: t0,
    })
  );
  await assertSucceeds(
    aliceDb.collection("ledger_entries").doc("led_1").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      entryType: "WITHDRAWAL_LOCK",
      tradeMode: "LIVE",
      amountDelta: -200.2,
      balanceAfter: 799.9,
      referenceId: "wd_1",
      note: "Locked withdrawal",
      actorUid: ALICE_UID,
      createdAt: pastDate(500),
    })
  );
  // Immutable ledger cannot be updated or deleted
  await assertFails(aliceDb.collection("ledger_entries").doc("led_1").update({ amountDelta: 5000 }));
  await assertFails(aliceDb.collection("ledger_entries").doc("led_1").delete());

  // Exact repository queries succeed
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("positions").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("deposit_requests").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("withdrawal_requests").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("ledger_entries").where("userId", "==", ALICE_UID).get());
});
