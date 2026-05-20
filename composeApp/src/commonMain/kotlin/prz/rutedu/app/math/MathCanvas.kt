package prz.rutedu.app.math

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * A general-purpose mathematical canvas that draws a coordinate system and a
 * list of [MathShape] objects on top of it.
 *
 * Usage:
 * ```
 * MathCanvas(
 *     shapes = listOf(
 *         MathShape.FunctionPlot(f = { x -> x * x }),
 *         MathShape.PointMark(Pt(2.0, 4.0), label = "(2, 4)")
 *     ),
 *     viewport = MathViewport(xMin = -4.0, xMax = 4.0, yMin = -1.0, yMax = 10.0)
 * )
 * ```
 */
@Composable
fun MathCanvas(
    shapes: List<MathShape>,
    modifier: Modifier = Modifier,
    viewport: MathViewport = MathViewport()
) {
    val tm = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        if (viewport.showGrid) drawGrid(viewport)
        if (viewport.showAxes) drawAxes(viewport, tm)
        shapes.forEach { shape ->
            when (shape) {
                is MathShape.FunctionPlot -> drawFunctionPlot(shape, viewport)
                is MathShape.Circle       -> drawCircleShape(shape, viewport)
                is MathShape.Triangle     -> drawTriangleShape(shape, viewport, tm)
                is MathShape.Rectangle    -> drawRectangleShape(shape, viewport)
                is MathShape.PointMark    -> drawPointMark(shape, viewport, tm)
                is MathShape.Segment      -> drawSegmentShape(shape, viewport)
                is MathShape.TextLabel    -> drawTextLabel(shape, viewport, tm)
            }
        }
    }
}

// ── coordinate transform ──────────────────────────────────────────────────────

private fun DrawScope.toCanvasX(wx: Double, vp: MathViewport): Float =
    ((wx - vp.xMin) / (vp.xMax - vp.xMin) * size.width).toFloat()

private fun DrawScope.toCanvasY(wy: Double, vp: MathViewport): Float =
    ((1.0 - (wy - vp.yMin) / (vp.yMax - vp.yMin)) * size.height).toFloat()

private fun DrawScope.toCanvas(pt: Pt, vp: MathViewport): Offset =
    Offset(toCanvasX(pt.x, vp), toCanvasY(pt.y, vp))

// ── grid ──────────────────────────────────────────────────────────────────────

private fun DrawScope.drawGrid(vp: MathViewport) {
    val color = Color(0xFFE8EAF0)
    var wx = ceil(vp.xMin / vp.gridStep) * vp.gridStep
    while (wx <= vp.xMax + 1e-9) {
        if (abs(wx) > 1e-10) {
            val x = toCanvasX(wx, vp)
            drawLine(color, Offset(x, 0f), Offset(x, size.height))
        }
        wx += vp.gridStep
    }
    var wy = ceil(vp.yMin / vp.gridStep) * vp.gridStep
    while (wy <= vp.yMax + 1e-9) {
        if (abs(wy) > 1e-10) {
            val y = toCanvasY(wy, vp)
            drawLine(color, Offset(0f, y), Offset(size.width, y))
        }
        wy += vp.gridStep
    }
}

// ── axes ──────────────────────────────────────────────────────────────────────

private fun DrawScope.drawAxes(vp: MathViewport, tm: TextMeasurer) {
    val axisColor = Color(0xFFBBC1CA)
    val labelStyle = TextStyle(fontSize = 10.sp, color = Color(0xFF9E9E9E))

    // x-axis
    if (vp.yMin <= 0.0 && vp.yMax >= 0.0) {
        val y0 = toCanvasY(0.0, vp)
        drawLine(axisColor, Offset(0f, y0), Offset(size.width, y0), strokeWidth = 1.5f)
        var wx = ceil(vp.xMin / vp.gridStep) * vp.gridStep
        while (wx <= vp.xMax + 1e-9) {
            if (abs(wx) > 1e-10) {
                val x = toCanvasX(wx, vp)
                drawLine(axisColor, Offset(x, y0 - 4f), Offset(x, y0 + 4f))
                val m = tm.measure(formatNum(wx), labelStyle)
                drawText(m, topLeft = Offset(x - m.size.width / 2f, y0 + 5f))
            }
            wx += vp.gridStep
        }
    }

    // y-axis
    if (vp.xMin <= 0.0 && vp.xMax >= 0.0) {
        val x0 = toCanvasX(0.0, vp)
        drawLine(axisColor, Offset(x0, 0f), Offset(x0, size.height), strokeWidth = 1.5f)
        var wy = ceil(vp.yMin / vp.gridStep) * vp.gridStep
        while (wy <= vp.yMax + 1e-9) {
            if (abs(wy) > 1e-10) {
                val y = toCanvasY(wy, vp)
                drawLine(axisColor, Offset(x0 - 4f, y), Offset(x0 + 4f, y))
                val m = tm.measure(formatNum(wy), labelStyle)
                drawText(m, topLeft = Offset(x0 + 6f, y - m.size.height / 2f))
            }
            wy += vp.gridStep
        }
    }
}

private fun formatNum(v: Double): String {
    val l = v.toLong()
    return if (v == l.toDouble()) l.toString() else v.toString()
}

// ── function plot ─────────────────────────────────────────────────────────────

private fun DrawScope.drawFunctionPlot(shape: MathShape.FunctionPlot, vp: MathViewport) {
    val path = Path()
    val dx = (vp.xMax - vp.xMin) / shape.samples
    var penDown = false
    for (i in 0..shape.samples) {
        val wx = vp.xMin + i * dx
        val wy = try { shape.f(wx) } catch (e: Exception) { Double.NaN }
        if (!wy.isFinite()) { penDown = false; continue }
        val cp = toCanvas(Pt(wx, wy), vp)
        if (!penDown) { path.moveTo(cp.x, cp.y); penDown = true }
        else path.lineTo(cp.x, cp.y)
    }
    drawPath(path, shape.color, style = Stroke(width = shape.strokeWidth.dp.toPx()))
}

// ── circle ────────────────────────────────────────────────────────────────────

private fun DrawScope.drawCircleShape(shape: MathShape.Circle, vp: MathViewport) {
    val center = toCanvas(Pt(shape.cx, shape.cy), vp)
    val radiusPx = (shape.r * size.width / (vp.xMax - vp.xMin)).toFloat()
    if (shape.filled) drawCircle(shape.color.copy(alpha = 0.12f), radiusPx, center)
    drawCircle(shape.color, radiusPx, center, style = Stroke(width = shape.strokeWidth.dp.toPx()))
}

// ── triangle ──────────────────────────────────────────────────────────────────

private fun DrawScope.drawTriangleShape(shape: MathShape.Triangle, vp: MathViewport, tm: TextMeasurer) {
    val pa = toCanvas(shape.a, vp)
    val pb = toCanvas(shape.b, vp)
    val pc = toCanvas(shape.c, vp)

    // Sides
    drawPath(Path().apply {
        moveTo(pa.x, pa.y); lineTo(pb.x, pb.y); lineTo(pc.x, pc.y); close()
    }, shape.color, style = Stroke(width = 2.5.dp.toPx()))

    // Angle arcs
    if (shape.showAngleArcs) {
        drawAngleArc(pa, pb, pc, shape.color)
        drawAngleArc(pb, pa, pc, shape.color)
        drawAngleArc(pc, pa, pb, shape.color)
    }

    // Vertex labels — placed outward from the centroid
    // "?" gets a distinct red colour and larger size to stand out against the triangle lines
    val gx = (pa.x + pb.x + pc.x) / 3f
    val gy = (pa.y + pb.y + pc.y) / 3f
    val vertexLabelOffsetPx = 40.dp.toPx()   // well clear of the 18dp angle arc

    listOf(pa to shape.labelA, pb to shape.labelB, pc to shape.labelC)
        .forEach { (v, lbl) ->
            if (lbl == null) return@forEach
            val isUnknown = lbl == "?"
            val style = if (isUnknown) {
                TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
            } else {
                TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
            }
            val dx = v.x - gx; val dy = v.y - gy
            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val m = tm.measure(lbl, style)
            drawText(m, topLeft = Offset(
                v.x + dx / len * vertexLabelOffsetPx - m.size.width / 2f,
                v.y + dy / len * vertexLabelOffsetPx - m.size.height / 2f
            ))
        }

    // Side labels — placed outward from the centroid at midpoints
    val midStyle = TextStyle(fontSize = 12.sp, color = Color(0xFF555555))
    val midLabelOffsetPx = 14.dp.toPx()
    listOf(
        Offset((pb.x + pc.x) / 2f, (pb.y + pc.y) / 2f) to shape.labelBC,
        Offset((pc.x + pa.x) / 2f, (pc.y + pa.y) / 2f) to shape.labelCA,
        Offset((pa.x + pb.x) / 2f, (pa.y + pb.y) / 2f) to shape.labelAB
    ).forEach { (mid, lbl) ->
        if (lbl == null) return@forEach
        val dx = mid.x - gx; val dy = mid.y - gy
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val m = tm.measure(lbl, midStyle)
        drawText(m, topLeft = Offset(
            mid.x + dx / len * midLabelOffsetPx - m.size.width / 2f,
            mid.y + dy / len * midLabelOffsetPx - m.size.height / 2f
        ))
    }
}

/**
 * Draws a small arc at [vertex] sweeping between the directions to [arm1] and [arm2].
 * Always picks the shorter (interior) arc — valid for any triangle angle < 180°.
 */
private fun DrawScope.drawAngleArc(vertex: Offset, arm1: Offset, arm2: Offset, color: Color) {
    val arcR = 18.dp.toPx()
    val d1 = (atan2((arm1.y - vertex.y).toDouble(), (arm1.x - vertex.x).toDouble()) * 180.0 / PI).toFloat()
    val d2 = (atan2((arm2.y - vertex.y).toDouble(), (arm2.x - vertex.x).toDouble()) * 180.0 / PI).toFloat()
    var sweep = (d2 - d1 + 360f) % 360f
    if (sweep > 180f) sweep -= 360f   // always take the shorter (interior) arc
    drawPath(
        Path().apply { addArc(Rect(center = vertex, radius = arcR), d1, sweep) },
        color.copy(alpha = 0.65f),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

// ── rectangle ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawRectangleShape(shape: MathShape.Rectangle, vp: MathViewport) {
    // World (x, y) is the bottom-left corner; canvas y is inverted
    val tl = toCanvas(Pt(shape.x, shape.y + shape.h), vp)
    val br = toCanvas(Pt(shape.x + shape.w, shape.y), vp)
    val s = Size(br.x - tl.x, br.y - tl.y)
    if (shape.filled) drawRect(shape.color.copy(alpha = 0.12f), tl, s)
    drawRect(shape.color, tl, s, style = Stroke(width = shape.strokeWidth.dp.toPx()))
}

// ── point mark ────────────────────────────────────────────────────────────────

private fun DrawScope.drawPointMark(shape: MathShape.PointMark, vp: MathViewport, tm: TextMeasurer) {
    val cp = toCanvas(shape.pt, vp)
    drawCircle(shape.color, shape.radiusDp.dp.toPx(), cp)
    if (shape.label != null) {
        val m = tm.measure(shape.label, TextStyle(fontSize = 11.sp, color = shape.color))
        drawText(m, topLeft = Offset(cp.x + 8f, cp.y - m.size.height / 2f))
    }
}

// ── segment ───────────────────────────────────────────────────────────────────

private fun DrawScope.drawSegmentShape(shape: MathShape.Segment, vp: MathViewport) {
    val from = toCanvas(shape.from, vp)
    val to   = toCanvas(shape.to,   vp)
    val effect = if (shape.dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 4f)) else null
    drawPath(
        Path().apply { moveTo(from.x, from.y); lineTo(to.x, to.y) },
        shape.color,
        style = Stroke(width = shape.strokeWidth.dp.toPx(), pathEffect = effect)
    )
}

// ── text label ────────────────────────────────────────────────────────────────

private fun DrawScope.drawTextLabel(shape: MathShape.TextLabel, vp: MathViewport, tm: TextMeasurer) {
    val cp = toCanvas(shape.pt, vp)
    val m = tm.measure(shape.text, TextStyle(fontSize = shape.sizeSp.sp, color = shape.color))
    drawText(m, topLeft = Offset(cp.x - m.size.width / 2f, cp.y - m.size.height / 2f))
}
