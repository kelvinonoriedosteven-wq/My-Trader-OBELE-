package com.example.data.market

import com.example.data.model.AssetClass
import com.example.data.model.AssetNewsItem
import com.example.data.model.MarketAsset
import com.example.data.model.MarketRegion

internal object GlobalCryptoCommoditiesFxCatalog {

    private fun crypto(
        symbol: String,
        name: String,
        sector: String,
        price: Double,
        changePct: Double,
        vol: Double,
        mcap: Double,
        target: Double,
        cgId: String,
        cgImageUrl: String,
        binance: String? = "${symbol}USDT",
        yahoo: String? = "$symbol-USD",
        news: List<AssetNewsItem> = emptyList()
    ): MarketAsset {
        val prev = if (1.0 + changePct / 100.0 != 0.0) price / (1.0 + changePct / 100.0) else price
        return MarketAsset(
            symbol = symbol,
            name = name,
            assetClass = AssetClass.CRYPTO,
            sector = sector,
            price = price,
            prevClose = prev,
            change24hPct = changePct,
            high24h = price * 1.024,
            low24h = price * 0.978,
            volume24hUsd = vol,
            marketCapUsd = mcap,
            peRatio = null,
            dividendYieldPct = 0.0,
            analystConsensus = if (changePct >= 0) "Strong Buy" else "Buy",
            analystBuyPct = 74,
            analystHoldPct = 19,
            analystSellPct = 7,
            analystTargetPrice = target,
            iconUrl = cgImageUrl,
            exchange = "GLOBAL CRYPTO",
            region = MarketRegion.GLOBAL.label,
            coingeckoId = cgId,
            binanceSymbol = binance,
            yahooSymbol = yahoo,
            news = news
        )
    }

    private fun commodity(
        symbol: String,
        name: String,
        sector: String,
        price: Double,
        changePct: Double,
        vol: Double,
        mcap: Double,
        target: Double,
        exchange: String = "COMEX / NYMEX",
        news: List<AssetNewsItem> = emptyList()
    ): MarketAsset {
        val prev = if (1.0 + changePct / 100.0 != 0.0) price / (1.0 + changePct / 100.0) else price
        return MarketAsset(
            symbol = symbol,
            name = name,
            assetClass = AssetClass.COMMODITY,
            sector = sector,
            price = price,
            prevClose = prev,
            change24hPct = changePct,
            high24h = price * 1.015,
            low24h = price * 0.986,
            volume24hUsd = vol,
            marketCapUsd = mcap,
            peRatio = null,
            dividendYieldPct = 0.0,
            analystConsensus = if (changePct >= 0) "Buy" else "Hold",
            analystBuyPct = 62,
            analystHoldPct = 28,
            analystSellPct = 10,
            analystTargetPrice = target,
            exchange = exchange,
            region = MarketRegion.GLOBAL.label,
            yahooSymbol = symbol,
            news = news
        )
    }

    private fun forex(
        symbol: String,
        name: String,
        region: MarketRegion,
        price: Double,
        changePct: Double,
        vol: Double,
        target: Double,
        yahooSymbol: String
    ): MarketAsset {
        val prev = if (1.0 + changePct / 100.0 != 0.0) price / (1.0 + changePct / 100.0) else price
        return MarketAsset(
            symbol = symbol,
            name = name,
            assetClass = AssetClass.FOREX,
            sector = "Global FX",
            price = price,
            prevClose = prev,
            change24hPct = changePct,
            high24h = price * 1.004,
            low24h = price * 0.996,
            volume24hUsd = vol,
            marketCapUsd = 0.0,
            peRatio = null,
            dividendYieldPct = 0.0,
            analystConsensus = "Hold",
            analystBuyPct = 48,
            analystHoldPct = 36,
            analystSellPct = 16,
            analystTargetPrice = target,
            exchange = "INTERBANK FX",
            region = region.label,
            yahooSymbol = yahooSymbol
        )
    }

    val cryptoAssets: List<MarketAsset> = listOf(
        crypto(
            "BTC", "Bitcoin", "Layer 1 Crypto", 94250.0, 2.65, 48_600_000_000.0, 1_865_000_000_000.0, 112000.0,
            "bitcoin", "https://coin-images.coingecko.com/coins/images/1/large/bitcoin.png",
            news = listOf(
                AssetNewsItem("btc1", "Spot ETF inflows reach 5-day high as institutional demand accelerates", "Bloomberg Markets", "2h ago", "Bullish"),
                AssetNewsItem("btc2", "Bitcoin hash rate hits fresh all-time milestone", "CoinDesk", "5h ago", "Bullish")
            )
        ),
        crypto(
            "ETH", "Ethereum", "Smart Contracts", 3340.5, 2.63, 21_400_000_000.0, 402_000_000_000.0, 4200.0,
            "ethereum", "https://coin-images.coingecko.com/coins/images/279/large/ethereum.png",
            news = listOf(AssetNewsItem("eth1", "Layer-2 settlement volume posts 34% quarterly expansion", "The Block", "3h ago", "Bullish"))
        ),
        crypto("SOL", "Solana", "High-Throughput L1", 218.4, 5.10, 6_850_000_000.0, 103_000_000_000.0, 265.0, "solana", "https://coin-images.coingecko.com/coins/images/4128/large/solana.png"),
        crypto("BNB", "BNB", "Exchange & L1", 642.15, 0.98, 2_100_000_000.0, 93_500_000_000.0, 720.0, "binancecoin", "https://coin-images.coingecko.com/coins/images/825/large/bnb-icon2_2x.png"),
        crypto("XRP", "XRP", "Payments Network", 1.48, 4.96, 4_920_000_000.0, 84_000_000_000.0, 1.85, "ripple", "https://coin-images.coingecko.com/coins/images/44/large/xrp-symbol-white-128.png"),
        crypto("USDT", "Tether USD", "Stablecoin", 1.0001, 0.01, 78_000_000_000.0, 128_000_000_000.0, 1.0, "tether", "https://coin-images.coingecko.com/coins/images/325/large/Tether.png", binance = null, yahoo = "USDT-USD"),
        crypto("ADA", "Cardano", "Layer 1 Crypto", 0.92, 3.85, 1_950_000_000.0, 32_400_000_000.0, 1.25, "cardano", "https://coin-images.coingecko.com/coins/images/975/large/cardano.png"),
        crypto("DOGE", "Dogecoin", "Payments & Community", 0.39, 4.12, 5_600_000_000.0, 57_000_000_000.0, 0.48, "dogecoin", "https://coin-images.coingecko.com/coins/images/5/large/dogecoin.png"),
        crypto("TRX", "TRON", "Payments & DeFi L1", 0.21, 1.94, 1_120_000_000.0, 18_100_000_000.0, 0.26, "tron", "https://coin-images.coingecko.com/coins/images/1094/large/tron-logo.png"),
        crypto("TON", "Toncoin", "Messaging Ecosystem L1", 5.65, 2.35, 380_000_000.0, 14_300_000_000.0, 7.10, "the-open-network", "https://coin-images.coingecko.com/coins/images/17980/large/ton_symbol.png"),
        crypto("AVAX", "Avalanche", "Subnet L1", 38.90, -1.77, 820_000_000.0, 15_900_000_000.0, 48.0, "avalanche-2", "https://coin-images.coingecko.com/coins/images/12559/large/Avalanche_Circle_RedWhite_Trans.png"),
        crypto("LINK", "Chainlink", "Oracle Infrastructure", 17.65, 3.70, 640_000_000.0, 11_100_000_000.0, 24.0, "chainlink", "https://coin-images.coingecko.com/coins/images/877/large/chainlink-new-logo.png"),
        crypto("DOT", "Polkadot", "Multichain Protocol", 7.82, 2.15, 420_000_000.0, 11_800_000_000.0, 10.50, "polkadot", "https://coin-images.coingecko.com/coins/images/12171/large/polkadot.png"),
        crypto("LTC", "Litecoin", "Digital Payments", 92.40, 1.80, 790_000_000.0, 6_950_000_000.0, 115.0, "litecoin", "https://coin-images.coingecko.com/coins/images/2/large/litecoin.png"),
        crypto("UNI", "Uniswap", "DeFi Exchange", 11.45, 2.90, 480_000_000.0, 6_880_000_000.0, 15.0, "uniswap", "https://coin-images.coingecko.com/coins/images/12504/large/uni.jpg"),
        crypto("NEAR", "NEAR Protocol", "AI & Sharded L1", 5.88, 4.45, 510_000_000.0, 7_120_000_000.0, 8.20, "near", "https://coin-images.coingecko.com/coins/images/10365/large/near.jpg"),
        crypto("SUI", "Sui", "Move Layer 1", 3.48, 6.20, 1_350_000_000.0, 9_900_000_000.0, 4.50, "sui", "https://coin-images.coingecko.com/coins/images/26375/large/sui-ocean-square.png"),
        crypto("SHIB", "Shiba Inu", "Ecosystem Token", 0.0000258, 3.10, 920_000_000.0, 15_200_000_000.0, 0.000032, "shiba-inu", "https://coin-images.coingecko.com/coins/images/11939/large/shiba.png")
    )

    val commodityAssets: List<MarketAsset> = listOf(
        commodity("GC=F", "Gold Futures", "Precious Metals", 2648.30, 1.09, 32_000_000_000.0, 18_200_000_000_000.0, 2820.0, "COMEX", listOf(AssetNewsItem("gold1", "Central bank bullion reserves expand for ninth consecutive month", "Financial Times", "4h ago", "Bullish"))),
        commodity("SI=F", "Silver Futures", "Precious Metals", 31.42, 1.75, 4_200_000_000.0, 1_760_000_000_000.0, 35.0, "COMEX"),
        commodity("PL=F", "Platinum Futures", "Precious Metals", 968.40, 0.84, 840_000_000.0, 260_000_000_000.0, 1080.0, "NYMEX"),
        commodity("PA=F", "Palladium Futures", "Precious Metals", 1012.50, -0.65, 510_000_000.0, 180_000_000_000.0, 1120.0, "NYMEX"),
        commodity("HG=F", "Copper Futures", "Industrial Metals", 4.18, 1.42, 3_600_000_000.0, 490_000_000_000.0, 4.65, "COMEX"),
        commodity("CL=F", "WTI Crude Oil", "Energy", 70.65, -1.19, 28_500_000_000.0, 2_800_000_000_000.0, 74.0, "NYMEX"),
        commodity("BZ=F", "Brent Crude Oil", "Energy", 74.20, -0.95, 24_200_000_000.0, 3_100_000_000_000.0, 78.5, "ICE EUROPE"),
        commodity("NG=F", "Natural Gas", "Energy", 3.18, 3.92, 5_100_000_000.0, 420_000_000_000.0, 3.60, "NYMEX"),
        commodity("ZW=F", "Wheat Futures", "Agriculture", 548.25, 0.72, 1_200_000_000.0, 160_000_000_000.0, 595.0, "CBOT"),
        commodity("ZC=F", "Corn Futures", "Agriculture", 428.50, 0.45, 1_850_000_000.0, 220_000_000_000.0, 460.0, "CBOT"),
        commodity("ZS=F", "Soybeans Futures", "Agriculture", 992.00, -0.38, 1_900_000_000.0, 245_000_000_000.0, 1060.0, "CBOT"),
        commodity("KC=F", "Coffee C Futures", "Agriculture", 295.40, 2.34, 980_000_000.0, 92_000_000_000.0, 315.0, "ICE US")
    )

    val forexAssets: List<MarketAsset> = listOf(
        forex("EUR/USD", "Euro / US Dollar", MarketRegion.EUROPE, 1.0584, 0.40, 142_000_000_000.0, 1.08, "EURUSD=X"),
        forex("GBP/USD", "British Pound / US Dollar", MarketRegion.EUROPE, 1.2692, 0.25, 96_000_000_000.0, 1.29, "GBPUSD=X"),
        forex("USD/JPY", "US Dollar / Japanese Yen", MarketRegion.ASIA_PACIFIC, 154.18, -0.48, 115_000_000_000.0, 149.5, "USDJPY=X"),
        forex("USD/CHF", "US Dollar / Swiss Franc", MarketRegion.EUROPE, 0.8842, -0.18, 44_000_000_000.0, 0.87, "USDCHF=X"),
        forex("AUD/USD", "Australian Dollar / USD", MarketRegion.ASIA_PACIFIC, 0.6515, 0.52, 52_000_000_000.0, 0.68, "AUDUSD=X"),
        forex("USD/CAD", "US Dollar / Canadian Dollar", MarketRegion.NORTH_AMERICA, 1.4018, -0.14, 48_000_000_000.0, 1.37, "USDCAD=X"),
        forex("EUR/GBP", "Euro / British Pound", MarketRegion.EUROPE, 0.8339, 0.12, 34_000_000_000.0, 0.84, "EURGBP=X"),
        forex("USD/CNY", "US Dollar / Chinese Yuan", MarketRegion.ASIA_PACIFIC, 7.2410, 0.08, 62_000_000_000.0, 7.15, "USDCNY=X"),
        forex("USD/INR", "US Dollar / Indian Rupee", MarketRegion.ASIA_PACIFIC, 84.42, 0.05, 39_000_000_000.0, 83.8, "USDINR=X"),
        forex("USD/KRW", "US Dollar / South Korean Won", MarketRegion.ASIA_PACIFIC, 1396.50, -0.22, 29_000_000_000.0, 1350.0, "USDKRW=X"),
        forex("USD/BRL", "US Dollar / Brazilian Real", MarketRegion.LATIN_AMERICA, 5.79, -0.35, 22_000_000_000.0, 5.55, "USDBRL=X"),
        forex("USD/MXN", "US Dollar / Mexican Peso", MarketRegion.LATIN_AMERICA, 20.35, 0.28, 31_000_000_000.0, 19.60, "USDMXN=X"),
        forex("USD/ZAR", "US Dollar / South African Rand", MarketRegion.MIDDLE_EAST_AFRICA, 18.12, -0.42, 18_500_000_000.0, 17.60, "USDZAR=X"),
        forex("USD/NGN", "US Dollar / Nigerian Naira", MarketRegion.MIDDLE_EAST_AFRICA, 1650.00, 0.15, 4_200_000_000.0, 1580.0, "USDNGN=X"),
        forex("USD/SAR", "US Dollar / Saudi Riyal", MarketRegion.MIDDLE_EAST_AFRICA, 3.7510, 0.01, 14_000_000_000.0, 3.75, "USDSAR=X"),
        forex("USD/AED", "US Dollar / UAE Dirham", MarketRegion.MIDDLE_EAST_AFRICA, 3.6725, 0.00, 16_000_000_000.0, 3.6725, "USDAED=X")
    )
}
