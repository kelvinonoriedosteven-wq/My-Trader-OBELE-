package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminRole
import com.example.data.model.AdminRoleType
import com.example.data.model.AuditLog
import com.example.data.model.DepositRequest
import com.example.data.model.DepositWallet
import com.example.data.model.KycStatus
import com.example.data.model.KycSubmission
import com.example.data.model.MarketAsset
import com.example.data.model.MarketControl
import com.example.data.model.RequestStatus
import com.example.data.model.SupportTicket
import com.example.data.model.TicketStatus
import com.example.data.model.TradeOrder
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.ui.theme.AmberDemo
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldGain
import com.example.util.Formatters
import com.example.util.ImagePickerHelper
import com.example.util.QrCodeGenerator

@Composable
fun AdminControlPanelScreen(
    users: List<UserProfile>,
    kycSubmissions: List<KycSubmission>,
    deposits: List<DepositRequest>,
    withdrawals: List<WithdrawalRequest>,
    orders: List<TradeOrder>,
    wallets: List<DepositWallet>,
    marketAssets: List<MarketAsset>,
    marketControls: List<MarketControl>,
    tickets: List<SupportTicket>,
    auditLogs: List<AuditLog>,
    adminRoles: List<AdminRole>,
    onExitAdmin: () -> Unit,
    onApproveKyc: (KycSubmission) -> Unit,
    onRejectKyc: (KycSubmission, String) -> Unit,
    onApproveDeposit: (DepositRequest) -> Unit,
    onRejectDeposit: (DepositRequest, String) -> Unit,
    onApproveWithdrawal: (WithdrawalRequest, String) -> Unit,
    onRejectWithdrawal: (WithdrawalRequest, String) -> Unit,
    onAdjustBalance: (UserProfile, Double, String) -> Unit,
    onToggleFreeze: (UserProfile, Boolean) -> Unit,
    onReset2fa: (UserProfile) -> Unit,
    onSaveWallet: (DepositWallet) -> Unit,
    onSeedWallets: () -> Unit,
    onSaveMarketControl: (MarketControl) -> Unit,
    onReplyTicket: (SupportTicket, TicketStatus, String) -> Unit,
    onAddAdminRole: (String, String, AdminRoleType) -> Unit,
    onRemoveAdminRole: (String) -> Unit
) {
    BackHandler(onBack = onExitAdmin)

    var selectedTab by remember { mutableIntStateOf(0) }
    val pendingKycCount = kycSubmissions.count { it.status == KycStatus.PENDING.name }
    val pendingDepCount = deposits.count { it.status == RequestStatus.PENDING.name }
    val pendingWdCount = withdrawals.count { it.status == RequestStatus.PENDING.name }

    val tabs = listOf(
        "Overview",
        "KYC ($pendingKycCount)",
        "Deposits ($pendingDepCount)",
        "Withdrawals ($pendingWdCount)",
        "Users (${users.size})",
        "Wallets (${wallets.size})",
        "Markets",
        "Audit & Roles"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = 820.dp).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExitAdmin) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to User App")
                    }
                    Column {
                        Text("ApexBroker Admin Command Center", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Live KYC, Treasury, Risk & Compliance Governance", style = MaterialTheme.typography.labelSmall, color = CobaltPrimary)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        label = { Text(label) },
                        modifier = Modifier.testTag("admin_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> AdminOverviewTab(
                    users = users,
                    pendingKyc = pendingKycCount,
                    pendingDeposits = pendingDepCount,
                    pendingWithdrawals = pendingWdCount,
                    orders = orders,
                    onJumpToTab = { selectedTab = it }
                )
                1 -> AdminKycQueueTab(
                    submissions = kycSubmissions,
                    onApprove = onApproveKyc,
                    onReject = onRejectKyc
                )
                2 -> AdminDepositsQueueTab(
                    deposits = deposits,
                    onApprove = onApproveDeposit,
                    onReject = onRejectDeposit
                )
                3 -> AdminWithdrawalsQueueTab(
                    withdrawals = withdrawals,
                    users = users,
                    onApprove = onApproveWithdrawal,
                    onReject = onRejectWithdrawal
                )
                4 -> AdminUsersTab(
                    users = users,
                    onAdjustBalance = onAdjustBalance,
                    onToggleFreeze = onToggleFreeze,
                    onReset2fa = onReset2fa
                )
                5 -> AdminWalletsTab(
                    wallets = wallets,
                    onSaveWallet = onSaveWallet,
                    onSeedDefaults = onSeedWallets
                )
                6 -> AdminMarketControlsTab(
                    assets = marketAssets,
                    controls = marketControls,
                    onSaveControl = onSaveMarketControl
                )
                else -> AdminGovernanceTab(
                    tickets = tickets,
                    auditLogs = auditLogs,
                    adminRoles = adminRoles,
                    onReplyTicket = onReplyTicket,
                    onAddAdminRole = onAddAdminRole,
                    onRemoveAdminRole = onRemoveAdminRole
                )
            }
        }
    }
}

@Composable
private fun AdminOverviewTab(
    users: List<UserProfile>,
    pendingKyc: Int,
    pendingDeposits: Int,
    pendingWithdrawals: Int,
    orders: List<TradeOrder>,
    onJumpToTab: (Int) -> Unit
) {
    val totalLiveLiquidity = users.sumOf { it.liveAvailableBalance + it.liveLockedBalance }
    val totalVolume = orders.sumOf { it.notionalUsd }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard("Platform Live Liquidity", Formatters.formatUsd(totalLiveLiquidity), EmeraldGain, Modifier.weight(1f))
                KpiCard("Total Order Volume", Formatters.formatUsd(totalVolume), CobaltPrimary, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard("Pending KYC", "$pendingKyc", AmberDemo, Modifier.weight(1f))
                KpiCard("Pending Deposits", "$pendingDeposits", AmberDemo, Modifier.weight(1f))
                KpiCard("Pending Payouts", "$pendingWithdrawals", CrimsonLoss, Modifier.weight(1f))
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Operational Shortcuts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = { onJumpToTab(1) }, modifier = Modifier.fillMaxWidth()) { Text("Review KYC Queue ($pendingKyc)") }
                Button(onClick = { onJumpToTab(2) }, modifier = Modifier.fillMaxWidth()) { Text("Approve Crypto Deposits ($pendingDeposits)") }
                Button(onClick = { onJumpToTab(3) }, modifier = Modifier.fillMaxWidth()) { Text("Process Withdrawal Payouts ($pendingWithdrawals)") }
                OutlinedButton(onClick = { onJumpToTab(5) }, modifier = Modifier.fillMaxWidth()) { Text("Manage Receiving Crypto Wallets") }
            }
        }
    }
}

@Composable
private fun KpiCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdminKycQueueTab(
    submissions: List<KycSubmission>,
    onApprove: (KycSubmission) -> Unit,
    onReject: (KycSubmission, String) -> Unit
) {
    var rejectTarget by remember { mutableStateOf<KycSubmission?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (submissions.isEmpty()) {
            item { Text("No KYC submissions yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(submissions, key = { it.id.ifBlank { it.userId } }) { kyc ->
            val statusColor = when (kyc.status) {
                KycStatus.APPROVED.name -> EmeraldGain
                KycStatus.PENDING.name -> AmberDemo
                else -> CrimsonLoss
            }
            val frontBmp = remember(kyc.idFrontBase64) { ImagePickerHelper.base64ToImageBitmap(kyc.idFrontBase64) }
            val backBmp = remember(kyc.idBackBase64) { ImagePickerHelper.base64ToImageBitmap(kyc.idBackBase64) }
            val selfieBmp = remember(kyc.selfieBase64) { ImagePickerHelper.base64ToImageBitmap(kyc.selfieBase64) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(kyc.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(kyc.status, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
                }
                Text("DOB: ${kyc.dateOfBirth} • Nationality: ${kyc.nationality} • Tax: ${kyc.taxResidency} (${kyc.taxIdLast4})", style = MaterialTheme.typography.bodyMedium)
                Text("Address: ${kyc.addressLine}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("ID: ${kyc.idType} • Risk: ${kyc.riskTolerance} • Income: ${kyc.annualIncomeRange}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (frontBmp != null || backBmp != null || selfieBmp != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        frontBmp?.let {
                            Image(bitmap = it, contentDescription = "ID Front", contentScale = ContentScale.Crop, modifier = Modifier.size(84.dp).clip(RoundedCornerShape(8.dp)))
                        }
                        backBmp?.let {
                            Image(bitmap = it, contentDescription = "ID Back", contentScale = ContentScale.Crop, modifier = Modifier.size(84.dp).clip(RoundedCornerShape(8.dp)))
                        }
                        selfieBmp?.let {
                            Image(bitmap = it, contentDescription = "Selfie", contentScale = ContentScale.Crop, modifier = Modifier.size(84.dp).clip(RoundedCornerShape(8.dp)))
                        }
                    }
                }

                if (kyc.status != KycStatus.APPROVED.name) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onApprove(kyc) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGain, contentColor = Color(0xFF042F1E)),
                            modifier = Modifier.weight(1f).testTag("admin_approve_kyc_${kyc.userId}")
                        ) { Text("Approve KYC", fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { rejectTarget = kyc },
                            modifier = Modifier.weight(1f)
                        ) { Text("Reject", color = CrimsonLoss) }
                    }
                }
            }
        }
    }

    rejectTarget?.let { kyc ->
        var reason by remember { mutableStateOf("Document photo blurred or expired. Please resubmit.") }
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject KYC: ${kyc.fullName}") },
            text = {
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Rejection Reason") })
            },
            confirmButton = {
                Button(onClick = {
                    onReject(kyc, reason)
                    rejectTarget = null
                }) { Text("Confirm Reject") }
            },
            dismissButton = { TextButton(onClick = { rejectTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminDepositsQueueTab(
    deposits: List<DepositRequest>,
    onApprove: (DepositRequest) -> Unit,
    onReject: (DepositRequest, String) -> Unit
) {
    var rejectTarget by remember { mutableStateOf<DepositRequest?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (deposits.isEmpty()) {
            item { Text("No deposit requests found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(deposits, key = { it.id }) { dep ->
            val proofBmp = remember(dep.proofImageBase64) { ImagePickerHelper.base64ToImageBitmap(dep.proofImageBase64) }
            val color = when (dep.status) {
                RequestStatus.APPROVED.name -> EmeraldGain
                RequestStatus.PENDING.name -> AmberDemo
                else -> CrimsonLoss
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${Formatters.formatUsd(dep.amountUsd)} • ${dep.assetSymbol} (${dep.network})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(dep.status, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                }
                Text("User: ${dep.userEmail} • ≈ ${Formatters.formatQty(dep.cryptoAmount)} ${dep.assetSymbol}", style = MaterialTheme.typography.bodyMedium)
                Text("TXID: ${dep.txHash}", style = MaterialTheme.typography.labelSmall, color = CobaltPrimary)
                proofBmp?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Deposit receipt proof",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp))
                    )
                }
                if (dep.status == RequestStatus.PENDING.name) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onApprove(dep) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGain, contentColor = Color(0xFF042F1E)),
                            modifier = Modifier.weight(1f).testTag("admin_approve_dep_${dep.id}")
                        ) { Text("Approve & Credit", fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { rejectTarget = dep },
                            modifier = Modifier.weight(1f)
                        ) { Text("Reject", color = CrimsonLoss) }
                    }
                }
            }
        }
    }

    rejectTarget?.let { dep ->
        var reason by remember { mutableStateOf("TXID not found on blockchain explorer.") }
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject Deposit (${Formatters.formatUsd(dep.amountUsd)})") },
            text = { OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") }) },
            confirmButton = {
                Button(onClick = {
                    onReject(dep, reason)
                    rejectTarget = null
                }) { Text("Reject") }
            },
            dismissButton = { TextButton(onClick = { rejectTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminWithdrawalsQueueTab(
    withdrawals: List<WithdrawalRequest>,
    users: List<UserProfile>,
    onApprove: (WithdrawalRequest, String) -> Unit,
    onReject: (WithdrawalRequest, String) -> Unit
) {
    val userMap = users.associateBy { it.userId }
    var approveTarget by remember { mutableStateOf<WithdrawalRequest?>(null) }
    var rejectTarget by remember { mutableStateOf<WithdrawalRequest?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (withdrawals.isEmpty()) {
            item { Text("No withdrawal requests found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(withdrawals, key = { it.id }) { w ->
            val user = userMap[w.userId]
            val qrBmp = remember(w.destinationAddress) { QrCodeGenerator.generateQrBitmap(w.destinationAddress, 240) }
            val color = when (w.status) {
                RequestStatus.APPROVED.name -> EmeraldGain
                RequestStatus.PENDING.name -> AmberDemo
                else -> CrimsonLoss
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${Formatters.formatUsd(w.amountUsd)} • ${w.assetSymbol} (${w.network})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(w.status, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    qrBmp?.let {
                        Image(bitmap = it, contentDescription = "Destination QR", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("User: ${w.userEmail}", style = MaterialTheme.typography.bodyMedium)
                        Text("Net Payout: ${Formatters.formatUsd(w.netAmountUsd)} (Fee ${Formatters.formatUsd(w.feeUsd)})", style = MaterialTheme.typography.labelSmall, color = EmeraldGain)
                        Text("Dest: ${w.destinationAddress}", style = MaterialTheme.typography.labelSmall, color = CobaltPrimary)
                        if (user != null) {
                            Text("User Avail: ${Formatters.formatUsd(user.liveAvailableBalance)} • Locked: ${Formatters.formatUsd(user.liveLockedBalance)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (w.status == RequestStatus.PENDING.name) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { approveTarget = w },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGain, contentColor = Color(0xFF042F1E)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Mark Paid", fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { rejectTarget = w },
                            modifier = Modifier.weight(1f)
                        ) { Text("Reject & Refund", color = CrimsonLoss) }
                    }
                }
            }
        }
    }

    approveTarget?.let { w ->
        var txHash by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { approveTarget = null },
            title = { Text("Confirm Payout TXID") },
            text = {
                OutlinedTextField(value = txHash, onValueChange = { txHash = it }, label = { Text("Blockchain Payout TX Hash") })
            },
            confirmButton = {
                Button(onClick = {
                    if (txHash.trim().length >= 6) {
                        onApprove(w, txHash)
                        approveTarget = null
                    }
                }) { Text("Confirm Paid") }
            },
            dismissButton = { TextButton(onClick = { approveTarget = null }) { Text("Cancel") } }
        )
    }

    rejectTarget?.let { w ->
        var reason by remember { mutableStateOf("Destination address check failed; funds returned to available balance.") }
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject & Refund Withdrawal") },
            text = { OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Rejection Reason") }) },
            confirmButton = {
                Button(onClick = {
                    onReject(w, reason)
                    rejectTarget = null
                }) { Text("Reject & Refund") }
            },
            dismissButton = { TextButton(onClick = { rejectTarget = null }) { Text("Cancel") } }
        )
    }
}
