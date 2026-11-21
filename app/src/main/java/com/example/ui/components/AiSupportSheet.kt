package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.localization.AppTranslationManager.tr
import com.example.data.model.MarketAsset
import com.example.data.model.PortfolioPosition
import com.example.data.model.UserProfile
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldGain
import com.example.ui.viewmodel.AiSupportUiState
import com.example.ui.viewmodel.AiSupportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSupportSheet(
    aiViewModel: AiSupportViewModel,
    profile: UserProfile,
    positions: List<PortfolioPosition>,
    marketAssets: List<MarketAsset>,
    initialPrompt: String? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val messages by aiViewModel.messages.collectAsStateWithLifecycle()
    val uiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember(initialPrompt) { mutableStateOf(initialPrompt.orEmpty()) }
    val listState = rememberLazyListState()

    val quickPrompts = listOf(
        "Review my account & KYC status",
        "How do Crypto deposits work?",
        "Explain Limit vs Bracket orders",
        "Summarize my open positions",
        "How do RSI and MACD signals help?"
    )

    LaunchedEffect(messages.size, uiState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = EmeraldGain,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(tr("ApexBroker AI Support"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(tr("24/7 Gemini Brokerage Concierge"), style = MaterialTheme.typography.labelSmall, color = EmeraldGain)
                    }
                }
                Row {
                    IconButton(onClick = { aiViewModel.clearChat() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear AI chat")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close AI Support")
                    }
                }
            }

            // Quick Prompt Chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPrompts.forEach { prompt ->
                    val localizedPrompt = tr(prompt)
                    FilterChip(
                        selected = false,
                        onClick = {
                            aiViewModel.sendSupportMessage(localizedPrompt, profile, positions, marketAssets)
                        },
                        label = { Text(localizedPrompt, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (msg.isUser) CobaltPrimary else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (msg.isUser) CobaltPrimary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
                        ) {
                                Text(
                                    text = if (msg.id == "welcome" || msg.id.startsWith("welcome_")) tr(msg.text) else msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                when (val st = uiState) {
                    is AiSupportUiState.Loading -> {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(tr("Analyzing your question..."), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    is AiSupportUiState.Streaming -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.88f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(1.dp, EmeraldGain.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Text(text = st.partialText, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    is AiSupportUiState.Error -> {
                        item {
                            Text(
                                text = "AI Support Error: ${st.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CrimsonLoss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CrimsonLoss.copy(alpha = 0.14f))
                                    .padding(12.dp)
                            )
                        }
                    }
                    else -> Unit
                }
            }

            // Input Row
            val isBusy = uiState is AiSupportUiState.Loading || uiState is AiSupportUiState.Streaming
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(tr("Ask AI Support about trading, KYC, funding...")) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.weight(1f).testTag("ai_support_input")
                )
                IconButton(
                    onClick = {
                        val toSend = inputText
                        inputText = ""
                        aiViewModel.sendSupportMessage(toSend, profile, positions, marketAssets)
                    },
                    enabled = !isBusy && inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isBusy && inputText.isNotBlank()) CobaltPrimary else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .testTag("ai_support_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send to AI Support",
                        tint = if (!isBusy && inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
