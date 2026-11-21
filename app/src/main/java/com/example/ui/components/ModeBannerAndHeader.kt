package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.localization.AppTranslationManager
import com.example.data.localization.AppTranslationManager.tr
import com.example.data.model.KycStatus
import com.example.data.model.TradeMode
import com.example.ui.theme.AmberDemo
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldGain
import com.example.util.Formatters

@Composable
fun BrokerageTopBar(
    activeMode: TradeMode,
    kycStatus: KycStatus,
    isGuestDemo: Boolean,
    isAdmin: Boolean,
    isAdminViewActive: Boolean,
    onToggleMode: (TradeMode) -> Unit,
    onToggleAdminView: () -> Unit,
    onKycBadgeClick: () -> Unit,
    onOpenAiSupport: () -> Unit = {},
    onOpenLanguagePicker: () -> Unit = {},
    onOpenCurrencyPicker: () -> Unit = {}
) {
    val activeLang = AppTranslationManager.activeLanguage
    val activeCur = Formatters.activeCurrency
    val isTranslating = AppTranslationManager.isTranslating
    val translationStatus = AppTranslationManager.translationStatus

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("ApexBroker"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (!isGuestDemo) {
                        val (kycColor, kycText) = when (kycStatus) {
                            KycStatus.APPROVED -> EmeraldGain to tr("KYC Verified")
                            KycStatus.PENDING -> AmberDemo to tr("KYC Pending")
                            KycStatus.REJECTED -> CrimsonLoss to tr("KYC Rejected")
                            KycStatus.NOT_SUBMITTED -> MaterialTheme.colorScheme.onSurfaceVariant to tr("Unverified")
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(kycColor.copy(alpha = 0.14f))
                                .clickable(onClick = onKycBadgeClick)
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "KYC status",
                                tint = kycColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = kycText,
                                style = MaterialTheme.typography.labelSmall,
                                color = kycColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Preferred Currency Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                            .clickable(onClick = onOpenCurrencyPicker)
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                            .testTag("topbar_currency_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(activeCur.flagEmoji, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = activeCur.code,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Preferred Language Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                            .clickable(onClick = onOpenLanguagePicker)
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                            .testTag("topbar_language_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Select App Language",
                            tint = CobaltPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = activeLang.languageCode.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // AI Support Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(EmeraldGain.copy(alpha = 0.14f))
                            .clickable(onClick = onOpenAiSupport)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("ai_support_topbar_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Open AI Support",
                            tint = EmeraldGain,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = tr("AI"),
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGain,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isAdmin) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isAdminViewActive) CobaltPrimary else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable(onClick = onToggleAdminView)
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("admin_mode_toggle_pill"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Toggle Admin Control Center",
                                tint = if (isAdminViewActive) Color.White else CobaltPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = tr(if (isAdminViewActive) "User App" else "Admin"),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAdminViewActive) Color.White else CobaltPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Global Mode Switcher Pill (LIVE ACCOUNT vs DEMO MODE)
                    val isDemo = activeMode == TradeMode.DEMO
                    val pillColor = if (isDemo) AmberDemo else EmeraldGain
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(pillColor.copy(alpha = 0.16f))
                            .border(1.dp, pillColor.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .clickable {
                                if (!isGuestDemo) {
                                    onToggleMode(if (isDemo) TradeMode.LIVE else TradeMode.DEMO)
                                }
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("global_mode_switcher_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(pillColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr(if (isDemo) "DEMO MODE" else "LIVE"),
                            style = MaterialTheme.typography.labelSmall,
                            color = pillColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (!isGuestDemo) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch mode",
                                tint = pillColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isTranslating) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = EmeraldGain,
                        trackColor = MaterialTheme.colorScheme.outline
                    )
                    if (!translationStatus.isNullOrBlank()) {
                        Text(
                            text = translationStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGain,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DemoModeBanner(
    virtualBalance: Double,
    isGuestDemo: Boolean,
    onResetBalance: () -> Unit,
    onSwitchToLiveOrUpgrade: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmberDemo.copy(alpha = 0.15f))
            .border(width = 0.5.dp, color = AmberDemo.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${tr("DEMO FUNDS")} • ${Formatters.formatUsd(virtualBalance)}",
                style = MaterialTheme.typography.labelSmall,
                color = AmberDemo,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tr(if (isGuestDemo) "Guest paper trading on real quotes" else "Simulated ledger — zero real capital risk"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onResetBalance,
                modifier = Modifier.size(36.dp).testTag("demo_reset_balance_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset demo balance",
                    tint = AmberDemo,
                    modifier = Modifier.size(18.dp)
                )
            }
            TextButton(
                onClick = onSwitchToLiveOrUpgrade,
                modifier = Modifier.testTag("demo_banner_action_button")
            ) {
                Text(
                    text = tr(if (isGuestDemo) "Open Real Account" else "Go Live"),
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldGain,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
