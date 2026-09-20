package com.example.geminiapi.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.geminiapi.ui.theme.AccentRed
import com.example.geminiapi.ui.theme.Lime

@Composable
private fun animatedEntry(target: Float, durationMillis: Int = 1000): State<Float> {
    val animation = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animation.animateTo(target, tween(durationMillis))
    }
    return animation.asState()
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 48.dp,
    strokeWidth: Dp = 5.dp,
    color: Color = Lime,
    trackColor: Color = color.copy(alpha = 0.12f),
    content: (@Composable () -> Unit)? = null
) {
    val animatedProgress by animatedEntry(progress)
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = trackColor, style = Stroke(strokeWidth.toPx()))
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        content?.invoke()
    }
}

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Lime,
    trackColor: Color = color.copy(alpha = 0.12f),
    height: Dp = 6.dp
) {
    val animatedProgress by animatedEntry(progress)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(color)
        )
    }
}

@Composable
fun BarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    barColors: List<Color> = listOf(Lime),
    labels: List<String>? = null,
    barWidth: Dp = 12.dp
) {
    val animatedProgress by animatedEntry(1f)
    val max = values.maxOrNull() ?: 1f

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val barCount = values.size
        val spacing = (width - (barWidth.toPx() * barCount)) / (barCount + 1)

        values.forEachIndexed { index, value ->
            val barHeight = (value / max) * height * animatedProgress
            val x = spacing + (index * (barWidth.toPx() + spacing))
            val y = height - barHeight
            
            drawRoundRect(
                color = barColors[index % barColors.size],
                topLeft = Offset(x, y),
                size = Size(barWidth.toPx(), barHeight),
                cornerRadius = CornerRadius(barWidth.toPx() / 2)
            )
        }
    }
}

@Composable
fun WaveformChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = AccentRed,
    barWidth: Dp = 3.dp
) {
    val animatedProgress by animatedEntry(1f)
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val barCount = values.size
        val spacing = (width - (barWidth.toPx() * barCount)) / (barCount - 1).coerceAtLeast(1)

        values.forEachIndexed { index, value ->
            val barHeight = (value * height) * animatedProgress
            val x = index * (barWidth.toPx() + spacing)
            val y = (height - barHeight) / 2
            
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth.toPx(), barHeight),
                cornerRadius = CornerRadius(barWidth.toPx() / 2)
            )
        }
    }
}

@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = AccentRed,
    strokeWidth: Dp = 2.dp
) {
    val animatedProgress by animatedEntry(1f)
    val max = values.maxOrNull() ?: 1f

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val spacing = width / (values.size - 1).coerceAtLeast(1)
        
        val path = Path()
        val fillPath = Path()
        
        values.forEachIndexed { index, value ->
            val x = index * spacing
            val y = height - ((value / max) * height * animatedProgress)
            
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (index - 1) * spacing
                val prevY = height - ((values[index - 1] / max) * height * animatedProgress)
                path.cubicTo(
                    prevX + spacing / 2, prevY,
                    x - spacing / 2, y,
                    x, y
                )
                fillPath.cubicTo(
                    prevX + spacing / 2, prevY,
                    x - spacing / 2, y,
                    x, y
                )
            }
            if (index == values.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.2f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
        
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
        
        // End point marker
        if (values.isNotEmpty()) {
            val lastX = (values.size - 1) * spacing
            val lastY = height - ((values.last() / max) * height * animatedProgress)
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
        }
    }
}

@Composable
fun RouteSketch(
    routeColor: Color = Lime,
    gridColor: Color = Color.White.copy(alpha = 0.05f),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        // Grid
        for (i in 0..5) {
            val x = (w / 5) * i
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), 1.dp.toPx())
            val y = (h / 5) * i
            drawLine(gridColor, Offset(0f, y), Offset(w, y), 1.dp.toPx())
        }
        
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.8f)
            cubicTo(w * 0.4f, h * 0.7f, w * 0.1f, h * 0.4f, w * 0.5f, h * 0.3f)
            cubicTo(w * 0.8f, h * 0.2f, w * 0.7f, h * 0.6f, w * 0.9f, h * 0.5f)
        }
        
        drawPath(path, routeColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        
        drawCircle(routeColor, 4.dp.toPx(), Offset(w * 0.2f, h * 0.8f))
        drawCircle(Color.White, 3.dp.toPx(), Offset(w * 0.9f, h * 0.5f), style = Stroke(2.dp.toPx()))
    }
}
