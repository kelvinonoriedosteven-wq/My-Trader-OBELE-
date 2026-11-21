package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.localization.AppTranslationManager.tr
import com.example.data.model.AssetClass
import com.example.data.model.MarketAsset
import com.example.data.model.PortfolioPosition
import com.example.data.model.PriceAlert
import com.example.data.model.TradeMode
import com.example.data.model.UserProfile
import com.example.ui.components.MarketAssetIcon
import com.example.ui.components.MiniSparkline
import com.example.ui.theme.AmberDemo
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.GoldAccent
import com.example.util.Formatters

@Composable
fun PortfolioDashboardScreen(
    profile: UserProfile,
    positions: List<PortfolioPosition>,
    marketAssets: List<MarketAsset>,
    watchlistSymbols: List<String>,
    priceAlerts: List<PriceAlert>,
    onQuickDeposit: () -> Unit,
    onQuickWithdraw: () -> Unit,
    onQuickTrade: () -> Unit,
    onQuickKyc: () -> Unit,
    onQuickReports: () -> Unit,
    onAssetClick: (String) -> Unit
) {
    val activeMode = profile.modeEnum
    val modePositions = positions.filter { it.tradeMode == activeMode.name }
    val assetMap = marketAssets.associateBy { it.symbol }

    val availableCash = if (activeMode == TradeMode.LIVE) profile.liveAvailableBalance else profile.demoAvailableBalance
    val lockedCash = if (activeMode == TradeMode.LIVE) profile.liveLockedBalance else 0.0
    val holdingsMarketValue = modePositions.sumOf { pos ->
        val px = assetMap[pos.symbol]?.price ?: pos.avgEntryPrice
        pos.quantity * px
    }
    val holdingsCostBasis = modePositions.sumOf { it.costBasis }
    val unrealizedPnl = holdingsMarketValue - holdingsCostBasis
    val realizedPnl = modePositions.sumOf { it.realizedPnl }
    val totalEquity = availableCash + lockedCash + holdingsMarketValue
    val buyingPower = availableCash * 2.0
    val marginUsedRatio = if (totalEquity > 0) (holdingsMarketValue / (totalEquity * 2.0)).toFloat().coerceIn(0f, 1f) else 0f

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Equity Card
            item {
                val accent = if (activeMode == TradeMode.DEMO) AmberDemo else EmeraldGain
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surface)
                            )
                        )
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(if (activeMode == TradeMode.DEMO) "DEMO PORTFOLIO VALUE" else "TOTAL ACCOUNT EQUITY"),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (profile.accountFrozen) tr("FROZEN") else "${Formatters.activeCurrency.flagEmoji} ${Formatters.currencyCode()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (profile.accountFrozen) CrimsonLoss else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = Formatters.formatUsd(totalEquity),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricTile(
                            label = tr("Available Cash"),
                            value = Formatters.formatUsd(availableCash),
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = tr("Locked / Pending"),
                            value = Formatters.formatUsd(lockedCash),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val uColor = if (unrealizedPnl >= 0) EmeraldGain else CrimsonLoss
                        MetricTile(
                            label = tr("Unrealized P&L"),
                            value = "${if (unrealizedPnl >= 0) "+" else ""}${Formatters.formatUsd(unrealizedPnl)}",
                            valueColor = uColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = tr("Buying Power (2x)"),
                            value = Formatters.formatUsd(buyingPower),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("Margin Utilization"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${(marginUsedRatio * 100).toInt()}% • ${tr("Realized")} ${Formatters.formatUsd(realizedPnl)}", style = MaterialTheme.typography.labelSmall)
                        }
                        LinearProgressIndicator(
                            progress = { marginUsedRatio },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                            color = accent,
                            trackColor = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Quick Action Row
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { QuickActionChip(tr("Deposit"), Icons.Default.ArrowDownward, EmeraldGain, "quick_deposit_btn", onQuickDeposit) }
                    item { QuickActionChip(tr("Withdraw"), Icons.Default.ArrowUpward, CobaltPrimary, "quick_withdraw_btn", onQuickWithdraw) }
                    item { QuickActionChip(tr("Trade"), Icons.Default.CandlestickChart, CyanAccent, "quick_trade_btn", onQuickTrade) }
                    item { QuickActionChip(tr("KYC"), Icons.Default.VerifiedUser, GoldAccent, "quick_kyc_btn", onQuickKyc) }
                    item { QuickActionChip(tr("Statements"), Icons.Default.Assessment, MaterialTheme.colorScheme.onSurface, "quick_reports_btn", onQuickReports) }
                }
            }

            // Asset Allocation Donut Card
            item {
                AllocationChartCard(
                    availableCash = availableCash + lockedCash,
                    positions = modePositions,
                    assetMap = assetMap
                )
            }

            // Active Positions
            item {
                Text(
                    text = "${tr("Active Positions")} (${activeMode.name} • ${modePositions.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (modePositions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(34.dp))
                        Text(tr("No open positions"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = tr(if (activeMode == TradeMode.DEMO) "No open positions in current mode." else "No open positions in current mode."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(modePositions, key = { it.id.ifBlank { it.symbol } }) { pos ->
                    val liveAsset = assetMap[pos.symbol]
                    val currentPx = liveAsset?.price ?: pos.avgEntryPrice
                    val marketVal = pos.quantity * currentPx
                    val pnl = marketVal - pos.costBasis
                    val pnlPct = if (pos.costBasis > 0) (pnl / pos.costBasis) * 100.0 else 0.0
                    val assetClass = AssetClass.entries.firstOrNull { it.name == pos.assetClass } ?: AssetClass.STOCK
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .clickable { onAssetClick(pos.symbol) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            MarketAssetIcon(
                                symbol = pos.symbol,
                                assetClass = assetClass,
                                iconUrl = liveAsset?.iconUrl,
                                websiteDomain = liveAsset?.websiteDomain,
                                countryCode = liveAsset?.countryCode
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(pos.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${Formatters.formatQty(pos.quantity)} ${tr("Units")} @ ${Formatters.formatAssetPrice(pos.avgEntryPrice, assetClass)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Formatters.formatUsd(marketVal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${Formatters.formatUsd(pnl)} (${Formatters.formatPct(pnlPct)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pnl >= 0) EmeraldGain else CrimsonLoss,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Watchlist Preview
            val watchlistAssets = marketAssets.filter { watchlistSymbols.contains(it.symbol) }
            if (watchlistAssets.isNotEmpty()) {
                item {
                    Text(tr("Watchlist Pulse"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(watchlistAssets.take(8), key = { "wl_${it.symbol}" }) { asset ->
                    val pos = asset.change24hPct >= 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .clickable { onAssetClick(asset.symbol) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            MarketAssetIcon(asset = asset, size = 34.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(asset.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(asset.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        MiniSparkline(
                            points = asset.sparkline,
                            isPositive = pos,
                            modifier = Modifier.width(64.dp).height(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Formatters.formatAssetPrice(asset.price, asset.assetClass), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(Formatters.formatPct(asset.change24hPct), style = MaterialTheme.typography.labelSmall, color = if (pos) EmeraldGain else CrimsonLoss)
                        }
                    }
                }
            }

            if (priceAlerts.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("${priceAlerts.size} ${tr("Price Alert")}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(86.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AllocationChartCard(
    availableCash: Double,
    positions: List<PortfolioPosition>,
    assetMap: Map<String, MarketAsset>
) {
    val buckets = mutableMapOf<String, Double>()
    if (availableCash > 0) buckets[tr("Cash")] = availableCash
    positions.forEach { pos ->
        val px = assetMap[pos.symbol]?.price ?: pos.avgEntryPrice
        val v = pos.quantity * px
        if (v > 0) buckets[tr(pos.assetClass)] = (buckets[tr(pos.assetClass)] ?: 0.0) + v
    }
    val total = buckets.values.sum().coerceAtLeast(1.0)
    val palette = listOf(CobaltPrimary, EmeraldGain, AmberDemo, CyanAccent, GoldAccent, CrimsonLoss)
    val entries = buckets.entries.toList()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Canvas(modifier = Modifier.size(86.dp)) {
            var startAngle = -90f
            entries.forEachIndexed { idx, (_, valUsd) ->
                val sweep = ((valUsd / total) * 360.0).toFloat()
                drawArc(
                    color = palette[idx % palette.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Asset Allocation"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            entries.forEachIndexed { idx, (label, valUsd) ->
                val pct = (valUsd / total) * 100.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(palette[idx % palette.size]))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                    Text("${String.format("%.1f", pct)}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
