package prz.rutedu.app.math

import kotlin.math.*

/**
 * Utility for computing triangle vertices from geometric descriptions.
 * All angles are in **degrees**. Vertices are centred at the origin.
 *
 * Convention: A is at bottom-left, B at bottom-right, C is the apex.
 */
object TriangleBuilder {

    /**
     * Build a triangle from two base angles and the base length.
     * Returns the three vertices (A, B, C) centred at the origin.
     *
     * @param angleA  angle at vertex A (degrees)
     * @param angleB  angle at vertex B (degrees)
     * @param baseLen length of side AB (world units, default 4.0)
     */
    fun fromAngles(
        angleA: Double,
        angleB: Double,
        baseLen: Double = 4.0
    ): Triple<Pt, Pt, Pt> {
        val angleC = 180.0 - angleA - angleB
        require(angleC > 0.0) { "Angles must sum to less than 180° (got $angleA + $angleB)" }

        val aRad = angleA * PI / 180.0
        val bRad = angleB * PI / 180.0
        val cRad = angleC * PI / 180.0

        // Law of sines: baseLen / sin(C) = AC / sin(B)
        val acLen = baseLen * sin(bRad) / sin(cRad)

        val ax = 0.0;  val ay = 0.0
        val bx = baseLen; val by = 0.0
        val cx = acLen * cos(aRad)
        val cy = acLen * sin(aRad)

        // Centre by centroid
        val gx = (ax + bx + cx) / 3.0
        val gy = (ay + by + cy) / 3.0

        return Triple(Pt(ax - gx, ay - gy), Pt(bx - gx, by - gy), Pt(cx - gx, cy - gy))
    }

    /**
     * Same as [fromAngles] but also returns a tight [MathViewport] around the
     * triangle with [margin] world-unit padding on each side.
     * Grid and axes are turned off — suitable for plain geometry diagrams.
     */
    fun fromAnglesWithViewport(
        angleA: Double,
        angleB: Double,
        baseLen: Double = 4.0,
        margin: Double = 1.2
    ): Pair<Triple<Pt, Pt, Pt>, MathViewport> {
        val verts = fromAngles(angleA, angleB, baseLen)
        val (a, b, c) = verts
        val xs = listOf(a.x, b.x, c.x)
        val ys = listOf(a.y, b.y, c.y)
        return Pair(
            verts,
            MathViewport(
                xMin = xs.minOrNull()!! - margin,
                xMax = xs.maxOrNull()!! + margin,
                yMin = ys.minOrNull()!! - margin,
                yMax = ys.maxOrNull()!! + margin,
                showGrid = false,
                showAxes = false
            )
        )
    }

    /** Equilateral triangle with given side length. */
    fun equilateral(side: Double = 4.0): Triple<Pt, Pt, Pt> = fromAngles(60.0, 60.0, side)

    /**
     * Right triangle with the right angle at A.
     * [legH] is the horizontal leg (AB), [legV] is the vertical leg (AC).
     */
    fun rightTriangle(legH: Double = 3.0, legV: Double = 4.0): Triple<Pt, Pt, Pt> {
        val gx = legH / 3.0
        val gy = legV / 3.0
        return Triple(
            Pt(-gx, -gy),
            Pt(legH - gx, -gy),
            Pt(-gx, legV - gy)
        )
    }
}
