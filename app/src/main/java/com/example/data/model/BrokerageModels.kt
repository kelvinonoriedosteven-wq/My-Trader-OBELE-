package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AccountTier {
    DEMO_ONLY,
    LIVE_UNVERIFIED,
    LIVE_PENDING_KYC,
    LIVE_VERIFIED;

    companion object {
        fun fromRaw(raw: String?): AccountTier =
            entries.firstOrNull { it.name == raw } ?: LIVE_UNVERIFIED
    }
}

enum class TradeMode {
    LIVE,
    DEMO;

    companion object {
        fun fromRaw(raw: String?): TradeMode =
            entries.firstOrNull { it.name == raw } ?: DEMO
    }
}

enum class KycStatus {
    NOT_SUBMITTED,
    PENDING,
    APPROVED,
    REJECTED;

    companion object {
        fun fromRaw(raw: String?): KycStatus =
            entries.firstOrNull { it.name == raw } ?: NOT_SUBMITTED
    }
}

enum class RiskTolerance(val label: String) {
    CONSERVATIVE("Conservative (Capital Preservation)"),
    MODERATE("Moderate (Balanced Growth)"),
    AGGRESSIVE("Aggressive (High Growth & Derivatives)");

    companion object {
        fun fromRaw(raw: String?): RiskTolerance =
            entries.firstOrNull { it.name == raw } ?: MODERATE
    }
}

enum class IdType(val label: String) {
    PASSPORT("Passport"),
    DRIVERS_LICENSE("Driver's License"),
    NATIONAL_ID("National ID Card");

    companion object {
        fun fromRaw(raw: String?): IdType =
            entries.firstOrNull { it.name == raw } ?: PASSPORT
    }
}

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED;

    companion object {
        fun fromRaw(raw: String?): RequestStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class OrderSide {
    BUY,
    SELL;

    companion object {
        fun fromRaw(raw: String?): OrderSide =
            entries.firstOrNull { it.name == raw } ?: BUY
    }
}

enum class OrderType(val label: String) {
    MARKET("Market"),
    LIMIT("Limit"),
    STOP_LOSS("Stop-Loss"),
    STOP_LIMIT("Stop-Limit"),
    TRAILING_STOP("Trailing Stop"),
    BRACKET("Bracket");

    companion object {
        fun fromRaw(raw: String?): OrderType =
            entries.firstOrNull { it.name == raw } ?: MARKET
    }
}

enum class TimeInForce {
    GTC,
    DAY;

    companion object {
        fun fromRaw(raw: String?): TimeInForce =
            entries.firstOrNull { it.name == raw } ?: GTC
    }
}

enum class OrderStatus {
    OPEN,
    FILLED,
    CANCELED;

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

enum class LedgerEntryType(val label: String) {
    DEPOSIT_CREDIT("Deposit Credit"),
    WITHDRAWAL_LOCK("Withdrawal Lock"),
    WITHDRAWAL_PAYOUT("Withdrawal Payout"),
    WITHDRAWAL_REFUND("Withdrawal Refund"),
    ADMIN_CREDIT("Admin Credit"),
    ADMIN_DEBIT("Admin Debit"),
    TRADE_BUY("Trade Buy"),
    TRADE_SELL("Trade Sell"),
    DEMO_RESET("Demo Reset");

    companion object {
        fun fromRaw(raw: String?): LedgerEntryType =
            entries.firstOrNull { it.name == raw } ?: TRADE_BUY
    }
}

enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED;

    companion object {
        fun fromRaw(raw: String?): TicketStatus =
            entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

enum class AdminRoleType(val label: String) {
    SUPER_ADMIN("Super Admin"),
    COMPLIANCE_OFFICER("Compliance Officer"),
    TREASURY_OPS("Treasury Ops");

    companion object {
        fun fromRaw(raw: String?): AdminRoleType =
            entries.firstOrNull { it.name == raw } ?: SUPER_ADMIN
    }
}

fun String.toSafeDocId(): String =
    replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "id_${hashCode().toUInt()}" }

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val accountTier: String = AccountTier.LIVE_UNVERIFIED.name,
    val activeMode: String = TradeMode.LIVE.name,
    val kycStatus: String = KycStatus.NOT_SUBMITTED.name,
    val kycRejectionReason: String? = null,
    val liveAvailableBalance: Double = 0.0,
    val liveLockedBalance: Double = 0.0,
    val demoAvailableBalance: Double = 10000.0,
    val accountFrozen: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val twoFactorPinHash: String? = null,
    val biometricEnabled: Boolean = false,
    val dripEnabled: Boolean = true,
    val whitelistedAddresses: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val tierEnum: AccountTier get() = AccountTier.fromRaw(accountTier)
    val modeEnum: TradeMode get() = TradeMode.fromRaw(activeMode)
    val kycEnum: KycStatus get() = KycStatus.fromRaw(kycStatus)
}

@Keep
@IgnoreExtraProperties
data class Watchlist(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val symbols: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PriceAlert(
    @DocumentId val id: String = "",
    val userId: String = "",
    val symbol: String = "",
    val targetPrice: Double = 0.0,
    val condition: String = "ABOVE",
    val active: Boolean = true,
    val triggered: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class KycSubmission(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val dateOfBirth: String = "",
    val nationality: String = "",
    val taxResidency: String = "",
    val taxIdLast4: String = "",
    val addressLine: String = "",
    val riskTolerance: String = RiskTolerance.MODERATE.name,
    val employmentStatus: String = "",
    val annualIncomeRange: String = "",
    val idType: String = IdType.PASSPORT.name,
    val idFrontBase64: String? = null,
    val idBackBase64: String? = null,
    val selfieBase64: String? = null,
    val proofOfAddressName: String? = null,
    val status: String = KycStatus.PENDING.name,
    val rejectionReason: String? = null,
    val reviewedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DepositWallet(
    @DocumentId val id: String = "",
    val symbol: String = "",
    val network: String = "",
    val address: String = "",
    val minDeposit: Double = 10.0,
    val confirmationsRequired: Long = 3L,
    val active: Boolean = true,
    val qrPayload: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DepositRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val assetSymbol: String = "",
    val network: String = "",
    val walletAddress: String = "",
    val amountUsd: Double = 0.0,
    val cryptoAmount: Double = 0.0,
    val txHash: String = "",
    val proofImageBase64: String? = null,
    val status: String = RequestStatus.PENDING.name,
    val adminNote: String? = null,
    val reviewedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val assetSymbol: String = "",
    val network: String = "",
    val destinationAddress: String = "",
    val amountUsd: Double = 0.0,
    val feeUsd: Double = 0.0,
    val netAmountUsd: Double = 0.0,
    val status: String = RequestStatus.PENDING.name,
    val payoutTxHash: String? = null,
    val adminNote: String? = null,
    val reviewedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TradeOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val symbol: String = "",
    val assetName: String = "",
    val assetClass: String = AssetClass.STOCK.name,
    val side: String = OrderSide.BUY.name,
    val orderType: String = OrderType.MARKET.name,
    val timeInForce: String = TimeInForce.GTC.name,
    val tradeMode: String = TradeMode.DEMO.name,
    val quantity: Double = 0.0,
    val notionalUsd: Double = 0.0,
    val limitPrice: Double? = null,
    val stopPrice: Double? = null,
    val takeProfitPrice: Double? = null,
    val executedPrice: Double = 0.0,
    val feeUsd: Double = 0.0,
    val realizedPnl: Double = 0.0,
    val status: String = OrderStatus.FILLED.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PortfolioPosition(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tradeMode: String = TradeMode.DEMO.name,
    val symbol: String = "",
    val assetName: String = "",
    val assetClass: String = AssetClass.STOCK.name,
    val quantity: Double = 0.0,
    val avgEntryPrice: Double = 0.0,
    val costBasis: Double = 0.0,
    val realizedPnl: Double = 0.0,
    val dripEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LedgerEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val entryType: String = LedgerEntryType.TRADE_BUY.name,
    val tradeMode: String = TradeMode.LIVE.name,
    val amountDelta: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val referenceId: String = "",
    val note: String = "",
    val actorUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val subject: String = "",
    val category: String = "General",
    val message: String = "",
    val status: String = TicketStatus.OPEN.name,
    val adminReply: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MarketControl(
    @DocumentId val id: String = "",
    val symbol: String = "",
    val tradingEnabled: Boolean = true,
    val spreadBps: Double = 4.0,
    val feeBps: Double = 10.0,
    val maxLeverage: Double = 5.0,
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRole(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val role: String = AdminRoleType.SUPER_ADMIN.name,
    val addedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
