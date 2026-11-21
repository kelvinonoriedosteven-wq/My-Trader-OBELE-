package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.localization.AppTranslationManager.tr
import com.example.data.localization.countryCodeToFlagEmoji
import com.example.data.model.AssetClass
import com.example.data.model.MarketAsset
import com.example.data.model.MarketRegion
import com.example.data.model.Watchlist
import com.example.ui.components.MarketAssetIcon
import com.example.ui.components.MiniSparkline
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.GoldAccent
import com.example.util.Formatters
import kotlinx.coroutines.delay

enum class ScreenerSort(val label: String) {
    DEFAULT("Featured"),
    GAINERS("Top Gainers"),
    LOSERS("Top Losers"),
    MARKET_CAP("Market Cap"),
    VOLUME("Volume"),
    DIVIDEND("Div Yield")
}

@Composable
fun MarketsScreenerScreen(
    assets: List<MarketAsset>,
    syncStatus: String,
    watchlists: List<Watchlist>,
    activeWatchlist: Watchlist?,
    globalSearchResults: List<MarketAsset> = emptyList(),
    isSearchingGlobal: Boolean = false,
    onSearchGlobalMarkets: (String) -> Unit = {},
    onSelectGlobalSearchAsset: (MarketAsset) -> Unit = {},
    onSelectAsset: (String) -> Unit,
    onToggleWatchlist: (Watchlist, String) -> Unit,
    onCreateWatchlist: (String, List<String>) -> Unit,
    onRefreshQuotes: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedRegion by remember { mutableStateOf(MarketRegion.ALL) }
    var sortMode by remember { mutableStateOf(ScreenerSort.DEFAULT) }
    var showCreateWatchlist by remember { mutableStateOf(false) }

    val watchlistSet = activeWatchlist?.symbols?.toSet() ?: emptySet()

    LaunchedEffect(search) {
        if (search.trim().length >= 2) {
            delay(420)
            onSearchGlobalMarkets(search.trim())
        } else {
            onSearchGlobalMarkets("")
        }
    }

    val filtered = remember(assets, search, selectedCategory, selectedRegion, sortMode, watchlistSet) {
        val base = assets.filter { asset ->
            val matchesQuery = search.isBlank() ||
                asset.symbol.contains(search, ignoreCase = true) ||
                asset.name.contains(search, ignoreCase = true) ||
                asset.sector.contains(search, ignoreCase = true) ||
                asset.exchange.contains(search, ignoreCase = true) ||
                asset.region.contains(search, ignoreCase = true)
            val matchesCat = when (selectedCategory) {
                "ALL" -> true
                "WATCHLIST" -> watchlistSet.contains(asset.symbol)
                else -> asset.assetClass.name == selectedCategory
            }
            val matchesRegion = selectedRegion == MarketRegion.ALL ||
                asset.region.equals(selectedRegion.label, ignoreCase = true)
            matchesQuery && matchesCat && matchesRegion
        }
        when (sortMode) {
            ScreenerSort.DEFAULT -> base
            ScreenerSort.GAINERS -> base.sortedByDescending { it.change24hPct }
            ScreenerSort.LOSERS -> base.sortedBy { it.change24hPct }
            ScreenerSort.MARKET_CAP -> base.sortedByDescending { it.marketCapUsd }
            ScreenerSort.VOLUME -> base.sortedByDescending { it.volume24hUsd }
            ScreenerSort.DIVIDEND -> base.sortedByDescending { it.dividendYieldPct }
        }
    }

    val indices = assets.filter { it.assetClass == AssetClass.INDEX || it.symbol == "BTC" || it.symbol == "GC=F" }
    val sectors = assets.groupBy { it.sector }.entries.take(8)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Feed Status Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tr("Markets & Screener"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${assets.size} ${tr("Global")} • $syncStatus",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row {
                        IconButton(onClick = { showCreateWatchlist = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New Watchlist")
                        }
                        IconButton(onClick = onRefreshQuotes, modifier = Modifier.testTag("refresh_quotes_button")) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh quotes")
                        }
                    }
                }
            }

            // World Indices & Benchmarks Ribbon
            item {
                Text(tr("Global Exchanges & Indices"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(indices, key = { "idx_${it.symbol}" }) { idx ->
                        val pos = idx.change24hPct >= 0
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable { onSelectAsset(idx.symbol) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MarketAssetIcon(asset = idx, size = 26.dp)
                            Column {
                                Text(idx.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(Formatters.formatAssetPrice(idx.price, idx.assetClass), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(Formatters.formatPct(idx.change24hPct), style = MaterialTheme.typography.labelSmall, color = if (pos) EmeraldGain else CrimsonLoss)
                                }
                            }
                        }
                    }
                }
            }

            // Global Search Bar
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text(tr("Search global ticker, company, crypto, exchange...")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (isSearchingGlobal) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("market_search_input")
                )
            }

            // Region Filter Chips (All Regions worldwide)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MarketRegion.entries.forEach { reg ->
                        FilterChip(
                            selected = selectedRegion == reg,
                            onClick = { selectedRegion = reg },
                            label = { Text(tr(reg.label), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Category Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == "ALL",
                        onClick = { selectedCategory = "ALL" },
                        label = { Text(tr("All")) }
                    )
                    FilterChip(
                        selected = selectedCategory == "WATCHLIST",
                        onClick = { selectedCategory = "WATCHLIST" },
                        label = { Text("${tr("Watchlist")} (${watchlistSet.size})") }
                    )
                    AssetClass.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = { selectedCategory = cat.name },
                            label = { Text(tr(cat.label)) }
                        )
                    }
                }
            }

            // Screener Sort Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScreenerSort.entries.forEach { sort ->
                        FilterChip(
                            selected = sortMode == sort,
                            onClick = { sortMode = sort },
                            label = { Text(tr(sort.label), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Global Exchange Live Search Results (for ANY stock/crypto/commodity worldwide)
            if (search.trim().length >= 2 && (globalSearchResults.isNotEmpty() || isSearchingGlobal)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isSearchingGlobal) tr("Searching global exchanges...") else "${tr("Global Search Results")} (${globalSearchResults.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CobaltPrimary
                        )
                    }
                }
                items(globalSearchResults, key = { "gsearch_${it.symbol}_${it.exchange}" }) { gAsset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CobaltPrimary.copy(alpha = 0.08f))
                            .border(1.dp, CobaltPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .clickable { onSelectGlobalSearchAsset(gAsset) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            MarketAssetIcon(asset = gAsset, size = 36.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(gAsset.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${tr(gAsset.assetClass.label)} • ${gAsset.exchange}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CobaltPrimary
                                    )
                                }
                                Text(
                                    text = gAsset.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(tr("Trade"), style = MaterialTheme.typography.labelMedium, color = EmeraldGain, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Sector Heatmap Strip
            item {
                Text(tr("Sector Heatmap"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sectors, key = { it.key }) { (sector, list) ->
                        val avg = list.map { it.change24hPct }.average()
                        val pos = avg >= 0
                        val bg = if (pos) EmeraldGain.copy(alpha = 0.14f) else CrimsonLoss.copy(alpha = 0.14f)
                        Column(
                            modifier = Modifier
                                .width(136.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .border(1.dp, (if (pos) EmeraldGain else CrimsonLoss).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Text(tr(sector), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = Formatters.formatPct(avg),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (pos) EmeraldGain else CrimsonLoss,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Asset List
            items(filtered, key = { it.symbol }) { asset ->
                val isPos = asset.change24hPct >= 0
                val isWatched = watchlistSet.contains(asset.symbol)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable { onSelectAsset(asset.symbol) }
                        .padding(12.dp)
                        .testTag("market_asset_${asset.symbol}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        MarketAssetIcon(asset = asset)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(asset.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                val flag = asset.countryCode?.let { "${countryCodeToFlagEmoji(it)} " } ?: ""
                                Text(
                                    text = "$flag${asset.exchange}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = asset.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    if (asset.marketCapUsd > 0) append("${tr("Market Cap")} ${Formatters.formatCompactUsd(asset.marketCapUsd)}")
                                    else append("24h ${tr("Volume")} ${Formatters.formatCompactUsd(asset.volume24hUsd)}")
                                    if (asset.peRatio != null) append(" • P/E ${String.format("%.1f", asset.peRatio)}")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    MiniSparkline(
                        points = asset.sparkline,
                        isPositive = isPos,
                        modifier = Modifier.width(50.dp).height(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = Formatters.formatAssetPrice(asset.price, asset.assetClass),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Formatters.formatPct(asset.change24hPct),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPos) EmeraldGain else CrimsonLoss,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (activeWatchlist != null) {
                        IconButton(
                            onClick = { onToggleWatchlist(activeWatchlist, asset.symbol) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isWatched) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Toggle watchlist",
                                tint = if (isWatched) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty() && search.trim().length >= 2) {
                item {
                    OutlinedButton(
                        onClick = { onSearchGlobalMarkets(search.trim()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Search Worldwide Markets"))
                    }
                }
            }
        }
    }

    if (showCreateWatchlist) {
        var wlName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateWatchlist = false },
            title = { Text(tr("Create Custom Watchlist")) },
            text = {
                OutlinedTextField(
                    value = wlName,
                    onValueChange = { wlName = it },
                    label = { Text(tr("Watchlist Name")) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (wlName.isNotBlank()) {
                        onCreateWatchlist(wlName, listOf("BTC", "NVDA", "SPY"))
                        showCreateWatchlist = false
                    }
                }) { Text(tr("Create")) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateWatchlist = false }) { Text(tr("Cancel")) }
            }
        )
    }
}
