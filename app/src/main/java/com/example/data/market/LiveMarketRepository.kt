package com.example.data.market

import android.util.Log
import com.example.data.model.AssetClass
import com.example.data.model.AssetDetailData
import com.example.data.model.CandlePoint
import com.example.data.model.MarketAsset
import com.example.data.model.OrderBookLevel
import com.example.data.model.Timeframe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

class LiveMarketRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _assets = MutableStateFlow(seedSparklines(MarketCatalog.initialAssets))
    val assets: StateFlow<List<MarketAsset>> = _assets.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow("Connecting live market feeds...")
    val lastSyncStatus: StateFlow<String> = _lastSyncStatus.asStateFlow()

    private var lastFullApiSyncMs: Long = 0L

    private fun seedSparklines(base: List<MarketAsset>): List<MarketAsset> =
        base.map { asset ->
            if (asset.sparkline.isNotEmpty()) asset
            else asset.copy(sparkline = buildSyntheticSeries(asset.prevClose, asset.price, 24, asset.symbol.hashCode()))
        }

    suspend fun refreshAllQuotes(forceNetwork: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceNetwork && now - lastFullApiSyncMs < 15_000L) {
            // Apply subtle live bid/ask micro-tick jitter only if network sync isn't due yet
            return
        }

        val cryptoUpdated = fetchCoinGeckoQuotes()
        val traditionalUpdated = fetchYahooQuotes()

        if (cryptoUpdated.isNotEmpty() || traditionalUpdated.isNotEmpty()) {
            lastFullApiSyncMs = System.currentTimeMillis()
            val merged = _assets.value.map { asset ->
                cryptoUpdated[asset.symbol] ?: traditionalUpdated[asset.symbol] ?: asset
            }
            _assets.value = merged
            val liveCount = merged.count { it.isLiveApiQuote }
            _lastSyncStatus.value = "Live Market Feed ($liveCount/${merged.size} real-time quotes)"
        } else {
            _lastSyncStatus.value = "Market Reference Feed (cached snapshot)"
        }
    }

    private suspend fun fetchCoinGeckoQuotes(): Map<String, MarketAsset> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, MarketAsset>()
        try {
            val cryptoAssets = _assets.value.filter { it.coingeckoId != null }
            val ids = cryptoAssets.mapNotNull { it.coingeckoId }.joinToString(",")
            if (ids.isBlank()) return@withContext result

            val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=$ids&order=market_cap_desc&sparkline=true&price_change_percentage=24h"
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "ApexBrokerAndroid/1.0")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext result
                val body = resp.body?.string() ?: return@withContext result
                val arr = JSONArray(body)
                val byId = cryptoAssets.associateBy { it.coingeckoId }
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    val existing = byId[id] ?: continue
                    val currentPrice = obj.optDouble("current_price", existing.price)
                    val changePct = obj.optDouble("price_change_percentage_24h", existing.change24hPct)
                    val high = obj.optDouble("high_24h", existing.high24h)
                    val low = obj.optDouble("low_24h", existing.low24h)
                    val vol = obj.optDouble("total_volume", existing.volume24hUsd)
                    val mcap = obj.optDouble("market_cap", existing.marketCapUsd)
                    val image = obj.optString("image").takeIf { it.startsWith("http") }
                    val sparkPrices = mutableListOf<Double>()
                    obj.optJSONObject("sparkline_in_7d")?.optJSONArray("price")?.let { sp ->
                        val step = max(1, sp.length() / 24)
                        var idx = 0
                        while (idx < sp.length()) {
                            sparkPrices.add(sp.optDouble(idx, currentPrice))
                            idx += step
                        }
                    }
                    val prevClose = if (1.0 + changePct / 100.0 != 0.0) currentPrice / (1.0 + changePct / 100.0) else existing.prevClose
                    result[existing.symbol] = existing.copy(
                        price = currentPrice,
                        prevClose = prevClose,
                        change24hPct = changePct,
                        high24h = high,
                        low24h = low,
                        volume24hUsd = vol,
                        marketCapUsd = mcap,
                        iconUrl = image ?: existing.iconUrl,
                        sparkline = if (sparkPrices.size >= 4) sparkPrices else existing.sparkline,
                        quoteSource = "CoinGecko Live",
                        isLiveApiQuote = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("LiveMarketRepo", "CoinGecko quote fetch skipped: ${e.message}")
        }
        result
    }

    private suspend fun fetchYahooQuotes(): Map<String, MarketAsset> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, MarketAsset>()
        val targets = _assets.value.filter { it.yahooSymbol != null && it.assetClass != AssetClass.CRYPTO }
        coroutineScope {
            targets.chunked(10).forEach { chunk ->
                val fetched = chunk.map { asset ->
                    async {
                        fetchSingleYahooSummary(asset)
                    }
                }.awaitAll()
                fetched.filterNotNull().forEach { updated ->
                    result[updated.symbol] = updated
                }
                delay(60)
            }
        }
        result
    }

    suspend fun registerAndLoadAsset(asset: MarketAsset) {
        val refreshed = withContext(Dispatchers.IO) {
            if (asset.coingeckoId != null) {
                fetchCoinGeckoSingle(asset) ?: asset
            } else if (asset.yahooSymbol != null) {
                fetchSingleYahooSummary(asset) ?: asset
            } else asset
        }
        val withSpark = if (refreshed.sparkline.isNotEmpty()) refreshed
        else refreshed.copy(sparkline = buildSyntheticSeries(refreshed.prevClose, refreshed.price, 24, refreshed.symbol.hashCode()))

        val current = _assets.value
        val idx = current.indexOfFirst { it.symbol.equals(withSpark.symbol, ignoreCase = true) }
        _assets.value = if (idx >= 0) {
            current.toMutableList().apply { set(idx, withSpark) }
        } else {
            listOf(withSpark) + current
        }
    }

    suspend fun searchGlobalMarkets(query: String): List<MarketAsset> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.length < 2) return@withContext emptyList()
        val existingSymbols = _assets.value.map { it.symbol.uppercase() }.toSet()

        val yahooDeferred = async { searchYahooFinance(clean, existingSymbols) }
        val cgDeferred = async { searchCoinGecko(clean, existingSymbols) }
        val combined = (cgDeferred.await() + yahooDeferred.await()).distinctBy { it.symbol.uppercase() }
        combined.take(20)
    }

    private fun searchCoinGecko(query: String, existingSymbols: Set<String>): List<MarketAsset> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.coingecko.com/api/v3/search?query=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "ApexBrokerAndroid/1.0")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val coins = JSONObject(resp.body?.string() ?: return emptyList()).optJSONArray("coins") ?: return emptyList()
                val found = mutableListOf<MarketAsset>()
                for (i in 0 until minOf(coins.length(), 6)) {
                    val c = coins.optJSONObject(i) ?: continue
                    val sym = c.optString("symbol").uppercase()
                    val id = c.optString("id")
                    val name = c.optString("name")
                    val thumb = c.optString("large").ifBlank { c.optString("thumb") }.takeIf { it.startsWith("http") }
                    if (sym.isBlank() || id.isBlank() || existingSymbols.contains(sym)) continue
                    found.add(
                        MarketAsset(
                            symbol = sym,
                            name = name,
                            assetClass = AssetClass.CRYPTO,
                            sector = "Global Crypto",
                            price = 1.0,
                            prevClose = 1.0,
                            change24hPct = 0.0,
                            high24h = 1.0,
                            low24h = 1.0,
                            volume24hUsd = 0.0,
                            marketCapUsd = 0.0,
                            peRatio = null,
                            dividendYieldPct = 0.0,
                            analystConsensus = "Market",
                            analystBuyPct = 60,
                            analystHoldPct = 30,
                            analystSellPct = 10,
                            analystTargetPrice = 1.0,
                            iconUrl = thumb,
                            exchange = "GLOBAL CRYPTO",
                            region = "Global",
                            coingeckoId = id,
                            binanceSymbol = "${sym}USDT",
                            yahooSymbol = "$sym-USD",
                            quoteSource = "CoinGecko Global"
                        )
                    )
                }
                found
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun searchYahooFinance(query: String, existingSymbols: Set<String>): List<MarketAsset> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://query1.finance.yahoo.com/v1/finance/search?q=$encoded&quotesCount=14&newsCount=0"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val quotes = JSONObject(resp.body?.string() ?: return emptyList()).optJSONArray("quotes") ?: return emptyList()
                val found = mutableListOf<MarketAsset>()
                for (i in 0 until quotes.length()) {
                    val q = quotes.optJSONObject(i) ?: continue
                    val sym = q.optString("symbol").trim()
                    if (sym.isBlank() || existingSymbols.contains(sym.uppercase())) continue
                    val quoteType = q.optString("quoteType").uppercase()
                    val assetClass = when (quoteType) {
                        "EQUITY" -> AssetClass.STOCK
                        "ETF", "MUTUALFUND" -> AssetClass.ETF
                        "CRYPTOCURRENCY" -> AssetClass.CRYPTO
                        "CURRENCY" -> AssetClass.FOREX
                        "INDEX" -> AssetClass.INDEX
                        "FUTURE", "COMMODITY" -> AssetClass.COMMODITY
                        else -> continue
                    }
                    val name = q.optString("longname").ifBlank { q.optString("shortname").ifBlank { sym } }
                    val exch = q.optString("exchDisp").ifBlank { q.optString("exchange").ifBlank { "GLOBAL" } }
                    val sector = q.optString("sectorDisp").ifBlank { q.optString("industryDisp").ifBlank { assetClass.label } }
                    found.add(
                        MarketAsset(
                            symbol = sym,
                            name = name,
                            assetClass = assetClass,
                            sector = sector,
                            price = 100.0,
                            prevClose = 100.0,
                            change24hPct = 0.0,
                            high24h = 100.0,
                            low24h = 100.0,
                            volume24hUsd = 0.0,
                            marketCapUsd = 0.0,
                            peRatio = null,
                            dividendYieldPct = 0.0,
                            analystConsensus = "Market",
                            analystBuyPct = 60,
                            analystHoldPct = 30,
                            analystSellPct = 10,
                            analystTargetPrice = 100.0,
                            exchange = exch,
                            region = inferRegionFromSymbolAndExchange(sym, exch),
                            countryCode = inferCountryFromSymbol(sym),
                            yahooSymbol = sym,
                            quoteSource = "$exch Global Feed"
                        )
                    )
                }
                found
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchCoinGeckoSingle(asset: MarketAsset): MarketAsset? {
        val id = asset.coingeckoId ?: return null
        return try {
            val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=$id&sparkline=true&price_change_percentage=24h"
            val req = Request.Builder().url(url).header("Accept", "application/json").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val arr = JSONArray(resp.body?.string() ?: return null)
                val obj = arr.optJSONObject(0) ?: return null
                val currentPrice = obj.optDouble("current_price", asset.price)
                val changePct = obj.optDouble("price_change_percentage_24h", asset.change24hPct)
                val prevClose = if (1.0 + changePct / 100.0 != 0.0) currentPrice / (1.0 + changePct / 100.0) else asset.prevClose
                asset.copy(
                    price = currentPrice,
                    prevClose = prevClose,
                    change24hPct = changePct,
                    high24h = obj.optDouble("high_24h", currentPrice),
                    low24h = obj.optDouble("low_24h", currentPrice),
                    volume24hUsd = obj.optDouble("total_volume", 0.0),
                    marketCapUsd = obj.optDouble("market_cap", 0.0),
                    iconUrl = obj.optString("image").takeIf { it.startsWith("http") } ?: asset.iconUrl,
                    quoteSource = "CoinGecko Live",
                    isLiveApiQuote = true
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun inferCountryFromSymbol(sym: String): String {
        val suffix = sym.substringAfterLast(".", "")
        return when (suffix.uppercase()) {
            "L" -> "GB"
            "PA" -> "FR"
            "DE", "F" -> "DE"
            "SW" -> "CH"
            "CO" -> "DK"
            "T" -> "JP"
            "HK" -> "HK"
            "SS", "SZ" -> "CN"
            "NS", "BO" -> "IN"
            "KS", "KQ" -> "KR"
            "AX" -> "AU"
            "SA" -> "BR"
            "SR" -> "SA"
            "JO" -> "ZA"
            "TO" -> "CA"
            "MX" -> "MX"
            else -> "US"
        }
    }

    private fun inferRegionFromSymbolAndExchange(sym: String, exch: String): String {
        val uSym = sym.uppercase()
        val uExch = exch.uppercase()
        return when {
            uSym.endsWith(".L") || uSym.endsWith(".PA") || uSym.endsWith(".DE") || uSym.endsWith(".SW") ||
                uExch.contains("LONDON") || uExch.contains("XETRA") || uExch.contains("PARIS") -> "Europe"
            uSym.endsWith(".T") || uSym.endsWith(".HK") || uSym.endsWith(".NS") || uSym.endsWith(".KS") || uSym.endsWith(".AX") ||
                uExch.contains("TOKYO") || uExch.contains("NSE") || uExch.contains("HONG KONG") || uExch.contains("ASX") -> "Asia-Pacific"
            uSym.endsWith(".SR") || uSym.endsWith(".JO") || uExch.contains("SAUDI") || uExch.contains("JOHANNESBURG") -> "Middle East & Africa"
            uSym.endsWith(".SA") || uSym.endsWith(".MX") || uExch.contains("SAO PAULO") -> "Latin America"
            uExch.contains("NYQ") || uExch.contains("NMS") || uExch.contains("NYSE") || uExch.contains("NASDAQ") || uSym.endsWith(".TO") -> "North America"
            else -> "Global"
        }
    }

    private fun fetchSingleYahooSummary(asset: MarketAsset): MarketAsset? {
        val yahooSym = asset.yahooSymbol ?: return null
        return try {
            val encoded = URLEncoder.encode(yahooSym, "UTF-8")
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$encoded?range=5d&interval=1h"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val chartResult = root.optJSONObject("chart")?.optJSONArray("result")?.optJSONObject(0) ?: return null
                val meta = chartResult.optJSONObject("meta") ?: return null
                val price = meta.optDouble("regularMarketPrice", asset.price)
                val prevClose = meta.optDouble("chartPreviousClose", meta.optDouble("previousClose", asset.prevClose))
                if (price <= 0.0) return null
                val changePct = if (prevClose > 0.0) ((price - prevClose) / prevClose) * 100.0 else asset.change24hPct
                val dayHigh = meta.optDouble("regularMarketDayHigh", max(price, asset.high24h))
                val dayLow = meta.optDouble("regularMarketDayLow", asset.low24h)
                val dayVol = meta.optDouble("regularMarketVolume", 0.0) * price

                val spark = mutableListOf<Double>()
                val closes = chartResult.optJSONObject("indicators")
                    ?.optJSONArray("quote")
                    ?.optJSONObject(0)
                    ?.optJSONArray("close")
                if (closes != null) {
                    val step = max(1, closes.length() / 24)
                    var i = 0
                    while (i < closes.length()) {
                        val v = closes.optDouble(i, Double.NaN)
                        if (!v.isNaN() && v > 0.0) spark.add(v)
                        i += step
                    }
                }
                asset.copy(
                    price = price,
                    prevClose = prevClose,
                    change24hPct = changePct,
                    high24h = if (dayHigh > 0) dayHigh else asset.high24h,
                    low24h = if (dayLow > 0) dayLow else asset.low24h,
                    volume24hUsd = if (dayVol > 0) dayVol else asset.volume24hUsd,
                    sparkline = if (spark.size >= 4) spark else asset.sparkline,
                    quoteSource = "Market Feed Live",
                    isLiveApiQuote = true
                )
            }
        } catch (e: Exception) {
            Log.w("LiveMarketRepo", "Yahoo summary skipped for ${asset.symbol}: ${e.message}")
            null
        }
    }

    suspend fun loadAssetDetail(symbol: String, timeframe: Timeframe): AssetDetailData = withContext(Dispatchers.IO) {
        val asset = _assets.value.firstOrNull { it.symbol == symbol } ?: MarketCatalog.initialAssets.first()

        var candles: List<CandlePoint> = emptyList()
        var bids: List<OrderBookLevel> = emptyList()
        var asks: List<OrderBookLevel> = emptyList()
        var isLiveChart = false

        if (asset.binanceSymbol != null) {
            val binanceCandles = fetchBinanceKlines(asset.binanceSymbol, timeframe.binanceInterval)
            if (binanceCandles.isNotEmpty()) {
                candles = binanceCandles
                isLiveChart = true
            }
            val depth = fetchBinanceOrderBook(asset.binanceSymbol)
            if (depth != null) {
                bids = depth.first
                asks = depth.second
            }
        }

        if (candles.isEmpty() && asset.yahooSymbol != null) {
            val yahooCandles = fetchYahooCandles(asset.yahooSymbol, timeframe.yahooRange, timeframe.yahooInterval)
            if (yahooCandles.isNotEmpty()) {
                candles = yahooCandles
                isLiveChart = true
            }
        }

        if (candles.isEmpty()) {
            candles = buildSyntheticCandles(asset, 48)
        }

        if (bids.isEmpty() || asks.isEmpty()) {
            val syntheticBook = buildSyntheticOrderBook(asset.price)
            bids = syntheticBook.first
            asks = syntheticBook.second
        }

        val closes = candles.map { it.close }
        val sma20 = computeSMA(closes, 20)
        val rsi14 = computeRSI(closes, 14)
        val (macdLine, macdSignal) = computeMACD(closes)

        AssetDetailData(
            asset = asset,
            candles = candles,
            bids = bids,
            asks = asks,
            sma20 = sma20,
            rsi14 = rsi14,
            macdLine = macdLine,
            macdSignal = macdSignal,
            isLiveChart = isLiveChart
        )
    }

    private fun fetchBinanceKlines(binanceSymbol: String, interval: String): List<CandlePoint> {
        return try {
            val url = "https://api.binance.com/api/v3/klines?symbol=$binanceSymbol&interval=$interval&limit=48"
            val req = Request.Builder().url(url).header("Accept", "application/json").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val arr = JSONArray(resp.body?.string() ?: return emptyList())
                val out = mutableListOf<CandlePoint>()
                for (i in 0 until arr.length()) {
                    val row = arr.optJSONArray(i) ?: continue
                    out.add(
                        CandlePoint(
                            timestampMs = row.optLong(0),
                            open = row.optString(1).toDoubleOrNull() ?: 0.0,
                            high = row.optString(2).toDoubleOrNull() ?: 0.0,
                            low = row.optString(3).toDoubleOrNull() ?: 0.0,
                            close = row.optString(4).toDoubleOrNull() ?: 0.0,
                            volume = row.optString(5).toDoubleOrNull() ?: 0.0
                        )
                    )
                }
                out.filter { it.close > 0.0 }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchBinanceOrderBook(binanceSymbol: String): Pair<List<OrderBookLevel>, List<OrderBookLevel>>? {
        return try {
            val url = "https://api.binance.com/api/v3/depth?symbol=$binanceSymbol&limit=10"
            val req = Request.Builder().url(url).header("Accept", "application/json").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val obj = JSONObject(resp.body?.string() ?: return null)
                val bidsArr = obj.optJSONArray("bids") ?: return null
                val asksArr = obj.optJSONArray("asks") ?: return null
                val bids = (0 until bidsArr.length()).mapNotNull { i ->
                    val row = bidsArr.optJSONArray(i) ?: return@mapNotNull null
                    val p = row.optString(0).toDoubleOrNull() ?: return@mapNotNull null
                    val q = row.optString(1).toDoubleOrNull() ?: return@mapNotNull null
                    OrderBookLevel(p, q, p * q)
                }
                val asks = (0 until asksArr.length()).mapNotNull { i ->
                    val row = asksArr.optJSONArray(i) ?: return@mapNotNull null
                    val p = row.optString(0).toDoubleOrNull() ?: return@mapNotNull null
                    val q = row.optString(1).toDoubleOrNull() ?: return@mapNotNull null
                    OrderBookLevel(p, q, p * q)
                }
                if (bids.isNotEmpty() && asks.isNotEmpty()) bids to asks else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchYahooCandles(yahooSymbol: String, range: String, interval: String): List<CandlePoint> {
        return try {
            val encoded = URLEncoder.encode(yahooSymbol, "UTF-8")
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$encoded?range=$range&interval=$interval"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val root = JSONObject(resp.body?.string() ?: return emptyList())
                val res = root.optJSONObject("chart")?.optJSONArray("result")?.optJSONObject(0) ?: return emptyList()
                val timestamps = res.optJSONArray("timestamp") ?: return emptyList()
                val quote = res.optJSONObject("indicators")?.optJSONArray("quote")?.optJSONObject(0) ?: return emptyList()
                val opens = quote.optJSONArray("open") ?: return emptyList()
                val highs = quote.optJSONArray("high") ?: return emptyList()
                val lows = quote.optJSONArray("low") ?: return emptyList()
                val closes = quote.optJSONArray("close") ?: return emptyList()
                val vols = quote.optJSONArray("volume")
                val out = mutableListOf<CandlePoint>()
                for (i in 0 until timestamps.length()) {
                    val c = closes.optDouble(i, Double.NaN)
                    val o = opens.optDouble(i, c)
                    val h = highs.optDouble(i, c)
                    val l = lows.optDouble(i, c)
                    if (c.isNaN() || c <= 0.0) continue
                    out.add(
                        CandlePoint(
                            timestampMs = timestamps.optLong(i) * 1000L,
                            open = if (o.isNaN()) c else o,
                            high = if (h.isNaN()) c else h,
                            low = if (l.isNaN()) c else l,
                            close = c,
                            volume = vols?.optDouble(i, 0.0)?.takeIf { !it.isNaN() } ?: 0.0
                        )
                    )
                }
                out.takeLast(48)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildSyntheticSeries(start: Double, end: Double, count: Int, seed: Int): List<Double> {
        val list = mutableListOf<Double>()
        for (i in 0 until count) {
            val t = i.toDouble() / max(1, count - 1)
            val base = start + (end - start) * t
            val wave = sin(i * 0.65 + seed) * 0.008 * base
            list.add(max(0.0001, base + wave))
        }
        return list
    }

    private fun buildSyntheticCandles(asset: MarketAsset, count: Int): List<CandlePoint> {
        val series = buildSyntheticSeries(asset.prevClose * 0.985, asset.price, count, asset.symbol.hashCode())
        val now = System.currentTimeMillis()
        return series.mapIndexed { idx, close ->
            val open = if (idx == 0) close * 0.996 else series[idx - 1]
            val spread = max(close * 0.004, abs(close - open) * 0.6)
            CandlePoint(
                timestampMs = now - (count - idx) * 3_600_000L,
                open = open,
                high = max(open, close) + spread * 0.6,
                low = max(0.0001, kotlin.math.min(open, close) - spread * 0.5),
                close = close,
                volume = 1200.0 + (idx % 7) * 450.0
            )
        }
    }

    private fun buildSyntheticOrderBook(midPrice: Double): Pair<List<OrderBookLevel>, List<OrderBookLevel>> {
        val bids = (1..8).map { i ->
            val p = midPrice * (1.0 - i * 0.0005)
            val q = (12.0 / i) + (i * 1.3)
            OrderBookLevel(p, q, p * q)
        }
        val asks = (1..8).map { i ->
            val p = midPrice * (1.0 + i * 0.0005)
            val q = (11.5 / i) + (i * 1.1)
            OrderBookLevel(p, q, p * q)
        }
        return bids to asks
    }

    companion object {
        fun computeSMA(values: List<Double>, period: Int): List<Double?> {
            if (values.isEmpty()) return emptyList()
            return values.indices.map { i ->
                if (i + 1 < period) null
                else values.subList(i + 1 - period, i + 1).average()
            }
        }

        fun computeRSI(closes: List<Double>, period: Int = 14): List<Double?> {
            if (closes.size <= period) return List(closes.size) { null }
            val result = MutableList<Double?>(closes.size) { null }
            var avgGain = 0.0
            var avgLoss = 0.0
            for (i in 1..period) {
                val diff = closes[i] - closes[i - 1]
                if (diff >= 0) avgGain += diff else avgLoss += -diff
            }
            avgGain /= period
            avgLoss /= period
            result[period] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
            for (i in (period + 1) until closes.size) {
                val diff = closes[i] - closes[i - 1]
                val gain = if (diff > 0) diff else 0.0
                val loss = if (diff < 0) -diff else 0.0
                avgGain = ((avgGain * (period - 1)) + gain) / period
                avgLoss = ((avgLoss * (period - 1)) + loss) / period
                result[i] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
            }
            return result
        }

        fun computeEMA(values: List<Double>, period: Int): List<Double?> {
            if (values.size < period) return List(values.size) { null }
            val k = 2.0 / (period + 1)
            val result = MutableList<Double?>(values.size) { null }
            var ema = values.take(period).average()
            result[period - 1] = ema
            for (i in period until values.size) {
                ema = (values[i] - ema) * k + ema
                result[i] = ema
            }
            return result
        }

        fun computeMACD(closes: List<Double>): Pair<List<Double?>, List<Double?>> {
            val ema12 = computeEMA(closes, 12)
            val ema26 = computeEMA(closes, 26)
            val macdLine = closes.indices.map { i ->
                val a = ema12[i]
                val b = ema26[i]
                if (a != null && b != null) a - b else null
            }
            val validMacd = macdLine.filterNotNull()
            val signalRaw = computeEMA(validMacd, 9)
            val signal = MutableList<Double?>(closes.size) { null }
            var sIdx = 0
            for (i in closes.indices) {
                if (macdLine[i] != null) {
                    signal[i] = signalRaw.getOrNull(sIdx)
                    sIdx++
                }
            }
            return macdLine to signal
        }
    }
}
