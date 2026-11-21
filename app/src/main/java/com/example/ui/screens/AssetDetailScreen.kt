package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.localization.AppTranslationManager.tr
import com.example.data.localization.countryCodeToFlagEmoji
import com.example.data.model.AssetClass
import com.example.data.model.AssetDetailData
import com.example.data.model.MarketControl
import com.example.data.model.OrderSide
import com.example.data.model.OrderType
import com.example.data.model.PortfolioPosition
import com.example.data.model.TimeInForce
import com.example.data.model.Timeframe
import com.example.data.model.TradeMode
import com.example.ui.components.InteractivePriceChart
import com.example.ui.components.MarketAssetIcon
import com.example.ui.components.OrderTicketModal
import com.example.ui.theme.AmberDemo
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.GoldAccent
import com.example.util.Formatters
import java.util.Locale
import kotlin.math.max

@Composable
fun AssetDetailScreen(
    detail: AssetDetailData?,
    selectedTimeframe: Timeframe,
    activeMode: TradeMode,
    availableBalance: Double,
    currentPosition: PortfolioPosition?,
    marketControl: MarketControl?,
    isWatchlisted: Boolean,
    canCreateAlerts: Boolean,
    onBack: (() -> Unit)?,
    onSelectTimeframe: (Timeframe) -> Unit,
    onToggleWatchlist: () -> Unit,
    onCreateAlert: (Double, String) -> Unit,
    onPlaceOrder: (
        side: OrderSide,
        orderType: OrderType,
        tif: TimeInForce,
        quantity: Double,
        limitPrice: Double?,
        stopPrice: Double?,
        takeProfitPrice: Double?
    ) -> Unit
) {
    if (detail == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(tr("Loading market depth & chart..."))
        }
        return
    }

    val asset = detail.asset
    val isPos = asset.change24hPct >= 0
    var isCandlestick by remember { mutableStateOf(true) }
    var showSma20 by remember { mutableStateOf(true) }
    var showRsi by remember { mutableStateOf(true) }
    var showMacd by remember { mutableStateOf(false) }
    var detailTab by remember { mutableIntStateOf(0) }
    var showOrderModal by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }

    val tradingHalted = marketControl != null && !marketControl.tradingEnabled

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (onBack != null) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                            MarketAssetIcon(asset = asset, size = 44.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("${asset.symbol} • ${asset.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                val flag = asset.countryCode?.let { "${countryCodeToFlagEmoji(it)} " } ?: ""
                                Text(
                                    text = "$flag${asset.exchange} • ${tr(asset.sector)} • ${if (detail.isLiveChart) "Live Feed" else asset.quoteSource}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGain
                                )
                            }
                        }
                        Row {
                            if (canCreateAlerts) {
                                IconButton(onClick = { showAlertDialog = true }) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = "Set price alert")
                                }
                            }
                            IconButton(onClick = onToggleWatchlist) {
                                Icon(
                                    imageVector = if (isWatchlisted) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Watchlist toggle",
                                    tint = if (isWatchlisted) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Price + Bid/Ask
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = Formatters.formatAssetPrice(asset.price, asset.assetClass),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${if (isPos) "+" else ""}${Formatters.formatAssetPrice(asset.changeUsd, asset.assetClass)} (${Formatters.formatPct(asset.change24hPct)}) 24h",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isPos) EmeraldGain else CrimsonLoss,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Bid ${Formatters.formatAssetPrice(asset.bidPrice, asset.assetClass)}", style = MaterialTheme.typography.labelSmall, color = EmeraldGain)
                            Text("Ask ${Formatters.formatAssetPrice(asset.askPrice, asset.assetClass)}", style = MaterialTheme.typography.labelSmall, color = CrimsonLoss)
                            Text(
                                text = "H ${Formatters.formatAssetPrice(asset.high24h, asset.assetClass)} / L ${Formatters.formatAssetPrice(asset.low24h, asset.assetClass)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (tradingHalted) {
                    item {
                        Text(
                            text = "NOTICE: Trading for ${asset.symbol} is temporarily halted by Risk Administration.",
                            style = MaterialTheme.typography.labelLarge,
                            color = CrimsonLoss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CrimsonLoss.copy(alpha = 0.14f))
                                .padding(10.dp)
                        )
                    }
                }

                // Timeframe & Indicator Controls
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Timeframe.entries.forEach { tf ->
                            FilterChip(
                                selected = selectedTimeframe == tf,
                                onClick = { onSelectTimeframe(tf) },
                                label = { Text(tf.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        FilterChip(selected = isCandlestick, onClick = { isCandlestick = !isCandlestick }, label = { Text(tr(if (isCandlestick) "Candles" else "Line"), style = MaterialTheme.typography.labelSmall) })
                        FilterChip(selected = showSma20, onClick = { showSma20 = !showSma20 }, label = { Text("SMA20", style = MaterialTheme.typography.labelSmall) })
                        FilterChip(selected = showRsi, onClick = { showRsi = !showRsi }, label = { Text("RSI", style = MaterialTheme.typography.labelSmall) })
                        FilterChip(selected = showMacd, onClick = { showMacd = !showMacd }, label = { Text("MACD", style = MaterialTheme.typography.labelSmall) })
                    }
                }

                // Interactive Chart
                item {
                    InteractivePriceChart(
                        detail = detail,
                        isCandlestick = isCandlestick,
                        showSma20 = showSma20,
                        showRsi = showRsi,
                        showMacd = showMacd
                    )
                }

                // Sub-tabs: Order Book / Financials / News
                item {
                    TabRow(selectedTabIndex = detailTab, containerColor = MaterialTheme.colorScheme.surface) {
                        Tab(selected = detailTab == 0, onClick = { detailTab = 0 }, text = { Text(tr("Level 2 Book")) })
                        Tab(selected = detailTab == 1, onClick = { detailTab = 1 }, text = { Text(tr("Financials")) })
                        Tab(selected = detailTab == 2, onClick = { detailTab = 2 }, text = { Text(tr("News")) })
                    }
                }

                when (detailTab) {
                    0 -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(tr("BID QTY"), style = MaterialTheme.typography.labelSmall, color = EmeraldGain)
                                    Text(tr("PRICE"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(tr("ASK QTY"), style = MaterialTheme.typography.labelSmall, color = CrimsonLoss)
                                }
                                val maxQty = max(
                                    detail.bids.maxOfOrNull { it.quantity } ?: 1.0,
                                    detail.asks.maxOfOrNull { it.quantity } ?: 1.0
                                )
                                val rows = minOf(detail.bids.size, detail.asks.size, 8)
                                for (i in 0 until rows) {
                                    val b = detail.bids[i]
                                    val a = detail.asks[i]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth((b.quantity / maxQty).toFloat().coerceIn(0.08f, 1f))
                                                    .height(22.dp)
                                                    .background(EmeraldGain.copy(alpha = 0.14f))
                                            )
                                            Text(
                                                text = "${Formatters.formatQty(b.quantity)} • ${Formatters.formatAssetPrice(b.price, asset.assetClass)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldGain,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth((a.quantity / maxQty).toFloat().coerceIn(0.08f, 1f))
                                                    .height(22.dp)
                                                    .background(CrimsonLoss.copy(alpha = 0.14f))
                                            )
                                            Text(
                                                text = "${Formatters.formatAssetPrice(a.price, asset.assetClass)} • ${Formatters.formatQty(a.quantity)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CrimsonLoss,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(tr("Fundamentals & Analyst Consensus"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                StatRow(tr("Market Cap"), if (asset.marketCapUsd > 0) Formatters.formatCompactUsd(asset.marketCapUsd) else "FX / Spot")
                                StatRow(tr("24h Dollar Volume"), Formatters.formatCompactUsd(asset.volume24hUsd))
                                StatRow(tr("P/E Ratio"), asset.peRatio?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A")
                                StatRow(tr("Dividend / Staking Yield"), "${String.format(Locale.US, "%.2f", asset.dividendYieldPct)}%")
                                StatRow("${tr("Analyst Rating")} (${asset.analystConsensus})", Formatters.formatAssetPrice(asset.analystTargetPrice, asset.assetClass))

                                LinearProgressIndicator(
                                    progress = { asset.analystBuyPct / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                                    color = EmeraldGain,
                                    trackColor = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Buy ${asset.analystBuyPct}% • Hold ${asset.analystHoldPct}% • Sell ${asset.analystSellPct}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (asset.dividendHistory.isNotEmpty()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                    Text(tr("Dividend History"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    asset.dividendHistory.forEach { d ->
                                        StatRow("Ex-Date ${d.exDate}", "${Formatters.formatUsd(d.amountPerShare)}/sh (${d.yieldPct}%)")
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                val items = asset.news.ifEmpty {
                                    listOf(
                                        com.example.data.model.AssetNewsItem(
                                            "gen1",
                                            "${asset.name} (${asset.symbol}) tracks institutional liquidity flows across ${asset.exchange}",
                                            "Apex Wire",
                                            "1h ago",
                                            if (isPos) "Bullish" else "Neutral"
                                        )
                                    )
                                }
                                items.forEach { news ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(news.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${news.source} • ${news.timeAgo} • ${news.sentiment}", style = MaterialTheme.typography.labelSmall, color = EmeraldGain)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sticky Trade Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(if (activeMode == TradeMode.DEMO) "Demo Cash" else "Live Cash"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (activeMode == TradeMode.DEMO) AmberDemo else EmeraldGain
                    )
                    Text(Formatters.formatUsd(availableBalance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { showOrderModal = true },
                    enabled = !tradingHalted,
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary, contentColor = Color.White),
                    modifier = Modifier.height(48.dp).testTag("open_order_ticket_button")
                ) {
                    Text("${tr("Trade")} ${asset.symbol}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showOrderModal) {
        OrderTicketModal(
            asset = asset,
            activeMode = activeMode,
            availableBalance = availableBalance,
            currentPosition = currentPosition,
            feeBps = marketControl?.feeBps ?: 10.0,
            onDismiss = { showOrderModal = false },
            onSubmitOrder = { side, type, tif, qty, limit, stop, tp ->
                onPlaceOrder(side, type, tif, qty, limit, stop, tp)
                showOrderModal = false
            }
        )
    }

    if (showAlertDialog) {
        val alertCurCode = if (asset.assetClass == AssetClass.FOREX) "FX" else Formatters.currencyCode()
        val initialLocal = if (asset.assetClass == AssetClass.FOREX) asset.price * 1.03 else Formatters.convertFromUsd(asset.price * 1.03)
        var targetText by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialLocal)) }
        var condition by remember { mutableStateOf("ABOVE") }
        AlertDialog(
            onDismissRequest = { showAlertDialog = false },
            title = { Text("${tr("Price Alert")}: ${asset.symbol}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = condition == "ABOVE", onClick = { condition = "ABOVE" }, label = { Text(tr("Goes Above")) })
                        FilterChip(selected = condition == "BELOW", onClick = { condition = "BELOW" }, label = { Text(tr("Drops Below")) })
                    }
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("${tr("PRICE")} ($alertCurCode)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    targetText.toDoubleOrNull()?.let { entered ->
                        if (entered > 0) {
                            val usdTarget = if (asset.assetClass == AssetClass.FOREX) entered else Formatters.convertToUsd(entered)
                            onCreateAlert(usdTarget, condition)
                            showAlertDialog = false
                        }
                    }
                }) { Text(tr("Set Alert")) }
            },
            dismissButton = {
                TextButton(onClick = { showAlertDialog = false }) { Text(tr("Cancel")) }
            }
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
