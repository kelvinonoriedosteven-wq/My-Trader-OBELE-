package com.example.data.localization

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

data class WorldLanguageOption(
    val bcp47Tag: String,
    val languageCode: String,
    val countryCode: String,
    val englishName: String,
    val nativeName: String,
    val countryName: String,
    val flagEmoji: String,
    val locale: Locale
)

object AppTranslationManager {
    private val KEY_LANGUAGE_TAG = stringPreferencesKey("preferred_language_tag")
    private const val CACHE_PREFIX = "lang_bundle_v2_"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var activeLanguage by mutableStateOf(
        WorldLanguageOption(
            bcp47Tag = "en-US",
            languageCode = "en",
            countryCode = "US",
            englishName = "English",
            nativeName = "English",
            countryName = "United States",
            flagEmoji = "🇺🇸",
            locale = Locale.US
        )
    )
        private set

    var activeTranslations by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var isTranslating by mutableStateOf(false)
        private set

    var translationStatus by mutableStateOf<String?>(null)
        private set

    // Curated high-frequency UI keys translated immediately in 2 fast parallel batches
    val coreUiPhrases: List<String> = listOf(
        // Navigation & Top Bar
        "Portfolio", "Markets", "Trade", "Activity", "Account",
        "ApexBroker", "KYC Verified", "KYC Pending", "KYC Rejected", "Unverified",
        "User App", "Admin", "DEMO MODE", "LIVE", "DEMO FUNDS",
        "Guest paper trading on real quotes", "Simulated ledger — zero real capital risk",
        "Open Real Account", "Go Live", "AI", "Language", "Currency",

        // Auth Landing
        "INSTITUTIONAL MULTI-ASSET BROKERAGE",
        "ApexBroker Terminal",
        "Stocks, Crypto, Commodities, Forex & Indices with live market depth, KYC compliance, and dual Live/Demo ledgers.",
        "Live Global Markets",
        "Multi-Asset & Fractional Execution",
        "Market, Limit, Stop-Loss, Stop-Limit, Trailing Stop & Bracket orders with RSI/MACD/SMA charts.",
        "KYC & Treasury Verification",
        "Complete identity onboarding, crypto deposit proofs with QR codes, and admin-audited payouts.",
        "Biometric & 2FA Vault Security",
        "Hardware biometric lock, 2FA PIN protection, and seamless single-ID Live/Demo switching.",
        "Sign In / Open Real Account",
        "Try Free Demo",

        // Portfolio Dashboard
        "DEMO PORTFOLIO VALUE", "TOTAL ACCOUNT EQUITY", "FROZEN",
        "Available Cash", "Locked / Pending", "Unrealized P&L", "Buying Power (2x)",
        "Margin Utilization", "Realized", "Deposit", "Withdraw", "KYC", "Statements",
        "Asset Allocation", "Active Positions", "No open positions",
        "Watchlist Pulse", "Cash",

        // Markets & Screener
        "Markets & Screener",
        "Search global ticker, company, crypto, exchange...",
        "All", "Watchlist", "All Regions",
        "North America", "Europe", "Asia-Pacific", "Middle East & Africa", "Latin America", "Global",
        "Crypto", "Stocks", "ETFs", "Commodities", "Forex", "Indices",
        "Featured", "Top Gainers", "Top Losers", "Market Cap", "Volume", "Div Yield",
        "Sector Heatmap", "Global Exchanges & Indices", "Search Worldwide Markets",
        "Searching global exchanges...", "Global Search Results",
        "Create Custom Watchlist", "Watchlist Name", "Create", "Cancel", "Save",

        // Asset Detail & Order Ticket
        "Loading market depth & chart...",
        "Candles", "Line", "Level 2 Book", "Financials", "News",
        "BID QTY", "PRICE", "ASK QTY",
        "Fundamentals & Analyst Consensus", "Market Cap", "24h Dollar Volume",
        "P/E Ratio", "Dividend / Staking Yield", "Analyst Rating", "Dividend History",
        "Demo Cash", "Live Cash", "Order Ticket",
        "BUY", "SELL", "Market", "Limit", "Stop-Loss", "Stop-Limit", "Trailing Stop", "Bracket",
        "Units", "Order Amount", "Quantity", "Limit Price", "Stop Price", "Take Profit Target",
        "Estimated Units", "Execution Price", "Estimated Fee", "Total Cost", "Est. Proceeds",
        "Available Buying Power", "Owned Position", "Price Alert", "Goes Above", "Drops Below", "Set Alert",

        // Activity & Funding
        "Positions", "Orders", "History",
        "Close Position", "No open positions in current mode.",
        "ALL", "OPEN", "FILLED", "CANCELED", "Modify Limit",
        "Live Crypto Deposits", "Crypto Deposit", "Switch to Live Account",
        "Select an admin-verified network wallet, transfer crypto, and submit your TXID proof.",
        "Transaction Hash (TXID)", "Upload Transfer Screenshot Proof", "Receipt Screenshot Attached",
        "Submit Deposit for Admin Approval",
        "Live Crypto Withdrawals", "KYC Verification Required", "Complete KYC Verification",
        "Crypto Withdrawal", "Destination Wallet Address", "Withdrawal Amount",
        "Lock Funds & Request Withdrawal",
        "Deposit Requests", "Withdrawal Requests", "Immutable Ledger",
        "Reports, Statements & Tax Center", "Export Statement & 1099-B Summary",
        "Recent Trade Confirmations (Contract Notes)",

        // Account, KYC, Localization & Security
        "Global Language & Currency",
        "App Language (Auto-Translate)",
        "Preferred Display Currency",
        "All prices, balances, and market totals convert automatically.",
        "Select App Language",
        "Select Preferred Currency",
        "Search language or country...",
        "Search currency or country...",
        "Upgrade to Live Account (Google Sign-In)",
        "Open Admin Control Panel",
        "Identity & KYC Compliance", "Verify", "Update",
        "Security & Terminal Settings",
        "Dark Obsidian Terminal", "Switch between institutional dark & light theme",
        "Biometric Hardware Lock", "2FA Withdrawal PIN", "DRIP Auto-Reinvestment",
        "Whitelisted Withdrawal Addresses", "Add trusted wallet address",
        "Ask 24/7 AI Support Concierge", "Learning Hub", "Support Ticket",
        "Sign Out", "Exit Guest Demo",
        "KYC Identity & Suitability", "Submit KYC", "Got It",

        // AI Support & Biometric
        "24/7 AI Support Concierge", "Powered by Gemini • Live Context",
        "Ask about orders, KYC, deposits, charts...",
        "ApexBroker Vault Locked",
        "Unlock with Biometrics", "Switch Account / Sign Out",
        "2FA Security Verification", "Enter your 6-digit security PIN to authorize this action."
    )

    private val pendingDynamicKeys = LinkedHashSet<String>()
    private var dynamicFlushScheduled = false
    private var appContext: Context? = null

    val allWorldLanguages: List<WorldLanguageOption> by lazy {
        val priorityTags = listOf(
            "en-US", "en-GB", "es-ES", "fr-FR", "ar-SA", "zh-CN", "hi-IN", "pt-BR",
            "ja-JP", "ko-KR", "de-DE", "it-IT", "ru-RU", "tr-TR", "yo-NG", "ig-NG",
            "ha-NG", "sw-KE", "bn-BD", "ur-PK", "id-ID", "vi-VN", "th-TH", "nl-NL",
            "pl-PL", "uk-UA", "fa-IR", "he-IL", "am-ET", "zu-ZA"
        )
        val priorityRank = priorityTags.withIndex().associate { it.value to it.index }

        val jvmLocales = Locale.getAvailableLocales()
            .asSequence()
            .filter { loc ->
                loc.language.isNotBlank() &&
                    loc.language != "und" &&
                    loc.country.length == 2 &&
                    loc.country.all { it.isLetter() }
            }
            .toList()

        // Ensure important world languages with country variants are always included even on trimmed JVMs
        val guaranteedTags = listOf(
            "en-US", "en-GB", "en-NG", "en-IN", "en-CA", "en-AU",
            "es-ES", "es-MX", "es-AR", "es-CO", "fr-FR", "fr-CA", "fr-SN",
            "pt-BR", "pt-PT", "ar-SA", "ar-AE", "ar-EG", "ar-MA",
            "zh-CN", "zh-TW", "zh-HK", "hi-IN", "bn-BD", "bn-IN", "ur-PK",
            "ja-JP", "ko-KR", "de-DE", "de-CH", "it-IT", "ru-RU", "uk-UA",
            "tr-TR", "nl-NL", "pl-PL", "sv-SE", "no-NO", "da-DK", "fi-FI",
            "el-GR", "cs-CZ", "ro-RO", "hu-HU", "th-TH", "vi-VN", "id-ID",
            "ms-MY", "fil-PH", "sw-KE", "yo-NG", "ig-NG", "ha-NG", "am-ET",
            "zu-ZA", "af-ZA", "fa-IR", "he-IL"
        ).map { Locale.forLanguageTag(it) }

        (jvmLocales + guaranteedTags)
            .mapNotNull { loc ->
                val tag = loc.toLanguageTag()
                if (tag.isBlank() || tag == "und") return@mapNotNull null
                val engLang = loc.getDisplayLanguage(Locale.ENGLISH).ifBlank { loc.language }
                val nativeLang = loc.getDisplayLanguage(loc).ifBlank { engLang }
                    .replaceFirstChar { it.uppercase() }
                val countryName = loc.getDisplayCountry(Locale.ENGLISH).ifBlank { loc.country }
                WorldLanguageOption(
                    bcp47Tag = tag,
                    languageCode = loc.language,
                    countryCode = loc.country.uppercase(Locale.US),
                    englishName = engLang.replaceFirstChar { it.uppercase() },
                    nativeName = nativeLang,
                    countryName = countryName,
                    flagEmoji = countryCodeToFlagEmoji(loc.country),
                    locale = loc
                )
            }
            .distinctBy { it.bcp47Tag }
            .sortedWith(
                compareBy<WorldLanguageOption> { priorityRank[it.bcp47Tag] ?: Int.MAX_VALUE }
                    .thenBy { it.englishName }
                    .thenBy { it.countryName }
            )
    }

    suspend fun initialize(context: Context) {
        appContext = context.applicationContext
        val savedTag = runCatching {
            context.apexPrefsDataStore.data.first()[KEY_LANGUAGE_TAG]
        }.getOrNull() ?: "en-US"
        selectLanguageByTag(context, savedTag)
    }

    suspend fun selectLanguage(context: Context, option: WorldLanguageOption) {
        appContext = context.applicationContext
        activeLanguage = option
        Locale.setDefault(option.locale)
        runCatching {
            context.apexPrefsDataStore.edit { prefs ->
                prefs[KEY_LANGUAGE_TAG] = option.bcp47Tag
            }
        }

        if (option.languageCode.equals("en", ignoreCase = true)) {
            activeTranslations = emptyMap()
            isTranslating = false
            translationStatus = null
            return
        }

        // Load from DataStore cache first for instant switching
        val cacheKey = stringPreferencesKey("$CACHE_PREFIX${option.bcp47Tag}")
        val cachedJson = runCatching {
            context.apexPrefsDataStore.data.first()[cacheKey]
        }.getOrNull()

        if (!cachedJson.isNullOrBlank()) {
            val parsed = parseJsonStringMap(cachedJson)
            if (parsed.isNotEmpty()) {
                activeTranslations = parsed
                if (parsed.size >= coreUiPhrases.size * 0.7) {
                    isTranslating = false
                    translationStatus = null
                    return
                }
            }
        }

        translateCoreCatalog(context, option)
    }

    suspend fun selectLanguageByTag(context: Context, bcp47Tag: String) {
        val match = allWorldLanguages.firstOrNull { it.bcp47Tag.equals(bcp47Tag, ignoreCase = true) }
            ?: allWorldLanguages.firstOrNull { it.languageCode.equals(bcp47Tag.substringBefore("-"), ignoreCase = true) }
            ?: activeLanguage
        selectLanguage(context, match)
    }

    private suspend fun translateCoreCatalog(context: Context, option: WorldLanguageOption) {
        isTranslating = true
        translationStatus = "Translating app to ${option.nativeName} (${option.countryName})..."
        try {
            val existing = activeTranslations.toMutableMap()
            val missing = coreUiPhrases.filter { !existing.containsKey(it) }
            if (missing.isNotEmpty()) {
                val chunks = missing.chunked(45)
                val translatedChunks = coroutineScope {
                    chunks.map { chunk ->
                        async(Dispatchers.IO) {
                            translateBatchWithGemini(chunk, option)
                        }
                    }.awaitAll()
                }
                translatedChunks.forEach { map -> existing.putAll(map) }
                if (activeLanguage.bcp47Tag == option.bcp47Tag) {
                    activeTranslations = existing.toMap()
                    persistTranslationCache(context, option.bcp47Tag, existing)
                }
            }
            translationStatus = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("AppTranslation", "Auto-translate failed for ${option.bcp47Tag}", e)
            translationStatus = "Locale active (${option.englishName})"
        } finally {
            isTranslating = false
        }
    }

    private suspend fun translateBatchWithGemini(
        phrases: List<String>,
        option: WorldLanguageOption
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, String>()
        val inputObj = JSONObject()
        phrases.forEachIndexed { idx, s -> inputObj.put(idx.toString(), s) }

        val model = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.1f
                responseMimeType = "application/json"
            }
        )

        val prompt = buildString {
            appendLine("Translate the following UI strings of a global financial brokerage app from English into ${option.englishName} (${option.nativeName}, region ${option.countryName}, locale ${option.bcp47Tag}).")
            appendLine("Return ONLY a valid JSON object with the exact same numeric string keys (\"0\", \"1\", ...) mapped to natural, idiomatic UI translations.")
            appendLine("Keep ticker symbols, currency codes, 'ApexBroker', 'KYC', '2FA', 'RSI', 'MACD', 'DRIP', 'ETF' clear and concise.")
            appendLine(inputObj.toString())
        }

        val response = withTimeout(30_000L) {
            model.generateContent(prompt)
        }
        val rawText = response.text?.trim().orEmpty()
        val jsonBody = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val outObj = JSONObject(jsonBody)
        phrases.forEachIndexed { idx, original ->
            val translated = outObj.optString(idx.toString()).trim()
            if (translated.isNotEmpty()) {
                result[original] = translated
            }
        }
        result
    }

    fun tr(englishText: String): String {
        val lang = activeLanguage
        if (lang.languageCode.equals("en", ignoreCase = true)) return englishText
        val found = activeTranslations[englishText]
        if (!found.isNullOrBlank()) return found

        // Queue dynamic untracked UI phrases for automatic background translation + caching
        if (englishText.length in 2..120 && !isTranslating) {
            synchronized(pendingDynamicKeys) {
                if (pendingDynamicKeys.size < 40 && pendingDynamicKeys.add(englishText) && !dynamicFlushScheduled) {
                    dynamicFlushScheduled = true
                    scope.launch {
                        kotlinx.coroutines.delay(450L)
                        flushDynamicTranslations()
                    }
                }
            }
        }
        return englishText
    }

    private suspend fun flushDynamicTranslations() {
        val batch: List<String>
        synchronized(pendingDynamicKeys) {
            batch = pendingDynamicKeys.toList()
            pendingDynamicKeys.clear()
            dynamicFlushScheduled = false
        }
        val option = activeLanguage
        if (batch.isEmpty() || option.languageCode.equals("en", ignoreCase = true)) return
        runCatching {
            val translated = translateBatchWithGemini(batch, option)
            if (translated.isNotEmpty() && activeLanguage.bcp47Tag == option.bcp47Tag) {
                val merged = activeTranslations + translated
                activeTranslations = merged
                appContext?.let { ctx -> persistTranslationCache(ctx, option.bcp47Tag, merged) }
            }
        }
    }

    private suspend fun persistTranslationCache(context: Context, bcp47Tag: String, map: Map<String, String>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        val key = stringPreferencesKey("$CACHE_PREFIX$bcp47Tag")
        runCatching {
            context.apexPrefsDataStore.edit { prefs ->
                prefs[key] = obj.toString()
            }
        }
    }

    private fun parseJsonStringMap(raw: String): Map<String, String> {
        return runCatching {
            val obj = JSONObject(raw)
            val out = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k)
                if (v.isNotBlank()) out[k] = v
            }
            out
        }.getOrDefault(emptyMap())
    }
}
