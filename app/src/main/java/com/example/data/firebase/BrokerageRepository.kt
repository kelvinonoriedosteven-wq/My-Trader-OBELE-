package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AccountTier
import com.example.data.model.AdminRole
import com.example.data.model.AdminRoleType
import com.example.data.model.AuditLog
import com.example.data.model.DepositRequest
import com.example.data.model.DepositWallet
import com.example.data.model.KycStatus
import com.example.data.model.KycSubmission
import com.example.data.model.LedgerEntry
import com.example.data.model.LedgerEntryType
import com.example.data.model.MarketAsset
import com.example.data.model.MarketControl
import com.example.data.model.OrderSide
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PortfolioPosition
import com.example.data.model.PriceAlert
import com.example.data.model.RequestStatus
import com.example.data.model.SupportTicket
import com.example.data.model.TicketStatus
import com.example.data.model.TimeInForce
import com.example.data.model.TradeMode
import com.example.data.model.TradeOrder
import com.example.data.model.UserProfile
import com.example.data.model.Watchlist
import com.example.data.model.WithdrawalRequest
import com.example.data.model.toSafeDocId
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import kotlin.math.max

class BrokerageRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun requireUserEmail(): String =
        auth.currentUser?.email ?: "investor@apexbroker.app"

    fun isBootstrappedAdmin(user: FirebaseUser?): Boolean {
        if (user == null) return false
        return user.isEmailVerified && user.email.equals(ADMIN_EMAIL, ignoreCase = true)
    }

    private fun <T> Flow<T>.guardStream(uid: String, op: OperationType, path: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(userId, OperationType.GET, path)
        )
    }

    fun observeIsAdminDoc(userId: String): Flow<Boolean> = flow {
        require(userId.isNotBlank())
        val path = "admins/$userId"
        emitAll(
            db.collection("admins").document(userId)
                .snapshots()
                .map { it.exists() }
                .guardStream(userId, OperationType.GET, path)
        )
    }

    fun observeWatchlists(userId: String): Flow<List<Watchlist>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/watchlists"
        emitAll(
            db.collection("users").document(userId).collection("watchlists")
                .snapshots()
                .map { snap -> snap.toObjects(Watchlist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(userId, OperationType.LIST, path)
        )
    }

    fun observePriceAlerts(userId: String): Flow<List<PriceAlert>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/price_alerts"
        emitAll(
            db.collection("users").document(userId).collection("price_alerts")
                .snapshots()
                .map { snap -> snap.toObjects(PriceAlert::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(userId, OperationType.LIST, path)
        )
    }

    fun observeMyKyc(userId: String): Flow<KycSubmission?> = flow {
        require(userId.isNotBlank())
        val path = "kyc_submissions/$userId"
        emitAll(
            db.collection("kyc_submissions").document(userId)
                .snapshots()
                .map { it.toObject(KycSubmission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(userId, OperationType.GET, path)
        )
    }

    fun observeDepositWallets(userId: String): Flow<List<DepositWallet>> = flow {
        require(userId.isNotBlank())
        emitAll(
            db.collection("deposit_wallets")
                .snapshots()
                .map { snap -> snap.toObjects(DepositWallet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(userId, OperationType.LIST, "deposit_wallets")
        )
    }

    fun observeMyDeposits(userId: String): Flow<List<DepositRequest>> = flow {
        require(userId.isNotBlank())
        emitAll(
            db.collection("deposit_requests")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DepositRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardStream(userId, OperationType.LIST, "deposit_requests")
        )
    }

    fun observeMyWithdrawals(userId: String): Flow<List<WithdrawalRequest>> = flow {
        require(userId.isNotBlank())
        emitAll(
            db.collection("withdrawal_requests")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardStream(userId, OperationType.LIST, "withdrawal_requests")
        )
    }

    fun observeMyOrders(userId: String): Flow<List<TradeOrder>> = flow {
        require(userId.isNotBlank())
        emitAll(
            db.collection("orders")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TradeOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardStream(userId, OperationType.LIST, "orders")
        )
    }

    fun observeMyPositions(userId: String): Flow<List<PortfolioPosition>> = flow {
        require(userId.isNotBlank())
        emitAll(
            db.collection("positions")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PortfolioPosition::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.quantity > 0.0000001 }
                }
                .guardStream(userId, OperationType.LIST, "positions")
        )
    }

    fun observeMyLedger(userId: String): Flow<List<LedgerEntry>> = flow {
        require(userId.isNotBlank())
        emitAll(
            db.collection("ledger_entries")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LedgerEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardStream(userId, OperationType.LIST, "ledger_entries")
        )
    }

    fun observeMyTickets(userId: String): Flow<List<SupportTicket>> = flow {
        require(userId.isNotBlank())
        emitAll(
            db.collection("support_tickets")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardStream(userId, OperationType.LIST, "support_tickets")
        )
    }

    fun observeMarketControls(userId: String): Flow<List<MarketControl>> = flow {
        require(userId.isNotBlank())
        emitAll(
            db.collection("market_controls")
                .snapshots()
                .map { snap -> snap.toObjects(MarketControl::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(userId, OperationType.LIST, "market_controls")
        )
    }

    // --- ADMIN STREAMS (only collected when verified admin) ---
    fun observeAllUsers(adminUid: String): Flow<List<UserProfile>> = flow {
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(adminUid, OperationType.LIST, "users")
        )
    }

    fun observeAllKyc(adminUid: String): Flow<List<KycSubmission>> = flow {
        emitAll(
            db.collection("kyc_submissions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(KycSubmission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardStream(adminUid, OperationType.LIST, "kyc_submissions")
        )
    }

    fun observeAllDeposits(adminUid: String): Flow<List<DepositRequest>> = flow {
        emitAll(
            db.collection("deposit_requests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(DepositRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardStream(adminUid, OperationType.LIST, "deposit_requests")
        )
    }

    fun observeAllWithdrawals(adminUid: String): Flow<List<WithdrawalRequest>> = flow {
        emitAll(
            db.collection("withdrawal_requests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardStream(adminUid, OperationType.LIST, "withdrawal_requests")
        )
    }

    fun observeAllOrders(adminUid: String): Flow<List<TradeOrder>> = flow {
        emitAll(
            db.collection("orders")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TradeOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardStream(adminUid, OperationType.LIST, "orders")
        )
    }

    fun observeAllTickets(adminUid: String): Flow<List<SupportTicket>> = flow {
        emitAll(
            db.collection("support_tickets")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardStream(adminUid, OperationType.LIST, "support_tickets")
        )
    }

    fun observeAuditLogs(adminUid: String): Flow<List<AuditLog>> = flow {
        emitAll(
            db.collection("audit_logs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .guardStream(adminUid, OperationType.LIST, "audit_logs")
        )
    }

    fun observeAdminRoles(adminUid: String): Flow<List<AdminRole>> = flow {
        emitAll(
            db.collection("admins")
                .snapshots()
                .map { snap -> snap.toObjects(AdminRole::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .guardStream(adminUid, OperationType.LIST, "admins")
        )
    }

    // --- USER MUTATIONS ---
    suspend fun ensureUserProvisioned(user: FirebaseUser, initialMode: TradeMode): Result<Unit> {
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            if (!userDoc.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "email" to (user.email ?: ""),
                    "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "Apex Investor"),
                    "accountTier" to AccountTier.LIVE_UNVERIFIED.name,
                    "activeMode" to initialMode.name,
                    "kycStatus" to KycStatus.NOT_SUBMITTED.name,
                    "liveAvailableBalance" to 0.0,
                    "liveLockedBalance" to 0.0,
                    "demoAvailableBalance" to 10000.0,
                    "accountFrozen" to false,
                    "twoFactorEnabled" to false,
                    "biometricEnabled" to false,
                    "dripEnabled" to true,
                    "whitelistedAddresses" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).set(payload).await()
            }

            val wlSnap = db.collection("users").document(uid).collection("watchlists").get().await()
            if (wlSnap.isEmpty) {
                val defaultWatchlist = mapOf(
                    "userId" to uid,
                    "name" to "Core Watchlist",
                    "symbols" to listOf("BTC", "ETH", "NVDA", "AAPL", "GC=F", "EUR/USD"),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).collection("watchlists")
                    .document("core_watchlist").set(defaultWatchlist).await()
            }

            if (isBootstrappedAdmin(user)) {
                seedAdminDefaults(user)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun seedAdminDefaults(user: FirebaseUser): Result<Unit> {
        return try {
            val adminRef = db.collection("admins").document(user.uid)
            if (!adminRef.get().await().exists()) {
                adminRef.set(
                    mapOf(
                        "uid" to user.uid,
                        "email" to (user.email ?: ADMIN_EMAIL),
                        "role" to AdminRoleType.SUPER_ADMIN.name,
                        "addedBy" to user.uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val walletsSnap = db.collection("deposit_wallets").get().await()
            if (walletsSnap.isEmpty) {
                val defaults = listOf(
                    Triple("USDT", "TRC20", "TX8nK9pLmQ2vRz7wJ4sYdF6hBc3aE9mN5u"),
                    Triple("USDT", "ERC20", "0x742d35Cc6634C0532925a3b844Bc454e4438f44e"),
                    Triple("BTC", "BTC", "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"),
                    Triple("ETH", "ERC20", "0x742d35Cc6634C0532925a3b844Bc454e4438f44e"),
                    Triple("SOL", "SOL", "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU")
                )
                defaults.forEach { (sym, net, addr) ->
                    val id = "${sym}_${net}".toSafeDocId()
                    db.collection("deposit_wallets").document(id).set(
                        mapOf(
                            "symbol" to sym,
                            "network" to net,
                            "address" to addr,
                            "minDeposit" to if (sym == "BTC") 25.0 else 10.0,
                            "confirmationsRequired" to if (sym == "BTC") 3L else 12L,
                            "active" to true,
                            "qrPayload" to addr,
                            "updatedBy" to user.uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "deposit_wallets")
            Result.failure(e)
        }
    }

    suspend fun switchTradeMode(mode: TradeMode): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf("activeMode" to mode.name, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun resetDemoBalance(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("users").document(uid).update(
                mapOf("demoAvailableBalance" to 10000.0, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            val demoPositions = db.collection("positions")
                .whereEqualTo("userId", uid)
                .get().await()
                .documents
                .filter { it.getString("tradeMode") == TradeMode.DEMO.name }
            demoPositions.forEach { it.reference.delete().await() }

            recordLedgerEntry(
                userId = uid,
                userEmail = requireUserEmail(),
                entryType = LedgerEntryType.DEMO_RESET,
                tradeMode = TradeMode.DEMO,
                amountDelta = 10000.0,
                balanceAfter = 10000.0,
                referenceId = "demo_reset_${System.currentTimeMillis()}",
                note = "Virtual demo balance reset to $10,000.00"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateSecuritySettings(
        biometricEnabled: Boolean? = null,
        twoFactorEnabled: Boolean? = null,
        twoFactorPinHash: String? = null,
        dripEnabled: Boolean? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val map = mutableMapOf<String, Any?>("updatedAt" to FieldValue.serverTimestamp())
            if (biometricEnabled != null) map["biometricEnabled"] = biometricEnabled
            if (twoFactorEnabled != null) map["twoFactorEnabled"] = twoFactorEnabled
            if (twoFactorPinHash != null) map["twoFactorPinHash"] = twoFactorPinHash
            if (dripEnabled != null) map["dripEnabled"] = dripEnabled
            db.collection("users").document(uid).update(map.filterValues { it != null }).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun addWhitelistedAddress(currentList: List<String>, newAddress: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val clean = newAddress.trim()
        if (clean.length < 8 || clean.length > 160) {
            return Result.failure(IllegalArgumentException("Wallet address must be 8-160 characters."))
        }
        return try {
            val updated = (currentList + clean).distinct().take(20)
            db.collection("users").document(uid).update(
                mapOf("whitelistedAddresses" to updated, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun toggleWatchlistSymbol(watchlist: Watchlist, symbol: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val wlId = watchlist.id.ifBlank { "core_watchlist" }
        val next = if (watchlist.symbols.contains(symbol)) {
            watchlist.symbols - symbol
        } else {
            (watchlist.symbols + symbol).take(50)
        }
        return try {
            db.collection("users").document(uid).collection("watchlists").document(wlId)
                .update(mapOf("symbols" to next, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/watchlists/$wlId")
            Result.failure(e)
        }
    }

    suspend fun createWatchlist(name: String, initialSymbols: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val clean = name.trim().take(50)
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Watchlist name required"))
        return try {
            val docRef = db.collection("users").document(uid).collection("watchlists").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to clean,
                    "symbols" to initialSymbols.take(50),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/watchlists")
            Result.failure(e)
        }
    }

    suspend fun createPriceAlert(symbol: String, targetPrice: Double, condition: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (targetPrice <= 0) return Result.failure(IllegalArgumentException("Target price must be positive"))
        return try {
            val docRef = db.collection("users").document(uid).collection("price_alerts").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "symbol" to symbol,
                    "targetPrice" to targetPrice,
                    "condition" to condition,
                    "active" to true,
                    "triggered" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/price_alerts")
            Result.failure(e)
        }
    }

    suspend fun deletePriceAlert(alertId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("users").document(uid).collection("price_alerts").document(alertId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/price_alerts/$alertId")
            Result.failure(e)
        }
    }

    suspend fun submitKyc(submission: KycSubmission): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val existing = db.collection("kyc_submissions").document(uid).get().await()
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "fullName" to submission.fullName.trim(),
                "dateOfBirth" to submission.dateOfBirth.trim(),
                "nationality" to submission.nationality.trim(),
                "taxResidency" to submission.taxResidency.trim(),
                "taxIdLast4" to submission.taxIdLast4.trim(),
                "addressLine" to submission.addressLine.trim(),
                "riskTolerance" to submission.riskTolerance,
                "employmentStatus" to submission.employmentStatus.trim(),
                "annualIncomeRange" to submission.annualIncomeRange.trim(),
                "idType" to submission.idType,
                "idFrontBase64" to submission.idFrontBase64,
                "idBackBase64" to submission.idBackBase64,
                "selfieBase64" to submission.selfieBase64,
                "proofOfAddressName" to submission.proofOfAddressName,
                "status" to KycStatus.PENDING.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                db.collection("kyc_submissions").document(uid).set(payload.filterValues { it != null }).await()
            } else {
                payload["rejectionReason"] = null
                db.collection("kyc_submissions").document(uid).update(
                    payload.filterKeys { it != "userId" && it != "createdAt" }.filterValues { it != null }
                ).await()
            }
            db.collection("users").document(uid).update(
                mapOf(
                    "kycStatus" to KycStatus.PENDING.name,
                    "accountTier" to AccountTier.LIVE_PENDING_KYC.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "kyc_submissions/$uid")
            Result.failure(e)
        }
    }

    suspend fun submitDepositRequest(
        wallet: DepositWallet,
        amountUsd: Double,
        cryptoAmount: Double,
        txHash: String,
        proofImageBase64: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (amountUsd < wallet.minDeposit) {
            return Result.failure(IllegalArgumentException("Minimum deposit is $${wallet.minDeposit}"))
        }
        if (txHash.trim().length < 6) {
            return Result.failure(IllegalArgumentException("Enter a valid blockchain TXID / hash"))
        }
        return try {
            val docRef = db.collection("deposit_requests").document()
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "userEmail" to requireUserEmail(),
                "assetSymbol" to wallet.symbol,
                "network" to wallet.network,
                "walletAddress" to wallet.address,
                "amountUsd" to amountUsd,
                "cryptoAmount" to cryptoAmount,
                "txHash" to txHash.trim(),
                "proofImageBase64" to proofImageBase64,
                "status" to RequestStatus.PENDING.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "deposit_requests")
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawalRequest(
        profile: UserProfile,
        assetSymbol: String,
        network: String,
        destinationAddress: String,
        amountUsd: Double
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (profile.accountFrozen) return Result.failure(IllegalStateException("Account is currently frozen."))
        if (profile.kycEnum != KycStatus.APPROVED) {
            return Result.failure(IllegalStateException("Complete KYC verification before withdrawing live funds."))
        }
        if (amountUsd <= 0.0 || amountUsd > profile.liveAvailableBalance) {
            return Result.failure(IllegalArgumentException("Insufficient available live balance."))
        }
        if (destinationAddress.trim().length < 8) {
            return Result.failure(IllegalArgumentException("Enter a valid destination wallet address."))
        }
        val feeUsd = max(2.0, amountUsd * 0.002)
        val netAmountUsd = (amountUsd - feeUsd).coerceAtLeast(0.01)
        return try {
            val newAvailable = (profile.liveAvailableBalance - amountUsd).coerceAtLeast(0.0)
            val newLocked = profile.liveLockedBalance + amountUsd
            db.collection("users").document(uid).update(
                mapOf(
                    "liveAvailableBalance" to newAvailable,
                    "liveLockedBalance" to newLocked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val reqRef = db.collection("withdrawal_requests").document()
            reqRef.set(
                mapOf(
                    "userId" to uid,
                    "userEmail" to profile.email,
                    "assetSymbol" to assetSymbol,
                    "network" to network,
                    "destinationAddress" to destinationAddress.trim(),
                    "amountUsd" to amountUsd,
                    "feeUsd" to feeUsd,
                    "netAmountUsd" to netAmountUsd,
                    "status" to RequestStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLedgerEntry(
                userId = uid,
                userEmail = profile.email,
                entryType = LedgerEntryType.WITHDRAWAL_LOCK,
                tradeMode = TradeMode.LIVE,
                amountDelta = -amountUsd,
                balanceAfter = newAvailable,
                referenceId = reqRef.id,
                note = "Locked $assetSymbol ($network) withdrawal request"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "withdrawal_requests")
            Result.failure(e)
        }
    }

    suspend fun placeOrder(
        profile: UserProfile,
        asset: MarketAsset,
        side: OrderSide,
        orderType: OrderType,
        timeInForce: TimeInForce,
        quantity: Double,
        limitPrice: Double?,
        stopPrice: Double?,
        takeProfitPrice: Double?,
        marketControl: MarketControl?,
        currentPosition: PortfolioPosition?
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (profile.accountFrozen) return Result.failure(IllegalStateException("Account is frozen."))
        if (marketControl != null && !marketControl.tradingEnabled) {
            return Result.failure(IllegalStateException("Trading is currently halted for ${asset.symbol} by Risk Admin."))
        }
        val mode = profile.modeEnum
        if (mode == TradeMode.LIVE && profile.kycEnum != KycStatus.APPROVED) {
            return Result.failure(IllegalStateException("KYC verification required for Live trading. Switch to Demo Mode or complete KYC."))
        }
        if (quantity <= 0.0) return Result.failure(IllegalArgumentException("Quantity must be greater than zero."))

        val spreadMult = (marketControl?.spreadBps ?: 4.0) / 10000.0
        val execPrice = when (side) {
            OrderSide.BUY -> asset.price * (1.0 + spreadMult)
            OrderSide.SELL -> asset.price * (1.0 - spreadMult)
        }
        val notional = quantity * execPrice
        val feeRate = (marketControl?.feeBps ?: 10.0) / 10000.0
        val feeUsd = notional * feeRate
        val activeBalance = if (mode == TradeMode.LIVE) profile.liveAvailableBalance else profile.demoAvailableBalance

        if (side == OrderSide.BUY && (notional + feeUsd) > activeBalance) {
            return Result.failure(IllegalArgumentException("Insufficient ${mode.name} buying power (need $${String.format("%.2f", notional + feeUsd)})."))
        }
        if (side == OrderSide.SELL && (currentPosition == null || currentPosition.quantity + 1e-8 < quantity)) {
            return Result.failure(IllegalArgumentException("Insufficient ${asset.symbol} position to sell."))
        }

        val immediateFill = orderType == OrderType.MARKET
        return try {
            val orderRef = db.collection("orders").document()
            var realizedPnl = 0.0
            if (immediateFill) {
                if (side == OrderSide.BUY) {
                    val newBalance = (activeBalance - (notional + feeUsd)).coerceAtLeast(0.0)
                    updateModeBalance(uid, mode, newBalance)
                    upsertPositionAfterBuy(uid, mode, asset, quantity, execPrice, profile.dripEnabled, currentPosition)
                    recordLedgerEntry(
                        userId = uid,
                        userEmail = profile.email,
                        entryType = LedgerEntryType.TRADE_BUY,
                        tradeMode = mode,
                        amountDelta = -(notional + feeUsd),
                        balanceAfter = newBalance,
                        referenceId = orderRef.id,
                        note = "Bought ${String.format("%.4f", quantity)} ${asset.symbol} @ ${String.format("%.2f", execPrice)}"
                    )
                } else {
                    val pos = currentPosition!!
                    val costRemoved = pos.avgEntryPrice * quantity
                    realizedPnl = (notional - feeUsd) - costRemoved
                    val newBalance = activeBalance + (notional - feeUsd)
                    updateModeBalance(uid, mode, newBalance)
                    reducePositionAfterSell(uid, mode, pos, quantity, realizedPnl)
                    recordLedgerEntry(
                        userId = uid,
                        userEmail = profile.email,
                        entryType = LedgerEntryType.TRADE_SELL,
                        tradeMode = mode,
                        amountDelta = notional - feeUsd,
                        balanceAfter = newBalance,
                        referenceId = orderRef.id,
                        note = "Sold ${String.format("%.4f", quantity)} ${asset.symbol} (P&L ${String.format("%.2f", realizedPnl)})"
                    )
                }
            }

            val orderPayload = mapOf<String, Any?>(
                "userId" to uid,
                "symbol" to asset.symbol,
                "assetName" to asset.name,
                "assetClass" to asset.assetClass.name,
                "side" to side.name,
                "orderType" to orderType.name,
                "timeInForce" to timeInForce.name,
                "tradeMode" to mode.name,
                "quantity" to quantity,
                "notionalUsd" to notional,
                "limitPrice" to limitPrice,
                "stopPrice" to stopPrice,
                "takeProfitPrice" to takeProfitPrice,
                "executedPrice" to if (immediateFill) execPrice else 0.0,
                "feeUsd" to if (immediateFill) feeUsd else 0.0,
                "realizedPnl" to realizedPnl,
                "status" to if (immediateFill) OrderStatus.FILLED.name else OrderStatus.OPEN.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            orderRef.set(orderPayload).await()
            val statusMsg = if (immediateFill) {
                "${side.name} ${asset.symbol} filled at $${String.format("%.2f", execPrice)} (${mode.name})"
            } else {
                "${orderType.label} order placed for ${asset.symbol} (${mode.name})"
            }
            Result.success(statusMsg)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "orders")
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(order: TradeOrder): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("orders").document(order.id).update(
                mapOf("status" to OrderStatus.CANCELED.name, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/${order.id}")
            Result.failure(e)
        }
    }

    suspend fun modifyOrderLimit(order: TradeOrder, newLimitPrice: Double): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (newLimitPrice <= 0.0) return Result.failure(IllegalArgumentException("Invalid limit price"))
        return try {
            db.collection("orders").document(order.id).update(
                mapOf("limitPrice" to newLimitPrice, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/${order.id}")
            Result.failure(e)
        }
    }

    suspend fun togglePositionDrip(position: PortfolioPosition, enabled: Boolean): Result<Unit> {
        return try {
            db.collection("positions").document(position.id).update(
                mapOf("dripEnabled" to enabled, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "positions/${position.id}")
            Result.failure(e)
        }
    }

    suspend fun submitSupportTicket(subject: String, category: String, message: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (subject.trim().length < 3 || message.trim().length < 5) {
            return Result.failure(IllegalArgumentException("Please enter a subject and detailed message."))
        }
        return try {
            db.collection("support_tickets").document().set(
                mapOf(
                    "userId" to uid,
                    "userEmail" to requireUserEmail(),
                    "subject" to subject.trim(),
                    "category" to category,
                    "message" to message.trim(),
                    "status" to TicketStatus.OPEN.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "support_tickets")
            Result.failure(e)
        }
    }

    // --- ADMIN MUTATIONS ---
    suspend fun adminApproveKyc(kyc: KycSubmission): Result<Unit> {
        val adminUid = requireUserId()
        return try {
            db.collection("kyc_submissions").document(kyc.id).update(
                mapOf(
                    "status" to KycStatus.APPROVED.name,
                    "reviewedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(kyc.userId).update(
                mapOf(
                    "kycStatus" to KycStatus.APPROVED.name,
                    "accountTier" to AccountTier.LIVE_VERIFIED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog("APPROVE_KYC", "kyc_submissions", kyc.id, "Approved KYC for ${kyc.fullName}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "kyc_submissions/${kyc.id}")
            Result.failure(e)
        }
    }

    suspend fun adminRejectKyc(kyc: KycSubmission, reason: String): Result<Unit> {
        val adminUid = requireUserId()
        val cleanReason = reason.trim().ifBlank { "Document unclear — please re-upload clear ID & selfie." }
        return try {
            db.collection("kyc_submissions").document(kyc.id).update(
                mapOf(
                    "status" to KycStatus.REJECTED.name,
                    "rejectionReason" to cleanReason,
                    "reviewedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(kyc.userId).update(
                mapOf(
                    "kycStatus" to KycStatus.REJECTED.name,
                    "kycRejectionReason" to cleanReason,
                    "accountTier" to AccountTier.LIVE_UNVERIFIED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog("REJECT_KYC", "kyc_submissions", kyc.id, "Rejected KYC: $cleanReason")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "kyc_submissions/${kyc.id}")
            Result.failure(e)
        }
    }

    suspend fun adminApproveDeposit(deposit: DepositRequest): Result<Unit> {
        val adminUid = requireUserId()
        return try {
            val userSnap = db.collection("users").document(deposit.userId).get().await()
            val currentLive = userSnap.getDouble("liveAvailableBalance") ?: 0.0
            val newLive = currentLive + deposit.amountUsd
            db.collection("users").document(deposit.userId).update(
                mapOf("liveAvailableBalance" to newLive, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            db.collection("deposit_requests").document(deposit.id).update(
                mapOf(
                    "status" to RequestStatus.APPROVED.name,
                    "adminNote" to "Verified & credited by Treasury",
                    "reviewedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLedgerEntry(
                userId = deposit.userId,
                userEmail = deposit.userEmail,
                entryType = LedgerEntryType.DEPOSIT_CREDIT,
                tradeMode = TradeMode.LIVE,
                amountDelta = deposit.amountUsd,
                balanceAfter = newLive,
                referenceId = deposit.id,
                note = "Approved ${deposit.assetSymbol} (${deposit.network}) deposit TX ${deposit.txHash.take(12)}"
            )
            writeAuditLog("APPROVE_DEPOSIT", "deposit_requests", deposit.id, "Credited $${deposit.amountUsd} to ${deposit.userEmail}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "deposit_requests/${deposit.id}")
            Result.failure(e)
        }
    }

    suspend fun adminRejectDeposit(deposit: DepositRequest, reason: String): Result<Unit> {
        val adminUid = requireUserId()
        val clean = reason.trim().ifBlank { "Transaction hash could not be verified on-chain." }
        return try {
            db.collection("deposit_requests").document(deposit.id).update(
                mapOf(
                    "status" to RequestStatus.REJECTED.name,
                    "adminNote" to clean,
                    "reviewedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog("REJECT_DEPOSIT", "deposit_requests", deposit.id, "Rejected deposit: $clean")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "deposit_requests/${deposit.id}")
            Result.failure(e)
        }
    }

    suspend fun adminApproveWithdrawal(withdrawal: WithdrawalRequest, payoutTxHash: String): Result<Unit> {
        val adminUid = requireUserId()
        val cleanHash = payoutTxHash.trim()
        if (cleanHash.length < 6) return Result.failure(IllegalArgumentException("Enter payout TX hash."))
        return try {
            val userSnap = db.collection("users").document(withdrawal.userId).get().await()
            val avail = userSnap.getDouble("liveAvailableBalance") ?: 0.0
            val locked = userSnap.getDouble("liveLockedBalance") ?: 0.0
            val newLocked = (locked - withdrawal.amountUsd).coerceAtLeast(0.0)
            db.collection("users").document(withdrawal.userId).update(
                mapOf("liveLockedBalance" to newLocked, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            db.collection("withdrawal_requests").document(withdrawal.id).update(
                mapOf(
                    "status" to RequestStatus.APPROVED.name,
                    "payoutTxHash" to cleanHash,
                    "adminNote" to "Payout dispatched",
                    "reviewedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLedgerEntry(
                userId = withdrawal.userId,
                userEmail = withdrawal.userEmail,
                entryType = LedgerEntryType.WITHDRAWAL_PAYOUT,
                tradeMode = TradeMode.LIVE,
                amountDelta = -withdrawal.amountUsd,
                balanceAfter = avail,
                referenceId = withdrawal.id,
                note = "Withdrawal paid (${withdrawal.assetSymbol} TX ${cleanHash.take(12)})"
            )
            writeAuditLog("APPROVE_WITHDRAWAL", "withdrawal_requests", withdrawal.id, "Paid $${withdrawal.netAmountUsd} to ${withdrawal.destinationAddress}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "withdrawal_requests/${withdrawal.id}")
            Result.failure(e)
        }
    }

    suspend fun adminRejectWithdrawal(withdrawal: WithdrawalRequest, reason: String): Result<Unit> {
        val adminUid = requireUserId()
        val clean = reason.trim().ifBlank { "Withdrawal rejected; locked funds returned to available balance." }
        return try {
            val userSnap = db.collection("users").document(withdrawal.userId).get().await()
            val avail = userSnap.getDouble("liveAvailableBalance") ?: 0.0
            val locked = userSnap.getDouble("liveLockedBalance") ?: 0.0
            val newAvail = avail + withdrawal.amountUsd
            val newLocked = (locked - withdrawal.amountUsd).coerceAtLeast(0.0)
            db.collection("users").document(withdrawal.userId).update(
                mapOf(
                    "liveAvailableBalance" to newAvail,
                    "liveLockedBalance" to newLocked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("withdrawal_requests").document(withdrawal.id).update(
                mapOf(
                    "status" to RequestStatus.REJECTED.name,
                    "adminNote" to clean,
                    "reviewedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordLedgerEntry(
                userId = withdrawal.userId,
                userEmail = withdrawal.userEmail,
                entryType = LedgerEntryType.WITHDRAWAL_REFUND,
                tradeMode = TradeMode.LIVE,
                amountDelta = withdrawal.amountUsd,
                balanceAfter = newAvail,
                referenceId = withdrawal.id,
                note = "Refunded locked withdrawal: $clean"
            )
            writeAuditLog("REJECT_WITHDRAWAL", "withdrawal_requests", withdrawal.id, "Refunded $${withdrawal.amountUsd}: $clean")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "withdrawal_requests/${withdrawal.id}")
            Result.failure(e)
        }
    }

    suspend fun adminAdjustBalance(target: UserProfile, deltaUsd: Double, reason: String): Result<Unit> {
        if (deltaUsd == 0.0) return Result.failure(IllegalArgumentException("Amount must be non-zero."))
        val newBalance = (target.liveAvailableBalance + deltaUsd).coerceAtLeast(0.0)
        val actualDelta = newBalance - target.liveAvailableBalance
        return try {
            db.collection("users").document(target.userId).update(
                mapOf("liveAvailableBalance" to newBalance, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            recordLedgerEntry(
                userId = target.userId,
                userEmail = target.email,
                entryType = if (actualDelta >= 0) LedgerEntryType.ADMIN_CREDIT else LedgerEntryType.ADMIN_DEBIT,
                tradeMode = TradeMode.LIVE,
                amountDelta = actualDelta,
                balanceAfter = newBalance,
                referenceId = "admin_adj_${System.currentTimeMillis()}",
                note = reason.trim().ifBlank { "Manual admin adjustment" }
            )
            writeAuditLog("BALANCE_ADJUST", "users", target.userId, "Adjusted ${target.email} by $actualDelta ($reason)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${target.userId}")
            Result.failure(e)
        }
    }

    suspend fun adminToggleFreeze(target: UserProfile, frozen: Boolean): Result<Unit> {
        return try {
            db.collection("users").document(target.userId).update(
                mapOf("accountFrozen" to frozen, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            writeAuditLog(if (frozen) "FREEZE_ACCOUNT" else "UNFREEZE_ACCOUNT", "users", target.userId, "${target.email} frozen=$frozen")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${target.userId}")
            Result.failure(e)
        }
    }

    suspend fun adminReset2fa(target: UserProfile): Result<Unit> {
        return try {
            db.collection("users").document(target.userId).update(
                mapOf("twoFactorEnabled" to false, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            writeAuditLog("RESET_2FA", "users", target.userId, "Reset 2FA for ${target.email}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${target.userId}")
            Result.failure(e)
        }
    }

    suspend fun adminSaveDepositWallet(wallet: DepositWallet): Result<Unit> {
        val adminUid = requireUserId()
        val id = wallet.id.ifBlank { "${wallet.symbol}_${wallet.network}".toSafeDocId() }
        return try {
            val docRef = db.collection("deposit_wallets").document(id)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "symbol" to wallet.symbol.uppercase().trim(),
                        "network" to wallet.network.uppercase().trim(),
                        "address" to wallet.address.trim(),
                        "minDeposit" to wallet.minDeposit,
                        "confirmationsRequired" to wallet.confirmationsRequired,
                        "active" to wallet.active,
                        "qrPayload" to wallet.address.trim(),
                        "updatedBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "symbol" to wallet.symbol.uppercase().trim(),
                        "network" to wallet.network.uppercase().trim(),
                        "address" to wallet.address.trim(),
                        "minDeposit" to wallet.minDeposit,
                        "confirmationsRequired" to wallet.confirmationsRequired,
                        "active" to wallet.active,
                        "qrPayload" to wallet.address.trim(),
                        "updatedBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeAuditLog("SAVE_DEPOSIT_WALLET", "deposit_wallets", id, "${wallet.symbol} (${wallet.network}) -> ${wallet.address}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "deposit_wallets/$id")
            Result.failure(e)
        }
    }

    suspend fun adminSaveMarketControl(control: MarketControl): Result<Unit> {
        val adminUid = requireUserId()
        val docId = control.symbol.toSafeDocId()
        return try {
            val docRef = db.collection("market_controls").document(docId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "symbol" to control.symbol,
                        "tradingEnabled" to control.tradingEnabled,
                        "spreadBps" to control.spreadBps,
                        "feeBps" to control.feeBps,
                        "maxLeverage" to control.maxLeverage,
                        "updatedBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "tradingEnabled" to control.tradingEnabled,
                        "spreadBps" to control.spreadBps,
                        "feeBps" to control.feeBps,
                        "maxLeverage" to control.maxLeverage,
                        "updatedBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeAuditLog("UPDATE_MARKET_CONTROL", "market_controls", docId, "${control.symbol} enabled=${control.tradingEnabled} spread=${control.spreadBps}bps")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "market_controls/$docId")
            Result.failure(e)
        }
    }

    suspend fun adminReplyTicket(ticket: SupportTicket, status: TicketStatus, reply: String): Result<Unit> {
        return try {
            db.collection("support_tickets").document(ticket.id).update(
                mapOf(
                    "status" to status.name,
                    "adminReply" to reply.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog("REPLY_TICKET", "support_tickets", ticket.id, "Status -> ${status.name}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "support_tickets/${ticket.id}")
            Result.failure(e)
        }
    }

    suspend fun adminAddRole(uid: String, email: String, role: AdminRoleType): Result<Unit> {
        val actorUid = requireUserId()
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) return Result.failure(IllegalArgumentException("UID required"))
        return try {
            val docRef = db.collection("admins").document(cleanUid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "uid" to cleanUid,
                        "email" to email.trim(),
                        "role" to role.name,
                        "addedBy" to actorUid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "email" to email.trim(),
                        "role" to role.name,
                        "addedBy" to actorUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeAuditLog("ASSIGN_ADMIN_ROLE", "admins", cleanUid, "Assigned ${role.name} to $email")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "admins/$cleanUid")
            Result.failure(e)
        }
    }

    suspend fun adminRemoveRole(uid: String): Result<Unit> {
        return try {
            db.collection("admins").document(uid).delete().await()
            writeAuditLog("REMOVE_ADMIN_ROLE", "admins", uid, "Removed admin role for $uid")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "admins/$uid")
            Result.failure(e)
        }
    }

    private suspend fun updateModeBalance(uid: String, mode: TradeMode, newBalance: Double) {
        val field = if (mode == TradeMode.LIVE) "liveAvailableBalance" else "demoAvailableBalance"
        db.collection("users").document(uid).update(
            mapOf(field to newBalance, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    private suspend fun upsertPositionAfterBuy(
        uid: String,
        mode: TradeMode,
        asset: MarketAsset,
        qtyAdded: Double,
        execPrice: Double,
        dripEnabled: Boolean,
        existing: PortfolioPosition?
    ) {
        val posId = "${uid}_${mode.name}_${asset.symbol.toSafeDocId()}".toSafeDocId()
        val docRef = db.collection("positions").document(posId)
        if (existing == null || existing.quantity <= 0.0) {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "tradeMode" to mode.name,
                    "symbol" to asset.symbol,
                    "assetName" to asset.name,
                    "assetClass" to asset.assetClass.name,
                    "quantity" to qtyAdded,
                    "avgEntryPrice" to execPrice,
                    "costBasis" to qtyAdded * execPrice,
                    "realizedPnl" to 0.0,
                    "dripEnabled" to dripEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val newQty = existing.quantity + qtyAdded
            val newCost = existing.costBasis + (qtyAdded * execPrice)
            val newAvg = if (newQty > 0) newCost / newQty else execPrice
            docRef.update(
                mapOf(
                    "quantity" to newQty,
                    "avgEntryPrice" to newAvg,
                    "costBasis" to newCost,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    private suspend fun reducePositionAfterSell(
        uid: String,
        mode: TradeMode,
        existing: PortfolioPosition,
        qtySold: Double,
        realizedPnl: Double
    ) {
        val posId = existing.id.ifBlank { "${uid}_${mode.name}_${existing.symbol.toSafeDocId()}".toSafeDocId() }
        val docRef = db.collection("positions").document(posId)
        val remaining = (existing.quantity - qtySold).coerceAtLeast(0.0)
        if (remaining <= 0.000001) {
            docRef.delete().await()
        } else {
            val newCost = remaining * existing.avgEntryPrice
            docRef.update(
                mapOf(
                    "quantity" to remaining,
                    "costBasis" to newCost,
                    "realizedPnl" to (existing.realizedPnl + realizedPnl),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    private suspend fun recordLedgerEntry(
        userId: String,
        userEmail: String,
        entryType: LedgerEntryType,
        tradeMode: TradeMode,
        amountDelta: Double,
        balanceAfter: Double,
        referenceId: String,
        note: String
    ) {
        val actorUid = auth.currentUser?.uid ?: userId
        db.collection("ledger_entries").document().set(
            mapOf(
                "userId" to userId,
                "userEmail" to userEmail,
                "entryType" to entryType.name,
                "tradeMode" to tradeMode.name,
                "amountDelta" to amountDelta,
                "balanceAfter" to balanceAfter,
                "referenceId" to referenceId,
                "note" to note.take(400),
                "actorUid" to actorUid,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private suspend fun writeAuditLog(action: String, targetType: String, targetId: String, details: String) {
        val actor = auth.currentUser ?: return
        db.collection("audit_logs").document().set(
            mapOf(
                "actorUid" to actor.uid,
                "actorEmail" to (actor.email ?: ""),
                "action" to action,
                "targetType" to targetType,
                "targetId" to targetId,
                "details" to details.take(1000),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    companion object {
        const val ADMIN_EMAIL = "patriciamiglioranza4@gmail.com"
    }
}
