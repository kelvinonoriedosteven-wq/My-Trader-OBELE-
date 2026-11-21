package com.example.data.market

import com.example.data.model.AssetClass
import com.example.data.model.AssetNewsItem
import com.example.data.model.DividendRecord
import com.example.data.model.MarketAsset
import com.example.data.model.MarketRegion

internal object GlobalEquitiesAndIndicesCatalog {

    private fun stock(
        symbol: String,
        name: String,
        sector: String,
        domain: String,
        country: String,
        exchange: String,
        region: MarketRegion,
        price: Double,
        changePct: Double,
        vol: Double,
        mcap: Double,
        pe: Double?,
        divYield: Double,
        target: Double,
        yahoo: String = symbol,
        dividends: List<DividendRecord> = emptyList(),
        news: List<AssetNewsItem> = emptyList()
    ): MarketAsset {
        val prev = if (1.0 + changePct / 100.0 != 0.0) price / (1.0 + changePct / 100.0) else price
        return MarketAsset(
            symbol = symbol,
            name = name,
            assetClass = AssetClass.STOCK,
            sector = sector,
            price = price,
            prevClose = prev,
            change24hPct = changePct,
            high24h = price * 1.016,
            low24h = price * 0.985,
            volume24hUsd = vol,
            marketCapUsd = mcap,
            peRatio = pe,
            dividendYieldPct = divYield,
            analystConsensus = if (changePct >= 0) "Buy" else "Hold",
            analystBuyPct = 74,
            analystHoldPct = 20,
            analystSellPct = 6,
            analystTargetPrice = target,
            websiteDomain = domain,
            countryCode = country,
            exchange = exchange,
            region = region.label,
            yahooSymbol = yahoo,
            dividendHistory = dividends,
            news = news
        )
    }

    private fun etf(
        symbol: String,
        name: String,
        sector: String,
        domain: String,
        country: String,
        exchange: String,
        region: MarketRegion,
        price: Double,
        changePct: Double,
        vol: Double,
        mcap: Double,
        pe: Double?,
        divYield: Double,
        target: Double,
        dividends: List<DividendRecord> = emptyList()
    ): MarketAsset {
        val prev = if (1.0 + changePct / 100.0 != 0.0) price / (1.0 + changePct / 100.0) else price
        return MarketAsset(
            symbol = symbol,
            name = name,
            assetClass = AssetClass.ETF,
            sector = sector,
            price = price,
            prevClose = prev,
            change24hPct = changePct,
            high24h = price * 1.012,
            low24h = price * 0.988,
            volume24hUsd = vol,
            marketCapUsd = mcap,
            peRatio = pe,
            dividendYieldPct = divYield,
            analystConsensus = "Buy",
            analystBuyPct = 76,
            analystHoldPct = 19,
            analystSellPct = 5,
            analystTargetPrice = target,
            websiteDomain = domain,
            countryCode = country,
            exchange = exchange,
            region = region.label,
            yahooSymbol = symbol,
            dividendHistory = dividends
        )
    }

    private fun index(
        symbol: String,
        name: String,
        sector: String,
        country: String,
        exchange: String,
        region: MarketRegion,
        price: Double,
        changePct: Double,
        mcap: Double,
        pe: Double?,
        divYield: Double,
        target: Double
    ): MarketAsset {
        val prev = if (1.0 + changePct / 100.0 != 0.0) price / (1.0 + changePct / 100.0) else price
        return MarketAsset(
            symbol = symbol,
            name = name,
            assetClass = AssetClass.INDEX,
            sector = sector,
            price = price,
            prevClose = prev,
            change24hPct = changePct,
            high24h = price * 1.009,
            low24h = price * 0.991,
            volume24hUsd = 0.0,
            marketCapUsd = mcap,
            peRatio = pe,
            dividendYieldPct = divYield,
            analystConsensus = "Buy",
            analystBuyPct = 69,
            analystHoldPct = 24,
            analystSellPct = 7,
            analystTargetPrice = target,
            countryCode = country,
            exchange = exchange,
            region = region.label,
            yahooSymbol = symbol
        )
    }

    val globalStocks: List<MarketAsset> = listOf(
        // North America
        stock("AAPL", "Apple Inc.", "Technology", "apple.com", "US", "NASDAQ", MarketRegion.NORTH_AMERICA, 229.80, 1.50, 9_450_000_000.0, 3_490_000_000_000.0, 34.2, 0.44, 252.0,
            dividends = listOf(DividendRecord("2026-08-11", 0.25, 0.44), DividendRecord("2026-05-12", 0.25, 0.45)),
            news = listOf(AssetNewsItem("aapl1", "Services revenue gross margin expands to record 74.2%", "Reuters", "1h ago", "Bullish"))
        ),
        stock("NVDA", "NVIDIA Corp.", "Semiconductors", "nvidia.com", "US", "NASDAQ", MarketRegion.NORTH_AMERICA, 144.65, 3.92, 28_900_000_000.0, 3_540_000_000_000.0, 52.8, 0.03, 168.0,
            dividends = listOf(DividendRecord("2026-09-12", 0.01, 0.03)),
            news = listOf(AssetNewsItem("nvda1", "Hyperscaler AI cluster orders extend data-center backlog visibility", "WSJ", "45m ago", "Bullish"))
        ),
        stock("MSFT", "Microsoft Corp.", "Technology", "microsoft.com", "US", "NASDAQ", MarketRegion.NORTH_AMERICA, 428.90, 1.13, 8_200_000_000.0, 3_190_000_000_000.0, 35.6, 0.78, 490.0,
            dividends = listOf(DividendRecord("2026-08-21", 0.83, 0.78))
        ),
        stock("TSLA", "Tesla Inc.", "Automotive & Energy", "tesla.com", "US", "NASDAQ", MarketRegion.NORTH_AMERICA, 338.25, -1.98, 19_600_000_000.0, 1_080_000_000_000.0, 88.4, 0.0, 325.0),
        stock("AMZN", "Amazon.com Inc.", "Cloud & Retail", "amazon.com", "US", "NASDAQ", MarketRegion.NORTH_AMERICA, 211.30, 1.64, 7_900_000_000.0, 2_210_000_000_000.0, 41.2, 0.0, 242.0),
        stock("GOOGL", "Alphabet Inc.", "Technology", "google.com", "US", "NASDAQ", MarketRegion.NORTH_AMERICA, 179.40, 1.24, 4_800_000_000.0, 2_200_000_000_000.0, 23.4, 0.45, 210.0),
        stock("META", "Meta Platforms", "Technology", "meta.com", "US", "NASDAQ", MarketRegion.NORTH_AMERICA, 586.20, 1.88, 6_100_000_000.0, 1_480_000_000_000.0, 27.5, 0.34, 650.0),
        stock("JPM", "JPMorgan Chase", "Financials", "jpmorganchase.com", "US", "NYSE", MarketRegion.NORTH_AMERICA, 244.10, 0.95, 2_100_000_000.0, 688_000_000_000.0, 13.8, 2.05, 265.0,
            dividends = listOf(DividendRecord("2026-10-04", 1.25, 2.05))
        ),
        stock("V", "Visa Inc.", "Financials", "visa.com", "US", "NYSE", MarketRegion.NORTH_AMERICA, 310.50, 0.78, 1_850_000_000.0, 620_000_000_000.0, 31.2, 0.76, 340.0),
        stock("LLY", "Eli Lilly & Co.", "Healthcare", "lilly.com", "US", "NYSE", MarketRegion.NORTH_AMERICA, 804.60, 1.42, 2_900_000_000.0, 765_000_000_000.0, 68.4, 0.65, 910.0),
        stock("SHOP", "Shopify Inc.", "Cloud & Retail", "shopify.com", "CA", "TSX / NYSE", MarketRegion.NORTH_AMERICA, 108.40, 2.65, 1_120_000_000.0, 140_000_000_000.0, 74.0, 0.0, 122.0),
        stock("RY", "Royal Bank of Canada", "Financials", "rbc.com", "CA", "TSX / NYSE", MarketRegion.NORTH_AMERICA, 124.80, 0.54, 410_000_000.0, 176_000_000_000.0, 14.6, 3.25, 134.0),

        // Europe & UK
        stock("ASML", "ASML Holding", "Semiconductors", "asml.com", "NL", "Euronext / NASDAQ", MarketRegion.EUROPE, 694.20, 2.18, 1_650_000_000.0, 275_000_000_000.0, 36.1, 0.96, 810.0),
        stock("MC.PA", "LVMH Moët Hennessy", "Luxury & Consumer", "lvmh.com", "FR", "Euronext Paris", MarketRegion.EUROPE, 638.50, 0.92, 680_000_000.0, 319_000_000_000.0, 22.8, 2.10, 725.0),
        stock("SAP.DE", "SAP SE", "Technology", "sap.com", "DE", "Xetra Frankfurt", MarketRegion.EUROPE, 232.10, 1.74, 590_000_000.0, 272_000_000_000.0, 44.5, 1.02, 260.0),
        stock("SIE.DE", "Siemens AG", "Industrials", "siemens.com", "DE", "Xetra Frankfurt", MarketRegion.EUROPE, 194.80, 0.66, 430_000_000.0, 155_000_000_000.0, 18.2, 2.55, 218.0),
        stock("NOVO-B.CO", "Novo Nordisk", "Healthcare", "novonordisk.com", "DK", "Nasdaq Copenhagen", MarketRegion.EUROPE, 108.40, -0.45, 720_000_000.0, 482_000_000_000.0, 34.8, 1.28, 128.0),
        stock("NESN.SW", "Nestlé S.A.", "Consumer Staples", "nestle.com", "CH", "SIX Swiss", MarketRegion.EUROPE, 89.20, 0.34, 490_000_000.0, 234_000_000_000.0, 19.6, 3.40, 98.0),
        stock("SHEL.L", "Shell plc", "Energy", "shell.com", "GB", "LSE London", MarketRegion.EUROPE, 32.85, -0.52, 620_000_000.0, 206_000_000_000.0, 10.4, 4.15, 37.5),
        stock("AZN.L", "AstraZeneca plc", "Healthcare", "astrazeneca.com", "GB", "LSE London", MarketRegion.EUROPE, 134.90, 1.15, 510_000_000.0, 209_000_000_000.0, 28.4, 2.20, 152.0),
        stock("HSBA.L", "HSBC Holdings", "Financials", "hsbc.com", "GB", "LSE London", MarketRegion.EUROPE, 9.48, 0.85, 440_000_000.0, 174_000_000_000.0, 8.9, 6.20, 10.6),

        // Asia-Pacific
        stock("TSM", "TSMC (Taiwan Semi)", "Semiconductors", "tsmc.com", "TW", "TWSE / NYSE", MarketRegion.ASIA_PACIFIC, 191.40, 2.84, 2_850_000_000.0, 992_000_000_000.0, 29.8, 1.32, 224.0),
        stock("7203.T", "Toyota Motor Corp.", "Automotive & Energy", "global.toyota", "JP", "TSE Tokyo", MarketRegion.ASIA_PACIFIC, 17.85, 0.95, 760_000_000.0, 240_000_000_000.0, 9.4, 2.85, 20.5),
        stock("6758.T", "Sony Group Corp.", "Technology", "sony.com", "JP", "TSE Tokyo", MarketRegion.ASIA_PACIFIC, 20.15, 1.62, 410_000_000.0, 122_000_000_000.0, 17.9, 0.65, 23.0),
        stock("9988.HK", "Alibaba Group", "Cloud & Retail", "alibabagroup.com", "HK", "HKEX Hong Kong", MarketRegion.ASIA_PACIFIC, 11.40, 1.95, 980_000_000.0, 218_000_000_000.0, 15.2, 1.10, 14.5),
        stock("0700.HK", "Tencent Holdings", "Technology", "tencent.com", "HK", "HKEX Hong Kong", MarketRegion.ASIA_PACIFIC, 52.80, 1.28, 1_150_000_000.0, 488_000_000_000.0, 20.4, 0.82, 62.0),
        stock("005930.KS", "Samsung Electronics", "Semiconductors", "samsung.com", "KR", "KRX Seoul", MarketRegion.ASIA_PACIFIC, 41.20, -0.75, 1_050_000_000.0, 276_000_000_000.0, 13.5, 2.45, 51.0),
        stock("RELIANCE.NS", "Reliance Industries", "Energy & Telecom", "ril.com", "IN", "NSE India", MarketRegion.ASIA_PACIFIC, 15.20, 0.88, 560_000_000.0, 206_000_000_000.0, 25.1, 0.42, 18.0),
        stock("TCS.NS", "Tata Consultancy", "Technology", "tcs.com", "IN", "NSE India", MarketRegion.ASIA_PACIFIC, 49.40, 1.10, 290_000_000.0, 179_000_000_000.0, 30.2, 1.55, 55.0),
        stock("BHP.AX", "BHP Group Ltd.", "Mining & Materials", "bhp.com", "AU", "ASX Sydney", MarketRegion.ASIA_PACIFIC, 26.40, 0.62, 340_000_000.0, 134_000_000_000.0, 13.2, 4.90, 30.0),

        // Middle East, Africa & Latin America
        stock("2222.SR", "Saudi Aramco", "Energy", "aramco.com", "SA", "Tadawul Riyadh", MarketRegion.MIDDLE_EAST_AFRICA, 7.42, 0.28, 320_000_000.0, 1_795_000_000_000.0, 15.8, 6.45, 8.10),
        stock("NPN.JO", "Naspers Limited", "Technology", "naspers.com", "ZA", "JSE Johannesburg", MarketRegion.MIDDLE_EAST_AFRICA, 231.50, 1.18, 145_000_000.0, 41_000_000_000.0, 22.4, 0.25, 260.0),
        stock("VALE", "Vale S.A.", "Mining & Materials", "vale.com", "BR", "B3 / NYSE", MarketRegion.LATIN_AMERICA, 10.18, 0.79, 410_000_000.0, 46_000_000_000.0, 5.8, 8.90, 12.80),
        stock("PETR4.SA", "Petrobras", "Energy", "petrobras.com.br", "BR", "B3 São Paulo", MarketRegion.LATIN_AMERICA, 6.45, 1.12, 380_000_000.0, 84_000_000_000.0, 4.9, 12.40, 7.50),
        stock("NU", "Nu Holdings (Nubank)", "Financials", "nubank.com.br", "BR", "NYSE", MarketRegion.LATIN_AMERICA, 14.85, 2.40, 620_000_000.0, 71_000_000_000.0, 39.2, 0.0, 17.50)
    )

    val globalEtfs: List<MarketAsset> = listOf(
        etf("SPY", "SPDR S&P 500 ETF", "Broad Equity ETF", "ssga.com", "US", "NYSE Arca", MarketRegion.NORTH_AMERICA, 594.20, 0.63, 26_000_000_000.0, 610_000_000_000.0, 24.8, 1.24, 635.0, listOf(DividendRecord("2026-09-20", 1.75, 1.24))),
        etf("QQQ", "Invesco QQQ Trust", "Tech Growth ETF", "invesco.com", "US", "NASDAQ", MarketRegion.NORTH_AMERICA, 508.40, 1.25, 18_400_000_000.0, 305_000_000_000.0, 31.4, 0.58, 550.0),
        etf("VTI", "Vanguard Total Stock", "Total Market ETF", "vanguard.com", "US", "NYSE Arca", MarketRegion.NORTH_AMERICA, 292.15, 0.60, 1_450_000_000.0, 448_000_000_000.0, 23.9, 1.35, 315.0),
        etf("EFA", "iShares MSCI EAFE", "Developed Intl ETF", "ishares.com", "EU", "NYSE Arca", MarketRegion.EUROPE, 81.40, 0.48, 1_120_000_000.0, 56_000_000_000.0, 15.2, 2.95, 88.0),
        etf("EEM", "iShares Emerging Mkts", "Emerging Mkts ETF", "ishares.com", "SG", "NYSE Arca", MarketRegion.GLOBAL, 44.15, 0.92, 1_480_000_000.0, 19_500_000_000.0, 13.8, 2.40, 49.0),
        etf("GLD", "SPDR Gold Shares", "Gold Bullion ETF", "ssga.com", "US", "NYSE Arca", MarketRegion.GLOBAL, 244.80, 1.05, 1_820_000_000.0, 74_000_000_000.0, null, 0.0, 265.0),
        etf("TLT", "iShares 20+ Yr Treasury", "Fixed Income Bond ETF", "ishares.com", "US", "NASDAQ", MarketRegion.NORTH_AMERICA, 91.65, 0.22, 3_400_000_000.0, 58_000_000_000.0, null, 4.18, 102.0),
        etf("IBIT", "iShares Bitcoin Trust", "Spot Bitcoin ETF", "ishares.com", "US", "NASDAQ", MarketRegion.GLOBAL, 53.60, 2.68, 2_900_000_000.0, 44_000_000_000.0, null, 0.0, 64.0)
    )

    val worldIndices: List<MarketAsset> = listOf(
        index("^GSPC", "S&P 500 Index", "US Equities Index", "US", "NYSE / NASDAQ", MarketRegion.NORTH_AMERICA, 5948.71, 0.54, 49_800_000_000_000.0, 25.1, 1.27, 6300.0),
        index("^IXIC", "NASDAQ Composite", "US Tech Index", "US", "NASDAQ", MarketRegion.NORTH_AMERICA, 19060.48, 1.16, 29_400_000_000_000.0, 32.6, 0.68, 20400.0),
        index("^DJI", "Dow Jones 30", "US Blue-Chip Index", "US", "NYSE", MarketRegion.NORTH_AMERICA, 43729.34, 0.42, 16_500_000_000_000.0, 22.8, 1.75, 46000.0),
        index("^GSPTSE", "S&P/TSX Composite", "Canada Index", "CA", "TSX Toronto", MarketRegion.NORTH_AMERICA, 25045.10, 0.49, 3_200_000_000_000.0, 17.4, 2.90, 26500.0),
        index("^FTSE", "FTSE 100 Index", "UK Equities Index", "GB", "LSE London", MarketRegion.EUROPE, 8262.10, 0.25, 2_650_000_000_000.0, 14.3, 3.65, 8650.0),
        index("^GDAXI", "DAX 40 Index", "Germany Equities Index", "DE", "Xetra Frankfurt", MarketRegion.EUROPE, 19254.80, 0.78, 2_100_000_000_000.0, 15.1, 2.85, 20400.0),
        index("^FCHI", "CAC 40 Index", "France Equities Index", "FR", "Euronext Paris", MarketRegion.EUROPE, 7338.90, 0.51, 2_380_000_000_000.0, 14.8, 3.10, 7800.0),
        index("^STOXX50E", "EURO STOXX 50", "Eurozone Blue-Chip", "EU", "Euronext / Eurex", MarketRegion.EUROPE, 4852.40, 0.62, 4_200_000_000_000.0, 14.9, 3.15, 5150.0),
        index("^SSMI", "Swiss Market SMI", "Switzerland Index", "CH", "SIX Zurich", MarketRegion.EUROPE, 11792.30, 0.35, 1_650_000_000_000.0, 18.2, 2.95, 12400.0),
        index("^N225", "Nikkei 225 Index", "Japan Equities Index", "JP", "TSE Tokyo", MarketRegion.ASIA_PACIFIC, 38535.70, 0.84, 5_800_000_000_000.0, 19.6, 1.72, 41500.0),
        index("^HSI", "Hang Seng Index", "Hong Kong Index", "HK", "HKEX Hong Kong", MarketRegion.ASIA_PACIFIC, 19688.20, 1.05, 3_400_000_000_000.0, 10.8, 3.55, 22000.0),
        index("000001.SS", "SSE Composite", "China Equities Index", "CN", "SSE Shanghai", MarketRegion.ASIA_PACIFIC, 3367.90, 0.44, 7_100_000_000_000.0, 12.6, 2.40, 3650.0),
        index("^NSEI", "NIFTY 50 Index", "India Equities Index", "IN", "NSE Mumbai", MarketRegion.ASIA_PACIFIC, 23907.25, 0.96, 2_750_000_000_000.0, 22.4, 1.25, 26000.0),
        index("^KS11", "KOSPI Composite", "South Korea Index", "KR", "KRX Seoul", MarketRegion.ASIA_PACIFIC, 2480.60, -0.32, 1_750_000_000_000.0, 11.4, 2.10, 2750.0),
        index("^AXJO", "S&P/ASX 200", "Australia Index", "AU", "ASX Sydney", MarketRegion.ASIA_PACIFIC, 8393.80, 0.47, 1_820_000_000_000.0, 18.5, 3.70, 8800.0),
        index("^BVSP", "Ibovespa Brazil", "LatAm Index", "BR", "B3 São Paulo", MarketRegion.LATIN_AMERICA, 129125.00, 0.68, 920_000_000_000.0, 8.9, 6.20, 140000.0)
    )
}
