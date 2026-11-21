package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.localization.AppTranslationManager.tr
import com.example.data.model.MarketAsset
import com.example.data.model.OrderSide
import com.example.data.model.OrderType
import com.example.data.model.PortfolioPosition
import com.example.data.model.TimeInForce
import com.example.data.model.TradeMode
import com.example.ui.theme.AmberDemo
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.EmeraldGain
import com.example.util.Formatters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTicketModal(
    asset: MarketAsset,
    activeMode: TradeMode,
    availableBalance: Double,
    currentPosition: PortfolioPosition?,
    feeBps: Double = 10.0,
    onDismiss: () -> Unit,
    onSubmitOrder: (
        side: OrderSide,
        orderType: OrderType,
        tif: TimeInForce,
        quantity: Double,
        limitPrice: Double?,
        stopPrice: Double?,
        takeProfitPrice: Double?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val curCode = Formatters.currencyCode()
    val curSymbol = Formatters.currencySymbol()
    val defaultLocalAmount = String.format(Locale.US, "%.2f", Formatters.convertFromUsd(250.0))

    var side by remember { mutableStateOf(OrderSide.BUY) }
    var orderType by remember { mutableStateOf(OrderType.MARKET) }
    var timeInForce by remember { mutableStateOf(TimeInForce.GTC) }
    var inputInCurrency by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf(defaultLocalAmount) }
    var limitText by remember { mutableStateOf(String.format(Locale.US, "%.2f", Formatters.convertFromUsd(asset.price))) }
    var stopText by remember { mutableStateOf(String.format(Locale.US, "%.2f", Formatters.convertFromUsd(asset.price * 0.96))) }
    var takeProfitText by remember { mutableStateOf(String.format(Locale.US, "%.2f", Formatters.convertFromUsd(asset.price * 1.08))) }

    val parsedInput = amountText.toDoubleOrNull() ?: 0.0
    val refPriceUsd = if (side == OrderSide.BUY) asset.askPrice else asset.bidPrice
    val inputUsdEquivalent = Formatters.convertToUsd(parsedInput)
    val quantity = if (inputInCurrency) (if (refPriceUsd > 0) inputUsdEquivalent / refPriceUsd else 0.0) else parsedInput
    val estimatedNotionalUsd = quantity * refPriceUsd
    val estimatedFeeUsd = estimatedNotionalUsd * (feeBps / 10000.0)
    val totalRequiredUsd = if (side == OrderSide.BUY) estimatedNotionalUsd + estimatedFeeUsd else estimatedNotionalUsd - estimatedFeeUsd

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${asset.symbol} ${tr("Order Ticket")}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${Formatters.formatAssetPrice(asset.price, asset.assetClass)} • ${activeMode.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (activeMode == TradeMode.DEMO) AmberDemo else EmeraldGain
                    )
                }
                MarketAssetIcon(asset = asset, size = 38.dp)
            }

            // BUY / SELL Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OrderSide.entries.forEach { s ->
                    val selected = side == s
                    val color = if (s == OrderSide.BUY) EmeraldGain else CrimsonLoss
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) color else Color.Transparent)
                            .clickable { side = s }
                            .padding(vertical = 10.dp)
                            .testTag("order_side_${s.name.lowercase()}"),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = tr(s.name),
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Order Type Chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OrderType.entries.forEach { type ->
                    FilterChip(
                        selected = orderType == type,
                        onClick = { orderType = type },
                        label = { Text(tr(type.label), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Currency vs Fractional Units Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = inputInCurrency,
                        onClick = { inputInCurrency = true },
                        label = { Text("$curCode ($curSymbol)") }
                    )
                    FilterChip(
                        selected = !inputInCurrency,
                        onClick = { inputInCurrency = false },
                        label = { Text(tr("Units")) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TimeInForce.entries.forEach { tif ->
                        FilterChip(
                            selected = timeInForce == tif,
                            onClick = { timeInForce = tif },
                            label = { Text(tif.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(if (inputInCurrency) "${tr("Order Amount")} ($curCode)" else "${tr("Quantity")} (${asset.symbol})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("order_amount_input")
            )

            // Quick Allocation Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = if (side == OrderSide.BUY) listOf(0.10, 0.25, 0.50, 0.95) else listOf(0.25, 0.50, 0.75, 1.0)
                presets.forEach { pct ->
                    val label = "${(pct * 100).toInt()}%"
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable {
                                if (side == OrderSide.BUY) {
                                    val amtUsd = availableBalance * pct
                                    amountText = if (inputInCurrency) String.format(Locale.US, "%.2f", Formatters.convertFromUsd(amtUsd))
                                    else String.format(Locale.US, "%.5f", if (refPriceUsd > 0) amtUsd / refPriceUsd else 0.0)
                                } else {
                                    val posQty = (currentPosition?.quantity ?: 0.0) * pct
                                    amountText = if (inputInCurrency) String.format(Locale.US, "%.2f", Formatters.convertFromUsd(posQty * refPriceUsd))
                                    else String.format(Locale.US, "%.5f", posQty)
                                }
                            }
                            .padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (orderType == OrderType.LIMIT || orderType == OrderType.STOP_LIMIT || orderType == OrderType.BRACKET) {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("${tr("Limit Price")} ($curCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (orderType == OrderType.STOP_LOSS || orderType == OrderType.STOP_LIMIT || orderType == OrderType.TRAILING_STOP || orderType == OrderType.BRACKET) {
                OutlinedTextField(
                    value = stopText,
                    onValueChange = { stopText = it },
                    label = { Text("${tr("Stop Price")} ($curCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (orderType == OrderType.BRACKET) {
                OutlinedTextField(
                    value = takeProfitText,
                    onValueChange = { takeProfitText = it },
                    label = { Text("${tr("Take Profit Target")} ($curCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Margin & Fee Preview Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PreviewRow(tr("Estimated Units"), Formatters.formatQty(quantity))
                PreviewRow(tr("Execution Price"), Formatters.formatAssetPrice(refPriceUsd, asset.assetClass))
                PreviewRow("${tr("Estimated Fee")} (${feeBps.toInt()} bps)", Formatters.formatUsd(estimatedFeeUsd))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                PreviewRow(
                    tr(if (side == OrderSide.BUY) "Total Cost" else "Est. Proceeds"),
                    Formatters.formatUsd(totalRequiredUsd),
                    bold = true
                )
                PreviewRow(
                    tr(if (side == OrderSide.BUY) "Available Buying Power" else "Owned Position"),
                    if (side == OrderSide.BUY) Formatters.formatUsd(availableBalance)
                    else "${Formatters.formatQty(currentPosition?.quantity ?: 0.0)} ${asset.symbol}"
                )
            }

            Button(
                onClick = {
                    onSubmitOrder(
                        side,
                        orderType,
                        timeInForce,
                        quantity,
                        limitText.toDoubleOrNull()?.let { Formatters.convertToUsd(it) },
                        stopText.toDoubleOrNull()?.let { Formatters.convertToUsd(it) },
                        takeProfitText.toDoubleOrNull()?.let { Formatters.convertToUsd(it) }
                    )
                },
                enabled = quantity > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (side == OrderSide.BUY) EmeraldGain else CrimsonLoss,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_order_button")
            ) {
                Text(
                    text = "${tr(side.name)} ${tr(orderType.label)} (${activeMode.name})",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}
