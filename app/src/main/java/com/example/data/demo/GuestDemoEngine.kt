package com.example.data.demo

import com.example.data.model.AccountTier
import com.example.data.model.KycStatus
import com.example.data.model.LedgerEntry
import com.example.data.model.LedgerEntryType
import com.example.data.model.MarketAsset
import com.example.data.model.OrderSide
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PortfolioPosition
import com.example.data.model.TimeInForce
import com.example.data.model.TradeMode
import com.example.data.model.TradeOrder
import com.example.data.model.UserProfile
import com.example.data.model.Watchlist
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * In-memory Demo Session engine for public visitors who tap "Try Free Demo" on the Landing Screen
 * before signing in with Google. Upgrades seamlessly when they sign in to open a real account.
 */
class GuestDemoEngine {
    private val _profile = MutableStateFlow(
        UserProfile(
            id = "guest_demo",
            userId = "guest_demo",
            email = "guest.demo@apexbroker.app",
            displayName = "Guest Demo Trader",
            accountTier = AccountTier.DEMO_ONLY.name,
            activeMode = TradeMode.DEMO.name,
            kycStatus = KycStatus.NOT_SUBMITTED.name,
            liveAvailableBalance = 0.0,
            liveLockedBalance = 0.0,
            demoAvailableBalance = 10000.0
        )
    )
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _positions = MutableStateFlow<List<PortfolioPosition>>(emptyList())
    val positions: StateFlow<List<PortfolioPosition>> = _positions.asStateFlow()

    private val _orders = MutableStateFlow<List<TradeOrder>>(emptyList())
    val orders: StateFlow<List<TradeOrder>> = _orders.asStateFlow()

    private val _ledger = MutableStateFlow<List<LedgerEntry>>(emptyList())
    val ledger: StateFlow<List<LedgerEntry>> = _ledger.asStateFlow()

    private val _watchlist = MutableStateFlow(
        Watchlist(
            id = "guest_wl",
            userId = "guest_demo",
            name = "Demo Watchlist",
            symbols = listOf("BTC", "ETH", "NVDA", "AAPL", "GC=F", "EUR/USD")
        )
    )
    val watchlist: StateFlow<Watchlist> = _watchlist.asStateFlow()

    fun toggleWatchlist(symbol: String) {
        val current = _watchlist.value
        val next = if (current.symbols.contains(symbol)) current.symbols - symbol else current.symbols + symbol
        _watchlist.value = current.copy(symbols = next)
    }

    fun resetDemo() {
        _profile.value = _profile.value.copy(demoAvailableBalance = 10000.0)
        _positions.value = emptyList()
        _ledger.value = listOf(
            LedgerEntry(
                id = UUID.randomUUID().toString(),
                userId = "guest_demo",
                userEmail = "guest.demo@apexbroker.app",
                entryType = LedgerEntryType.DEMO_RESET.name,
                tradeMode = TradeMode.DEMO.name,
                amountDelta = 10000.0,
                balanceAfter = 10000.0,
                referenceId = "guest_reset",
                note = "Demo balance reset to $10,000.00",
                actorUid = "guest_demo",
                createdAt = Timestamp.now()
            )
        ) + _ledger.value
    }

    fun placeDemoOrder(
        asset: MarketAsset,
        side: OrderSide,
        orderType: OrderType,
        timeInForce: TimeInForce,
        quantity: Double,
        limitPrice: Double?,
        stopPrice: Double?,
        takeProfitPrice: Double?
    ): Result<String> {
        if (quantity <= 0.0) return Result.failure(IllegalArgumentException("Quantity must be positive"))
        val execPrice = if (side == OrderSide.BUY) asset.askPrice else asset.bidPrice
        val notional = quantity * execPrice
        val fee = notional * 0.001
        val bal = _profile.value.demoAvailableBalance
        val existing = _positions.value.firstOrNull { it.symbol == asset.symbol }

        if (side == OrderSide.BUY && notional + fee > bal) {
            return Result.failure(IllegalArgumentException("Insufficient virtual balance"))
        }
        if (side == OrderSide.SELL && (existing == null || existing.quantity + 1e-8 < quantity)) {
            return Result.failure(IllegalArgumentException("Insufficient ${asset.symbol} position"))
        }

        val immediate = orderType == OrderType.MARKET
        var realizedPnl = 0.0
        val orderId = UUID.randomUUID().toString()

        if (immediate) {
            if (side == OrderSide.BUY) {
                val nextBal = (bal - (notional + fee)).coerceAtLeast(0.0)
                _profile.value = _profile.value.copy(demoAvailableBalance = nextBal)
                val updatedPos = if (existing == null) {
                    PortfolioPosition(
                        id = "guest_${asset.symbol}",
                        userId = "guest_demo",
                        tradeMode = TradeMode.DEMO.name,
                        symbol = asset.symbol,
                        assetName = asset.name,
                        assetClass = asset.assetClass.name,
                        quantity = quantity,
                        avgEntryPrice = execPrice,
                        costBasis = notional,
                        dripEnabled = true
                    )
                } else {
                    val nq = existing.quantity + quantity
                    val nc = existing.costBasis + notional
                    existing.copy(quantity = nq, costBasis = nc, avgEntryPrice = nc / nq)
                }
                _positions.value = _positions.value.filterNot { it.symbol == asset.symbol } + updatedPos
                _ledger.value = listOf(
                    LedgerEntry(
                        id = UUID.randomUUID().toString(),
                        userId = "guest_demo",
                        userEmail = "guest.demo@apexbroker.app",
                        entryType = LedgerEntryType.TRADE_BUY.name,
                        tradeMode = TradeMode.DEMO.name,
                        amountDelta = -(notional + fee),
                        balanceAfter = nextBal,
                        referenceId = orderId,
                        note = "Demo bought ${String.format("%.4f", quantity)} ${asset.symbol}",
                        actorUid = "guest_demo",
                        createdAt = Timestamp.now()
                    )
                ) + _ledger.value
            } else {
                val pos = existing!!
                realizedPnl = (notional - fee) - (pos.avgEntryPrice * quantity)
                val nextBal = bal + (notional - fee)
                _profile.value = _profile.value.copy(demoAvailableBalance = nextBal)
                val rem = (pos.quantity - quantity).coerceAtLeast(0.0)
                _positions.value = if (rem <= 0.000001) {
                    _positions.value.filterNot { it.symbol == asset.symbol }
                } else {
                    _positions.value.map {
                        if (it.symbol == asset.symbol) it.copy(quantity = rem, costBasis = rem * pos.avgEntryPrice, realizedPnl = it.realizedPnl + realizedPnl)
                        else it
                    }
                }
                _ledger.value = listOf(
                    LedgerEntry(
                        id = UUID.randomUUID().toString(),
                        userId = "guest_demo",
                        userEmail = "guest.demo@apexbroker.app",
                        entryType = LedgerEntryType.TRADE_SELL.name,
                        tradeMode = TradeMode.DEMO.name,
                        amountDelta = notional - fee,
                        balanceAfter = nextBal,
                        referenceId = orderId,
                        note = "Demo sold ${String.format("%.4f", quantity)} ${asset.symbol}",
                        actorUid = "guest_demo",
                        createdAt = Timestamp.now()
                    )
                ) + _ledger.value
            }
        }

        val order = TradeOrder(
            id = orderId,
            userId = "guest_demo",
            symbol = asset.symbol,
            assetName = asset.name,
            assetClass = asset.assetClass.name,
            side = side.name,
            orderType = orderType.name,
            timeInForce = timeInForce.name,
            tradeMode = TradeMode.DEMO.name,
            quantity = quantity,
            notionalUsd = notional,
            limitPrice = limitPrice,
            stopPrice = stopPrice,
            takeProfitPrice = takeProfitPrice,
            executedPrice = if (immediate) execPrice else 0.0,
            feeUsd = if (immediate) fee else 0.0,
            realizedPnl = realizedPnl,
            status = if (immediate) OrderStatus.FILLED.name else OrderStatus.OPEN.name,
            createdAt = Timestamp.now()
        )
        _orders.value = listOf(order) + _orders.value
        return Result.success(
            if (immediate) "Demo ${side.name} ${asset.symbol} filled at $${String.format("%.2f", execPrice)}"
            else "Demo ${orderType.label} order placed"
        )
    }

    fun cancelOrder(orderId: String) {
        _orders.value = _orders.value.map {
            if (it.id == orderId && it.status == OrderStatus.OPEN.name) it.copy(status = OrderStatus.CANCELED.name)
            else it
        }
    }
}
