package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.localization.AppTranslationManager
import com.example.data.localization.AppTranslationManager.tr
import com.example.data.model.IdType
import com.example.data.model.KycStatus
import com.example.data.model.KycSubmission
import com.example.data.model.PriceAlert
import com.example.data.model.RiskTolerance
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
import com.example.ui.theme.AmberDemo
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldGain
import com.example.util.BiometricAuthManager
import com.example.util.Formatters
import com.example.util.ImagePickerHelper

@Composable
fun AccountKycSecurityScreen(
    profile: UserProfile,
    isGuestDemo: Boolean,
    isAdmin: Boolean,
    darkTheme: Boolean,
    kycSubmission: KycSubmission?,
    priceAlerts: List<PriceAlert>,
    tickets: List<SupportTicket>,
    onToggleTheme: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onSetupTwoFactor: (Boolean, String?) -> Unit,
    onToggleDrip: (Boolean) -> Unit,
    onAddWhitelistAddress: (String) -> Unit,
    onDeleteAlert: (String) -> Unit,
    onSubmitKyc: (KycSubmission) -> Unit,
    onSubmitTicket: (String, String, String) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onOpenAiSupport: () -> Unit = {},
    onOpenLanguagePicker: () -> Unit = {},
    onOpenCurrencyPicker: () -> Unit = {},
    onUpgradeGuest: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var showKycModal by remember { mutableStateOf(false) }
    var show2faSetupDialog by remember { mutableStateOf(false) }
    var showTicketDialog by remember { mutableStateOf(false) }
    var showLearningHub by remember { mutableStateOf(false) }
    var whitelistInput by remember { mutableStateOf("") }

    val activeLang = AppTranslationManager.activeLanguage
    val activeCur = Formatters.activeCurrency

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Summary
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(profile.displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(profile.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${profile.accountTier} • ${profile.activeMode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CobaltPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (isGuestDemo) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(onClick = onUpgradeGuest, modifier = Modifier.fillMaxWidth()) {
                            Text(tr("Upgrade to Live Account (Google Sign-In)"))
                        }
                    }
                }
            }

            // Global Language & Currency Preferences Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, CobaltPrimary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = CobaltPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Global Language & Currency"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = tr("All prices, balances, and market totals convert automatically."),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable(onClick = onOpenLanguagePicker)
                            .padding(12.dp)
                            .testTag("account_language_picker_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(activeLang.flagEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(tr("App Language (Auto-Translate)"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${activeLang.nativeName} • ${activeLang.countryName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable(onClick = onOpenCurrencyPicker)
                            .padding(12.dp)
                            .testTag("account_currency_picker_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(activeCur.flagEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(tr("Preferred Display Currency"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${activeCur.code} (${activeCur.symbol}) • ${activeCur.ratesUpdatedLabel}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = EmeraldGain)
                    }
                }
            }

            // Admin Control Center Entry
            if (isAdmin) {
                item {
                    Button(
                        onClick = onOpenAdminPanel,
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("open_admin_panel_button")
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Open Admin Control Panel"), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // KYC Verification Card
            if (!isGuestDemo) {
                item {
                    val kycColor = when (profile.kycEnum) {
                        KycStatus.APPROVED -> EmeraldGain
                        KycStatus.PENDING -> AmberDemo
                        KycStatus.REJECTED -> CrimsonLoss
                        KycStatus.NOT_SUBMITTED -> CobaltPrimary
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, kycColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_kyc_shield_1795228818668),
                            contentDescription = "KYC verification shield",
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(tr("Identity & KYC Compliance"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = profile.kycStatus,
                                style = MaterialTheme.typography.labelMedium,
                                color = kycColor,
                                fontWeight = FontWeight.Bold
                            )
                            if (profile.kycEnum == KycStatus.REJECTED && !profile.kycRejectionReason.isNullOrBlank()) {
                                Text(profile.kycRejectionReason, style = MaterialTheme.typography.labelSmall, color = CrimsonLoss)
                            }
                        }
                        if (profile.kycEnum != KycStatus.APPROVED) {
                            Button(
                                onClick = { showKycModal = true },
                                modifier = Modifier.testTag("start_kyc_button")
                            ) {
                                Text(tr(if (profile.kycEnum == KycStatus.PENDING) "Update" else "Verify"))
                            }
                        }
                    }
                }
            }

            // Security & Preferences
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldGain)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Security & Terminal Settings"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    SettingToggleRow(
                        tr("Dark Obsidian Terminal"),
                        tr("Switch between institutional dark & light theme"),
                        darkTheme,
                        onToggleTheme
                    )

                    if (!isGuestDemo) {
                        SettingToggleRow(
                            title = tr("Biometric Hardware Lock"),
                            subtitle = if (BiometricAuthManager.isBiometricAvailable(context)) "Fingerprint / Face / PIN"
                            else "Device credential verification",
                            checked = profile.biometricEnabled,
                            onCheckedChange = { wantEnable ->
                                if (wantEnable && BiometricAuthManager.isBiometricAvailable(context)) {
                                    BiometricAuthManager.authenticate(
                                        context = context,
                                        title = "Enable Biometric Security",
                                        subtitle = "Verify biometrics to activate vault lock",
                                        onSuccess = { onToggleBiometric(true) },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                    )
                                } else {
                                    onToggleBiometric(wantEnable)
                                }
                            }
                        )

                        SettingToggleRow(
                            title = tr("2FA Withdrawal PIN"),
                            subtitle = if (profile.twoFactorEnabled) "Active" else "6-Digit 2FA PIN",
                            checked = profile.twoFactorEnabled,
                            onCheckedChange = { wantEnable ->
                                if (wantEnable) show2faSetupDialog = true
                                else onSetupTwoFactor(false, null)
                            }
                        )

                        SettingToggleRow(
                            title = tr("DRIP Auto-Reinvestment"),
                            subtitle = tr("Dividend / Staking Yield"),
                            checked = profile.dripEnabled,
                            onCheckedChange = onToggleDrip
                        )
                    }
                }
            }

            // Whitelisted Addresses & Active Session Info
            if (!isGuestDemo) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(tr("Whitelisted Withdrawal Addresses"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = whitelistInput,
                                onValueChange = { whitelistInput = it },
                                label = { Text(tr("Add trusted wallet address")) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = {
                                if (whitelistInput.trim().length >= 8) {
                                    onAddWhitelistAddress(whitelistInput)
                                    whitelistInput = ""
                                }
                            }) { Text(tr("Save")) }
                        }
                        profile.whitelistedAddresses.forEach { addr ->
                            Text("• $addr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Active Price Alerts
                if (priceAlerts.isNotEmpty()) {
                    item {
                        Text("${tr("Price Alert")} (${priceAlerts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(priceAlerts, key = { it.id }) { alert ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${alert.symbol} ${alert.condition} ${Formatters.formatAssetPrice(alert.targetPrice)}", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { onDeleteAlert(alert.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete alert", tint = CrimsonLoss)
                            }
                        }
                    }
                }
            }

            // AI Support Concierge, Learning Hub & Support Ticket
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenAiSupport,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGain, contentColor = Color(0xFF042F1E)),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("account_ai_support_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Ask 24/7 AI Support Concierge"), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showLearningHub = true }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Default.School, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Learning Hub"))
                        }
                        if (!isGuestDemo) {
                            OutlinedButton(onClick = { showTicketDialog = true }, modifier = Modifier.weight(1f).height(48.dp)) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("Support Ticket"))
                            }
                        }
                    }
                }
            }

            if (tickets.isNotEmpty()) {
                item { Text("${tr("Support Ticket")} (${tickets.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(tickets, key = { it.id }) { t ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(t.status, style = MaterialTheme.typography.labelSmall, color = CobaltPrimary)
                        }
                        Text(t.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        t.adminReply?.let { reply ->
                            Text("Admin Reply: $reply", style = MaterialTheme.typography.labelSmall, color = EmeraldGain)
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = CrimsonLoss)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr(if (isGuestDemo) "Exit Guest Demo" else "Sign Out"), color = CrimsonLoss, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showKycModal) {
        KycSubmissionDialog(
            existing = kycSubmission,
            defaultName = profile.displayName,
            onDismiss = { showKycModal = false },
            onSubmit = {
                onSubmitKyc(it)
                showKycModal = false
            }
        )
    }

    if (show2faSetupDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { show2faSetupDialog = false },
            title = { Text(tr("2FA Withdrawal PIN")) },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it.filter(Char::isDigit) },
                    label = { Text("4-6 digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pin.length >= 4) {
                            onSetupTwoFactor(true, BiometricAuthManager.sha256Pin(pin))
                            show2faSetupDialog = false
                        }
                    }
                ) { Text(tr("Save")) }
            },
            dismissButton = { TextButton(onClick = { show2faSetupDialog = false }) { Text(tr("Cancel")) } }
        )
    }

    if (showTicketDialog) {
        var subject by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Funding") }
        var message by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTicketDialog = false },
            title = { Text(tr("Support Ticket")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Funding", "KYC", "Trading").forEach { cat ->
                            FilterChip(selected = category.startsWith(cat), onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Describe your issue") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (subject.trim().length >= 3 && message.trim().length >= 5) {
                        onSubmitTicket(subject, category, message)
                        showTicketDialog = false
                    }
                }) { Text(tr("Save")) }
            },
            dismissButton = { TextButton(onClick = { showTicketDialog = false }) { Text(tr("Cancel")) } }
        )
    }

    if (showLearningHub) {
        AlertDialog(
            onDismissRequest = { showLearningHub = false },
            title = { Text(tr("Learning Hub")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Market vs Limit Orders: Market executes immediately at best ask/bid; Limit guarantees your exact or better price.", style = MaterialTheme.typography.bodyMedium)
                    Text("• RSI (14) & MACD: RSI > 70 signals overbought momentum; RSI < 30 signals oversold potential.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Paper Trading Best Practice: Test stop-loss & bracket orders in Demo Mode before committing live funds.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { Button(onClick = { showLearningHub = false }) { Text(tr("Got It")) } }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun KycSubmissionDialog(
    existing: KycSubmission?,
    defaultName: String,
    onDismiss: () -> Unit,
    onSubmit: (KycSubmission) -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(existing?.fullName ?: defaultName) }
    var dob by remember { mutableStateOf(existing?.dateOfBirth ?: "1994-05-18") }
    var nationality by remember { mutableStateOf(existing?.nationality ?: AppTranslationManager.activeLanguage.countryName) }
    var taxResidency by remember { mutableStateOf(existing?.taxResidency ?: AppTranslationManager.activeLanguage.countryName) }
    var taxIdLast4 by remember { mutableStateOf(existing?.taxIdLast4 ?: "4821") }
    var address by remember { mutableStateOf(existing?.addressLine ?: "450 Lexington Ave, New York, NY") }
    var employment by remember { mutableStateOf(existing?.employmentStatus ?: "Employed") }
    var income by remember { mutableStateOf(existing?.annualIncomeRange ?: "$100,000 - $250,000") }
    var risk by remember { mutableStateOf(RiskTolerance.fromRaw(existing?.riskTolerance)) }
    var idType by remember { mutableStateOf(IdType.fromRaw(existing?.idType)) }
    var idFrontB64 by remember { mutableStateOf(existing?.idFrontBase64) }
    var idBackB64 by remember { mutableStateOf(existing?.idBackBase64) }
    var selfieB64 by remember { mutableStateOf(existing?.selfieBase64) }
    var proofName by remember { mutableStateOf(existing?.proofOfAddressName ?: "utility_bill.pdf") }

    val frontPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { idFrontB64 = ImagePickerHelper.uriToCompressedBase64(context, it) }
    }
    val backPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { idBackB64 = ImagePickerHelper.uriToCompressedBase64(context, it) }
    }
    val selfiePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { selfieB64 = ImagePickerHelper.uriToCompressedBase64(context, it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("KYC Identity & Suitability")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Legal Name") }, singleLine = true)
                OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("Date of Birth (YYYY-MM-DD)") }, singleLine = true)
                OutlinedTextField(value = nationality, onValueChange = { nationality = it }, label = { Text("Nationality") }, singleLine = true)
                OutlinedTextField(value = taxResidency, onValueChange = { taxResidency = it }, label = { Text("Tax Residency Country") }, singleLine = true)
                OutlinedTextField(value = taxIdLast4, onValueChange = { taxIdLast4 = it }, label = { Text("Tax ID / SSN Last 4") }, singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Residential Address") })
                OutlinedTextField(value = employment, onValueChange = { employment = it }, label = { Text("Employment Status") }, singleLine = true)
                OutlinedTextField(value = income, onValueChange = { income = it }, label = { Text("Annual Income Range") }, singleLine = true)

                Text("Risk Tolerance", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RiskTolerance.entries.forEach { r ->
                        FilterChip(selected = risk == r, onClick = { risk = r }, label = { Text(r.name) })
                    }
                }

                Text("Government ID Document", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IdType.entries.forEach { t ->
                        FilterChip(selected = idType == t, onClick = { idType = t }, label = { Text(t.label) })
                    }
                }

                OutlinedButton(
                    onClick = { frontPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (idFrontB64 != null) "ID Front Uploaded ✓" else "Upload ID Front")
                }
                OutlinedButton(
                    onClick = { backPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (idBackB64 != null) "ID Back Uploaded ✓" else "Upload ID Back")
                }
                OutlinedButton(
                    onClick = { selfiePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selfieB64 != null) "Live Selfie Uploaded ✓" else "Upload Selfie Verification")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        KycSubmission(
                            fullName = fullName,
                            dateOfBirth = dob,
                            nationality = nationality,
                            taxResidency = taxResidency,
                            taxIdLast4 = taxIdLast4,
                            addressLine = address,
                            riskTolerance = risk.name,
                            employmentStatus = employment,
                            annualIncomeRange = income,
                            idType = idType.name,
                            idFrontBase64 = idFrontB64,
                            idBackBase64 = idBackB64,
                            selfieBase64 = selfieB64,
                            proofOfAddressName = proofName
                        )
                    )
                },
                enabled = fullName.trim().length >= 2 && address.trim().length >= 5,
                modifier = Modifier.testTag("submit_kyc_button")
            ) { Text(tr("Submit KYC")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } }
    )
}
