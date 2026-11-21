package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.localization.countryCodeToFlagEmoji
import com.example.data.model.AssetClass
import com.example.data.model.MarketAsset

@Composable
fun MarketAssetIcon(
    asset: MarketAsset,
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    MarketAssetIcon(
        symbol = asset.symbol,
        assetClass = asset.assetClass,
        iconUrl = asset.iconUrl,
        websiteDomain = asset.websiteDomain,
        countryCode = asset.countryCode,
        size = size,
        modifier = modifier
    )
}

@Composable
fun MarketAssetIcon(
    symbol: String,
    assetClass: AssetClass,
    iconUrl: String? = null,
    websiteDomain: String? = null,
    countryCode: String? = null,
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val resolvedIconUrl = iconUrl ?: websiteDomain?.takeIf { it.isNotBlank() }?.let { domain ->
        "https://www.google.com/s2/favicons?domain=$domain&sz=128"
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when (assetClass) {
            AssetClass.FOREX -> ForexDualFlagIcon(symbol = symbol, size = size)
            AssetClass.COMMODITY -> CommodityBadgeIcon(symbol = symbol, size = size)
            AssetClass.INDEX -> IndexBadgeIcon(symbol = symbol, countryCode = countryCode, size = size)
            AssetClass.CRYPTO -> {
                if (!resolvedIconUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = resolvedIconUrl,
                        contentDescription = "$symbol official icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF131C2E))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
                        error = { CryptoMonogramIcon(symbol = symbol, size = size) },
                        loading = { CryptoMonogramIcon(symbol = symbol, size = size) }
                    )
                } else {
                    CryptoMonogramIcon(symbol = symbol, size = size)
                }
            }
            AssetClass.STOCK, AssetClass.ETF -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!resolvedIconUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .padding(size * 0.14f),
                            contentAlignment = Alignment.Center
                        ) {
                            SubcomposeAsyncImage(
                                model = resolvedIconUrl,
                                contentDescription = "$symbol official company logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                error = { StockMonogramIcon(symbol = symbol, assetClass = assetClass, size = size) },
                                loading = { StockMonogramIcon(symbol = symbol, assetClass = assetClass, size = size) }
                            )
                        }
                    } else {
                        StockMonogramIcon(symbol = symbol, assetClass = assetClass, size = size)
                    }
                    // Country badge for non-US global stocks
                    if (!countryCode.isNullOrBlank() && countryCode != "US" && size >= 34.dp) {
                        Text(
                            text = countryCodeToFlagEmoji(countryCode),
                            fontSize = (size.value * 0.30f).sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CryptoMonogramIcon(symbol: String, size: Dp) {
    val (bg, fg, glyph) = when (symbol.uppercase()) {
        "BTC" -> Triple(Color(0xFFF7931A), Color.White, "₿")
        "ETH" -> Triple(Color(0xFF627EEA), Color.White, "Ξ")
        "USDT" -> Triple(Color(0xFF26A17B), Color.White, "₮")
        "SOL" -> Triple(Color(0xFF141824), Color(0xFF14F195), "S")
        "BNB" -> Triple(Color(0xFFF3BA2F), Color(0xFF12161C), "B")
        "XRP" -> Triple(Color(0xFF1E293B), Color(0xFF38BDF8), "X")
        "ADA" -> Triple(Color(0xFF0033AD), Color.White, "₳")
        "DOGE" -> Triple(Color(0xFFC2A633), Color.White, "Ð")
        "AVAX" -> Triple(Color(0xFFE84142), Color.White, "A")
        "LINK" -> Triple(Color(0xFF2A5ADA), Color.White, "⬡")
        "TON" -> Triple(Color(0xFF0098EA), Color.White, "T")
        "TRX" -> Triple(Color(0xFFEB0029), Color.White, "T")
        "SUI" -> Triple(Color(0xFF4DA2FF), Color.White, "S")
        else -> Triple(Color(0xFF3B82F6), Color.White, symbol.take(2).uppercase())
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.44f).sp
        )
    }
}

@Composable
private fun StockMonogramIcon(symbol: String, assetClass: AssetClass, size: Dp) {
    val cleanSym = symbol.substringBefore(".")
    val bg = when (cleanSym.uppercase()) {
        "AAPL" -> Color(0xFF1F2937)
        "NVDA" -> Color(0xFF76B900)
        "MSFT" -> Color(0xFF00A4EF)
        "TSLA" -> Color(0xFFCC0000)
        "AMZN" -> Color(0xFFFF9900)
        "GOOGL" -> Color(0xFF4285F4)
        "META" -> Color(0xFF0668E1)
        "JPM" -> Color(0xFF117ACA)
        "ASML" -> Color(0xFF0F238C)
        "MC" -> Color(0xFF1C1917)
        "SAP" -> Color(0xFF0FAAFF)
        "TSM" -> Color(0xFF991B1B)
        "7203" -> Color(0xFFEB0A1E)
        "SPY" -> Color(0xFF0F766E)
        "QQQ" -> Color(0xFF4338CA)
        "VTI" -> Color(0xFF9F1239)
        else -> if (assetClass == AssetClass.ETF) Color(0xFF0E7490) else Color(0xFF1D4ED8)
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(bg, bg.copy(alpha = 0.78f))))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val label = cleanSym.take(if (cleanSym.length <= 3) 3 else 2).uppercase()
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * (if (label.length >= 3) 0.27f else 0.34f)).sp
        )
    }
}

@Composable
private fun ForexDualFlagIcon(symbol: String, size: Dp) {
    val parts = symbol.split("/")
    val base = parts.getOrNull(0) ?: "EUR"
    val quote = parts.getOrNull(1) ?: "USD"
    val flagSize = size * 0.66f
    Box(modifier = Modifier.size(size)) {
        CurrencyFlagCircle(
            currency = quote,
            flagSize = flagSize,
            modifier = Modifier
                .size(flagSize)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
        CurrencyFlagCircle(
            currency = base,
            flagSize = flagSize,
            modifier = Modifier
                .size(flagSize)
                .align(Alignment.TopStart)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}

@Composable
private fun CurrencyFlagCircle(currency: String, flagSize: Dp, modifier: Modifier = Modifier) {
    val code = currency.uppercase()
    val vectorSupported = code in setOf("USD", "EUR", "GBP", "JPY", "CHF", "NGN", "CNY", "INR", "BRL", "SAR", "AED")
    if (vectorSupported) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            when (code) {
                "USD" -> {
                    drawRect(Color.White)
                    val stripeH = h / 7f
                    for (i in 0 until 7 step 2) {
                        drawRect(Color(0xFFB91C1C), topLeft = Offset(0f, i * stripeH), size = Size(w, stripeH))
                    }
                    drawRect(Color(0xFF1E3A8A), size = Size(w * 0.5f, h * 0.55f))
                }
                "EUR" -> {
                    drawRect(Color(0xFF003399))
                    drawCircle(color = Color(0xFFFFCC00), radius = w * 0.28f, style = Stroke(width = w * 0.08f))
                }
                "GBP" -> {
                    drawRect(Color(0xFF012169))
                    drawLine(Color.White, Offset(0f, 0f), Offset(w, h), strokeWidth = w * 0.16f)
                    drawLine(Color.White, Offset(w, 0f), Offset(0f, h), strokeWidth = w * 0.16f)
                    drawLine(Color.White, Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = w * 0.24f)
                    drawLine(Color.White, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = w * 0.24f)
                    drawLine(Color(0xFFC8102E), Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = w * 0.12f)
                    drawLine(Color(0xFFC8102E), Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = w * 0.12f)
                }
                "JPY" -> {
                    drawRect(Color.White)
                    drawCircle(Color(0xFFBC002D), radius = w * 0.28f)
                }
                "CHF" -> {
                    drawRect(Color(0xFFD52B1E))
                    drawRect(Color.White, topLeft = Offset(w * 0.41f, h * 0.22f), size = Size(w * 0.18f, h * 0.56f))
                    drawRect(Color.White, topLeft = Offset(w * 0.22f, h * 0.41f), size = Size(w * 0.56f, h * 0.18f))
                }
                "NGN" -> {
                    drawRect(Color(0xFF008751))
                    drawRect(Color.White, topLeft = Offset(w * 0.33f, 0f), size = Size(w * 0.34f, h))
                }
                "CNY" -> {
                    drawRect(Color(0xFFDE2910))
                    drawCircle(Color(0xFFFFDE00), radius = w * 0.16f, center = Offset(w * 0.32f, h * 0.35f))
                }
                "INR" -> {
                    drawRect(Color(0xFFFF9933), size = Size(w, h * 0.34f))
                    drawRect(Color.White, topLeft = Offset(0f, h * 0.33f), size = Size(w, h * 0.34f))
                    drawRect(Color(0xFF138808), topLeft = Offset(0f, h * 0.66f), size = Size(w, h * 0.34f))
                    drawCircle(Color(0xFF000080), radius = w * 0.13f, style = Stroke(width = w * 0.04f))
                }
                "BRL" -> {
                    drawRect(Color(0xFF009C3B))
                    drawCircle(Color(0xFFFFDF00), radius = w * 0.30f)
                    drawCircle(Color(0xFF002776), radius = w * 0.16f)
                }
                "SAR" -> {
                    drawRect(Color(0xFF006C35))
                    drawLine(Color.White, Offset(w * 0.25f, h * 0.62f), Offset(w * 0.75f, h * 0.62f), strokeWidth = h * 0.09f)
                }
                "AED" -> {
                    drawRect(Color(0xFF00732F), topLeft = Offset(w * 0.26f, 0f), size = Size(w * 0.74f, h * 0.34f))
                    drawRect(Color.White, topLeft = Offset(w * 0.26f, h * 0.33f), size = Size(w * 0.74f, h * 0.34f))
                    drawRect(Color(0xFF111111), topLeft = Offset(w * 0.26f, h * 0.66f), size = Size(w * 0.74f, h * 0.34f))
                    drawRect(Color(0xFFFF0000), size = Size(w * 0.28f, h))
                }
            }
        }
    } else {
        val country = when (code) {
            "AUD" -> "AU"; "CAD" -> "CA"; "NZD" -> "NZ"; "KRW" -> "KR"
            "ZAR" -> "ZA"; "MXN" -> "MX"; "SGD" -> "SG"; "HKD" -> "HK"
            "KES" -> "KE"; "GHS" -> "GH"; "EGP" -> "EG"; "TRY" -> "TR"
            else -> code.take(2)
        }
        Box(
            modifier = modifier.background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = countryCodeToFlagEmoji(country),
                fontSize = (flagSize.value * 0.62f).sp
            )
        }
    }
}

@Composable
private fun CommodityBadgeIcon(symbol: String, size: Dp) {
    val u = symbol.uppercase()
    val (bg, accent) = when {
        u.startsWith("GC") -> Color(0xFF3A2B05) to Color(0xFFFBBF24)
        u.startsWith("SI") -> Color(0xFF1E293B) to Color(0xFFE2E8F0)
        u.startsWith("PL") || u.startsWith("PA") -> Color(0xFF1E293B) to Color(0xFF93C5FD)
        u.startsWith("HG") -> Color(0xFF3B1A0E) to Color(0xFFEA580C)
        u.startsWith("CL") || u.startsWith("BZ") -> Color(0xFF0F172A) to Color(0xFF38BDF8)
        u.startsWith("NG") -> Color(0xFF172554) to Color(0xFF60A5FA)
        u.startsWith("ZW") || u.startsWith("ZC") || u.startsWith("ZS") -> Color(0xFF2E2307) to Color(0xFFFACC15)
        u.startsWith("KC") -> Color(0xFF2B1810) to Color(0xFFD97706)
        else -> Color(0xFF2D160B) to Color(0xFFFB923C)
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.50f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.22f)) {
            val w = this.size.width
            val h = this.size.height
            when {
                u.startsWith("GC") || u.startsWith("SI") || u.startsWith("PL") || u.startsWith("PA") || u.startsWith("HG") -> {
                    val path = Path().apply {
                        moveTo(w * 0.15f, h * 0.26f)
                        lineTo(w * 0.85f, h * 0.26f)
                        lineTo(w * 0.96f, h * 0.74f)
                        lineTo(w * 0.04f, h * 0.74f)
                        close()
                    }
                    drawPath(path, color = accent)
                }
                u.startsWith("CL") || u.startsWith("BZ") -> {
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(w * 0.2f, h * 0.1f),
                        size = Size(w * 0.6f, h * 0.8f),
                        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
                    )
                    drawLine(bg, Offset(w * 0.18f, h * 0.36f), Offset(w * 0.82f, h * 0.36f), strokeWidth = h * 0.08f)
                    drawLine(bg, Offset(w * 0.18f, h * 0.64f), Offset(w * 0.82f, h * 0.64f), strokeWidth = h * 0.08f)
                }
                u.startsWith("ZW") || u.startsWith("ZC") || u.startsWith("ZS") || u.startsWith("KC") -> {
                    drawOval(color = accent, topLeft = Offset(w * 0.24f, h * 0.14f), size = Size(w * 0.52f, h * 0.72f))
                    drawLine(bg, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.82f), strokeWidth = w * 0.09f)
                }
                else -> {
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.08f)
                        cubicTo(w * 0.95f, h * 0.45f, w * 0.85f, h * 0.92f, w * 0.5f, h * 0.92f)
                        cubicTo(w * 0.15f, h * 0.92f, w * 0.05f, h * 0.45f, w * 0.5f, h * 0.08f)
                        close()
                    }
                    drawPath(path, color = accent)
                }
            }
        }
    }
}

@Composable
private fun IndexBadgeIcon(symbol: String, countryCode: String?, size: Dp) {
    val (bg, code, resolvedCountry) = when (symbol.uppercase()) {
        "^GSPC" -> Triple(Color(0xFF0F294A), "SPX", "US")
        "^IXIC" -> Triple(Color(0xFF0E3A5D), "NDX", "US")
        "^DJI" -> Triple(Color(0xFF172554), "DJI", "US")
        "^GSPTSE" -> Triple(Color(0xFF450A0A), "TSX", "CA")
        "^FTSE" -> Triple(Color(0xFF1B2B4B), "UKX", "GB")
        "^GDAXI" -> Triple(Color(0xFF1E293B), "DAX", "DE")
        "^FCHI" -> Triple(Color(0xFF1E3A8A), "CAC", "FR")
        "^STOXX50E" -> Triple(Color(0xFF003399), "SX5E", "EU")
        "^SSMI" -> Triple(Color(0xFF7F1D1D), "SMI", "CH")
        "^N225" -> Triple(Color(0xFF3F0D12), "N225", "JP")
        "^HSI" -> Triple(Color(0xFF4C0519), "HSI", "HK")
        "000001.SS" -> Triple(Color(0xFF7C2D12), "SSE", "CN")
        "^NSEI" -> Triple(Color(0xFF064E3B), "NIFTY", "IN")
        "^KS11" -> Triple(Color(0xFF1E1B4B), "KOSPI", "KR")
        "^AXJO" -> Triple(Color(0xFF0C4A6E), "ASX", "AU")
        "^BVSP" -> Triple(Color(0xFF14532D), "IBOV", "BR")
        else -> Triple(Color(0xFF1E293B), symbol.removePrefix("^").take(3).uppercase(), countryCode ?: "US")
    }
    Box(modifier = Modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(bg)
                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = code.take(4),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * (if (code.length >= 4) 0.22f else 0.27f)).sp
            )
        }
        Text(
            text = countryCodeToFlagEmoji(resolvedCountry),
            fontSize = (size.value * 0.34f).sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
        )
    }
}
