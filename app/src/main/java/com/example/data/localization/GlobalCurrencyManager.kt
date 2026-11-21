package com.example.data.localization

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Currency
import java.util.Locale
import java.util.concurrent.TimeUnit

internal val Context.apexPrefsDataStore by preferencesDataStore(name = "apex_global_prefs")

data class WorldCurrencyOption(
    val code: String,
    val name: String,
    val symbol: String,
    val flagEmoji: String,
    val defaultDecimals: Int
)

data class ActiveCurrencyState(
    val code: String = "USD",
    val symbol: String = "$",
    val name: String = "US Dollar",
    val flagEmoji: String = "🇺🇸",
    val rateFromUsd: Double = 1.0,
    val decimals: Int = 2,
    val ratesUpdatedLabel: String = "Live FX"
)

object GlobalCurrencyManager {
    private val KEY_CURRENCY = stringPreferencesKey("preferred_currency_code")

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Comprehensive offline baseline rates vs 1 USD so every world currency converts instantaneously
    private val baselineUsdRates: MutableMap<String, Double> = mutableMapOf(
        "USD" to 1.0, "EUR" to 0.945, "GBP" to 0.788, "JPY" to 154.2, "CHF" to 0.884,
        "CAD" to 1.402, "AUD" to 1.535, "NZD" to 1.692, "CNY" to 7.24, "CNH" to 7.24,
        "HKD" to 7.78, "SGD" to 1.34, "INR" to 84.4, "KRW" to 1396.0, "TWD" to 32.5,
        "NGN" to 1650.0, "ZAR" to 18.1, "KES" to 129.0, "GHS" to 15.8, "EGP" to 49.3,
        "MAD" to 9.95, "TND" to 3.12, "ETB" to 121.0, "UGX" to 3670.0, "TZS" to 2640.0,
        "RWF" to 1365.0, "XOF" to 619.0, "XAF" to 619.0, "DZD" to 133.5, "ZMW" to 27.1,
        "SAR" to 3.75, "AED" to 3.6725, "QAR" to 3.64, "KWD" to 0.307, "BHD" to 0.376,
        "OMR" to 0.385, "JOD" to 0.709, "ILS" to 3.73, "TRY" to 34.4, "BRL" to 5.79,
        "MXN" to 20.35, "ARS" to 1002.0, "CLP" to 974.0, "COP" to 4410.0, "PEN" to 3.78,
        "UYU" to 42.1, "DOP" to 60.2, "CRC" to 512.0, "GTQ" to 7.72, "PYG" to 7780.0,
        "BOB" to 6.91, "PAB" to 1.0, "SEK" to 10.95, "NOK" to 11.04, "DKK" to 7.05,
        "PLN" to 4.10, "CZK" to 23.9, "HUF" to 386.0, "RON" to 4.70, "BGN" to 1.85,
        "ISK" to 138.0, "RSD" to 110.5, "UAH" to 41.3, "RUB" to 99.5, "KZT" to 495.0,
        "GEL" to 2.73, "AMD" to 388.0, "AZN" to 1.70, "PKR" to 277.8, "BDT" to 119.5,
        "LKR" to 292.0, "NPR" to 135.0, "THB" to 34.8, "VND" to 25400.0, "IDR" to 15880.0,
        "MYR" to 4.47, "PHP" to 58.8, "MMK" to 2100.0, "KHR" to 4065.0, "LAK" to 21900.0,
        "MNT" to 3410.0, "UZS" to 12800.0, "IQD" to 1310.0, "LBP" to 89500.0,
        "JMD" to 157.5, "TTD" to 6.78, "BBD" to 2.0, "BSD" to 1.0, "BMD" to 1.0,
        "FJD" to 2.26, "PGK" to 3.98, "MUR" to 46.5, "SCR" to 13.7, "BWP" to 13.5,
        "NAD" to 18.1, "SZL" to 18.1, "LSL" to 18.1, "MWK" to 1734.0, "MZN" to 63.9,
        "AOA" to 912.0, "CDF" to 2850.0, "GNF" to 8600.0, "SLL" to 22500.0, "SLE" to 22.5
    )

    private val specialCurrencyCountryOverrides = mapOf(
        "USD" to "US", "EUR" to "EU", "GBP" to "GB", "XOF" to "SN", "XAF" to "CM",
        "XCD" to "AG", "XPF" to "PF", "ANG" to "CW", "AUD" to "AU", "NZD" to "NZ",
        "CHF" to "CH", "ZAR" to "ZA", "INR" to "IN", "CNY" to "CN", "CNH" to "CN",
        "JPY" to "JP", "KRW" to "KR", "NGN" to "NG", "BRL" to "BR", "CAD" to "CA",
        "SAR" to "SA", "AED" to "AE", "MXN" to "MX", "TRY" to "TR", "SGD" to "SG",
        "HKD" to "HK", "TWD" to "TW", "SEK" to "SE", "NOK" to "NO", "DKK" to "DK",
        "PLN" to "PL", "RUB" to "RU", "UAH" to "UA", "EGP" to "EG", "KES" to "KE",
        "GHS" to "GH", "PKR" to "PK", "BDT" to "BD", "IDR" to "ID", "THB" to "TH",
        "VND" to "VN", "PHP" to "PH", "MYR" to "MY", "ARS" to "AR", "CLP" to "CL",
        "COP" to "CO", "PEN" to "PE", "ILS" to "IL"
    )

    val allWorldCurrencies: List<WorldCurrencyOption> by lazy {
        val priorityOrder = listOf(
            "USD", "EUR", "GBP", "JPY", "NGN", "INR", "CAD", "AUD", "CHF", "CNY",
            "AED", "SAR", "BRL", "ZAR", "KRW", "SGD", "HKD", "MXN", "TRY", "KES",
            "GHS", "EGP", "PLN", "SEK", "NOK", "DKK", "PHP", "IDR", "THB", "VND", "PKR"
        )
        val priorityRank = priorityOrder.withIndex().associate { it.value to it.index }

        Currency.getAvailableCurrencies()
            .asSequence()
            .filter { c ->
                val code = c.currencyCode
                code.length == 3 &&
                    !code.startsWith("X") && // filter non-circulating special units except keep real ones below
                    code != "XXX" && code != "XTS" &&
                    c.numericCode > 0
            }
            .plus(
                listOf("XOF", "XAF").mapNotNull { runCatching { Currency.getInstance(it) }.getOrNull() }
            )
            .distinctBy { it.currencyCode }
            .map { c ->
                val code = c.currencyCode
                val country = specialCurrencyCountryOverrides[code] ?: code.take(2)
                WorldCurrencyOption(
                    code = code,
                    name = c.getDisplayName(Locale.ENGLISH).replaceFirstChar { it.uppercase() },
                    symbol = c.getSymbol(Locale.ENGLISH).takeIf { it.isNotBlank() } ?: code,
                    flagEmoji = countryCodeToFlagEmoji(country),
                    defaultDecimals = c.defaultFractionDigits.coerceIn(0, 2)
                )
            }
            .sortedWith(
                compareBy<WorldCurrencyOption> { priorityRank[it.code] ?: Int.MAX_VALUE }
                    .thenBy { it.code }
            )
            .toList()
    }

    private val _liveRates = MutableStateFlow<Map<String, Double>>(baselineUsdRates.toMap())
    val liveRates: StateFlow<Map<String, Double>> = _liveRates.asStateFlow()

    private val _activeCurrency = MutableStateFlow(ActiveCurrencyState())
    val activeCurrency: StateFlow<ActiveCurrencyState> = _activeCurrency.asStateFlow()

    suspend fun initialize(context: Context) {
        val savedCode = runCatching {
            context.apexPrefsDataStore.data.first()[KEY_CURRENCY]
        }.getOrNull() ?: "USD"
        applyCurrencySelection(savedCode)
        refreshLiveFxRates()
    }

    suspend fun selectCurrency(context: Context, currencyCode: String) {
        val upper = currencyCode.uppercase(Locale.US)
        applyCurrencySelection(upper)
        runCatching {
            context.apexPrefsDataStore.edit { prefs ->
                prefs[KEY_CURRENCY] = upper
            }
        }
        if (_liveRates.value[upper] == null) {
            refreshLiveFxRates()
        }
    }

    private fun applyCurrencySelection(currencyCode: String) {
        val option = allWorldCurrencies.firstOrNull { it.code == currencyCode }
            ?: allWorldCurrencies.firstOrNull { it.code == "USD" }
            ?: WorldCurrencyOption("USD", "US Dollar", "$", "🇺🇸", 2)
        val rate = _liveRates.value[option.code] ?: baselineUsdRates[option.code] ?: 1.0
        _activeCurrency.value = ActiveCurrencyState(
            code = option.code,
            symbol = option.symbol,
            name = option.name,
            flagEmoji = option.flagEmoji,
            rateFromUsd = rate,
            decimals = option.defaultDecimals,
            ratesUpdatedLabel = if (option.code == "USD") "Base USD" else "1 USD = ${String.format(Locale.US, "%.4f", rate)} ${option.code}"
        )
    }

    suspend fun refreshLiveFxRates() = withContext(Dispatchers.IO) {
        val merged = _liveRates.value.toMutableMap()
        var updated = false

        // Primary: open.er-api.com (covers 160+ world currencies, zero API key)
        runCatching {
            val req = Request.Builder()
                .url("https://open.er-api.com/v6/latest/USD")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val ratesObj = JSONObject(body).optJSONObject("rates")
                        if (ratesObj != null) {
                            val keys = ratesObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                val v = ratesObj.optDouble(k, -1.0)
                                if (v > 0.0) {
                                    merged[k.uppercase(Locale.US)] = v
                                    updated = true
                                }
                            }
                        }
                    }
                }
            }
        }.onFailure {
            Log.w("GlobalCurrency", "Primary FX feed skipped: ${it.message}")
        }

        // Secondary fallback: api.frankfurter.app
        if (!updated) {
            runCatching {
                val req = Request.Builder()
                    .url("https://api.frankfurter.app/latest?from=USD")
                    .header("Accept", "application/json")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val ratesObj = JSONObject(body).optJSONObject("rates")
                            if (ratesObj != null) {
                                val keys = ratesObj.keys()
                                while (keys.hasNext()) {
                                    val k = keys.next()
                                    val v = ratesObj.optDouble(k, -1.0)
                                    if (v > 0.0) merged[k.uppercase(Locale.US)] = v
                                }
                            }
                        }
                    }
                }
            }
        }

        merged["USD"] = 1.0
        _liveRates.value = merged
        applyCurrencySelection(_activeCurrency.value.code)
    }

    fun getRate(code: String): Double =
        _liveRates.value[code.uppercase(Locale.US)]
            ?: baselineUsdRates[code.uppercase(Locale.US)]
            ?: 1.0

    fun convertFromUsd(usdAmount: Double): Double =
        usdAmount * _activeCurrency.value.rateFromUsd

    fun convertToUsd(localAmount: Double): Double {
        val r = _activeCurrency.value.rateFromUsd
        return if (r > 0.0) localAmount / r else localAmount
    }
}

fun countryCodeToFlagEmoji(countryCode: String?): String {
    val clean = countryCode?.trim()?.uppercase(Locale.US) ?: return "🌐"
    if (clean == "EU") return "🇪🇺"
    if (clean.length != 2 || !clean[0].isLetter() || !clean[1].isLetter()) return "🌐"
    val first = Character.codePointAt(clean, 0) - 'A'.code + 0x1F1E6
    val second = Character.codePointAt(clean, 1) - 'A'.code + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}
