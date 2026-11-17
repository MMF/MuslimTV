package com.mfouad.muslimtv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dark gradient with a faint tiled eight-point star (khatam) pattern. The path is built once
 * per size in drawWithCache, so it costs nothing per frame on low-end boxes.
 */
@Composable
fun GeometricBackground(
    modifier: Modifier = Modifier,
    patternAlpha: Float = 0.05f,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MtvTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(colors.backgroundElevated, colors.background), radius = 2200f))
            .drawWithCache {
                val tile = 120.dp.toPx()
                val star = starPath(tile * 0.36f)
                val stroke = Stroke(width = 1.dp.toPx())
                val color = colors.gold.copy(alpha = patternAlpha)
                onDrawBehind {
                    var y = 0f
                    var row = 0
                    while (y < size.height + tile) {
                        var x = if (row % 2 == 0) 0f else tile / 2
                        while (x < size.width + tile) {
                            translateStar(this, star, Offset(x, y), color, stroke)
                            x += tile
                        }
                        y += tile * 0.5f
                        row++
                    }
                }
            },
        content = content,
    )
}

private fun translateStar(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    path: Path,
    at: Offset,
    color: Color,
    stroke: Stroke,
) {
    scope.drawContext.transform.translate(at.x, at.y)
    scope.drawPath(path, color, style = stroke)
    scope.drawContext.transform.translate(-at.x, -at.y)
}

/** Two overlapping squares rotated 45°: the classic eight-point star. */
private fun starPath(radius: Float): Path = Path().apply {
    val inner = radius * 0.75f
    for (i in 0 until 16) {
        val r = if (i % 2 == 0) radius else inner
        val a = i * PI / 8 - PI / 2
        val x = (r * cos(a)).toFloat()
        val y = (r * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** TV overscan safe area: 5% margins on every side. */
@Composable
fun SafeArea(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(safeAreaPadding(maxWidth, maxHeight)), content = content)
    }
}

fun safeAreaPadding(width: Dp, height: Dp) = PaddingValues(horizontal = width * 0.05f, vertical = height * 0.05f)
