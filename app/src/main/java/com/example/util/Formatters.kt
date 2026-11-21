package com.example.util

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.localization.ActiveCurrencyState
import com.example.data.localization.AppTranslationManager
import com.example.data.model.AssetClass
import com.google.firebase.Timestamp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {
    var activeCurrency by mutableStateOf(ActiveCurrencyState())
        private set

    fun syncCurrencyState(state: ActiveCurrencyState) {
        activeCurrency = state
    }

    fun convertFromUsd(usdValue: Double): Double = usdValue * activeCurrency.rateFromUsd

    fun convertToUsd(localValue: Double): Double {
        val rate = activeCurrency.rateFromUsd
        return if (rate > 0.0) localValue / rate else localValue
    }

    fun currencySymbol(): String = activeCurrency.symbol

    fun currencyCode(): String = activeCurrency.code

    fun formatUsd(valueUsd: Double, decimals: Int? = null): String {
        val cur = activeCurrency
        val converted = valueUsd * cur.rateFromUsd
        val dec = decimals ?: cur.decimals
        return formatLocalCurrencyAmount(converted, cur.symbol, dec)
    }

    fun formatLocalCurrencyAmount(
        localAmount: Double,
        symbol: String = activeCurrency.symbol,
        decimals: Int = activeCurrency.decimals
    ): String {
        val pattern = if (decimals <= 0) "#,##0" else "#,##0.${"0".repeat(decimals)}"
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat(pattern, symbols)
        val sign = if (localAmount < 0) "-" else ""
        val formattedNum = df.format(abs(localAmount))
        val sep = if (symbol.length > 1 && symbol.all { it.isLetter() }) " " else ""
        return "$sign$symbol$sep$formattedNum"
    }

    fun formatAssetPrice(valueUsd: Double, assetClass: AssetClass? = null): String {
        if (assetClass == AssetClass.FOREX) {
            return when {
                valueUsd >= 100.0 -> String.format(Locale.US, "%.2f", valueUsd)
                valueUsd >= 10.0 -> String.format(Locale.US, "%.3f", valueUsd)
                else -> String.format(Locale.US, "%.4f", valueUsd)
            }
        }
        val cur = activeCurrency
        val converted = valueUsd * cur.rateFromUsd
        val dec = when {
            converted >= 1000.0 -> cur.decimals.coerceAtMost(2)
            converted >= 10.0 -> cur.decimals.coerceAtLeast(2).coerceAtMost(2)
            converted >= 1.0 -> 4
            else -> 5
        }
        return formatLocalCurrencyAmount(converted, cur.symbol, dec)
    }

    fun formatPct(pct: Double): String {
        val sign = if (pct >= 0) "+" else ""
        return String.format(Locale.US, "%s%.2f%%", sign, pct)
    }

    fun formatCompactUsd(valueUsd: Double): String {
        val cur = activeCurrency
        val converted = valueUsd * cur.rateFromUsd
        val absVal = abs(converted)
        val sep = if (cur.symbol.length > 1 && cur.symbol.all { it.isLetter() }) " " else ""
        val prefix = "${if (converted < 0) "-" else ""}${cur.symbol}$sep"
        return when {
            absVal >= 1_000_000_000_000.0 -> String.format(Locale.US, "%s%.2fT", prefix, absVal / 1_000_000_000_000.0)
            absVal >= 1_000_000_000.0 -> String.format(Locale.US, "%s%.2fB", prefix, absVal / 1_000_000_000.0)
            absVal >= 1_000_000.0 -> String.format(Locale.US, "%s%.2fM", prefix, absVal / 1_000_000.0)
            absVal >= 1_000.0 -> String.format(Locale.US, "%s%.1fK", prefix, absVal / 1_000.0)
            else -> formatLocalCurrencyAmount(converted, cur.symbol, cur.decimals)
        }
    }

    fun formatQty(qty: Double): String {
        return when {
            qty >= 100.0 -> String.format(Locale.US, "%.2f", qty)
            qty >= 1.0 -> String.format(Locale.US, "%.4f", qty)
            else -> String.format(Locale.US, "%.6f", qty)
        }
    }

    fun formatTimestamp(ts: Timestamp?): String {
        val date = ts?.toDate() ?: Date()
        val loc = AppTranslationManager.activeLanguage.locale
        return SimpleDateFormat("MMM dd, HH:mm", loc).format(date)
    }

    fun shareStatementReport(context: Context, title: String, reportBody: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, reportBody)
        }
        context.startActivity(Intent.createChooser(intent, "Export $title"))
    }
}
