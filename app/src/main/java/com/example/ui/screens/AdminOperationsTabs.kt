package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminRole
import com.example.data.model.AdminRoleType
import com.example.data.model.AuditLog
import com.example.data.model.DepositWallet
import com.example.data.model.MarketAsset
import com.example.data.model.MarketControl
import com.example.data.model.SupportTicket
import com.example.data.model.TicketStatus
import com.example.data.model.UserProfile
import com.example.ui.theme.AmberDemo
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldGain
import com.example.util.Formatters

@Composable
fun AdminUsersTab(
    users: List<UserProfile>,
    onAdjustBalance: (UserProfile, Double, String) -> Unit,
    onToggleFreeze: (UserProfile, Boolean) -> Unit,
    onReset2fa: (UserProfile) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var adjustTarget by remember { mutableStateOf<UserProfile?>(null) }

    val filtered = users.filter {
        search.isBlank() || it.email.contains(search, true) || it.displayName.contains(search, true) || it.userId.contains(search, true)
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search users by name, email, or UID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(filtered, key = { it.userId }) { user ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = if (user.accountFrozen) "FROZEN" else user.kycStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (user.accountFrozen) CrimsonLoss else EmeraldGain,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Live: ${Formatters.formatUsd(user.liveAvailableBalance)} | Locked: ${Formatters.formatUsd(user.liveLockedBalance)} | Demo: ${Formatters.formatUsd(user.demoAvailableBalance)}",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { adjustTarget = user },
                        modifier = Modifier.testTag("admin_adjust_balance_${user.userId}")
                    ) { Text("Credit / Debit") }
                    OutlinedButton(onClick = { onToggleFreeze(user, !user.accountFrozen) }) {
                        Text(if (user.accountFrozen) "Unfreeze" else "Freeze", color = if (user.accountFrozen) EmeraldGain else CrimsonLoss)
                    }
                    if (user.twoFactorEnabled) {
                        OutlinedButton(onClick = { onReset2fa(user) }) { Text("Reset 2FA") }
                    }
                }
            }
        }
    }

    adjustTarget?.let { target ->
        var amountStr by remember { mutableStateOf("500") }
        var reason by remember { mutableStateOf("Treasury promotional / reconciliation adjustment") }
        var isCredit by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { adjustTarget = null },
            title = { Text("Adjust Live Balance: ${target.displayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = isCredit, onClick = { isCredit = true }, label = { Text("Credit (+)") })
                        FilterChip(selected = !isCredit, onClick = { isCredit = false }, label = { Text("Debit (-)") })
                    }
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Audit Reason") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onAdjustBalance(target, if (isCredit) amt else -amt, reason)
                        adjustTarget = null
                    }
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { adjustTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AdminWalletsTab(
    wallets: List<DepositWallet>,
    onSaveWallet: (DepositWallet) -> Unit,
    onSeedDefaults: () -> Unit
) {
    var editingWallet by remember { mutableStateOf<DepositWallet?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        editingWallet = DepositWallet(symbol = "USDT", network = "TRC20", address = "", minDeposit = 10.0, confirmationsRequired = 12, active = true)
                    },
                    modifier = Modifier.weight(1f).testTag("admin_add_wallet_button")
                ) { Text("Add Wallet") }
                OutlinedButton(
                    onClick = onSeedDefaults,
                    modifier = Modifier.weight(1f).testTag("admin_seed_wallets_button")
                ) { Text("Seed Defaults") }
            }
        }
        items(wallets, key = { it.id.ifBlank { "${it.symbol}_${it.network}" } }) { wallet ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${wallet.symbol} • ${wallet.network}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Switch(checked = wallet.active, onCheckedChange = { onSaveWallet(wallet.copy(active = it)) })
                }
                Text(wallet.address, style = MaterialTheme.typography.labelSmall, color = CobaltPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Min $${wallet.minDeposit} • ${wallet.confirmationsRequired} conf", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { editingWallet = wallet }) { Text("Edit") }
                }
            }
        }
    }

    editingWallet?.let { w ->
        var symbol by remember { mutableStateOf(w.symbol) }
        var network by remember { mutableStateOf(w.network) }
        var address by remember { mutableStateOf(w.address) }
        var minDep by remember { mutableStateOf(w.minDeposit.toString()) }
        var confs by remember { mutableStateOf(w.confirmationsRequired.toString()) }
        AlertDialog(
            onDismissRequest = { editingWallet = null },
            title = { Text("Configure Crypto Deposit Wallet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text("Symbol (e.g. BTC, USDT)") }, singleLine = true)
                    OutlinedTextField(value = network, onValueChange = { network = it }, label = { Text("Network (e.g. TRC20, ERC20)") }, singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Receiving Address") })
                    OutlinedTextField(value = minDep, onValueChange = { minDep = it }, label = { Text("Min Deposit USD") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = confs, onValueChange = { confs = it }, label = { Text("Required Confirmations") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (symbol.trim().length >= 2 && network.trim().length >= 2 && address.trim().length >= 8) {
                        onSaveWallet(
                            w.copy(
                                symbol = symbol.trim().uppercase(),
                                network = network.trim().uppercase(),
                                address = address.trim(),
                                minDeposit = minDep.toDoubleOrNull() ?: 10.0,
                                confirmationsRequired = (confs.toLongOrNull() ?: 3L).coerceIn(1L, 100L)
                            )
                        )
                        editingWallet = null
                    }
                }) { Text("Save Wallet") }
            },
            dismissButton = { TextButton(onClick = { editingWallet = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AdminMarketControlsTab(
    assets: List<MarketAsset>,
    controls: List<MarketControl>,
    onSaveControl: (MarketControl) -> Unit
) {
    val controlMap = controls.associateBy { it.symbol }
    var editingSymbol by remember { mutableStateOf<String?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(assets.take(15), key = { it.symbol }) { asset ->
            val ctrl = controlMap[asset.symbol] ?: MarketControl(symbol = asset.symbol)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${asset.symbol} • ${asset.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Spread ${ctrl.spreadBps}bps • Fee ${ctrl.feeBps}bps • Max ${ctrl.maxLeverage}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { editingSymbol = asset.symbol }) { Text("Params") }
                    Switch(
                        checked = ctrl.tradingEnabled,
                        onCheckedChange = { onSaveControl(ctrl.copy(tradingEnabled = it)) }
                    )
                }
            }
        }
    }

    editingSymbol?.let { sym ->
        val existing = controlMap[sym] ?: MarketControl(symbol = sym)
        var spread by remember { mutableStateOf(existing.spreadBps.toString()) }
        var fee by remember { mutableStateOf(existing.feeBps.toString()) }
        var lev by remember { mutableStateOf(existing.maxLeverage.toString()) }
        AlertDialog(
            onDismissRequest = { editingSymbol = null },
            title = { Text("Risk Parameters: $sym") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = spread, onValueChange = { spread = it }, label = { Text("Spread (bps)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = fee, onValueChange = { fee = it }, label = { Text("Execution Fee (bps)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = lev, onValueChange = { lev = it }, label = { Text("Max Leverage (x)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveControl(
                        existing.copy(
                            spreadBps = (spread.toDoubleOrNull() ?: 4.0).coerceIn(0.0, 500.0),
                            feeBps = (fee.toDoubleOrNull() ?: 10.0).coerceIn(0.0, 500.0),
                            maxLeverage = (lev.toDoubleOrNull() ?: 5.0).coerceIn(1.0, 100.0)
                        )
                    )
                    editingSymbol = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingSymbol = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AdminGovernanceTab(
    tickets: List<SupportTicket>,
    auditLogs: List<AuditLog>,
    adminRoles: List<AdminRole>,
    onReplyTicket: (SupportTicket, TicketStatus, String) -> Unit,
    onAddAdminRole: (String, String, AdminRoleType) -> Unit,
    onRemoveAdminRole: (String) -> Unit
) {
    var replyTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var showAddAdmin by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Admin Roles (${adminRoles.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = { showAddAdmin = true }) { Text("Assign Admin") }
            }
        }
        items(adminRoles, key = { "role_${it.uid}" }) { role ->
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
                    Text(role.email.ifBlank { role.uid }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${role.role} • UID ${role.uid.take(12)}...", style = MaterialTheme.typography.labelSmall, color = CobaltPrimary)
                }
                OutlinedButton(onClick = { onRemoveAdminRole(role.uid) }) {
                    Text("Revoke", color = CrimsonLoss)
                }
            }
        }

        item { Text("Support Tickets (${tickets.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (tickets.isEmpty()) {
            item { Text("No support tickets submitted.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(tickets, key = { "adm_t_${it.id}" }) { t ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(t.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(t.status, style = MaterialTheme.typography.labelSmall, color = AmberDemo)
                    }
                    Text("${t.userEmail} • ${t.message}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    t.adminReply?.let { Text("Reply: $it", style = MaterialTheme.typography.labelSmall, color = EmeraldGain) }
                    Button(onClick = { replyTicket = t }) { Text("Reply / Update Status") }
                }
            }
        }

        item { Text("Immutable Audit Log (${auditLogs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(auditLogs.take(40), key = { "log_${it.id}" }) { log ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(log.action, style = MaterialTheme.typography.labelLarge, color = CobaltPrimary, fontWeight = FontWeight.Bold)
                    Text(Formatters.formatTimestamp(log.createdAt), style = MaterialTheme.typography.labelSmall)
                }
                Text("${log.actorEmail} • ${log.details}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    replyTicket?.let { ticket ->
        var reply by remember { mutableStateOf(ticket.adminReply ?: "") }
        var status by remember { mutableStateOf(TicketStatus.RESOLVED) }
        AlertDialog(
            onDismissRequest = { replyTicket = null },
            title = { Text("Reply: ${ticket.subject}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TicketStatus.entries.forEach { st ->
                            FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.name) })
                        }
                    }
                    OutlinedTextField(value = reply, onValueChange = { reply = it }, label = { Text("Admin Reply") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onReplyTicket(ticket, status, reply)
                    replyTicket = null
                }) { Text("Send Reply") }
            },
            dismissButton = { TextButton(onClick = { replyTicket = null }) { Text("Cancel") } }
        )
    }

    if (showAddAdmin) {
        var uid by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(AdminRoleType.COMPLIANCE_OFFICER) }
        AlertDialog(
            onDismissRequest = { showAddAdmin = false },
            title = { Text("Assign Admin Privilege") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = uid, onValueChange = { uid = it }, label = { Text("Firebase User UID") }, singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Admin Email") }, singleLine = true)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AdminRoleType.entries.forEach { r ->
                            FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.label) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (uid.isNotBlank()) {
                        onAddAdminRole(uid, email, role)
                        showAddAdmin = false
                    }
                }) { Text("Grant Access") }
            },
            dismissButton = { TextButton(onClick = { showAddAdmin = false }) { Text("Cancel") } }
        )
    }
}
