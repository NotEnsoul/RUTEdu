package com.example.myapplication.models

enum class MathOperator(val symbol: String) {
    ADD("+"), SUBTRACT("−"), MULTIPLY("×"), DIVIDE("÷"), POWER("^")
}

/**
 * Hint shown in the bottom sheet.
 * @param mainText  text in the left-border box (may contain [boldPart] in bold)
 * @param boldPart  substring of [mainText] to render bold
 * @param sectionTitle  small-caps label above [items]
 * @param items     bullet rows below [sectionTitle]
 * @param steps     "Krok po kroku" list
 */
data class Hint(
    val mainText: String,
    val boldPart: String? = null,
    val sectionTitle: String? = null,
    val items: List<String> = emptyList(),
    val steps: List<String> = emptyList()
)

enum class MapRegion(
    val lonMin: Float, val lonMax: Float,
    val latMin: Float, val latMax: Float
) {
    EUROPE(-27f, 60f, 27f, 75f),
    CENTRAL_EUROPE(-8f, 35f, 44f, 60f),
    ASIA(25f, 155f, 5f, 75f)
}

sealed class Question(open val id: Int) {
    /** User types the numeric answer */
    data class FindAnswer(
        override val id: Int,
        val operand1: Int,
        val operand2: Int,
        val operator: MathOperator,
        val hint: Hint = Hint("")
    ) : Question(id) {
        val correctAnswer: Int = when (operator) {
            MathOperator.ADD      -> operand1 + operand2
            MathOperator.SUBTRACT -> operand1 - operand2
            MathOperator.MULTIPLY -> operand1 * operand2
            MathOperator.DIVIDE   -> operand1 / operand2
            MathOperator.POWER    -> {
                var r = 1
                repeat(operand2) { r *= operand1 }
                r
            }
        }
    }

    /** User drags the correct operator into the blank */
    data class FindOperator(
        override val id: Int,
        val operand1: Int,
        val operand2: Int,
        val result: Int,
        val correctOperator: MathOperator,
        val hint: Hint = Hint("")
    ) : Question(id)

    /**
     * User selects one or multiple correct options from a list.
     * @param multiSelect  false = radio (exactly one correct), true = checkbox (one or more correct)
     */
    data class SelectFromList(
        override val id: Int,
        val prompt: String,
        val options: List<String>,
        val correctIndices: Set<Int>,
        val multiSelect: Boolean = false,
        val hint: Hint = Hint("")
    ) : Question(id)

    /**
     * User types a numeric answer. Optionally shows a triangle diagram.
     * @param triangleAngles  pair of known angles; unknown is 180 - a - b
     * @param inlineHint      always-visible hint row beneath the input
     */
    data class TypeAnswer(
        override val id: Int,
        val prompt: String,
        val correctAnswer: Int,
        val unit: String = "",
        val triangleAngles: Pair<Int, Int>? = null,
        val inlineHint: String? = null,
        val hint: Hint = Hint("")
    ) : Question(id)

    /**
     * User taps the correct country on a map.
     * @param countryKey    English name as in GeoJSON (e.g. "Poland")
     * @param questionText  Full grammatically correct question in Polish
     * @param region        Map viewport to use
     */
    data class MapQuiz(
        override val id: Int,
        val countryKey: String,
        val questionText: String,
        val region: MapRegion = MapRegion.EUROPE,
        val hint: Hint = Hint("")
    ) : Question(id)

    /**
     * Full zoomable periodic table: user selects an element from the tray and
     * taps the correct empty slot to place it.
     * @param missingAtomicNumbers  atomic numbers of the 4–5 elements removed from the table
     */
    data class PeriodicTableQuiz(
        override val id: Int,
        val questionText: String,
        val missingAtomicNumbers: List<Int>,
        val hint: Hint = Hint("")
    ) : Question(id)

    /**
     * Zoomable periodic table — user sees a shell config (e.g. "2,8,1") and taps
     * the matching element cell.
     */
    data class PeriodicTableByShell(
        override val id: Int,
        val shellConfig: String,        // e.g. "2,8,1"
        val targetAtomicNumber: Int,
        val hint: Hint = Hint("")
    ) : Question(id)

    /**
     * Zoomable periodic table — user sees an element name (Polish) and taps
     * the matching element cell.
     */
    data class PeriodicTableByName(
        override val id: Int,
        val elementNamePL: String,      // e.g. "Sód"
        val targetAtomicNumber: Int,
        val hint: Hint = Hint("")
    ) : Question(id)
}
