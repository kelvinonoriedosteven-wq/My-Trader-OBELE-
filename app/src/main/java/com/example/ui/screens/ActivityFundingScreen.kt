package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.localization.AppTranslationManager.tr
import com.example.data.model.DepositRequest
import com.example.data.model.DepositWallet
import com.example.data.model.KycStatus
import com.example.data.model.LedgerEntry
import com.example.data.model.MarketAsset
import com.example.data.model.OrderSide
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PortfolioPosition
import com.example.data.model.RequestStatus
import com.example.data.model.TradeMode
import com.example.data.model.TradeOrder
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.MarketAssetIcon
import com.example.ui.components.TwoFactorVerificationDialog
import com.example.ui.theme.AmberDemo
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldGain
import com.example.util.BiometricAuthManager
import com.example.util.Formatters
import com.example.util.ImagePickerHelper
import com.example.util.QrCodeGenerator
import java.util.Locale

@Composable
fun ActivityFundingScreen(
    initialTab: Int = 0,
    profile: UserProfile,
    isGuestDemo: Boolean,
    marketAssets: List<MarketAsset>,
    positions: List<PortfolioPosition>,
    orders: List<TradeOrder>,
    depositWallets: List<DepositWallet>,
    deposits: List<DepositRequest>,
    withdrawals: List<WithdrawalRequest>,
    ledgerEntries: List<LedgerEntry>,
    onSwitchToLive: () -> Unit,
    onUpgradeGuestToReal: () -> Unit,
    onNavigateToKyc: () -> Unit,
    onClosePosition: (PortfolioPosition, MarketAsset) -> Unit,
    onTogglePositionDrip: (PortfolioPosition, Boolean) -> Unit,
    onCancelOrder: (TradeOrder) -> Unit,
    onModifyOrderLimit: (TradeOrder, Double) -> Unit,
    onSubmitDeposit: (DepositWallet, Double, Double, String, String?) -> Unit,
    onSubmitWithdrawal: (String, String, String, Double) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf("Positions", "Orders", "Deposit", "Withdraw", "History", "Statements")
    val activeMode = profile.modeEnum
    val assetMap = marketAssets.associateBy { it.symbol }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        label = { Text(tr(label)) },
                        modifier = Modifier.testTag("activity_tab_${label.lowercase()}")
                    )
                }
            }

            when (selectedTab) {
                0 -> PositionsTab(
                    mode = activeMode,
                    positions = positions.filter { it.tradeMode == activeMode.name },
                    assetMap = assetMap,
                    onClosePosition = onClosePosition,
                    onToggleDrip = onTogglePositionDrip
                )
                1 -> OrdersTab(
                    mode = activeMode,
                    orders = orders.filter { it.tradeMode == activeMode.name },
                    onCancelOrder = onCancelOrder,
                    onModifyLimit = onModifyOrderLimit
                )
                2 -> CryptoDepositTab(
                    activeMode = activeMode,
                    isGuestDemo = isGuestDemo,
                    wallets = depositWallets.filter { it.active },
                    marketAssets = marketAssets,
                    onSwitchToLive = onSwitchToLive,
                    onUpgradeGuest = onUpgradeGuestToReal,
                    onSubmitDeposit = onSubmitDeposit
                )
                3 -> CryptoWithdrawTab(
                    profile = profile,
                    isGuestDemo = isGuestDemo,
                    onSwitchToLive = onSwitchToLive,
                    onUpgradeGuest = onUpgradeGuestToReal,
                    onNavigateToKyc = onNavigateToKyc,
                    onSubmitWithdrawal = onSubmitWithdrawal
                )
                4 -> HistoryTab(deposits = deposits, withdrawals = withdrawals, ledger = ledgerEntries)
                else -> StatementsTaxTab(profile = profile, orders = orders, ledger = ledgerEntries)
            }
        }
    }
}

@Composable
private fun PositionsTab(
    mode: TradeMode,
    positions: List<PortfolioPosition>,
    assetMap: Map<String, MarketAsset>,
    onClosePosition: (PortfolioPosition, MarketAsset) -> Unit,
    onToggleDrip: (PortfolioPosition, Boolean) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "${tr("Open Positions")} (${mode.name} • ${positions.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        if (positions.isEmpty()) {
            item { EmptyStateCard("${tr("No open positions")} (${mode.name})") }
        } else {
            items(positions, key = { it.id.ifBlank { it.symbol } }) { pos ->
                val asset = assetMap[pos.symbol]
                val px = asset?.price ?: pos.avgEntryPrice
                val mktVal = pos.quantity * px
                val pnl = mktVal - pos.costBasis
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (asset != null) {
                                MarketAssetIcon(asset = asset, size = 36.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column {
                                Text("${pos.symbol} • ${pos.assetName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${tr("Qty")} ${Formatters.formatQty(pos.quantity)} • ${tr("Avg")} ${Formatters.formatAssetPrice(pos.avgEntryPrice, asset?.assetClass)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Formatters.formatUsd(mktVal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${if (pnl >= 0) "+" else ""}${Formatters.formatUsd(pnl)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pnl >= 0) EmeraldGain else CrimsonLoss,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("DRIP", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(checked = pos.dripEnabled, onCheckedChange = { onToggleDrip(pos, it) })
                        }
                        if (asset != null) {
                            Button(
                                onClick = { onClosePosition(pos, asset) },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonLoss, contentColor = Color.White)
                            ) {
                                Text(tr("Close Position"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersTab(
    mode: TradeMode,
    orders: List<TradeOrder>,
    onCancelOrder: (TradeOrder) -> Unit,
    onModifyLimit: (TradeOrder, Double) -> Unit
) {
    var editingOrder by remember { mutableStateOf<TradeOrder?>(null) }
    var filterStatus by remember { mutableStateOf("ALL") }

    val filtered = orders.filter { filterStatus == "ALL" || it.status == filterStatus }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("${tr("Orders")} & ${tr("Fills")} (${mode.name})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", "OPEN", "FILLED", "CANCELED").forEach { st ->
                    FilterChip(selected = filterStatus == st, onClick = { filterStatus = st }, label = { Text(tr(st)) })
                }
            }
        }
        if (filtered.isEmpty()) {
            item { EmptyStateCard("${tr("No orders found")} (${filterStatus.lowercase()} • ${mode.name})") }
        } else {
            items(filtered, key = { it.id }) { order ->
                val isBuy = order.side == OrderSide.BUY.name
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "${order.side} ${order.symbol} (${order.orderType})",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isBuy) EmeraldGain else CrimsonLoss,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(order.status),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (order.status) {
                                OrderStatus.FILLED.name -> EmeraldGain
                                OrderStatus.OPEN.name -> AmberDemo
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${tr("Qty")} ${Formatters.formatQty(order.quantity)} • ${tr("Notional")} ${Formatters.formatUsd(order.notionalUsd)} • ${order.timeInForce}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (order.limitPrice != null) {
                        Text("${tr("Limit")} ${Formatters.formatAssetPrice(order.limitPrice)}", style = MaterialTheme.typography.labelSmall, color = CobaltPrimary)
                    }
                    if (order.status == OrderStatus.OPEN.name) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (order.orderType == OrderType.LIMIT.name || order.orderType == OrderType.STOP_LIMIT.name) {
                                OutlinedButton(onClick = { editingOrder = order }) { Text(tr("Modify Limit")) }
                            }
                            OutlinedButton(onClick = { onCancelOrder(order) }) { Text(tr("Cancel"), color = CrimsonLoss) }
                        }
                    }
                }
            }
        }
    }

    editingOrder?.let { order ->
        var newLimit by remember(order.id, Formatters.activeCurrency.code) {
            val localVal = Formatters.convertFromUsd(order.limitPrice ?: 0.0)
            mutableStateOf(String.format(Locale.US, "%.2f", localVal))
        }
        AlertDialog(
            onDismissRequest = { editingOrder = null },
            title = { Text("${tr("Modify Limit Price")}: ${order.symbol}") },
            text = {
                OutlinedTextField(
                    value = newLimit,
                    onValueChange = { newLimit = it },
                    label = { Text("${tr("New Limit Price")} (${Formatters.currencyCode()})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = {
                Button(onClick = {
                    newLimit.toDoubleOrNull()?.let { localPx ->
                        if (localPx > 0) {
                            onModifyLimit(order, Formatters.convertToUsd(localPx))
                            editingOrder = null
                        }
                    }
                }) { Text(tr("Save")) }
            },
            dismissButton = { TextButton(onClick = { editingOrder = null }) { Text(tr("Cancel")) } }
        )
    }
}

@Composable
private fun CryptoDepositTab(
    activeMode: TradeMode,
    isGuestDemo: Boolean,
    wallets: List<DepositWallet>,
    marketAssets: List<MarketAsset>,
    onSwitchToLive: () -> Unit,
    onUpgradeGuest: () -> Unit,
    onSubmitDeposit: (DepositWallet, Double, Double, String, String?) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    if (isGuestDemo || activeMode == TradeMode.DEMO) {
        ModeRestrictionCard(
            title = tr("Live Crypto Deposits"),
            message = if (isGuestDemo) tr("Guest Demo sessions use virtual funds only. Open a real account to make crypto deposits.")
            else tr("You are currently in Demo Mode. Switch back to Live Account to deposit real funds."),
            buttonLabel = if (isGuestDemo) tr("Open Real Account") else tr("Switch to Live Account"),
            onAction = if (isGuestDemo) onUpgradeGuest else onSwitchToLive
        )
        return
    }

    var selectedWalletIdx by remember { mutableIntStateOf(0) }
    val wallet = wallets.getOrNull(selectedWalletIdx)
    var amountLocalText by remember(Formatters.activeCurrency.code) {
        mutableStateOf(String.format(Locale.US, "%.0f", Formatters.convertFromUsd(500.0)))
    }
    var txHashText by remember { mutableStateOf("") }
    var proofBase64 by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            proofBase64 = ImagePickerHelper.uriToCompressedBase64(context, uri)
            Toast.makeText(context, "Receipt attached", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(tr("Crypto Deposit"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(tr("Select an admin-verified network wallet, transfer crypto, and submit your TXID proof."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (wallets.isEmpty()) {
            item {
                EmptyStateCard(tr("No active deposit wallets configured by Admin yet. Admin can seed or add wallets in Admin Panel -> Wallets."))
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wallets.forEachIndexed { idx, w ->
                        FilterChip(
                            selected = selectedWalletIdx == idx,
                            onClick = { selectedWalletIdx = idx },
                            label = { Text("${w.symbol} (${w.network})") }
                        )
                    }
                }
            }

            if (wallet != null) {
                val coinPrice = marketAssets.firstOrNull { it.symbol.equals(wallet.symbol, ignoreCase = true) }?.price ?: 1.0
                val localVal = amountLocalText.toDoubleOrNull() ?: 0.0
                val usdVal = Formatters.convertToUsd(localVal)
                val cryptoQty = if (coinPrice > 0) usdVal / coinPrice else usdVal
                item {
                    val qrBitmap = remember(wallet.qrPayload) { QrCodeGenerator.generateQrBitmap(wallet.qrPayload, 440) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("${wallet.symbol} • ${wallet.network} ${tr("Deposit Address")}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "Deposit QR Code",
                                modifier = Modifier.size(164.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = wallet.address,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(wallet.address))
                                    Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy address", modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = "${tr("Min Deposit")}: ${Formatters.formatUsd(wallet.minDeposit)} • ${wallet.confirmationsRequired} ${tr("confirmations required")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = amountLocalText,
                        onValueChange = { amountLocalText = it },
                        label = { Text("${tr("Deposit Amount")} (${Formatters.currencyCode()})") },
                        supportingText = { Text("≈ ${Formatters.formatQty(cryptoQty)} ${wallet.symbol}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = txHashText,
                        onValueChange = { txHashText = it },
                        label = { Text(tr("Transaction Hash (TXID)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("deposit_txid_input")
                    )
                }

                item {
                    OutlinedButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (proofBase64 != null) tr("Receipt Screenshot Attached") else tr("Upload Transfer Screenshot Proof"))
                    }
                }

                item {
                    Button(
                        onClick = {
                            onSubmitDeposit(wallet, usdVal, cryptoQty, txHashText, proofBase64)
                            txHashText = ""
                            proofBase64 = null
                        },
                        enabled = usdVal >= wallet.minDeposit && txHashText.trim().length >= 6,
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_deposit_button")
                    ) {
                        Text(tr("Submit Deposit for Admin Approval"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CryptoWithdrawTab(
    profile: UserProfile,
    isGuestDemo: Boolean,
    onSwitchToLive: () -> Unit,
    onUpgradeGuest: () -> Unit,
    onNavigateToKyc: () -> Unit,
    onSubmitWithdrawal: (String, String, String, Double) -> Unit
) {
    val context = LocalContext.current
    if (isGuestDemo || profile.modeEnum == TradeMode.DEMO) {
        ModeRestrictionCard(
            title = tr("Live Crypto Withdrawals"),
            message = if (isGuestDemo) tr("Guest Demo funds cannot be withdrawn. Open a real account to trade & withdraw live funds.")
            else tr("Switch to Live Account to request external crypto withdrawals."),
            buttonLabel = if (isGuestDemo) tr("Open Real Account") else tr("Switch to Live Account"),
            onAction = if (isGuestDemo) onUpgradeGuest else onSwitchToLive
        )
        return
    }

    if (profile.kycEnum != KycStatus.APPROVED) {
        ModeRestrictionCard(
            title = tr("KYC Verification Required"),
            message = tr("Compliance policy requires approved KYC identity verification before unlocking live withdrawals."),
            buttonLabel = tr("Complete KYC Verification"),
            onAction = onNavigateToKyc
        )
        return
    }

    var selectedCoin by remember { mutableStateOf("USDT") }
    var selectedNetwork by remember { mutableStateOf("TRC20") }
    var destAddress by remember { mutableStateOf(profile.whitelistedAddresses.firstOrNull() ?: "") }
    var amountText by remember { mutableStateOf("") }
    var show2faPrompt by remember { mutableStateOf(false) }

    val localAmount = amountText.toDoubleOrNull() ?: 0.0
    val amountUsd = Formatters.convertToUsd(localAmount)
    val feeUsd = if (amountUsd > 0) kotlin.math.max(2.0, amountUsd * 0.002) else 0.0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(tr("Crypto Withdrawal"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "${tr("Available")}: ${Formatters.formatUsd(profile.liveAvailableBalance)} • ${tr("Locked")}: ${Formatters.formatUsd(profile.liveLockedBalance)}",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldGain
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("USDT" to "TRC20", "USDT" to "ERC20", "BTC" to "BTC", "ETH" to "ERC20", "SOL" to "SOL").forEach { (c, n) ->
                    FilterChip(
                        selected = selectedCoin == c && selectedNetwork == n,
                        onClick = { selectedCoin = c; selectedNetwork = n },
                        label = { Text("$c ($n)") }
                    )
                }
            }
        }

        if (profile.whitelistedAddresses.isNotEmpty()) {
            item {
                Text(tr("Whitelisted Addresses"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.whitelistedAddresses.forEach { addr ->
                        FilterChip(
                            selected = destAddress == addr,
                            onClick = { destAddress = addr },
                            label = { Text(addr.take(12) + "...") }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = destAddress,
                onValueChange = { destAddress = it },
                label = { Text(tr("Destination Wallet Address")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("withdraw_address_input")
            )
        }

        item {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("${tr("Withdrawal Amount")} (${Formatters.currencyCode()})") },
                supportingText = { Text("${tr("Fee")}: ${Formatters.formatUsd(feeUsd)} • ${tr("Net Payout")}: ${Formatters.formatUsd((amountUsd - feeUsd).coerceAtLeast(0.0))}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input")
            )
        }

        item {
            Button(
                onClick = {
                    val triggerSubmit = {
                        onSubmitWithdrawal(selectedCoin, selectedNetwork, destAddress, amountUsd)
                        amountText = ""
                    }
                    if (profile.twoFactorEnabled) {
                        show2faPrompt = true
                    } else if (profile.biometricEnabled && BiometricAuthManager.isBiometricAvailable(context)) {
                        BiometricAuthManager.authenticate(
                            context = context,
                            title = "Authorize Withdrawal",
                            subtitle = "Confirm payout of ${Formatters.formatUsd(amountUsd)}",
                            onSuccess = triggerSubmit,
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    } else {
                        triggerSubmit()
                    }
                },
                enabled = amountUsd > 2.0 && amountUsd <= profile.liveAvailableBalance && destAddress.trim().length >= 8,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_withdraw_button")
            ) {
                Text(tr("Lock Funds & Request Withdrawal"), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (show2faPrompt) {
        TwoFactorVerificationDialog(
            expectedPinHash = profile.twoFactorPinHash,
            onVerified = {
                show2faPrompt = false
                onSubmitWithdrawal(selectedCoin, selectedNetwork, destAddress, amountUsd)
                amountText = ""
            },
            onDismiss = { show2faPrompt = false }
        )
    }
}

@Composable
private fun HistoryTab(
    deposits: List<DepositRequest>,
    withdrawals: List<WithdrawalRequest>,
    ledger: List<LedgerEntry>
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("${tr("Deposit Requests")} (${deposits.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (deposits.isEmpty()) item { EmptyStateCard(tr("No deposit requests yet.")) }
        items(deposits, key = { "dep_${it.id}" }) { d ->
            StatusCard(
                title = "${tr("Deposit")} ${Formatters.formatUsd(d.amountUsd)} • ${d.assetSymbol} (${d.network})",
                subtitle = "TX: ${d.txHash.take(20)} • ${Formatters.formatTimestamp(d.createdAt)}",
                status = d.status,
                note = d.adminNote
            )
        }

        item { Text("${tr("Withdrawal Requests")} (${withdrawals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (withdrawals.isEmpty()) item { EmptyStateCard(tr("No withdrawal requests yet.")) }
        items(withdrawals, key = { "wd_${it.id}" }) { w ->
            StatusCard(
                title = "${tr("Withdraw")} ${Formatters.formatUsd(w.amountUsd)} • ${w.assetSymbol} (${w.network})",
                subtitle = "${tr("To")} ${w.destinationAddress.take(18)}... • ${tr("Net")} ${Formatters.formatUsd(w.netAmountUsd)}",
                status = w.status,
                note = w.payoutTxHash?.let { "Payout TX: $it" } ?: w.adminNote
            )
        }

        item { Text("${tr("Immutable Ledger")} (${ledger.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(ledger.take(30), key = { "led_${it.id}" }) { entry ->
            val pos = entry.amountDelta >= 0
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
                Column(modifier = Modifier.weight(1f)) {
                    Text("${entry.entryType} • ${entry.tradeMode}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(entry.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (pos) "+" else ""}${Formatters.formatUsd(entry.amountDelta)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (pos) EmeraldGain else CrimsonLoss,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${tr("Bal")} ${Formatters.formatUsd(entry.balanceAfter)}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun StatementsTaxTab(
    profile: UserProfile,
    orders: List<TradeOrder>,
    ledger: List<LedgerEntry>
) {
    val context = LocalContext.current
    val liveOrders = orders.filter { it.tradeMode == TradeMode.LIVE.name && it.status == OrderStatus.FILLED.name }
    val realizedGain = liveOrders.sumOf { it.realizedPnl }
    val totalFees = liveOrders.sumOf { it.feeUsd }
    val activeCurCode = Formatters.currencyCode()

    val reportText = remember(profile, liveOrders, ledger, activeCurCode) {
        buildString {
            appendLine("APEXBROKER MONTHLY STATEMENT & TAX SUMMARY ($activeCurCode)")
            appendLine("Account: ${profile.displayName} (${profile.email})")
            appendLine("Account Tier: ${profile.accountTier} | KYC: ${profile.kycStatus}")
            appendLine("Live Available Cash: ${Formatters.formatUsd(profile.liveAvailableBalance)}")
            appendLine("Live Locked Cash: ${Formatters.formatUsd(profile.liveLockedBalance)}")
            appendLine("Total Live Executed Trades: ${liveOrders.size}")
            appendLine("Net Realized Gain/Loss: ${Formatters.formatUsd(realizedGain)}")
            appendLine("Total Trading Fees Paid: ${Formatters.formatUsd(totalFees)}")
            appendLine("\n--- CONTRACT NOTES / TRADE CONFIRMATIONS ---")
            liveOrders.take(25).forEach { o ->
                appendLine("${Formatters.formatTimestamp(o.createdAt)} | ${o.side} ${o.symbol} | Qty ${Formatters.formatQty(o.quantity)} @ ${Formatters.formatAssetPrice(o.executedPrice)} | Fee ${Formatters.formatUsd(o.feeUsd)} | PnL ${Formatters.formatUsd(o.realizedPnl)}")
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(tr("Reports, Statements & Tax Center"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
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
                Text(tr("2026 YTD Tax & Realized P&L Summary"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${tr("Realized Gain/Loss")}: ${Formatters.formatUsd(realizedGain)}", color = if (realizedGain >= 0) EmeraldGain else CrimsonLoss, fontWeight = FontWeight.Bold)
                Text("${tr("Commission & Fees Paid")}: ${Formatters.formatUsd(totalFees)}", style = MaterialTheme.typography.bodyMedium)
                Text("${tr("Contract Notes Generated")}: ${liveOrders.size}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { Formatters.shareStatementReport(context, "ApexBroker Statement", reportText) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Export Statement & 1099-B Summary"))
                }
            }
        }
        item {
            Text(tr("Recent Trade Confirmations (Contract Notes)"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (liveOrders.isEmpty()) {
            item { EmptyStateCard(tr("No live executed trade confirmations yet.")) }
        } else {
            items(liveOrders.take(15), key = { "cn_${it.id}" }) { o ->
                StatusCard(
                    title = "${o.side} ${o.symbol} • ${Formatters.formatQty(o.quantity)} @ ${Formatters.formatAssetPrice(o.executedPrice)}",
                    subtitle = "${Formatters.formatTimestamp(o.createdAt)} • ${tr("Fee")} ${Formatters.formatUsd(o.feeUsd)}",
                    status = "CONFIRMED",
                    note = "${tr("Realized P&L")}: ${Formatters.formatUsd(o.realizedPnl)}"
                )
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, subtitle: String, status: String, note: String?) {
    val color = when (status) {
        RequestStatus.APPROVED.name, "CONFIRMED" -> EmeraldGain
        RequestStatus.PENDING.name -> AmberDemo
        else -> CrimsonLoss
    }
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
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(tr(status), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!note.isNullOrBlank()) {
            Text(note, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun ModeRestrictionCard(title: String, message: String, buttonLabel: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AmberDemo.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(buttonLabel, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
