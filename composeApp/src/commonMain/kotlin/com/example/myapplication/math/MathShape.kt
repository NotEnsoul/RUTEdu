package com.example.myapplication.math

import androidx.compose.ui.graphics.Color

/** A 2-D point in mathematical (world) coordinates. */
data class Pt(val x: Double, val y: Double)

/**
 * Defines the visible region of the coordinate plane for [MathCanvas].
 * @param showGrid  draw light grid lines
 * @param showAxes  draw x/y axes with tick labels
 * @param gridStep  spacing between grid lines / tick marks (in world units)
 */
data class MathViewport(
    val xMin: Double = -5.0,
    val xMax: Double = 5.0,
    val yMin: Double = -4.0,
    val yMax: Double = 4.0,
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val gridStep: Double = 1.0
)

/** All drawable objects that [MathCanvas] understands. */
sealed class MathShape {

    /** Plots y = f(x) by sampling at [samples] evenly-spaced x values. */
    data class FunctionPlot(
        val f: (Double) -> Double,
        val color: Color = Color(0xFF4A80F0),
        val label: String? = null,
        val strokeWidth: Float = 2.5f,
        val samples: Int = 300
    ) : MathShape()

    /** Circle with centre (cx, cy) and radius r. */
    data class Circle(
        val cx: Double,
        val cy: Double,
        val r: Double,
        val color: Color = Color(0xFF4A80F0),
        val filled: Boolean = false,
        val strokeWidth: Float = 2f
    ) : MathShape()

    /**
     * Triangle defined by three vertices.
     * Optional vertex labels (e.g. "60°", "?") are placed outside each corner.
     * Optional side labels are placed at midpoints of sides AB, BC, CA.
     */
    data class Triangle(
        val a: Pt,
        val b: Pt,
        val c: Pt,
        val color: Color = Color(0xFF4A80F0),
        val showAngleArcs: Boolean = true,
        val labelA: String? = null,
        val labelB: String? = null,
        val labelC: String? = null,
        val labelAB: String? = null,
        val labelBC: String? = null,
        val labelCA: String? = null
    ) : MathShape()

    /** Axis-aligned rectangle with bottom-left corner at (x, y). */
    data class Rectangle(
        val x: Double,
        val y: Double,
        val w: Double,
        val h: Double,
        val color: Color = Color(0xFF4A80F0),
        val filled: Boolean = false,
        val strokeWidth: Float = 2f
    ) : MathShape()

    /** A filled dot with an optional text label to its right. */
    data class PointMark(
        val pt: Pt,
        val label: String? = null,
        val color: Color = Color(0xFF4A80F0),
        val radiusDp: Float = 4f
    ) : MathShape()

    /** A straight line segment between two world-space points. */
    data class Segment(
        val from: Pt,
        val to: Pt,
        val color: Color = Color(0xFF4A80F0),
        val dashed: Boolean = false,
        val strokeWidth: Float = 2f
    ) : MathShape()

    /** Free-floating text centred at a world-space point. */
    data class TextLabel(
        val pt: Pt,
        val text: String,
        val color: Color = Color(0xFF1A1A1A),
        val sizeSp: Float = 13f
    ) : MathShape()
}
