package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AssetDetailData
import com.example.ui.theme.AmberDemo
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.CrimsonLoss
import com.example.util.Formatters
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun InteractivePriceChart(
    detail: AssetDetailData,
    isCandlestick: Boolean,
    showSma20: Boolean,
    showRsi: Boolean,
    showMacd: Boolean,
    modifier: Modifier = Modifier
) {
    val candles = detail.candles
    if (candles.isEmpty()) return

    var selectedIdx by remember(detail.asset.symbol, candles.size) { mutableStateOf<Int?>(null) }
    val activeCandle = selectedIdx?.let { candles.getOrNull(it) } ?: candles.last()

    Column(modifier = modifier.fillMaxWidth()) {
        // Live OHLCV Crosshair Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "O ${Formatters.formatAssetPrice(activeCandle.open)}  H ${Formatters.formatAssetPrice(activeCandle.high)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "L ${Formatters.formatAssetPrice(activeCandle.low)}  C ${Formatters.formatAssetPrice(activeCandle.close)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (activeCandle.close >= activeCandle.open) EmeraldGain else CrimsonLoss,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        val isPositiveTrend = candles.last().close >= candles.first().open
        val lineColor = if (isPositiveTrend) EmeraldGain else CrimsonLoss

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .pointerInput(candles) {
                    detectTapGestures { offset ->
                        val idx = ((offset.x / size.width.toFloat()) * (candles.size - 1))
                            .roundToInt().coerceIn(0, candles.lastIndex)
                        selectedIdx = if (selectedIdx == idx) null else idx
                    }
                }
                .pointerInput(candles) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, _ ->
                            val idx = ((change.position.x / size.width.toFloat()) * (candles.size - 1))
                                .roundToInt().coerceIn(0, candles.lastIndex)
                            selectedIdx = idx
                        },
                        onDragEnd = { },
                        onDragCancel = { }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
                val w = size.width
                val h = size.height
                val chartH = h * 0.80f
                val volTop = h * 0.84f
                val volH = h * 0.16f

                val maxHigh = candles.maxOf { it.high }
                val minLow = candles.minOf { it.low }
                val range = max(0.00001, maxHigh - minLow)
                val maxVol = max(1.0, candles.maxOf { it.volume })

                fun yForPrice(price: Double): Float {
                    val norm = ((price - minLow) / range).toFloat()
                    return chartH - (norm * chartH)
                }

                // Horizontal grid lines
                for (i in 0..3) {
                    val y = chartH * (i / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }

                val stepX = w / max(1, candles.size).toFloat()

                // Volume histogram
                candles.forEachIndexed { idx, c ->
                    val x = idx * stepX + stepX * 0.2f
                    val barH = ((c.volume / maxVol).toFloat() * volH).coerceAtLeast(2f)
                    val color = if (c.close >= c.open) EmeraldGain.copy(alpha = 0.35f) else CrimsonLoss.copy(alpha = 0.35f)
                    drawRect(
                        color = color,
                        topLeft = Offset(x, volTop + (volH - barH)),
                        size = Size(max(2f, stepX * 0.6f), barH)
                    )
                }

                if (isCandlestick) {
                    candles.forEachIndexed { idx, c ->
                        val centerX = idx * stepX + stepX / 2f
                        val bullish = c.close >= c.open
                        val candleColor = if (bullish) EmeraldGain else CrimsonLoss
                        val highY = yForPrice(c.high)
                        val lowY = yForPrice(c.low)
                        val openY = yForPrice(c.open)
                        val closeY = yForPrice(c.close)

                        drawLine(
                            color = candleColor,
                            start = Offset(centerX, highY),
                            end = Offset(centerX, lowY),
                            strokeWidth = 2.2f
                        )
                        val topY = min(openY, closeY)
                        val bodyH = max(3f, kotlin.math.abs(closeY - openY))
                        val bodyW = max(3.5f, stepX * 0.58f)
                        drawRect(
                            color = candleColor,
                            topLeft = Offset(centerX - bodyW / 2f, topY),
                            size = Size(bodyW, bodyH)
                        )
                    }
                } else {
                    val linePath = Path()
                    val fillPath = Path()
                    candles.forEachIndexed { idx, c ->
                        val x = idx * stepX + stepX / 2f
                        val y = yForPrice(c.close)
                        if (idx == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, chartH)
                            fillPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                        if (idx == candles.lastIndex) {
                            fillPath.lineTo(x, chartH)
                            fillPath.close()
                        }
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.32f), Color.Transparent),
                            startY = 0f,
                            endY = chartH
                        )
                    )
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                }

                // SMA(20) overlay
                if (showSma20) {
                    val smaPath = Path()
                    var started = false
                    detail.sma20.forEachIndexed { idx, v ->
                        if (v != null) {
                            val x = idx * stepX + stepX / 2f
                            val y = yForPrice(v)
                            if (!started) {
                                smaPath.moveTo(x, y)
                                started = true
                            } else {
                                smaPath.lineTo(x, y)
                            }
                        }
                    }
                    if (started) {
                        drawPath(smaPath, color = AmberDemo, style = Stroke(width = 2.5f))
                    }
                }

                // Crosshair highlight
                selectedIdx?.let { idx ->
                    val cx = idx * stepX + stepX / 2f
                    val cy = yForPrice(candles[idx].close)
                    drawLine(
                        color = CobaltPrimary.copy(alpha = 0.7f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, h),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    drawCircle(color = CobaltPrimary, radius = 7f, center = Offset(cx, cy))
                }
            }
        }

        if (showRsi) {
            Spacer(modifier = Modifier.height(6.dp))
            val latestRsi = detail.rsi14.lastOrNull { it != null }
            IndicatorSubChart(
                title = "RSI (14)",
                valueLabel = latestRsi?.let { String.format("%.1f", it) } ?: "--",
                seriesA = detail.rsi14,
                seriesB = null,
                colorA = CyanAccent,
                colorB = AmberDemo,
                fixedMin = 0.0,
                fixedMax = 100.0,
                refLine = 70.0,
                refLine2 = 30.0
            )
        }

        if (showMacd) {
            Spacer(modifier = Modifier.height(6.dp))
            val latestMacd = detail.macdLine.lastOrNull { it != null }
            IndicatorSubChart(
                title = "MACD (12,26,9)",
                valueLabel = latestMacd?.let { String.format("%.2f", it) } ?: "--",
                seriesA = detail.macdLine,
                seriesB = detail.macdSignal,
                colorA = CobaltPrimary,
                colorB = AmberDemo
            )
        }
    }
}

@Composable
private fun IndicatorSubChart(
    title: String,
    valueLabel: String,
    seriesA: List<Double?>,
    seriesB: List<Double?>?,
    colorA: Color,
    colorB: Color,
    fixedMin: Double? = null,
    fixedMax: Double? = null,
    refLine: Double? = null,
    refLine2: Double? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueLabel, style = MaterialTheme.typography.labelSmall, color = colorA, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(54.dp)) {
            val allVals = (seriesA + (seriesB ?: emptyList())).filterNotNull()
            if (allVals.isEmpty()) return@Canvas
            val minV = fixedMin ?: allVals.minOrNull()!!
            val maxV = fixedMax ?: allVals.maxOrNull()!!
            val range = max(0.0001, maxV - minV)
            val w = size.width
            val h = size.height
            val stepX = w / max(1, seriesA.size).toFloat()

            fun yFor(v: Double) = h - (((v - minV) / range).toFloat() * h)

            refLine?.let {
                drawLine(Color.Gray.copy(alpha = 0.35f), Offset(0f, yFor(it)), Offset(w, yFor(it)), 1f)
            }
            refLine2?.let {
                drawLine(Color.Gray.copy(alpha = 0.35f), Offset(0f, yFor(it)), Offset(w, yFor(it)), 1f)
            }

            fun drawSeries(data: List<Double?>, color: Color) {
                val p = Path()
                var started = false
                data.forEachIndexed { idx, v ->
                    if (v != null) {
                        val x = idx * stepX + stepX / 2f
                        val y = yFor(v)
                        if (!started) { p.moveTo(x, y); started = true } else p.lineTo(x, y)
                    }
                }
                if (started) drawPath(p, color = color, style = Stroke(width = 2.4f))
            }

            drawSeries(seriesA, colorA)
            if (seriesB != null) drawSeries(seriesB, colorB)
        }
    }
}

@Composable
fun MiniSparkline(
    points: List<Double>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val strokeColor = if (isPositive) EmeraldGain else CrimsonLoss
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val minVal = points.minOrNull() ?: return@Canvas
        val maxVal = points.maxOrNull() ?: return@Canvas
        val range = max(0.00001, maxVal - minVal)
        val stepX = size.width / (points.size - 1).toFloat()
        val path = Path()
        points.forEachIndexed { idx, p ->
            val x = idx * stepX
            val y = size.height - (((p - minVal) / range).toFloat() * size.height)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = strokeColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    }
}
