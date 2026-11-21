package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.GoogleSignInButton
import com.example.data.localization.AppTranslationManager
import com.example.data.localization.AppTranslationManager.tr
import com.example.data.model.MarketAsset
import com.example.ui.components.MarketAssetIcon
import com.example.ui.theme.AmberDemo
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldGain
import com.example.util.Formatters

@Composable
fun AuthLandingScreen(
    previewAssets: List<MarketAsset>,
    onOpenLanguagePicker: () -> Unit = {},
    onOpenCurrencyPicker: () -> Unit = {},
    onAuthSuccess: () -> Unit,
    onTryFreeDemo: () -> Unit
) {
    var authError by remember { mutableStateOf<String?>(null) }
    val activeLang = AppTranslationManager.activeLanguage
    val activeCur = Formatters.activeCurrency

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // World Language & Currency Quick Selector Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenLanguagePicker)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("landing_language_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${activeLang.flagEmoji} ${activeLang.nativeName}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenCurrencyPicker)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("landing_currency_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = EmeraldGain, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${activeCur.flagEmoji} ${activeCur.code} (${activeCur.symbol})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            if (AppTranslationManager.isTranslating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)),
                    color = EmeraldGain
                )
            }

            // Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_terminal_1795228819096),
                    contentDescription = "ApexBroker institutional trading terminal",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.25f), Color(0xFF0A0E17).copy(alpha = 0.92f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tr("INSTITUTIONAL MULTI-ASSET BROKERAGE"),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldGain,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr("ApexBroker Terminal"),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr("Stocks, Crypto, Commodities, Forex & Indices with live market depth, KYC compliance, and dual Live/Demo ledgers."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            // Live Market Ticker Strip
            Text(
                text = tr("Live Global Markets"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(previewAssets.take(12), key = { it.symbol }) { asset ->
                    val pos = asset.change24hPct >= 0
                    Column(
                        modifier = Modifier
                            .width(148.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MarketAssetIcon(asset = asset, size = 28.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(asset.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = Formatters.formatAssetPrice(asset.price, asset.assetClass),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Formatters.formatPct(asset.change24hPct),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pos) EmeraldGain else CrimsonLoss,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Feature Highlights
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureRow(
                    Icons.Default.CandlestickChart,
                    tr("Multi-Asset & Fractional Execution"),
                    tr("Market, Limit, Stop-Loss, Stop-Limit, Trailing Stop & Bracket orders with RSI/MACD/SMA charts.")
                )
                FeatureRow(
                    Icons.Default.VerifiedUser,
                    tr("KYC & Treasury Verification"),
                    tr("Complete identity onboarding, crypto deposit proofs with QR codes, and admin-audited payouts.")
                )
                FeatureRow(
                    Icons.Default.Fingerprint,
                    tr("Biometric & 2FA Vault Security"),
                    tr("Hardware biometric lock, 2FA PIN protection, and seamless single-ID Live/Demo switching.")
                )
            }

            authError?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Dual-Entry Actions
            GoogleSignInButton(
                label = tr("Sign In / Open Real Account"),
                testTag = "landing_google_sign_in_button",
                onAuthSuccess = {
                    authError = null
                    onAuthSuccess()
                },
                onAuthError = { authError = it }
            )

            OutlinedButton(
                onClick = onTryFreeDemo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("try_free_demo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = AmberDemo,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${tr("Try Free Demo")} (${Formatters.formatUsd(10000.0)})",
                    color = AmberDemo,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
