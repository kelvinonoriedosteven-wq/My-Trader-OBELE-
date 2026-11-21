package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.localization.AppTranslationManager
import com.example.data.model.MarketAsset
import com.example.data.model.PortfolioPosition
import com.example.data.model.TradeMode
import com.example.data.model.UserProfile
import com.example.util.Formatters
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String
)

sealed interface AiSupportUiState {
    data object Idle : AiSupportUiState
    data object Loading : AiSupportUiState
    data class Streaming(val partialText: String) : AiSupportUiState
    data object Ready : AiSupportUiState
    data class Error(val message: String) : AiSupportUiState
}

class AiSupportViewModel : ViewModel() {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are ApexBroker AI Support, an institutional multi-asset retail brokerage concierge covering global stocks, ETFs, crypto, commodities, forex, and world indices. " +
                        "Help users with platform features (KYC verification, Crypto deposits with QR/TXID proofs, " +
                        "withdrawals & 2FA/biometric locks, Live vs Demo Mode switching, Market/Limit/Stop-Loss/" +
                        "Stop-Limit/Trailing Stop/Bracket orders, RSI/MACD/SMA20 charts, multi-currency conversion, and statements). " +
                        "ALWAYS respond in the user's PreferredLanguage specified in context, and express monetary values in their PreferredCurrency. " +
                        "Be concise, factual, structured, and clearly note that market analysis is educational and not personalized financial advice."
                )
            }
        )
    }

    private val _messages = MutableStateFlow(
        listOf(
            AiChatMessage(
                id = "welcome",
                isUser = false,
                text = "Hello! I'm your ApexBroker AI Support assistant. Ask me about placing advanced orders, global markets, KYC verification, crypto deposits/withdrawals, technical indicators, or your portfolio."
            )
        )
    )
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<AiSupportUiState>(AiSupportUiState.Idle)
    val uiState: StateFlow<AiSupportUiState> = _uiState.asStateFlow()

    fun clearChat() {
        _messages.value = listOf(
            AiChatMessage(
                id = "welcome_${System.currentTimeMillis()}",
                isUser = false,
                text = "Conversation cleared. How can I assist you with ApexBroker today?"
            )
        )
        _uiState.value = AiSupportUiState.Idle
    }

    fun sendSupportMessage(
        userPrompt: String,
        profile: UserProfile,
        positions: List<PortfolioPosition>,
        topAssets: List<MarketAsset>
    ) {
        val clean = userPrompt.trim()
        if (clean.isBlank() || _uiState.value is AiSupportUiState.Loading || _uiState.value is AiSupportUiState.Streaming) {
            return
        }

        val userMsg = AiChatMessage(id = "u_${System.currentTimeMillis()}", isUser = true, text = clean)
        val priorHistory: List<Content> = _messages.value
            .filter { it.id != "welcome" && !it.id.startsWith("welcome_") }
            .takeLast(8)
            .map { msg ->
                content(if (msg.isUser) "user" else "model") { text(msg.text) }
            }

        _messages.value = _messages.value + userMsg
        _uiState.value = AiSupportUiState.Loading

        val activeLang = AppTranslationManager.activeLanguage
        val activeCur = Formatters.activeCurrency
        val mode = profile.modeEnum
        val modeCash = if (mode == TradeMode.LIVE) profile.liveAvailableBalance else profile.demoAvailableBalance
        val modePositions = positions.filter { it.tradeMode == mode.name }
        val quotesSummary = topAssets.take(6).joinToString("; ") {
            "${it.symbol}=${Formatters.formatAssetPrice(it.price, it.assetClass)} (${Formatters.formatPct(it.change24hPct)})"
        }
        val contextSnapshot = buildString {
            append("[Account Context: PreferredLanguage=${activeLang.englishName} (${activeLang.bcp47Tag}), ")
            append("PreferredCurrency=${activeCur.code} (${activeCur.symbol}), ")
            append("Mode=${mode.name}, KYC=${profile.kycStatus}, ")
            append("AvailableCash=${Formatters.formatUsd(modeCash)}, ")
            append("LockedCash=${Formatters.formatUsd(profile.liveLockedBalance)}, ")
            append("OpenPositions=${modePositions.size}, ")
            append("2FA=${profile.twoFactorEnabled}, Biometric=${profile.biometricEnabled}. ")
            if (quotesSummary.isNotBlank()) append("Live Quotes: $quotesSummary.")
            append("]\nUser Question: $clean")
        }

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                val chat = model.startChat(history = priorHistory)
                withTimeout(60_000L) {
                    chat.sendMessageStream(contextSnapshot).collect { chunk ->
                        val part = chunk.text.orEmpty()
                        if (part.isNotEmpty()) {
                            accumulated.append(part)
                            _uiState.value = AiSupportUiState.Streaming(accumulated.toString())
                        }
                    }
                }
                val finalText = accumulated.toString().trim()
                if (finalText.isNotEmpty()) {
                    _messages.value = _messages.value + AiChatMessage(
                        id = "a_${System.currentTimeMillis()}",
                        isUser = false,
                        text = finalText
                    )
                    _uiState.value = AiSupportUiState.Ready
                } else {
                    _uiState.value = AiSupportUiState.Error("AI Support returned an empty response. Please try again.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Support request timed out", e)
                _uiState.value = AiSupportUiState.Error("AI Support timed out after 60 seconds. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI Logic error", e)
                _uiState.value = AiSupportUiState.Error(
                    e.localizedMessage ?: "Unable to reach AI Support right now."
                )
            }
        }
    }

    companion object {
        private const val TAG = "AiSupportVM"
    }
}
