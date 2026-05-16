package com.example.myapplication.data

import com.example.myapplication.models.*
import com.example.myapplication.models.ElementCategory.*
import com.example.myapplication.models.Question.*
import kotlin.math.roundToInt
import kotlin.random.Random

object ChemistryQuestionGenerator {

    fun generateFor(lessonId: String): List<Question> {
        val seed = Random.nextLong()
        return when (lessonId) {
            "chemia_1_1" -> chemia_1_1(seed)
            "chemia_1_2" -> chemia_1_2(seed)
            "chemia_1_3" -> chemia_1_3(seed)
            "chemia_1_4" -> chemia_1_4(seed)
            "chemia_2_1" -> chemia_2_1(seed)
            "chemia_2_2" -> chemia_2_2(seed)
            else -> emptyList()
        }
    }

    // ── lesson generators ─────────────────────────────────────────────────────

    private fun chemia_1_1(seed: Long): List<Question> {
        val rng = Random(seed)
        return shellConfigByNumber.entries
            .filter { it.key in 1..54 }
            .shuffled(rng)
            .take(15)
            .mapIndexed { i, (z, shell) ->
                val el = elementByNumber[z]!!
                PeriodicTableByShell(
                    id = 1100 + i,
                    shellConfig = shell,
                    targetAtomicNumber = z,
                    hint = Hint(
                        mainText = "${el.namePL} (${el.symbol}) ma $z elektronów: $shell.",
                        boldPart = el.symbol,
                        steps = shellSteps(z, shell)
                    )
                )
            }
    }

    private fun chemia_1_2(seed: Long): List<Question> {
        val rng = Random(seed)
        return ELEMENTS
            .filter { it.atomicNumber <= 86 && it.category !in listOf(LANTHANIDE, ACTINIDE) }
            .shuffled(rng)
            .take(15)
            .mapIndexed { i, el ->
                PeriodicTableByName(
                    id = 1200 + i,
                    elementNamePL = el.namePL,
                    targetAtomicNumber = el.atomicNumber,
                    hint = Hint(
                        mainText = "${el.namePL} (${el.symbol}) — gr. ${el.tableCol}, okres ${el.tableRow}.",
                        boldPart = el.symbol
                    )
                )
            }
    }

    private fun chemia_1_3(seed: Long): List<Question> {
        val rng = Random(seed)
        return moleculeQuestions(rng).take(15)
    }

    private fun chemia_1_4(seed: Long): List<Question> {
        val rng = Random(seed)
        return ELEMENTS
            .filter { it.atomicNumber in 1..36 }
            .flatMap { el ->
                listOf(electronQ(el, rng), protonQ(el, rng), massQ(el, rng), periodQ(el, rng))
            }
            .shuffled(rng)
            .take(15)
            .mapIndexed { i, q -> q.copy(id = 1400 + i) }
    }

    private fun chemia_2_1(seed: Long): List<Question> {
        val rng = Random(seed)
        return periodicTableSets().shuffled(rng).take(10).mapIndexed { i, (title, nums, hint) ->
            PeriodicTableQuiz(id = 2100 + i, questionText = title, missingAtomicNumbers = nums, hint = hint)
        }
    }

    private fun chemia_2_2(seed: Long): List<Question> {
        val rng = Random(seed)
        return propertyQuestions(rng).take(15)
    }

    // ── ElementCardQuiz builders ──────────────────────────────────────────────

    private fun electronQ(el: Element, rng: Random): ElementCardQuiz {
        val z = el.atomicNumber
        val opts = buildOptions(z.toString(), distractors(z, 1, 120, rng), rng)
        return ElementCardQuiz(
            id = 0,
            prompt = "Ile elektronów posiada atom ${el.namePL}?",
            atomicNumber = z,
            options = opts,
            correctIndex = opts.indexOf(z.toString()),
            hint = Hint(
                "Elektrony = liczba atomowa Z = $z.",
                steps = listOf("Z = $z", "Elektronów = $z")
            )
        )
    }

    private fun protonQ(el: Element, rng: Random): ElementCardQuiz {
        val z = el.atomicNumber
        val opts = buildOptions(z.toString(), distractors(z, 1, 120, rng), rng)
        return ElementCardQuiz(
            id = 0,
            prompt = "Ile protonów posiada atom ${el.namePL}?",
            atomicNumber = z,
            options = opts,
            correctIndex = opts.indexOf(z.toString()),
            hint = Hint(
                "Protonów = liczba atomowa Z = $z.",
                steps = listOf("Z = $z", "Protonów = $z")
            )
        )
    }

    private fun massQ(el: Element, rng: Random): ElementCardQuiz {
        val m = el.atomicMass.roundToInt()
        val opts = buildOptions(m.toString(), distractors(m, 1, 300, rng), rng)
        return ElementCardQuiz(
            id = 0,
            prompt = "Przybliżona masa atomowa ${el.symbol} wynosi:",
            atomicNumber = el.atomicNumber,
            options = opts,
            correctIndex = opts.indexOf(m.toString()),
            hint = Hint(
                "Masa atomowa ${el.symbol} ≈ $m u.",
                steps = listOf("Odczytaj masę atomową z karty pierwiastka")
            )
        )
    }

    private fun periodQ(el: Element, rng: Random): ElementCardQuiz {
        val p = el.tableRow.coerceIn(1, 7)
        val dists = (1..7).filter { it != p }.shuffled(rng).take(3).map { it.toString() }
        val opts = buildOptions(p.toString(), dists, rng)
        return ElementCardQuiz(
            id = 0,
            prompt = "W którym okresie leży pierwiastek ${el.symbol}?",
            atomicNumber = el.atomicNumber,
            options = opts,
            correctIndex = opts.indexOf(p.toString()),
            hint = Hint(
                "${el.namePL} leży w $p. okresie układu.",
                steps = listOf("Policz wiersz od góry tabeli — to numer okresu")
            )
        )
    }

    // ── distractor & option helpers ───────────────────────────────────────────

    private fun distractors(correct: Int, min: Int, max: Int, rng: Random): List<String> {
        val step = (correct / 5).coerceAtLeast(1)
        return (1..step * 10)
            .flatMap { d -> listOf(correct - d, correct + d) }
            .filter { it != correct && it in min..max }
            .distinct()
            .shuffled(rng)
            .take(3)
            .map { it.toString() }
    }

    private fun buildOptions(correct: String, wrongs: List<String>, rng: Random): List<String> =
        (wrongs.take(3) + correct).shuffled(rng)

    // ── periodic table sets ───────────────────────────────────────────────────

    private fun periodicTableSets(): List<Triple<String, List<Int>, Hint>> = listOf(
        Triple(
            "Umieść brakujące gazy szlachetne w układzie",
            listOf(2, 10, 18, 36, 54),
            Hint("Gazy szlachetne — gr. 18 (ostatnia kolumna).",
                items = listOf("He — gr.18, p.1", "Ne — gr.18, p.2", "Ar — gr.18, p.3", "Kr — gr.18, p.4", "Xe — gr.18, p.5"))
        ),
        Triple(
            "Uzupełnij halogeny w układzie",
            listOf(9, 17, 35, 53, 85),
            Hint("Halogeny — gr. 17.",
                items = listOf("F — gr.17, p.2", "Cl — gr.17, p.3", "Br — gr.17, p.4", "I — gr.17, p.5", "At — gr.17, p.6"))
        ),
        Triple(
            "Uzupełnij litowce w układzie",
            listOf(3, 11, 19, 37, 55),
            Hint("Litowce — gr. 1.",
                items = listOf("Li — gr.1, p.2", "Na — gr.1, p.3", "K — gr.1, p.4", "Rb — gr.1, p.5", "Cs — gr.1, p.6"))
        ),
        Triple(
            "Uzupełnij berylowce w układzie",
            listOf(4, 12, 20, 38, 56),
            Hint("Berylowce — gr. 2.",
                items = listOf("Be — gr.2, p.2", "Mg — gr.2, p.3", "Ca — gr.2, p.4", "Sr — gr.2, p.5", "Ba — gr.2, p.6"))
        ),
        Triple(
            "Umieść brakujące pierwiastki w układzie",
            listOf(6, 7, 8, 11, 17),
            Hint("Patrz na numer grupy i okresu.",
                items = listOf("C — gr.14, p.2", "N — gr.15, p.2", "O — gr.16, p.2", "Na — gr.1, p.3", "Cl — gr.17, p.3"))
        ),
        Triple(
            "Umieść brakujące metale szlachetne w układzie",
            listOf(26, 28, 29, 47, 79),
            Hint("Metale przejściowe — gr. 3–12.",
                items = listOf("Fe — gr.8", "Ni — gr.10", "Cu — gr.11", "Ag — gr.11, p.5", "Au — gr.11, p.6"))
        ),
        Triple(
            "Uzupełnij halogeny i litowce w układzie",
            listOf(3, 9, 19, 35, 53),
            Hint("Litowce → gr. 1, halogeny → gr. 17.",
                items = listOf("Li — gr.1, p.2", "F — gr.17, p.2", "K — gr.1, p.4", "Br — gr.17, p.4", "I — gr.17, p.5"))
        ),
        Triple(
            "Znajdź miejsca pierwiastków 3. okresu",
            listOf(12, 13, 14, 15, 16),
            Hint("Wszystkie w 3. wierszu układu.",
                items = listOf("Mg — gr.2", "Al — gr.13", "Si — gr.14", "P — gr.15", "S — gr.16"))
        ),
        Triple(
            "Znajdź miejsca pierwiastków 2. okresu",
            listOf(3, 5, 6, 7, 8),
            Hint("Wszystkie w 2. wierszu układu.",
                items = listOf("Li — gr.1", "B — gr.13", "C — gr.14", "N — gr.15", "O — gr.16"))
        ),
        Triple(
            "Znajdź miejsca metali przejściowych okresu 4",
            listOf(22, 24, 25, 26, 28),
            Hint("Ti, Cr, Mn, Fe, Ni — środkowy blok, okres 4.",
                items = listOf("Ti — gr.4", "Cr — gr.6", "Mn — gr.7", "Fe — gr.8", "Ni — gr.10"))
        ),
        Triple(
            "Uzupełnij niemetale reaktywne w układzie",
            listOf(6, 7, 8, 15, 16),
            Hint("C, N, O, P, S — niemetale reaktywne.",
                items = listOf("C — gr.14, p.2", "N — gr.15, p.2", "O — gr.16, p.2", "P — gr.15, p.3", "S — gr.16, p.3"))
        ),
        Triple(
            "Uzupełnij pierwiastki bloku s w układzie",
            listOf(1, 2, 11, 12, 19),
            Hint("Blok s — gr. 1 i 2 (oraz H i He).",
                items = listOf("H — gr.1, p.1", "He — gr.18, p.1", "Na — gr.1, p.3", "Mg — gr.2, p.3", "K — gr.1, p.4"))
        ),
        Triple(
            "Znajdź miejsca niemetali 3. okresu",
            listOf(14, 15, 16, 17, 18),
            Hint("Prawa strona 3. wiersza — niemetale.",
                items = listOf("Si — gr.14", "P — gr.15", "S — gr.16", "Cl — gr.17", "Ar — gr.18"))
        ),
        Triple(
            "Znajdź miejsca pierwiastków 4. okresu — blok p",
            listOf(31, 32, 33, 34, 35),
            Hint("Prawa strona 4. wiersza — po metalach przejściowych.",
                items = listOf("Ga — gr.13", "Ge — gr.14", "As — gr.15", "Se — gr.16", "Br — gr.17"))
        ),
        Triple(
            "Uzupełnij koniec i początek okresu 4 i 5",
            listOf(19, 36, 37, 54, 55),
            Hint("Gr. 1 (litowce) i gr. 18 (gazy szlachetne).",
                items = listOf("K — gr.1, p.4", "Kr — gr.18, p.4", "Rb — gr.1, p.5", "Xe — gr.18, p.5", "Cs — gr.1, p.6"))
        ),
    )

    // ── molecule questions ────────────────────────────────────────────────────

    private val molecules: List<Pair<String, String>> = listOf(
        "H₂O"      to "woda",
        "CO₂"      to "dwutlenek węgla",
        "NaCl"     to "chlorek sodu",
        "NH₃"      to "amoniak",
        "CH₄"      to "metan",
        "NaOH"     to "wodorotlenek sodu",
        "Ca(OH)₂"  to "wodorotlenek wapnia",
        "CaO"      to "tlenek wapnia",
        "Fe₂O₃"    to "tlenek żelaza(III)",
        "SO₂"      to "dwutlenek siarki",
        "SO₃"      to "trójtlenek siarki",
        "N₂"       to "azot cząsteczkowy",
        "O₂"       to "tlen cząsteczkowy",
        "H₂"       to "wodór cząsteczkowy",
        "CaCO₃"    to "węglan wapnia",
        "Al₂O₃"    to "tlenek glinu",
        "Na₂O"     to "tlenek sodu",
        "CO"       to "tlenek węgla(II)",
        "HF"       to "kwas fluorowodorowy",
        "HBr"      to "kwas bromowodorowy"
    )

    private fun moleculeQuestions(rng: Random): List<Question> {
        val allNames = molecules.map { it.second }
        val allFormulas = molecules.map { it.first }
        val qs = molecules.flatMap { (formula, name) ->
            val wrongNames = allNames.filter { it != name }.shuffled(rng).take(3)
            val opts1 = (wrongNames + name).shuffled(rng)
            val q1 = SelectFromList(
                id = 0,
                prompt = "Jak nazywa się związek o wzorze $formula?",
                options = opts1,
                correctIndices = setOf(opts1.indexOf(name)),
                hint = Hint("$formula to $name.", boldPart = name)
            )

            val wrongForms = allFormulas.filter { it != formula }.shuffled(rng).take(3)
            val opts2 = (wrongForms + formula).shuffled(rng)
            val q2 = SelectFromList(
                id = 0,
                prompt = "Jaki jest wzór chemiczny: $name?",
                options = opts2,
                correctIndices = setOf(opts2.indexOf(formula)),
                hint = Hint("Wzór $name to $formula.", boldPart = formula)
            )

            listOf(q1, q2)
        }
        return qs.shuffled(rng).mapIndexed { i, q -> q.copy(id = 1300 + i) }
    }

    // ── property questions ────────────────────────────────────────────────────

    private val metalCats = setOf(ALKALI_METAL, ALKALINE_EARTH, TRANSITION_METAL, POST_TRANSITION)
    private val nonMetalCats = setOf(REACTIVE_NONMETAL, HALOGEN)

    private fun propertyQuestions(rng: Random): List<Question> {
        val qs = mutableListOf<SelectFromList>()

        ELEMENTS.filter { it.atomicNumber in 1..54 }.forEach { el ->
            val type = when (el.category) {
                NOBLE_GAS  -> "gaz szlachetny"
                in metalCats -> "metal"
                in nonMetalCats -> "niemetal"
                METALLOID  -> "metaloid"
                else -> return@forEach
            }
            val opts = listOf("metal", "niemetal", "metaloid", "gaz szlachetny")
            qs += SelectFromList(
                id = 0,
                prompt = "Do jakiej grupy zaliczamy ${el.namePL} (${el.symbol})?",
                options = opts,
                correctIndices = setOf(opts.indexOf(type)),
                hint = Hint("${el.namePL} to $type.", boldPart = type)
            )
        }

        listOf(
            Triple("metalem",          metalCats,    nonMetalCats + setOf(METALLOID, NOBLE_GAS)),
            Triple("niemetalem",        nonMetalCats, metalCats    + setOf(METALLOID, NOBLE_GAS)),
            Triple("gazem szlachetnym", setOf(NOBLE_GAS),  metalCats + nonMetalCats + setOf(METALLOID))
        ).forEach { (label, correctCats, wrongCats) ->
            val correctPool = ELEMENTS.filter { it.category in correctCats && it.atomicNumber in 1..54 }
            val wrongPool   = ELEMENTS.filter { it.category in wrongCats   && it.atomicNumber in 1..54 }
            if (correctPool.isNotEmpty() && wrongPool.size >= 3) {
                repeat(5) {
                    val c  = correctPool.random(rng)
                    val ws = wrongPool.shuffled(rng).take(3)
                    val opts = (ws.map { it.namePL } + c.namePL).shuffled(rng)
                    qs += SelectFromList(
                        id = 0,
                        prompt = "Który z tych pierwiastków jest $label?",
                        options = opts,
                        correctIndices = setOf(opts.indexOf(c.namePL)),
                        hint = Hint("${c.namePL} to $label.", boldPart = c.namePL)
                    )
                }
            }
        }

        return qs.shuffled(rng).mapIndexed { i, q -> q.copy(id = 2200 + i) }
    }

    // ── shared utils ──────────────────────────────────────────────────────────

    private fun shellSteps(z: Int, config: String): List<String> {
        val shellNames = listOf("K", "L", "M", "N", "O", "P")
        return config.split(",").mapIndexed { i, n ->
            "Powłoka ${shellNames.getOrElse(i) { "?" }}: $n"
        } + "Razem: $z → ${elementByNumber[z]?.namePL} (${elementByNumber[z]?.symbol})"
    }
}
