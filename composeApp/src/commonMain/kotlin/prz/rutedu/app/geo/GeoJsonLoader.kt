package prz.rutedu.app.geo

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import rutedu.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Parsed representation of one country from the GeoJSON asset.
 *
 * A country may consist of multiple disjoint polygons (e.g. an archipelago), hence
 * [rings] is a list of rings rather than a single polygon. Each ring is the outer
 * contour of one polygon - holes are discarded because the quiz only needs a fill
 * and hit-test surface.
 *
 * @property name  English country name as stored in the GeoJSON `"name"` property.
 *                 This is the value compared against [prz.rutedu.app.models.Question.MapQuiz.countryKey].
 * @property iso2  ISO 3166-1 alpha-2 country code (e.g. `"PL"` for Poland). Not used
 *                 in the current quiz logic but kept for potential future features.
 * @property rings Outer contour rings in geographic coordinates (longitude, latitude pairs).
 *                 Each ring has already been simplified by [simplify].
 */
data class CountryFeature(
    val name: String,
    val iso2: String,
    val rings: List<List<LonLat>>
)

/**
 * A geographic coordinate in decimal degrees.
 *
 * @property lon Longitude (east-west), range −180..180. Positive = East.
 * @property lat Latitude (north-south), range −90..90. Positive = North.
 */
data class LonLat(val lon: Float, val lat: Float)

/**
 * Module-level cache populated on the first [loadCountries] call.
 * Subsequent calls return the cached list without re-reading the asset.
 */
private var cachedCountries: List<CountryFeature>? = null

/**
 * Removes points from a polygon ring that are closer than [epsilon] degrees to the
 * previous kept point. This reduces the number of draw calls at the cost of a small
 * loss in polygon fidelity - acceptable for a quiz map where pixel-perfect borders
 * are not required.
 *
 * The first and last points of the ring are always kept to preserve closure.
 *
 * @receiver The original ring as a list of [LonLat] points.
 * @param epsilon  Minimum distance (in degrees) between consecutive kept points.
 *                 Default 0.025° ~ 2.8 km at the equator.
 * @return A simplified ring with redundant intermediate points removed.
 */
private fun List<LonLat>.simplify(epsilon: Float = 0.025f): List<LonLat> {
    if (size <= 3) return this
    val result = mutableListOf(first())
    for (i in 1 until size) {
        val prev = result.last()
        val curr = this[i]
        val dx = curr.lon - prev.lon
        val dy = curr.lat - prev.lat
        if (dx * dx + dy * dy > epsilon * epsilon) result.add(curr)
    }
    // Always keep the last point to ensure the ring closes properly
    if (result.last() != last()) result.add(last())
    return result
}

/**
 * Parses one GeoJSON coordinate ring (an array of [lon, lat] pairs) into a list of [LonLat].
 * Points with fewer than two coordinates are silently skipped.
 */
private fun parseRing(ringArray: kotlinx.serialization.json.JsonArray): List<LonLat> {
    return ringArray.mapNotNull { ptElem ->
        val pt = ptElem.jsonArray
        if (pt.size >= 2) LonLat(pt[0].jsonPrimitive.float, pt[1].jsonPrimitive.float) else null
    }
}

/**
 * Loads and parses the bundled `countries.geojson` asset, returning a list of
 * [CountryFeature] objects ready for rendering and hit-testing.
 *
 * The function is **suspend** because reading a bundled resource is an IO operation on
 * some platforms. The result is cached in memory after the first call so repeated
 * invocations are instant.
 *
 * Supported GeoJSON geometry types:
 * - `"Polygon"` - a single polygon (outer ring only; holes are ignored).
 * - `"MultiPolygon"` - multiple polygons (outer rings only), e.g. island chains.
 *
 * Countries with no valid rings after simplification are excluded from the result.
 *
 * @return The full list of [CountryFeature] objects, cached after the first load.
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun loadCountries(): List<CountryFeature> {
    cachedCountries?.let { return it }

    val bytes = Res.readBytes("files/countries.geojson")
    val jsonStr = bytes.decodeToString()
    val root = Json.parseToJsonElement(jsonStr).jsonObject
    val features = root["features"]!!.jsonArray

    val result = mutableListOf<CountryFeature>()
    for (featureElem in features) {
        val featureObj = featureElem.jsonObject
        val props = featureObj["properties"]!!.jsonObject
        val name = props["name"]?.jsonPrimitive?.content ?: continue
        val iso2 = props["ISO3166-1-Alpha-2"]?.jsonPrimitive?.content ?: ""
        val geomObj = featureObj["geometry"]?.jsonObject ?: continue
        val geomType = geomObj["type"]?.jsonPrimitive?.content ?: continue
        val coordsArr = geomObj["coordinates"]?.jsonArray ?: continue

        val rings = mutableListOf<List<LonLat>>()

        when (geomType) {
            "Polygon" -> {
                // coordsArr = [ outerRing, hole1, hole2, ... ] - only outer ring is used
                val outerRing = parseRing(coordsArr[0].jsonArray).simplify()
                if (outerRing.size >= 3) rings.add(outerRing)
            }
            "MultiPolygon" -> {
                // coordsArr = [ polygon1, polygon2, ... ] where polygon = [ outerRing, holes… ]
                for (polyElem in coordsArr) {
                    val polyArr = polyElem.jsonArray
                    if (polyArr.isEmpty()) continue
                    val outerRing = parseRing(polyArr[0].jsonArray).simplify()
                    if (outerRing.size >= 3) rings.add(outerRing)
                }
            }
            else -> continue
        }

        if (rings.isEmpty()) continue
        result.add(CountryFeature(name, iso2, rings))
    }

    cachedCountries = result
    return result
}
