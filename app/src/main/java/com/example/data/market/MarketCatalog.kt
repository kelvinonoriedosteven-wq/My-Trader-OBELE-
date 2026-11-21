package com.example.data.market

import com.example.data.model.MarketAsset

object MarketCatalog {
    val initialAssets: List<MarketAsset> =
        GlobalCryptoCommoditiesFxCatalog.cryptoAssets +
            GlobalEquitiesAndIndicesCatalog.globalStocks +
            GlobalEquitiesAndIndicesCatalog.globalEtfs +
            GlobalCryptoCommoditiesFxCatalog.commodityAssets +
            GlobalCryptoCommoditiesFxCatalog.forexAssets +
            GlobalEquitiesAndIndicesCatalog.worldIndices
}
