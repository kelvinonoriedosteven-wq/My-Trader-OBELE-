package com.example.data.model

enum class AssetClass(val label: String) {
    CRYPTO("Crypto"),
    STOCK("Stocks"),
    ETF("ETFs"),
    COMMODITY("Commodities"),
    FOREX("Forex"),
    INDEX("Indices")
}

enum class MarketRegion(val label: String) {
    ALL("All Regions"),
    NORTH_AMERICA("North America"),
    EUROPE("Europe"),
    ASIA_PACIFIC("Asia-Pacific"),
    MIDDLE_EAST_AFRICA("Middle East & Africa"),
    LATIN_AMERICA("Latin America"),
    GLOBAL("Global");
}

enum class Timeframe(val label: String, val binanceInterval: String, val yahooRange: String, val yahooInterval: String) {
    M15("15M", "15m", "1d", "15m"),
    H1("1H", "1h", "5d", "1h"),
    D1("1D", "4h", "1mo", "1d"),
    W1("1W", "1d", "6mo", "1wk"),
    M1("1M", "1w", "2y", "1mo")
}

data class CandlePoint(
    val timestampMs: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class OrderBookLevel(
    val price: Double,
    val quantity: Double,
    val totalUsd: Double
)

data class DividendRecord(
    val exDate: String,
    val amountPerShare: Double,
    val yieldPct: Double
)

data class AssetNewsItem(
    val id: String,
    val title: String,
    val source: String,
    val timeAgo: String,
    val sentiment: String
)

data class MarketAsset(
    val symbol: String,
    val name: String,
    val assetClass: AssetClass,
    val sector: String,
    val price: Double,
    val prevClose: Double,
    val change24hPct: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24hUsd: Double,
    val marketCapUsd: Double,
    val peRatio: Double?,
    val dividendYieldPct: Double,
    val analystConsensus: String,
    val analystBuyPct: Int,
    val analystHoldPct: Int,
    val analystSellPct: Int,
    val analystTargetPrice: Double,
    val iconUrl: String? = null,
    val websiteDomain: String? = null,
    val countryCode: String? = null,
    val exchange: String = "GLOBAL",
    val region: String = MarketRegion.GLOBAL.label,
    val coingeckoId: String? = null,
    val binanceSymbol: String? = null,
    val yahooSymbol: String? = null,
    val sparkline: List<Double> = emptyList(),
    val dividendHistory: List<DividendRecord> = emptyList(),
    val news: List<AssetNewsItem> = emptyList(),
    val quoteSource: String = "Live Feed",
    val isLiveApiQuote: Boolean = false
) {
    val bidPrice: Double get() = price * 0.9996
    val askPrice: Double get() = price * 1.0004
    val changeUsd: Double get() = price - prevClose
}

data class AssetDetailData(
    val asset: MarketAsset,
    val candles: List<CandlePoint>,
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
    val sma20: List<Double?>,
    val rsi14: List<Double?>,
    val macdLine: List<Double?>,
    val macdSignal: List<Double?>,
    val isLiveChart: Boolean
)
