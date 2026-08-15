package com.gelengeden.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** A tap-based 3×3 pattern grid. Each node can be selected once, in order. */
@Composable
fun PatternGrid(
    selectedNodes: List<Int>,
    onNodeTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color,
    inactiveColor: Color,
    contentDescription: String
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 300.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(inactiveColor.copy(alpha = 0.12f))
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures { offset ->
                        val column = (offset.x / (size.width / 3f)).toInt().coerceIn(0, 2)
                        val row = (offset.y / (size.height / 3f)).toInt().coerceIn(0, 2)
                        onNodeTapped(row * 3 + column)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / 3f
            val cellHeight = size.height / 3f
            val radius = min(cellWidth, cellHeight) * 0.12f
            val centers = (0 until 9).map { index ->
                Offset(
                    x = (index % 3 + 0.5f) * cellWidth,
                    y = (index / 3 + 0.5f) * cellHeight
                )
            }

            if (selectedNodes.size > 1) {
                val path = Path().apply {
                    moveTo(centers[selectedNodes.first()].x, centers[selectedNodes.first()].y)
                    selectedNodes.drop(1).forEach { node ->
                        lineTo(centers[node].x, centers[node].y)
                    }
                }
                drawPath(
                    path = path,
                    color = activeColor,
                    style = Stroke(width = radius * 0.45f)
                )
            }

            centers.forEachIndexed { index, center ->
                val selected = index in selectedNodes
                drawCircle(
                    color = if (selected) activeColor else inactiveColor,
                    radius = radius,
                    center = center
                )
                if (!selected) {
                    drawCircle(
                        color = inactiveColor.copy(alpha = 0.35f),
                        radius = radius * 0.55f,
                        center = center
                    )
                }
            }
        }
    }
}
