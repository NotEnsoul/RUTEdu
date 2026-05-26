package prz.rutedu.app.data

import prz.rutedu.app.data.ChemistryQuestionGenerator.generateFor
import prz.rutedu.app.models.ELEMENTS
import prz.rutedu.app.models.Element
import prz.rutedu.app.models.ElementCategory.ACTINIDE
import prz.rutedu.app.models.ElementCategory.ALKALINE_EARTH
import prz.rutedu.app.models.ElementCategory.ALKALI_METAL
import prz.rutedu.app.models.ElementCategory.HALOGEN
import prz.rutedu.app.models.ElementCategory.LANTHANIDE
import prz.rutedu.app.models.ElementCategory.METALLOID
import prz.rutedu.app.models.ElementCategory.NOBLE_GAS
import prz.rutedu.app.models.ElementCategory.POST_TRANSITION
import prz.rutedu.app.models.ElementCategory.REACTIVE_NONMETAL
import prz.rutedu.app.models.ElementCategory.TRANSITION_METAL
import prz.rutedu.app.models.Hint
import prz.rutedu.app.models.Question
import prz.rutedu.app.models.Question.BalanceTerm
import prz.rutedu.app.models.Question.ElementCardQuiz
import prz.rutedu.app.models.Question.EquationBalance
import prz.rutedu.app.models.Question.PeriodicTableByName
import prz.rutedu.app.models.Question.PeriodicTableByShell
import prz.rutedu.app.models.Question.PeriodicTableQuiz
import prz.rutedu.app.models.Question.SelectFromList
import prz.rutedu.app.models.elementByNumber
import prz.rutedu.app.models.shellConfigByNumber
import prz.rutedu.app.models.localizedName
import prz.rutedu.app.models.localizedGroupName
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Procedurally generates chemistry quiz questions from a random seed.
 *
 * Unlike the static [QuestionBank], chemistry lessons require a large and varied question
 * pool (covering all elements, all acids, all reaction types, etc.). Generating them at
 * runtime from [prz.rutedu.app.models.ELEMENTS] and similar data tables avoids
 * duplicating hundreds of questions by hand while still allowing reproducible session order
 * (same `seed` = same shuffled list).
 *
 * ## Session flow
 *
 * 1. `LessonGameScreen` retrieves (or creates) a seed via [ChemistrySessionStore.getOrCreateSeed].
 * 2. It fetches the set of already-answered question IDs via [ChemistrySessionStore.getAnsweredIds].
 * 3. It calls [generateFor] which shuffles the full pool with the seed, then removes answered IDs.
 * 4. Each correctly answered question is added to the answered set so it won't reappear
 *    in the next session.
 *
 * ## Adding a new chemistry lesson
 *
 * 1. Add the lesson to [SubjectRepository] with an id starting with `"chemia_"`.
 * 2. Implement a private `fun chemia_X_Y(seed: Long): List<Question>` here.
 * 3. Register it in the `when` block inside [generateFor].
 */
object ChemistryQuestionGenerator {

    /**
     * Generates the question list for a chemistry lesson, filtered by [excludeIds].
     *
     * The full pool is produced by the lesson-specific generator function, shuffled
     * deterministically using `seed`, then filtered to remove already-answered questions.
     *
     * @param lessonId   Chemistry lesson identifier (must start with `"chemia_"`).
     * @param seed       Random seed for shuffling - stable within a session via [ChemistrySessionStore].
     * @param excludeIds Question IDs answered in previous sessions; these are removed from the result.
     * @return Filtered, ordered list of questions ready for presentation.
     */
    fun generateFor(lessonId: String, seed: Long, excludeIds: Set<Int> = emptySet()): List<Question> {
        val all = when (lessonId) {
            "chemia_1_1" -> chemia_1_1(seed)
            "chemia_1_2" -> chemia_1_2(seed)
            "chemia_1_3" -> chemia_1_3(seed)
            "chemia_1_4" -> chemia_1_4(seed)
            "chemia_2_1" -> chemia_2_1(seed)
            "chemia_2_2" -> chemia_2_2(seed)
            "chemia_3_3" -> chemia_3_3(seed)
            "chemia_3_4" -> chemia_3_4(seed)
            "chemia_4_1" -> chemia_4_1(seed)
            "chemia_4_2" -> chemia_4_2(seed)
            "chemia_5_1" -> chemia_5_1(seed)
            "chemia_5_2" -> chemia_5_2(seed)
            "chemia_6_1" -> chemia_6_1(seed)
            else -> emptyList()
        }
        return if (excludeIds.isEmpty()) all else all.filter { it.id !in excludeIds }
    }

    /**
     * Returns the total number of questions available for [lessonId] before any filtering.
     * Useful for capping the configurable question count in [SubjectConfigStore].
     *
     * @param lessonId Chemistry lesson identifier.
     */
    fun totalFor(lessonId: String): Int = generateFor(lessonId, seed = 0L).size

    /**
     * Generates questions for Lesson 1-1: "Budowa atomu" (Atomic Structure).
     *
     * Prompts the student to identify the correct electron shell configuration (2, 8, 18...)
     * of chemical elements up to atomic number 54 (Xenon).
     *
     * @param seed Random seed for shuffling elements and generating stable options.
     * @return A list of [PeriodicTableByShell] question objects.
     */
    private fun chemia_1_1(seed: Long): List<Question> {
        val rng = Random(seed)
        return shellConfigByNumber.entries
            .filter { it.key in 1..54 }
            .shuffled(rng)
            .mapIndexed { i, (z, shell) ->
                val el = elementByNumber[z]!!
                PeriodicTableByShell(
                    id = 1100 + i,
                    shellConfig = shell,
                    targetAtomicNumber = z,
                    hint = Hint(
                        mainText = "${el.name} (${el.symbol}) ma $z elektronów: $shell.",
                        boldPart = el.symbol,
                        steps = shellSteps(z, shell)
                    )
                )
            }
    }

    /**
     * Generates questions for Lesson 1-2: "Wskazywanie atomów w układzie okresowym".
     *
     * Taps into the interactive periodic table view, asking students to tap on the correct
     * location of a given element (up to Radon, excluding lanthanides/actinides).
     *
     * @param seed Random seed for element sequence randomization.
     * @return A list of [PeriodicTableQuiz] question objects.
     */
    private fun chemia_1_2(seed: Long): List<Question> {
        val rng = Random(seed)
        return ELEMENTS
            .filter { it.atomicNumber <= 86 && it.category !in listOf(LANTHANIDE, ACTINIDE) }
            .shuffled(rng)
            .mapIndexed { i, el ->
                PeriodicTableByName(
                    id = 1200 + i,
                    elementName = el.name,
                    targetAtomicNumber = el.atomicNumber,
                    hint = Hint(
                        mainText = "${el.name} (${el.symbol}) — gr. ${el.tableCol}, okres ${el.tableRow}.",
                        boldPart = el.symbol
                    )
                )
            }
    }

    /**
     * Generates questions for Lesson 1-3: "Cząsteczki i wzory" (Molecules and Formulas).
     *
     * Asks students to match a chemical molecule name to its molecular formula or vice versa.
     *
     * @param seed Random seed for shuffling questions and distractors.
     * @return A list of formula-matching [SelectFromList] question objects.
     */
    private fun chemia_1_3(seed: Long): List<Question> {
        val rng = Random(seed)
        return moleculeQuestions(rng)
    }

    /**
     * Generates questions for Lesson 1-4: "Elektrony w atomach" (Electrons in Atoms).
     *
     * Prompts students to find the counts of electrons, protons, atomic mass, or period number
     * for chemical elements up to Krypton (Z <= 36).
     *
     * @param seed Random seed for question selection and option generation.
     * @return A shuffled list of [ElementCardQuiz] question objects.
     */
    private fun chemia_1_4(seed: Long): List<Question> {
        val rng = Random(seed)
        return ELEMENTS
            .filter { it.atomicNumber in 1..36 }
            .flatMap { el ->
                listOf(electronQ(el, rng), protonQ(el, rng), massQ(el, rng), periodQ(el, rng))
            }
            .shuffled(rng)
            .mapIndexed { i, q -> q.copy(id = 1400 + i) }
    }

    /**
     * Generates questions for Lesson 2-1: "Układ okresowy - grupy i okresy".
     *
     * Requires students to correctly place elements belonging to specific sets (e.g. halogens,
     * noble gases, alkali metals) onto the periodic table grid.
     *
     * @param seed Random seed for element selection order.
     * @return A list of [PeriodicTableQuiz] question objects.
     */
    private fun chemia_2_1(seed: Long): List<Question> {
        val rng = Random(seed)
        return periodicTableSets().shuffled(rng).mapIndexed { i, (title, nums, hint) ->
            PeriodicTableQuiz(id = 2100 + i, questionText = title, missingAtomicNumbers = nums, hint = hint)
        }
    }

    /**
     * Generates questions for Lesson 2-2: "Właściwości pierwiastków" (Properties of Elements).
     *
     * Quizzes students on element groups, metallic/non-metallic nature, state of matter,
     * and physical/chemical properties.
     *
     * @param seed Random seed for shufflers.
     * @return A list of [SelectFromList] question objects.
     */
    private fun chemia_2_2(seed: Long): List<Question> {
        val rng = Random(seed)
        return propertyQuestions(rng)
    }


    private val phRealWorld = listOf(
        Triple("Sok cytrynowy (pH ≈ 2)", 2, "kwasowy"),
        Triple("Ocet (pH ≈ 3)", 3, "kwasowy"),
        Triple("Kawa (pH ≈ 5)", 5, "kwasowy"),
        Triple("Mleko (pH ≈ 6)", 6, "kwasowy"),
        Triple("Woda destylowana (pH = 7)", 7, "obojętny"),
        Triple("Krew ludzka (pH ≈ 7)", 7, "obojętny"),
        Triple("Woda morska (pH ≈ 8)", 8, "zasadowy"),
        Triple("Soda oczyszczona w wodzie (pH ≈ 9)", 9, "zasadowy"),
        Triple("Mydło (pH ≈ 10)", 10, "zasadowy"),
        Triple("Mleko wapienne (pH ≈ 12)", 12, "zasadowy"),
    )

    /**
     * Generates questions for Lesson 4-1: "Skala pH i wskaźniki" (pH Scale and Indicators).
     *
     * Quizzes students on pH values (0-14), classification of solutions as acidic, neutral,
     * or basic, and real-world examples (like coffee, milk, vinegar).
     *
     * @param seed Random seed for shuffling.
     * @return A list of pH classification [SelectFromList] question objects.
     */
    private fun chemia_4_1(seed: Long): List<Question> {
        val rng = Random(seed)
        val qs = mutableListOf<SelectFromList>()
        val phSteps = listOf("pH < 7 → odczyn kwasowy", "pH = 7 → odczyn obojętny", "pH > 7 → odczyn zasadowy")

        // 15 questions: classify each pH 0–14
        (0..14).forEach { ph ->
            val type = when {
                ph < 7  -> "kwasowy"
                ph == 7 -> "obojętny"
                else    -> "zasadowy"
            }
            val opts = listOf("kwasowy", "obojętny", "zasadowy")
            qs += SelectFromList(
                id = 0,
                prompt = "Roztwór o pH = $ph ma odczyn:",
                options = opts,
                correctIndices = setOf(opts.indexOf(type)),
                hint = Hint(
                    mainText = when {
                        ph < 7  -> "pH $ph jest mniejsze od 7 — odczyn kwasowy."
                        ph == 7 -> "pH = 7 — odczyn obojętny."
                        else    -> "pH $ph jest większe od 7 — odczyn zasadowy."
                    },
                    boldPart = type,
                    steps = phSteps
                )
            )
        }

        // 10 real-world examples
        phRealWorld.forEach { (desc, _, type) ->
            val opts = listOf("kwasowy", "obojętny", "zasadowy")
            qs += SelectFromList(
                id = 0,
                prompt = "$desc ma odczyn:",
                options = opts,
                correctIndices = setOf(opts.indexOf(type)),
                hint = Hint(mainText = "$desc → $type.", boldPart = type, steps = phSteps)
            )
        }

        // 6 "pick acidic pH" questions - each with unique set of options
        val acidPHs = (1..6).toList().shuffled(rng)
        val basePHs = (8..13).toList()
        acidPHs.forEach { ph ->
            val wrongs = (listOf(7) + basePHs.shuffled(rng).take(2)).map { it.toString() }
            val opts = (wrongs + ph.toString()).shuffled(rng)
            qs += SelectFromList(
                id = 0,
                prompt = "Które z poniższych pH odpowiada odczynowi kwasowemu?",
                options = opts,
                correctIndices = setOf(opts.indexOf(ph.toString())),
                hint = Hint("Odczyn kwasowy → pH < 7.", boldPart = "pH < 7", steps = phSteps)
            )
        }

        // 6 "pick basic pH" questions
        basePHs.shuffled(rng).take(6).forEach { ph ->
            val wrongs = (listOf(7) + acidPHs.take(2)).map { it.toString() }
            val opts = (wrongs + ph.toString()).shuffled(rng)
            qs += SelectFromList(
                id = 0,
                prompt = "Które z poniższych pH odpowiada odczynowi zasadowemu?",
                options = opts,
                correctIndices = setOf(opts.indexOf(ph.toString())),
                hint = Hint("Odczyn zasadowy → pH > 7.", boldPart = "pH > 7", steps = phSteps)
            )
        }

        return qs.shuffled(rng).mapIndexed { i, q -> q.copy(id = 4100 + i) }
    }

    /**
     * Lookup record for a single electrolyte used in chemia_4_2 dissociation question generation.
     *
     * @property formula Chemical formula (e.g. `"HCl"`).
     * @property name  Polish name (e.g. `"kwas solny"`).
     * @property ions    Ion pair produced on dissociation (e.g. `"H⁺ i Cl⁻"`).
     * @property type    Compound class: `"kwas"`, `"zasada"`, or `"sól"`.
     */
    private data class DissocEntry(
        val formula: String,
        val name: String,
        val ions: String,
        val type: String
    )

    private val dissocData = listOf(
        DissocEntry("HCl",       "kwas solny",               "H⁺ i Cl⁻",        "kwas"),
        DissocEntry("HBr",       "kwas bromowodorowy",       "H⁺ i Br⁻",        "kwas"),
        DissocEntry("HI",        "kwas jodowodorowy",        "H⁺ i I⁻",         "kwas"),
        DissocEntry("HNO₃",     "kwas azotowy(V)",          "H⁺ i NO₃⁻",       "kwas"),
        DissocEntry("H₂SO₄",    "kwas siarkowy(VI)",        "2H⁺ i SO₄²⁻",     "kwas"),
        DissocEntry("HClO₄",    "kwas chlorowy(VII)",       "H⁺ i ClO₄⁻",      "kwas"),
        DissocEntry("NaOH",      "wodorotlenek sodu",        "Na⁺ i OH⁻",        "zasada"),
        DissocEntry("KOH",       "wodorotlenek potasu",      "K⁺ i OH⁻",         "zasada"),
        DissocEntry("Ca(OH)₂",  "wodorotlenek wapnia",     "Ca²⁺ i 2OH⁻",      "zasada"),
        DissocEntry("Ba(OH)₂",  "wodorotlenek baru",       "Ba²⁺ i 2OH⁻",      "zasada"),
        DissocEntry("NaCl",      "chlorek sodu",             "Na⁺ i Cl⁻",        "sól"),
        DissocEntry("KCl",       "chlorek potasu",           "K⁺ i Cl⁻",         "sól"),
        DissocEntry("CaCl₂",    "chlorek wapnia",           "Ca²⁺ i 2Cl⁻",      "sól"),
        DissocEntry("K₂SO₄",    "siarczan(VI) potasu",     "2K⁺ i SO₄²⁻",      "sól"),
        DissocEntry("Na₂CO₃",   "węglan sodu",              "2Na⁺ i CO₃²⁻",     "sól"),
    )

    /**
     * Generates questions for Lesson 4-2: "Dysocjacja elektrolityczna" (Electrolytic Dissociation).
     *
     * Evaluates knowledge of dissociation formulas, resulting cations and anions, and the
     * classification of electrolytes (acids, bases, salts).
     *
     * @param seed Random seed for options shuffling.
     * @return A list of dissociation [SelectFromList] question objects.
     */
    private fun chemia_4_2(seed: Long): List<Question> {
        val rng = Random(seed)
        val qs = mutableListOf<SelectFromList>()
        val allIons     = dissocData.map { it.ions }
        val allFormulas = dissocData.map { it.formula }
        val typeOpts    = listOf("kwas", "zasada", "sól")
        val typeHints   = mapOf(
            "kwas"   to "Kwasy dysocjują oddając jon H⁺.",
            "zasada" to "Zasady dysocjują oddając jon OH⁻.",
            "sól"    to "Sole to produkty reakcji kwasu z zasadą — dysocjują na kationy metalu i aniony reszty kwasowej."
        )

        dissocData.forEach { entry ->
            val wrongIons = allIons.filter { it != entry.ions }.shuffled(rng).take(3)
            val opts1 = (wrongIons + entry.ions).shuffled(rng)
            qs += SelectFromList(
                id = 0,
                prompt = "Podczas dysocjacji ${entry.formula} (${entry.name}) powstają jony:",
                options = opts1,
                correctIndices = setOf(opts1.indexOf(entry.ions)),
                hint = Hint("${entry.formula} → ${entry.ions}.", boldPart = entry.ions)
            )

            val wrongForms = allFormulas.filter { it != entry.formula }.shuffled(rng).take(3)
            val opts2 = (wrongForms + entry.formula).shuffled(rng)
            qs += SelectFromList(
                id = 0,
                prompt = "Który związek chemiczny dysocjuje dając jony ${entry.ions}?",
                options = opts2,
                correctIndices = setOf(opts2.indexOf(entry.formula)),
                hint = Hint("${entry.ions} to jony z ${entry.formula}.", boldPart = entry.formula)
            )

            qs += SelectFromList(
                id = 0,
                prompt = "${entry.formula} (${entry.name}) jest:",
                options = typeOpts,
                correctIndices = setOf(typeOpts.indexOf(entry.type)),
                hint = Hint(typeHints[entry.type]!!, boldPart = entry.type)
            )
        }

        return qs.shuffled(rng).mapIndexed { i, q -> q.copy(id = 4200 + i) }
    }

    /**
     * Lookup record for a single hydrocarbon used in chemia_5_1 question generation.
     *
     * @property formula Chemical formula (e.g. `"CH₄"`).
     * @property name  Polish name (e.g. `"metan"`).
     * @property cCount  Number of carbon atoms in the molecule.
     * @property type    Homologous series: `"alkan"`, `"alken"`, or `"alkyn"`.
     */
    private data class Hydrocarbon(
        val formula: String,
        val name: String,
        val cCount: Int,
        val type: String
    )

    private val hydrocarbons = listOf(
        Hydrocarbon("CH₄",    "metan",   1, "alkan"),
        Hydrocarbon("C₂H₆",  "etan",    2, "alkan"),
        Hydrocarbon("C₃H₈",  "propan",  3, "alkan"),
        Hydrocarbon("C₄H₁₀", "butan",   4, "alkan"),
        Hydrocarbon("C₅H₁₂", "pentan",  5, "alkan"),
        Hydrocarbon("C₆H₁₄", "heksan",  6, "alkan"),
        Hydrocarbon("C₂H₄",  "eten",    2, "alken"),
        Hydrocarbon("C₃H₆",  "propen",  3, "alken"),
        Hydrocarbon("C₄H₈",  "buten",   4, "alken"),
        Hydrocarbon("C₅H₁₀", "penten",  5, "alken"),
        Hydrocarbon("C₂H₂",  "etyn",    2, "alkyn"),
        Hydrocarbon("C₃H₄",  "propyn",  3, "alkyn"),
        Hydrocarbon("C₄H₆",  "butyn",   4, "alkyn"),
    )

    private val hcTypeHints = mapOf(
        "alkan" to "Alkany — CₙH₂ₙ₊₂. Tylko wiązania pojedyncze C−C.",
        "alken" to "Alkeny — CₙH₂ₙ. Zawierają jedno wiązanie podwójne C=C.",
        "alkyn" to "Alkiny — CₙH₂ₙ₋₂. Zawierają jedno wiązanie potrójne C≡C."
    )

    /**
     * Generates questions for Lesson 5-1: "Węglowodory" (Hydrocarbons).
     *
     * Quizzes students on names, formulas, and classifications of alkanes, alkenes, and alkynes.
     *
     * @param seed Random seed for options shuffling.
     * @return A list of hydrocarbon [SelectFromList] question objects.
     */
    private fun chemia_5_1(seed: Long): List<Question> {
        val rng = Random(seed)
        val qs = mutableListOf<SelectFromList>()
        val allNames    = hydrocarbons.map { it.name }
        val allFormulas = hydrocarbons.map { it.formula }
        val typeOpts    = listOf("alkan", "alken", "alkyn")

        hydrocarbons.forEach { hc ->
            val wNames = allNames.filter { it != hc.name }.shuffled(rng).take(3)
            val opts1 = (wNames + hc.name).shuffled(rng)
            qs += SelectFromList(
                id = 0,
                prompt = "Jak nazywa się związek o wzorze ${hc.formula}?",
                options = opts1,
                correctIndices = setOf(opts1.indexOf(hc.name)),
                hint = Hint("${hc.formula} to ${hc.name}.", boldPart = hc.name)
            )

            val wForms = allFormulas.filter { it != hc.formula }.shuffled(rng).take(3)
            val opts2 = (wForms + hc.formula).shuffled(rng)
            qs += SelectFromList(
                id = 0,
                prompt = "Jaki jest wzór sumaryczny ${hc.name}?",
                options = opts2,
                correctIndices = setOf(opts2.indexOf(hc.formula)),
                hint = Hint("Wzór ${hc.name} to ${hc.formula}.", boldPart = hc.formula)
            )

            qs += SelectFromList(
                id = 0,
                prompt = "${hc.name} (${hc.formula}) należy do szeregu:",
                options = typeOpts,
                correctIndices = setOf(typeOpts.indexOf(hc.type)),
                hint = Hint(hcTypeHints[hc.type]!!, boldPart = hc.type)
            )

            val wC = (1..8).filter { it != hc.cCount }.shuffled(rng).take(3).map { it.toString() }
            val opts4 = (wC + hc.cCount.toString()).shuffled(rng)
            qs += SelectFromList(
                id = 0,
                prompt = "Ile atomów węgla zawiera ${hc.name} (${hc.formula})?",
                options = opts4,
                correctIndices = setOf(opts4.indexOf(hc.cCount.toString())),
                hint = Hint("${hc.formula}: ${hc.cCount} atomy C.", boldPart = "${hc.cCount}")
            )
        }

        return qs.shuffled(rng).mapIndexed { i, q -> q.copy(id = 5100 + i) }
    }

    /**
     * Lookup record for a single organic compound used in chemia_5_2 question generation.
     *
     * @property formula    Chemical formula (e.g. `"CH₃OH"`).
     * @property name     Polish name (e.g. `"metanol"`).
     * @property group      Functional group notation: `"-OH"`, `"-COOH"`, `"-NH₂"`, or `"ester"`.
     * @property groupName  Compound class name: `"alkohol"`, `"kwas karboksylowy"`, `"amina"`, or `"ester"`.
     */
    private data class OrgCompound(
        val formula: String,
        val name: String,
        val group: String,
        val groupName: String
    )

    private val orgCompounds = listOf(
        OrgCompound("CH₃OH",         "metanol",           "-OH",   "alkohol"),
        OrgCompound("C₂H₅OH",        "etanol",            "-OH",   "alkohol"),
        OrgCompound("C₃H₇OH",        "propanol",          "-OH",   "alkohol"),
        OrgCompound("C₄H₉OH",        "butanol",           "-OH",   "alkohol"),
        OrgCompound("HCOOH",          "kwas metanowy",     "-COOH", "kwas karboksylowy"),
        OrgCompound("CH₃COOH",       "kwas etanowy",      "-COOH", "kwas karboksylowy"),
        OrgCompound("C₂H₅COOH",     "kwas propanowy",    "-COOH", "kwas karboksylowy"),
        OrgCompound("C₃H₇COOH",     "kwas butanowy",     "-COOH", "kwas karboksylowy"),
        OrgCompound("CH₃NH₂",        "metyloamina",       "-NH₂",  "amina"),
        OrgCompound("C₂H₅NH₂",      "etyloamina",        "-NH₂",  "amina"),
        OrgCompound("HCOOCH₃",       "mrówczan metylu",   "ester", "ester"),
        OrgCompound("CH₃COOC₂H₅",  "octan etylu",       "ester", "ester"),
    )

    private val orgGroupHints = mapOf(
        "alkohol"           to "Alkohole zawierają grupę hydroksylową -OH.",
        "kwas karboksylowy" to "Kwasy karboksylowe zawierają grupę karboksylową -COOH.",
        "amina"             to "Aminy zawierają grupę aminową -NH₂.",
        "ester"             to "Estry powstają z reakcji kwasu karboksylowego z alkoholem."
    )

    /**
     * Generates questions for Lesson 5-2: "Pochodne węglowodorów" (Organic Compounds).
     *
     * Prompts students to classify and identify functional groups for organic compounds
     * (alcohols, carboxylic acids, amines, esters).
     *
     * @param seed Random seed for options shuffling.
     * @return A list of organic compound [SelectFromList] question objects.
     */
    private fun chemia_5_2(seed: Long): List<Question> {
        val rng = Random(seed)
        val qs = mutableListOf<SelectFromList>()
        val allNames   = orgCompounds.map { it.name }
        val typeOpts   = listOf("alkohol", "kwas karboksylowy", "amina", "ester")
        val groupOpts  = listOf("-OH", "-COOH", "-NH₂", "ester")

        orgCompounds.forEach { oc ->
            val wNames = allNames.filter { it != oc.name }.shuffled(rng).take(3)
            val opts1 = (wNames + oc.name).shuffled(rng)
            qs += SelectFromList(
                id = 0,
                prompt = "Jak nazywa się związek o wzorze ${oc.formula}?",
                options = opts1,
                correctIndices = setOf(opts1.indexOf(oc.name)),
                hint = Hint("${oc.formula} to ${oc.name}.", boldPart = oc.name)
            )

            qs += SelectFromList(
                id = 0,
                prompt = "${oc.name} (${oc.formula}) należy do grupy:",
                options = typeOpts,
                correctIndices = setOf(typeOpts.indexOf(oc.groupName)),
                hint = Hint(
                    orgGroupHints[oc.groupName]!!,
                    boldPart = oc.groupName,
                    items = listOf(
                        "Alkohole — gr. -OH",
                        "Kwasy karboksylowe — gr. -COOH",
                        "Aminy — gr. -NH₂",
                        "Estry — z kwasu + alkoholu"
                    )
                )
            )

            qs += SelectFromList(
                id = 0,
                prompt = "Jaką grupę funkcyjną zawiera ${oc.name} (${oc.formula})?",
                options = groupOpts,
                correctIndices = setOf(groupOpts.indexOf(oc.group)),
                hint = Hint("Grupa ${oc.group} → ${oc.groupName}.", boldPart = oc.group)
            )
        }

        return qs.shuffled(rng).mapIndexed { i, q -> q.copy(id = 5200 + i) }
    }

    /**
     * Lookup record for a single hydroxide used in chemia_3_3 question generation.
     *
     * @property formula  Chemical formula (e.g. `"NaOH"`).
     * @property name   Polish name (e.g. `"wodorotlenek sodu"`).
     * @property soluble  `true` if the hydroxide dissolves readily in water.
     */
    private data class BaseEntry(val formula: String, val name: String, val soluble: Boolean)

    private val hydroxides = listOf(
        BaseEntry("NaOH",      "wodorotlenek sodu",         true),
        BaseEntry("KOH",       "wodorotlenek potasu",       true),
        BaseEntry("LiOH",      "wodorotlenek litu",         true),
        BaseEntry("Ca(OH)₂",  "wodorotlenek wapnia",       true),
        BaseEntry("Ba(OH)₂",  "wodorotlenek baru",         true),
        BaseEntry("Mg(OH)₂",  "wodorotlenek magnezu",      false),
        BaseEntry("Fe(OH)₂",  "wodorotlenek żelaza(II)",   false),
        BaseEntry("Fe(OH)₃",  "wodorotlenek żelaza(III)",  false),
        BaseEntry("Cu(OH)₂",  "wodorotlenek miedzi(II)",   false),
        BaseEntry("Al(OH)₃",  "wodorotlenek glinu",        false),
        BaseEntry("Zn(OH)₂",  "wodorotlenek cynku",        false),
        BaseEntry("Mn(OH)₂",  "wodorotlenek manganu(II)",  false),
        BaseEntry("Ni(OH)₂",  "wodorotlenek niklu(II)",    false),
    )

    private val hydroxideFormingReactions: List<EquationBalance> = listOf(
        EquationBalance(
            id = 0, instruction = "Uzupełnij reakcję otrzymywania zasady",
            subInstruction = "Tlenek zasadowy + woda → zasada",
            reactants = listOf(BalanceTerm("Na₂O", fixedCoefficient = 1), BalanceTerm("H₂O", fixedCoefficient = null, correctCoefficient = 1)),
            products  = listOf(BalanceTerm("NaOH", fixedCoefficient = null, correctCoefficient = 2)),
            hint = Hint("Na₂O + H₂O → 2NaOH. 2 atomy Na → 2 cząsteczki NaOH.", boldPart = "2NaOH",
                steps = listOf("Na₂O ma 2Na → 2×NaOH", "H₂O daje grupę OH⁻ dla każdego Na"))
        ),
        EquationBalance(
            id = 0, instruction = "Uzupełnij reakcję gaszenia wapna",
            subInstruction = "Wapno palone + woda → wapno gaszone",
            reactants = listOf(BalanceTerm("CaO", fixedCoefficient = 1), BalanceTerm("H₂O", fixedCoefficient = null, correctCoefficient = 1)),
            products  = listOf(BalanceTerm("Ca(OH)₂", fixedCoefficient = null, correctCoefficient = 1)),
            hint = Hint("CaO + H₂O → Ca(OH)₂. Wszystkie współczynniki = 1.", boldPart = "Ca(OH)₂")
        ),
        EquationBalance(
            id = 0, instruction = "Uzupełnij reakcję otrzymywania zasady",
            reactants = listOf(BalanceTerm("K₂O", fixedCoefficient = null, correctCoefficient = 1), BalanceTerm("H₂O", fixedCoefficient = 1)),
            products  = listOf(BalanceTerm("KOH", fixedCoefficient = null, correctCoefficient = 2)),
            hint = Hint("K₂O + H₂O → 2KOH. 2 atomy K → 2 cząsteczki KOH.", boldPart = "2KOH")
        ),
        EquationBalance(
            id = 0, instruction = "Uzupełnij reakcję otrzymywania zasady",
            reactants = listOf(BalanceTerm("BaO", fixedCoefficient = null, correctCoefficient = 1), BalanceTerm("H₂O", fixedCoefficient = 1)),
            products  = listOf(BalanceTerm("Ba(OH)₂", fixedCoefficient = null, correctCoefficient = 1)),
            hint = Hint("BaO + H₂O → Ba(OH)₂. Wszystkie współczynniki = 1.", boldPart = "Ba(OH)₂")
        ),
        EquationBalance(
            id = 0, instruction = "Uzupełnij reakcję otrzymywania zasady",
            reactants = listOf(BalanceTerm("Li₂O", fixedCoefficient = 1), BalanceTerm("H₂O", fixedCoefficient = null, correctCoefficient = 1)),
            products  = listOf(BalanceTerm("LiOH", fixedCoefficient = null, correctCoefficient = 2)),
            hint = Hint("Li₂O + H₂O → 2LiOH. 2 atomy Li → 2 cząsteczki LiOH.", boldPart = "2LiOH")
        ),
    )

    /**
     * Generates questions for Lesson 3-3: "Wodorotlenki i zasady" (Hydroxides and Bases).
     *
     * Prompts students to match hydroxide formulas with names, identify their solubility,
     * and balance base preparation reactions.
     *
     * @param seed Random seed for shuffling.
     * @return A list of base-themed question objects.
     */
    private fun chemia_3_3(seed: Long): List<Question> {
        val rng = Random(seed)
        val qs = mutableListOf<Question>()
        val allFormulas = hydroxides.map { it.formula }
        val allNames    = hydroxides.map { it.name }
        val solubHint   = "Rozpuszczalne zasady (mocne): NaOH, KOH, LiOH, Ca(OH)₂, Ba(OH)₂. Pozostałe są trudno rozpuszczalne."
        val solubItems  = listOf("NaOH, KOH, LiOH — gr. 1", "Ca(OH)₂, Ba(OH)₂ — gr. 2", "Inne wodorotlenki — trudno rozpuszczalne")

        hydroxides.forEach { base ->
            val wNames = allNames.filter { it != base.name }.shuffled(rng).take(3)
            val opts1  = (wNames + base.name).shuffled(rng)
            qs += SelectFromList(id = 0,
                prompt = "Wodorotlenek o wzorze ${base.formula} to:",
                options = opts1, correctIndices = setOf(opts1.indexOf(base.name)),
                hint = Hint("${base.formula} to ${base.name}.", boldPart = base.name))

            val wForms = allFormulas.filter { it != base.formula }.shuffled(rng).take(3)
            val opts2  = (wForms + base.formula).shuffled(rng)
            qs += SelectFromList(id = 0,
                prompt = "Wzór ${base.name} to:",
                options = opts2, correctIndices = setOf(opts2.indexOf(base.formula)),
                hint = Hint("Wzór ${base.name} to ${base.formula}.", boldPart = base.formula))

            val sType = if (base.soluble) "rozpuszczalny" else "trudno rozpuszczalny"
            val sOpts = listOf("rozpuszczalny", "trudno rozpuszczalny")
            qs += SelectFromList(id = 0,
                prompt = "${base.name} (${base.formula}) jest:",
                options = sOpts, correctIndices = setOf(sOpts.indexOf(sType)),
                hint = Hint(solubHint, boldPart = sType, sectionTitle = "Rozpuszczalność zasad", items = solubItems))
        }

        qs.addAll(hydroxideFormingReactions.shuffled(rng))
        return qs.shuffled(rng).mapIndexed { i, q ->
            when (q) {
                is SelectFromList -> q.copy(id = 3300 + i)
                is EquationBalance -> q.copy(id = 3300 + i)
                else -> q
            }
        }
    }

    /**
     * Lookup record for a single salt used in chemia_3_4 question generation.
     *
     * @property formula   Chemical formula (e.g. `"NaCl"`).
     * @property name    Polish name (e.g. `"chlorek sodu"`).
     * @property acidName  Polish name of the parent acid (e.g. `"kwas chlorowodorowy"`).
     */
    private data class SaltEntry(val formula: String, val name: String, val acidName: String)

    private val salts = listOf(
        SaltEntry("NaCl",          "chlorek sodu",              "kwas chlorowodorowy"),
        SaltEntry("KCl",           "chlorek potasu",             "kwas chlorowodorowy"),
        SaltEntry("CaCl₂",         "chlorek wapnia",             "kwas chlorowodorowy"),
        SaltEntry("MgCl₂",         "chlorek magnezu",            "kwas chlorowodorowy"),
        SaltEntry("FeCl₂",         "chlorek żelaza(II)",         "kwas chlorowodorowy"),
        SaltEntry("FeCl₃",         "chlorek żelaza(III)",        "kwas chlorowodorowy"),
        SaltEntry("AlCl₃",         "chlorek glinu",              "kwas chlorowodorowy"),
        SaltEntry("ZnCl₂",         "chlorek cynku",              "kwas chlorowodorowy"),
        SaltEntry("Na₂SO₄",        "siarczan(VI) sodu",          "kwas siarkowy(VI)"),
        SaltEntry("CaSO₄",         "siarczan(VI) wapnia",        "kwas siarkowy(VI)"),
        SaltEntry("MgSO₄",         "siarczan(VI) magnezu",       "kwas siarkowy(VI)"),
        SaltEntry("ZnSO₄",         "siarczan(VI) cynku",         "kwas siarkowy(VI)"),
        SaltEntry("FeSO₄",         "siarczan(VI) żelaza(II)",    "kwas siarkowy(VI)"),
        SaltEntry("BaSO₄",         "siarczan(VI) baru",          "kwas siarkowy(VI)"),
        SaltEntry("K₂SO₄",         "siarczan(VI) potasu",        "kwas siarkowy(VI)"),
        SaltEntry("Na₂CO₃",        "węglan sodu",                "kwas węglowy"),
        SaltEntry("CaCO₃",         "węglan wapnia",              "kwas węglowy"),
        SaltEntry("MgCO₃",         "węglan magnezu",             "kwas węglowy"),
        SaltEntry("NaNO₃",         "azotan(V) sodu",             "kwas azotowy(V)"),
        SaltEntry("Ca(NO₃)₂",      "azotan(V) wapnia",           "kwas azotowy(V)"),
        SaltEntry("KNO₃",          "azotan(V) potasu",           "kwas azotowy(V)"),
        SaltEntry("Na₃PO₄",        "fosforan(V) sodu",           "kwas fosforowy(V)"),
        SaltEntry("Ca₃(PO₄)₂",    "fosforan(V) wapnia",         "kwas fosforowy(V)"),
    )

    /**
     * Generates questions for Lesson 3-4: "Sole" (Salts).
     *
     * Tests students on salt naming, formulas, and identifying the parent acid
     * from which a salt is derived.
     *
     * @param seed Random seed for shuffling.
     * @return A list of salt-themed [SelectFromList] question objects.
     */
    private fun chemia_3_4(seed: Long): List<Question> {
        val rng  = Random(seed)
        val qs   = mutableListOf<SelectFromList>()
        val allFormulas = salts.map { it.formula }
        val allNames    = salts.map { it.name }
        val allAcids    = salts.map { it.acidName }.distinct()
        val saltItems   = listOf(
            "Chlorki — od HCl (kwas chlorowodorowy)",
            "Siarczany(VI) — od H₂SO₄",
            "Azotany(V) — od HNO₃",
            "Węglany — od H₂CO₃",
            "Fosforany(V) — od H₃PO₄"
        )

        salts.forEach { salt ->
            val wNames = allNames.filter { it != salt.name }.shuffled(rng).take(3)
            val opts1  = (wNames + salt.name).shuffled(rng)
            qs += SelectFromList(id = 0,
                prompt = "Sól o wzorze ${salt.formula} to:",
                options = opts1, correctIndices = setOf(opts1.indexOf(salt.name)),
                hint = Hint("${salt.formula} to ${salt.name}.", boldPart = salt.name,
                    sectionTitle = "Nazewnictwo soli", items = saltItems))

            val wForms = allFormulas.filter { it != salt.formula }.shuffled(rng).take(3)
            val opts2  = (wForms + salt.formula).shuffled(rng)
            qs += SelectFromList(id = 0,
                prompt = "Wzór ${salt.name} to:",
                options = opts2, correctIndices = setOf(opts2.indexOf(salt.formula)),
                hint = Hint("Wzór ${salt.name} to ${salt.formula}.", boldPart = salt.formula))

            val wAcids = allAcids.filter { it != salt.acidName }.shuffled(rng).take(3)
            val opts3  = (wAcids + salt.acidName).shuffled(rng)
            qs += SelectFromList(id = 0,
                prompt = "${salt.name} (${salt.formula}) pochodzi od:",
                options = opts3, correctIndices = setOf(opts3.indexOf(salt.acidName)),
                hint = Hint("${salt.formula} pochodzi od ${salt.acidName}.", boldPart = salt.acidName))
        }

        return qs.shuffled(rng).mapIndexed { i, q -> q.copy(id = 3400 + i) }
    }

    /**
     * Lookup record for a single oxide used in chemia_6_1 question generation.
     *
     * @property formula Chemical formula (e.g. `"Na₂O"`).
     * @property name  Polish name (e.g. `"tlenek sodu"`).
     * @property type    Oxide class: `"zasadowy"` (basic), `"kwasowy"` (acidic), or `"amfoteryczny"` (amphoteric).
     */
    private data class OxideEntry(val formula: String, val name: String, val type: String)

    private val oxides = listOf(
        OxideEntry("Na₂O",   "tlenek sodu",           "zasadowy"),
        OxideEntry("K₂O",    "tlenek potasu",          "zasadowy"),
        OxideEntry("CaO",    "tlenek wapnia",          "zasadowy"),
        OxideEntry("MgO",    "tlenek magnezu",         "zasadowy"),
        OxideEntry("BaO",    "tlenek baru",            "zasadowy"),
        OxideEntry("FeO",    "tlenek żelaza(II)",      "zasadowy"),
        OxideEntry("Fe₂O₃",  "tlenek żelaza(III)",     "zasadowy"),
        OxideEntry("CuO",    "tlenek miedzi(II)",      "zasadowy"),
        OxideEntry("Li₂O",   "tlenek litu",            "zasadowy"),
        OxideEntry("CO₂",    "dwutlenek węgla",        "kwasowy"),
        OxideEntry("SO₂",    "dwutlenek siarki",       "kwasowy"),
        OxideEntry("SO₃",    "trójtlenek siarki",      "kwasowy"),
        OxideEntry("N₂O₅",   "tlenek azotu(V)",        "kwasowy"),
        OxideEntry("P₂O₅",   "tlenek fosforu(V)",      "kwasowy"),
        OxideEntry("SiO₂",   "dwutlenek krzemu",       "kwasowy"),
        OxideEntry("CO",     "tlenek węgla(II)",       "obojętny"),
        OxideEntry("NO",     "tlenek azotu(II)",       "obojętny"),
        OxideEntry("ZnO",    "tlenek cynku",           "amfoteryczny"),
        OxideEntry("Al₂O₃",  "tlenek glinu",           "amfoteryczny"),
    )

    private val oxideFormingReactions: List<EquationBalance> = listOf(
        EquationBalance(
            id = 0, instruction = "Zbilansuj reakcję tworzenia tlenku",
            reactants = listOf(BalanceTerm("Mg", fixedCoefficient = null, correctCoefficient = 2), BalanceTerm("O₂", fixedCoefficient = 1)),
            products  = listOf(BalanceTerm("MgO", fixedCoefficient = null, correctCoefficient = 2)),
            hint = Hint("2Mg + O₂ → 2MgO. O₂ = 2 atomy O → 2×MgO.", boldPart = "2MgO",
                steps = listOf("O₂ dostarcza 2O", "2O → 2×MgO", "2×MgO wymaga 2×Mg"))
        ),
        EquationBalance(
            id = 0, instruction = "Zbilansuj reakcję tworzenia tlenku",
            reactants = listOf(BalanceTerm("Ca", fixedCoefficient = null, correctCoefficient = 2), BalanceTerm("O₂", fixedCoefficient = 1)),
            products  = listOf(BalanceTerm("CaO", fixedCoefficient = null, correctCoefficient = 2)),
            hint = Hint("2Ca + O₂ → 2CaO.", boldPart = "2CaO",
                steps = listOf("O₂ = 2O → 2×CaO → 2×Ca"))
        ),
        EquationBalance(
            id = 0, instruction = "Zbilansuj reakcję tworzenia tlenku",
            reactants = listOf(BalanceTerm("Na", fixedCoefficient = null, correctCoefficient = 4), BalanceTerm("O₂", fixedCoefficient = 1)),
            products  = listOf(BalanceTerm("Na₂O", fixedCoefficient = null, correctCoefficient = 2)),
            hint = Hint("4Na + O₂ → 2Na₂O.", boldPart = "2Na₂O",
                steps = listOf("O₂ = 2O → 2×Na₂O", "2×Na₂O ma 4Na → 4×Na"))
        ),
        EquationBalance(
            id = 0, instruction = "Zbilansuj reakcję tworzenia tlenku kwasowego",
            reactants = listOf(BalanceTerm("C", fixedCoefficient = null, correctCoefficient = 1), BalanceTerm("O₂", fixedCoefficient = 1)),
            products  = listOf(BalanceTerm("CO₂", fixedCoefficient = null, correctCoefficient = 1)),
            hint = Hint("C + O₂ → CO₂. Tlenek kwasowy — reaguje z zasadami i z wodą.", boldPart = "CO₂")
        ),
        EquationBalance(
            id = 0, instruction = "Zbilansuj reakcję tworzenia tlenku kwasowego",
            reactants = listOf(BalanceTerm("S", fixedCoefficient = 1), BalanceTerm("O₂", fixedCoefficient = null, correctCoefficient = 1)),
            products  = listOf(BalanceTerm("SO₂", fixedCoefficient = null, correctCoefficient = 1)),
            hint = Hint("S + O₂ → SO₂. Dwutlenek siarki — przyczyna kwaśnych deszczy.", boldPart = "SO₂")
        ),
        EquationBalance(
            id = 0, instruction = "Uzupełnij reakcję tlenku kwasowego z wodą",
            reactants = listOf(BalanceTerm("SO₃", fixedCoefficient = null, correctCoefficient = 1), BalanceTerm("H₂O", fixedCoefficient = 1)),
            products  = listOf(BalanceTerm("H₂SO₄", fixedCoefficient = null, correctCoefficient = 1)),
            hint = Hint("SO₃ + H₂O → H₂SO₄. Tlenek kwasowy + woda → kwas.", boldPart = "H₂SO₄")
        ),
        EquationBalance(
            id = 0, instruction = "Uzupełnij reakcję tlenku zasadowego z wodą",
            subInstruction = "Wapno palone + woda → wapno gaszone",
            reactants = listOf(BalanceTerm("CaO", fixedCoefficient = 1), BalanceTerm("H₂O", fixedCoefficient = null, correctCoefficient = 1)),
            products  = listOf(BalanceTerm("Ca(OH)₂", fixedCoefficient = null, correctCoefficient = 1)),
            hint = Hint("CaO + H₂O → Ca(OH)₂. Tlenek zasadowy + woda → zasada.", boldPart = "Ca(OH)₂")
        ),
        EquationBalance(
            id = 0, instruction = "Uzupełnij reakcję tlenku kwasowego z wodą",
            reactants = listOf(BalanceTerm("CO₂", fixedCoefficient = null, correctCoefficient = 1), BalanceTerm("H₂O", fixedCoefficient = 1)),
            products  = listOf(BalanceTerm("H₂CO₃", fixedCoefficient = null, correctCoefficient = 1)),
            hint = Hint("CO₂ + H₂O → H₂CO₃. Tak powstaje kwas węglowy w napojach gazowanych.", boldPart = "H₂CO₃")
        ),
    )

    /**
     * Generates questions for Lesson 6-1: "Tlenki" (Oxides).
     *
     * Quizzes students on naming, chemical formulas, and the classification of oxides into
     * acidic, basic, amphoteric, or neutral categories.
     *
     * @param seed Random seed for shuffling.
     * @return A list of oxide-themed question objects.
     */
    private fun chemia_6_1(seed: Long): List<Question> {
        val rng = Random(seed)
        val qs  = mutableListOf<Question>()
        val allFormulas = oxides.map { it.formula }
        val allNames    = oxides.map { it.name }
        val typeOpts    = listOf("zasadowy", "kwasowy", "amfoteryczny", "obojętny")
        val typeHint    = "Tlenki zasadowe = tlenki metali; tlenki kwasowe = tlenki niemetali. Amfoteryczne reagują zarówno z kwasami jak i zasadami."
        val typeItems   = listOf(
            "zasadowy: tlenek metalu → reaguje z kwasami (→ sól + H₂O)",
            "kwasowy: tlenek niemetalu → reaguje z zasadami (→ sól + H₂O) i z H₂O (→ kwas)",
            "amfoteryczny: reaguje z kwasami i z zasadami (ZnO, Al₂O₃)",
            "obojętny: nie reaguje z kwasami ani zasadami (CO, NO)"
        )

        oxides.forEach { oxide ->
            val wNames = allNames.filter { it != oxide.name }.shuffled(rng).take(3)
            val opts1  = (wNames + oxide.name).shuffled(rng)
            qs += SelectFromList(id = 0,
                prompt = "Tlenek o wzorze ${oxide.formula} to:",
                options = opts1, correctIndices = setOf(opts1.indexOf(oxide.name)),
                hint = Hint("${oxide.formula} to ${oxide.name}.", boldPart = oxide.name))

            val wForms = allFormulas.filter { it != oxide.formula }.shuffled(rng).take(3)
            val opts2  = (wForms + oxide.formula).shuffled(rng)
            qs += SelectFromList(id = 0,
                prompt = "Wzór ${oxide.name} to:",
                options = opts2, correctIndices = setOf(opts2.indexOf(oxide.formula)),
                hint = Hint("Wzór ${oxide.name} to ${oxide.formula}.", boldPart = oxide.formula))

            qs += SelectFromList(id = 0,
                prompt = "${oxide.name} (${oxide.formula}) jest tlenkiem:",
                options = typeOpts, correctIndices = setOf(typeOpts.indexOf(oxide.type)),
                hint = Hint(typeHint, boldPart = oxide.type, sectionTitle = "Typy tlenków", items = typeItems))
        }

        qs.addAll(oxideFormingReactions.shuffled(rng))
        return qs.shuffled(rng).mapIndexed { i, q ->
            when (q) {
                is SelectFromList  -> q.copy(id = 6100 + i)
                is EquationBalance -> q.copy(id = 6100 + i)
                else -> q
            }
        }
    }

    /**
     * Generates a question prompting the student to identify the number of electrons for an element.
     *
     * @param el The [Element] to query.
     * @param rng Random instance used to shuffle options and select distractors.
     * @return An [ElementCardQuiz] question.
     */
    private fun electronQ(el: Element, rng: Random): ElementCardQuiz {
        val z = el.atomicNumber
        val opts = buildOptions(z.toString(), distractors(z, 1, 120, rng), rng)
        return ElementCardQuiz(
            id = 0,
            prompt = "Ile elektronów posiada atom ${el.name}?",
            atomicNumber = z,
            options = opts,
            correctIndex = opts.indexOf(z.toString()),
            hint = Hint(
                "Elektrony = liczba atomowa Z = $z.",
                steps = listOf("Z = $z", "Elektronów = $z")
            )
        )
    }

    /**
     * Generates a question prompting the student to identify the number of protons for an element.
     *
     * @param el The [Element] to query.
     * @param rng Random instance used to shuffle options and select distractors.
     * @return An [ElementCardQuiz] question.
     */
    private fun protonQ(el: Element, rng: Random): ElementCardQuiz {
        val z = el.atomicNumber
        val opts = buildOptions(z.toString(), distractors(z, 1, 120, rng), rng)
        return ElementCardQuiz(
            id = 0,
            prompt = "Ile protonów posiada atom ${el.name}?",
            atomicNumber = z,
            options = opts,
            correctIndex = opts.indexOf(z.toString()),
            hint = Hint(
                "Protonów = liczba atomowa Z = $z.",
                steps = listOf("Z = $z", "Protonów = $z")
            )
        )
    }

    /**
     * Generates a question prompting the student to identify the rounded atomic mass of an element.
     *
     * @param el The [Element] to query.
     * @param rng Random instance used to shuffle options and select distractors.
     * @return An [ElementCardQuiz] question.
     */
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

    /**
     * Generates a question prompting the student to identify the period number of an element.
     *
     * @param el The [Element] to query.
     * @param rng Random instance used to shuffle options and select distractors.
     * @return An [ElementCardQuiz] question.
     */
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
                "${el.name} leży w $p. okresie układu.",
                steps = listOf("Policz wiersz od góry tabeli — to numer okresu")
            )
        )
    }

    /**
     * Generates a list of random incorrect numerical options (distractors) near the correct answer.
     *
     * @param correct The correct numerical answer.
     * @param min The minimum allowed value for a distractor.
     * @param max The maximum allowed value for a distractor.
     * @param rng Random instance.
     * @return A list of three distinct incorrect option strings.
     */
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

    /**
     * Combines the correct answer and a list of wrong answers into a single shuffled list of options.
     *
     * @param correct The correct answer string.
     * @param wrongs The list of incorrect answer strings.
     * @param rng Random instance.
     * @return A shuffled list of four option strings.
     */
    private fun buildOptions(correct: String, wrongs: List<String>, rng: Random): List<String> =
        (wrongs.take(3) + correct).shuffled(rng)

    /**
     * Returns a collection of element groups/sets (e.g. noble gases, alkali metals) used
     * to populate the interactive periodic table quiz in lesson 2-1.
     *
     * @return A list of Triples containing the prompt text, target atomic numbers, and the lesson hint.
     */
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

    /**
     * Generates a list of questions mapping molecule names to formulas and vice versa.
     *
     * @param rng Random instance.
     * @return Shuffled list of name-to-formula and formula-to-name questions.
     */
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

    private val metalCats = setOf(ALKALI_METAL, ALKALINE_EARTH, TRANSITION_METAL, POST_TRANSITION)
    private val nonMetalCats = setOf(REACTIVE_NONMETAL, HALOGEN)

    /**
     * Generates chemical and physical property classification questions for elements.
     *
     * @param rng Random instance.
     * @return Shuffled list of property matching questions.
     */
    private fun propertyQuestions(rng: Random): List<Question> {
        val qs = mutableListOf<SelectFromList>()

        // One question per element: "Do jakiej grupy zaliczamy X?"
        ELEMENTS.filter { it.atomicNumber in 1..54 }.forEach { el ->
            val type = when (el.category) {
                NOBLE_GAS        -> "gaz szlachetny"
                in metalCats     -> "metal"
                in nonMetalCats  -> "niemetal"
                METALLOID        -> "metaloid"
                else -> return@forEach
            }
            val opts = listOf("metal", "niemetal", "metaloid", "gaz szlachetny")
            qs += SelectFromList(
                id = 0,
                prompt = "Do jakiej grupy zaliczamy ${el.name} (${el.symbol})?",
                options = opts,
                correctIndices = setOf(opts.indexOf(type)),
                hint = Hint("${el.name} to $type.", boldPart = type)
            )
        }

        // "Which element is a metal/non-metal/noble-gas?" - each correct element used at most once
        listOf(
            Triple("metalem",          metalCats,    nonMetalCats + setOf(METALLOID, NOBLE_GAS)),
            Triple("niemetalem",        nonMetalCats, metalCats    + setOf(METALLOID, NOBLE_GAS)),
            Triple("gazem szlachetnym", setOf(NOBLE_GAS),  metalCats + nonMetalCats + setOf(METALLOID))
        ).forEach { (label, correctCats, wrongCats) ->
            val correctPool = ELEMENTS.filter { it.category in correctCats && it.atomicNumber in 1..54 }
            val wrongPool   = ELEMENTS.filter { it.category in wrongCats   && it.atomicNumber in 1..54 }
            if (correctPool.isNotEmpty() && wrongPool.size >= 3) {
                correctPool.shuffled(rng).forEach { c ->
                    val ws   = wrongPool.shuffled(rng).take(3)
                    val opts = (ws.map { it.name } + c.name).shuffled(rng)
                    qs += SelectFromList(
                        id = 0,
                        prompt = "Który z tych pierwiastków jest $label?",
                        options = opts,
                        correctIndices = setOf(opts.indexOf(c.name)),
                        hint = Hint("${c.name} to $label.", boldPart = c.name)
                    )
                }
            }
        }

        return qs.shuffled(rng).mapIndexed { i, q -> q.copy(id = 2200 + i) }
    }

    /**
     * Generates the step-by-step breakdown text of an element's electron shell configuration.
     *
     * Used to populate hints for shell-configuration questions.
     *
     * @param z The atomic number of the element.
     * @param config The comma-separated shell configuration string (e.g. `"2,8,1"`).
     * @return A list of strings showing the count of electrons per shell level.
     */
    private fun shellSteps(z: Int, config: String): List<String> {
        val shellNames = listOf("K", "L", "M", "N", "O", "P")
        return config.split(",").mapIndexed { i, n ->
            "Powłoka ${shellNames.getOrElse(i) { "?" }}: $n"
        } + "Razem: $z → ${elementByNumber[z]?.name} (${elementByNumber[z]?.symbol})"
    }

    /**
     * Localizes a chemistry question to the specified target language.
     *
     * Translates prompts, options, hints, steps, and sub-instructions using a
     * comprehensive chemistry terminology dictionary.
     *
     * @param question The question model to translate.
     * @param lang     Target language code (e.g. `"en"`).
     * @return Localized copy of the question, or the original if lang is `"pl"`.
     */
    fun localize(question: Question, lang: String): Question {
        if (lang == "pl") return question
        return when (question) {
            is SelectFromList -> {
                val el = ELEMENTS.find { question.prompt.contains("(${it.symbol})") }
                val promptText = if (el != null) {
                    if (question.prompt.startsWith("Do jakiej grupy zaliczamy ")) {
                        "To which group do we classify ${el.localizedName(lang)} (${el.symbol})?"
                    } else {
                        translate(question.prompt, lang)
                    }
                } else {
                    translate(question.prompt, lang)
                }

                question.copy(
                    prompt = promptText,
                    options = question.options.map { translate(it, lang) },
                    hint = localizeHint(question.hint, lang)
                )
            }
            is ElementCardQuiz -> {
                val el = elementByNumber[question.atomicNumber]!!
                val engName = el.localizedName(lang)
                val promptText = when {
                    question.prompt.startsWith("Ile elektronów posiada atom ") -> "How many electrons does the $engName atom have?"
                    question.prompt.startsWith("Ile protonów posiada atom ") -> "How many protons does the $engName atom have?"
                    question.prompt.startsWith("Przybliżona masa atomowa ") -> "The approximate atomic mass of ${el.symbol} is:"
                    question.prompt.startsWith("W którym okresie leży pierwiastek ") -> "In which period is the element ${el.symbol} located?"
                    else -> translate(question.prompt, lang)
                }
                val mainText = when {
                    question.hint.mainText.startsWith("Elektrony = liczba atomowa Z = ") -> "Electrons = atomic number Z = ${question.atomicNumber}."
                    question.hint.mainText.startsWith("Protonów = liczba atomowa Z = ") -> "Protons = atomic number Z = ${question.atomicNumber}."
                    question.hint.mainText.startsWith("Masa atomowa ") -> "Atomic mass of ${el.symbol} ≈ ${el.atomicMass.roundToInt()} u."
                    question.hint.mainText.contains(" leży w ") -> "$engName is in period ${el.tableRow} of the periodic table."
                    else -> translate(question.hint.mainText, lang)
                }
                val steps = question.hint.steps.map { step ->
                    when (step) {
                        "Elektronów = ${question.atomicNumber}" -> "Electrons = ${question.atomicNumber}"
                        "Protonów = ${question.atomicNumber}" -> "Protons = ${question.atomicNumber}"
                        "Odczytaj masę atomową z karty pierwiastka" -> "Read the atomic mass from the element card"
                        "Policz wiersz od góry tabeli — to numer okresu" -> "Count the row from the top of the table — this is the period number"
                        else -> translate(step, lang)
                    }
                }
                question.copy(
                    prompt = promptText,
                    subtitle = if (question.subtitle == "Wybierz poprawną odpowiedź na podstawie karty pierwiastka.") "Choose the correct answer based on the element card." else translate(question.subtitle, lang),
                    hint = Hint(mainText = mainText, boldPart = question.hint.boldPart?.let { translate(it, lang) }, steps = steps)
                )
            }
            is PeriodicTableByShell -> {
                val el = elementByNumber[question.targetAtomicNumber]!!
                val engName = el.localizedName(lang)
                val mainText = "$engName (${el.symbol}) has ${question.targetAtomicNumber} electrons: ${question.shellConfig}."
                val steps = question.hint.steps.map { step ->
                    when {
                        step.startsWith("Powłoka ") -> {
                            val parts = step.removePrefix("Powłoka ").split(":")
                            "Shell ${parts.getOrNull(0) ?: ""}:${parts.getOrNull(1) ?: ""}"
                        }
                        step.startsWith("Razem: ") -> {
                            "Total: ${question.targetAtomicNumber} → $engName (${el.symbol})"
                        }
                        else -> translate(step, lang)
                    }
                }
                question.copy(
                    hint = Hint(mainText = mainText, boldPart = el.symbol, steps = steps)
                )
            }
            is PeriodicTableByName -> {
                val el = elementByNumber[question.targetAtomicNumber]!!
                val engName = el.localizedName(lang)
                val mainText = "$engName (${el.symbol}) — group ${el.tableCol}, period ${el.tableRow}."
                question.copy(
                    elementName = engName,
                    hint = Hint(mainText = mainText, boldPart = el.symbol)
                )
            }
            is PeriodicTableQuiz -> {
                question.copy(
                    questionText = translate(question.questionText, lang),
                    hint = localizeHint(question.hint, lang)
                )
            }
            is EquationBalance -> {
                question.copy(
                    instruction = translate(question.instruction, lang),
                    subInstruction = translate(question.subInstruction, lang),
                    hint = localizeHint(question.hint, lang)
                )
            }
            else -> question
        }
    }

    /**
     * Recursively translates all textual components of a [Hint] object.
     */
    private fun localizeHint(hint: Hint, lang: String): Hint {
        return Hint(
            mainText = translate(hint.mainText, lang),
            boldPart = hint.boldPart?.let { translate(it, lang) },
            sectionTitle = hint.sectionTitle?.let { translate(it, lang) },
            items = hint.items.map { translate(it, lang) },
            steps = hint.steps.map { translate(it, lang) }
        )
    }

    /**
     * Translates a name-formula combination, handling parenthesized parts dynamically.
     * Works for both "Formula (Name)" and "Name (Formula)" patterns.
     */
    private fun translateNameFormula(nameFormula: String, lang: String): String {
        if (!nameFormula.endsWith(")")) return translate(nameFormula, lang)
        val openParen = nameFormula.lastIndexOf("(")
        if (openParen == -1) return translate(nameFormula, lang)
        val outside = nameFormula.substring(0, openParen).trim()
        val inside = nameFormula.substring(openParen + 1, nameFormula.length - 1).trim()
        return "${translate(outside, lang)} (${translate(inside, lang)})"
    }

    /**
     * Translates a given Polish chemistry term, prompt, or sentence to English.
     * Matches against a static dictionary of common terms/prompts, capital-insensitive
     * element names, and dynamic sentence patterns.
     */
    private fun translate(text: String, lang: String): String {
        if (lang == "pl") return text
        val trimmed = text.trim()

        val dictMap = mapOf(
            "gaz szlachetny" to "noble gas",
            "metal" to "metal",
            "niemetal" to "nonmetal",
            "metaloid" to "metalloid",
            "obojętny" to "neutral",
            "zasadowy" to "basic",
            "kwasowy" to "acidic",
            "amfoteryczny" to "amphoteric",

            "metalem" to "a metal",
            "niemetalem" to "a nonmetal",
            "gazem szlachetnym" to "a noble gas",

            "rozpuszczalny" to "soluble",
            "trudno rozpuszczalny" to "sparingly soluble",
            "trudno rozpuszczalne" to "sparingly soluble",

            "beztlenowy" to "binary acid",
            "tlenowy" to "oxyacid",

            "alkan" to "alkane",
            "alken" to "alkene",
            "alkyn" to "alkyne",
            "alkohol" to "alcohol",
            "kwas karboksylowy" to "carboxylic acid",
            "amina" to "amine",
            "ester" to "ester",

            // Molecules names
            "woda" to "water",
            "dwutlenek węgla" to "carbon dioxide",
            "chlorek sodu" to "sodium chloride",
            "amoniak" to "ammonia",
            "metan" to "methane",
            "wodorotlenek sodu" to "sodium hydroxide",
            "wodorotlenek wapnia" to "calcium hydroxide",
            "tlenek wapnia" to "calcium oxide",
            "tlenek żelaza(III)" to "iron(III) oxide",
            "dwutlenek siarki" to "sulfur dioxide",
            "trójtlenek siarki" to "sulfur trioxide",
            "azot cząsteczkowy" to "molecular nitrogen",
            "tlen cząsteczkowy" to "molecular oxygen",
            "wodór cząsteczkowy" to "molecular hydrogen",
            "węglan wapnia" to "calcium carbonate",
            "tlenek glinu" to "aluminium oxide",
            "tlenek sodu" to "sodium oxide",
            "tlenek węgla(II)" to "carbon monoxide",
            "kwas fluorowodorowy" to "hydrofluoric acid",
            "kwas bromowodorowy" to "hydrobromic acid",

            // Acids names
            "kwas chlorowodorowy" to "hydrochloric acid",
            "kwas jodowodorowy" to "hydroiodic acid",
            "kwas siarkowodorowy" to "hydrosulfuric acid",
            "kwas siarkowy(VI)" to "sulfuric acid",
            "kwas siarkowy(IV)" to "sulfurous acid",
            "kwas azotowy(V)" to "nitric acid",
            "kwas azotowy(III)" to "nitrous acid",
            "kwas fosforowy(V)" to "phosphoric acid",
            "kwas węglowy" to "carbonic acid",
            "kwas chlorowy(VII)" to "perchloric acid",
            "kwas chlorowy(V)" to "chloric acid",
            "kwas chlorowy(III)" to "chlorous acid",
            "kwas chlorowy(I)" to "hypochlorous acid",
            "kwas borowy" to "boric acid",

            // Hydroxides names
            "wodorotlenek potasu" to "potassium hydroxide",
            "wodorotlenek litu" to "lithium hydroxide",
            "wodorotlenek baru" to "barium hydroxide",
            "wodorotlenek magnezu" to "magnesium hydroxide",
            "wodorotlenek żelaza(II)" to "iron(II) hydroxide",
            "wodorotlenek żelaza(III)" to "iron(III) hydroxide",
            "wodorotlenek miedzi(II)" to "copper(II) hydroxide",
            "wodorotlenek cynku" to "zinc hydroxide",
            "wodorotlenek manganu(II)" to "manganese(II) hydroxide",
            "wodorotlenek niklu(II)" to "nickel(II) hydroxide",

            // Salts names
            "chlorek potasu" to "potassium chloride",
            "chlorek wapnia" to "calcium chloride",
            "chlorek magnezu" to "magnesium chloride",
            "chlorek żelaza(II)" to "iron(II) chloride",
            "chlorek żelaza(III)" to "iron(III) chloride",
            "chlorek glinu" to "aluminium chloride",
            "chlorek cynku" to "zinc chloride",
            "siarczan(VI) sodu" to "sodium sulfate",
            "siarczan(VI) wapnia" to "calcium sulfate",
            "siarczan(VI) magnezu" to "magnesium sulfate",
            "siarczan(VI) cynku" to "zinc sulfate",
            "siarczan(VI) żelaza(II)" to "iron(II) sulfate",
            "siarczan(VI) baru" to "barium sulfate",
            "siarczan(VI) potasu" to "potassium sulfate",
            "węglan sodu" to "sodium carbonate",
            "węglan magnezu" to "magnesium carbonate",
            "azotan(V) sodu" to "sodium nitrate",
            "azotan(V) wapnia" to "calcium nitrate",
            "azotan(V) potasu" to "potassium nitrate",
            "fosforan(V) sodu" to "sodium phosphate",
            "fosforan(V) wapnia" to "calcium phosphate",

            // Oxides names
            "tlenek azotu(V)" to "nitrogen(V) oxide",
            "tlenek fosforu(V)" to "phosphorus(V) oxide",
            "dwutlenek krzemu" to "silicon dioxide",
            "tlenek azotu(II)" to "nitrogen(II) oxide",

            // Parent acids
            "kwas solny" to "hydrochloric acid",

            // Dissociation types
            "kwas" to "acid",
            "zasada" to "base",
            "sól" to "salt",

            // General prompts and subtitles
            "Uzupełnij równanie reakcji" to "Complete the reaction equation",
            "Dobierz odpowiednie współczynniki stechiometryczne" to "Choose appropriate stoichiometric coefficients",
            "Wybierz poprawną odpowiedź na podstawie karty pierwiastka." to "Choose the correct answer based on the element card.",
            "Wstaw brakujące pierwiastki w puste miejsca w układzie okresowym." to "Insert the missing elements into the blank spaces in the periodic table.",

            // periodicTableSets titles
            "Umieść brakujące gazy szlachetne w układzie" to "Place the missing noble gases in the table",
            "Uzupełnij halogeny w układzie" to "Complete the halogens in the table",
            "Uzupełnij litowce w układzie" to "Complete the alkali metals in the table",
            "Uzupełnij berylowce w układzie" to "Complete the alkaline earth metals in the table",
            "Umieść brakujące pierwiastki w układzie" to "Place the missing elements in the table",
            "Umieść brakujące metale szlachetne w układzie" to "Place the missing noble metals in the table",
            "Uzupełnij halogeny i litowce w układzie" to "Complete the halogens and alkali metals in the table",
            "Znajdź miejsca pierwiastków 3. okresu" to "Find the positions of the period 3 elements",
            "Znajdź miejsca pierwiastków 2. okresu" to "Find the positions of the period 2 elements",
            "Znajdź miejsca metali przejściowych okresu 4" to "Find the positions of the transition metals of period 4",
            "Uzupełnij niemetale reaktywne w układzie" to "Complete the reactive nonmetals in the table",
            "Uzupełnij pierwiastki bloku s w układzie" to "Complete the s-block elements in the table",
            "Znajdź miejsca niemetali 3. okresu" to "Find the positions of the period 3 nonmetals",
            "Znajdź miejsca pierwiastków 4. okresu — blok p" to "Find the positions of the period 4 elements — p-block",
            "Uzupełnij koniec i początek okresu 4 i 5" to "Complete the start and end of periods 4 and 5",

            // periodicTableSets hints
            "Gazy szlachetne — gr. 18 (ostatnia kolumna)." to "Noble gases — group 18 (last column).",
            "Halogeny — gr. 17." to "Halogens — group 17.",
            "Litowce — gr. 1." to "Alkali metals — group 1.",
            "Berylowce — gr. 2." to "Alkaline earth metals — group 2.",
            "Patrz na numer grupy i okresu." to "Look at the group and period numbers.",
            "Metale przejściowe — gr. 3–12." to "Transition metals — groups 3–12.",
            "Litowce → gr. 1, halogeny → gr. 17." to "Alkali metals → group 1, halogens → group 17.",
            "Wszystkie w 3. wierszu układu." to "All in the 3rd row of the table.",
            "Wszystkie w 2. wierszu układu." to "All in the 2nd row of the table.",
            "Ti, Cr, Mn, Fe, Ni — środkowy blok, okres 4." to "Ti, Cr, Mn, Fe, Ni — middle block, period 4.",
            "C, N, O, P, S — niemetale reaktywne." to "C, N, O, P, S — reactive nonmetals.",
            "Blok s — gr. 1 i 2 (oraz H i He)." to "s-block — groups 1 and 2 (plus H and He).",
            "Prawa strona 3. wiersza — niemetale." to "Right side of the 3rd row — nonmetals.",
            "Prawa strona 4. wiersza — po metalach przejściowych." to "Right side of the 4th row — after transition metals.",
            "Gr. 1 (litowce) i gr. 18 (gazy szlachetne)." to "Group 1 (alkali metals) and group 18 (noble gases).",

            // Acid Hints and Types (chemia_3_1)
            "Kwasy beztlenowe nie zawierają tlenu (HX, H₂X). Kwasy tlenowe zawierają tlen (np. HNO₃, H₂SO₄)." to "Binary acids do not contain oxygen (HX, H₂X). Oxyacids contain oxygen (e.g. HNO₃, H₂SO₄).",
            "HF – fluorowodorowy, trawiący szkło." to "HF – hydrofluoric, etches glass.",
            "HCl – chlorowodorowy (solny), składnik soku żołądkowego." to "HCl – hydrochloric (salts), component of gastric juice.",
            "HBr – bromowodorowy, podobny do HCl." to "HBr – hydrobromic, similar to HCl.",
            "HI – jodowodorowy, silny kwas beztlenowy." to "HI – hydroiodic, strong binary acid.",
            "H₂S – siarkowodorowy, znany ze smrodu zgniłych jaj." to "H₂S – hydrosulfuric, known for the smell of rotten eggs.",
            "H₂SO₄ – siarkowy(VI), siarka na +6. Najważniejszy kwas przemysłowy." to "H₂SO₄ – sulfuric, sulfur at +6. Most important industrial acid.",
            "H₂SO₃ – siarkowy(IV), siarka na +4. Przyczyna kwaśnych deszczy." to "H₂SO₃ – sulfurous, sulfur at +4. Cause of acid rain.",
            "HNO₃ – azotowy(V), azot na +5. Używany do nawozów i materiałów wybuchowych." to "HNO₃ – nitric, nitrogen at +5. Used for fertilizers and explosives.",
            "HNO₂ – azotowy(III), azot na +3. Słabszy kwas niż HNO₃." to "HNO₂ – nitrous, nitrogen at +3. Weaker acid than HNO₃.",
            "H₃PO₄ – fosforowy(V). Składnik nawozów i napojów cola." to "H₃PO₄ – phosphoric. Ingredient in fertilizers and cola drinks.",
            "H₂CO₃ – węglowy. Powstaje gdy CO₂ rozpuszcza się w wodzie." to "H₂CO₃ – carbonic. Formed when CO₂ dissolves in water.",
            "HClO₄ – chlorowy(VII), chlor na +7. Jeden z najmocniejszych kwasów." to "HClO₄ – perchloric, chlorine at +7. One of the strongest acids.",
            "HClO₃ – chlorowy(V), chlor na +5." to "HClO₃ – chloric, chlorine at +5.",
            "HClO₂ – chlorowy(III), chlor na +3." to "HClO₂ – chlorous, chlorine at +3.",
            "HClO – chlorowy(I), chlor na +1. Słaby kwas, właściwości odkażające." to "HClO – hypochlorous, chlorine at +1. Weak acid, sanitizing properties.",
            "H₃BO₃ – borowy. Stosowany w okulistyce jako środek dezynfekcyjny." to "H₃BO₃ – boric. Used in ophthalmology as a disinfectant.",

            // pH Prompts and Hints
            "Które z poniższych pH odpowiada odczynowi kwasowemu?" to "Which of the following pH values corresponds to an acidic reaction?",
            "Które z poniższych pH odpowiada odczynowi zasadowemu?" to "Which of the following pH values corresponds to a basic reaction?",
            "Odczyn kwasowy → pH < 7." to "Acidic reaction → pH < 7.",
            "Odczyn zasadowy → pH > 7." to "Basic reaction → pH > 7.",
            "pH = 7 — odczyn obojętny." to "pH = 7 — neutral reaction.",

            "Rozpuszczalność zasad" to "Solubility of bases",
            "Nazewnictwo soli" to "Naming of salts",

            // Balancing Instructions & Sub-instructions
            "Zbilansuj reakcję syntezy kwasu" to "Balance the acid synthesis reaction",
            "Uzupełnij reakcję otrzymywania kwasu" to "Complete the acid preparation reaction",
            "Zbilansuj równanie reakcji" to "Balance the reaction equation",
            "Zbilansuj reakcję syntezy wody" to "Balance the water synthesis reaction",
            "Zbilansuj reakcję metalu z kwasem" to "Balance the metal-acid reaction",
            "Zbilansuj reakcję zobojętniania" to "Balance the neutralization reaction",
            "Zbilansuj reakcję spalania" to "Balance the combustion reaction",
            "Zbilansuj spalanie metanu" to "Balance methane combustion",
            "Zbilansuj spalanie etanu" to "Balance ethane combustion",
            "Zbilansuj reakcję rozkładu" to "Balance the decomposition reaction",
            "Zbilansuj reakcję prażenia wapienia" to "Balance the limestone roasting reaction",
            "Zbilansuj reakcję elektrolizy wody" to "Balance the water electrolysis reaction",
            "Uzupełnij reakcję otrzymywania zasady" to "Complete the base preparation reaction",
            "Uzupełnij reakcję gaszenia wapna" to "Complete the lime slaking reaction",
            "Zbilansuj reakcję tworzenia tlenku" to "Balance the oxide formation reaction",
            "Zbilansuj reakcję tworzenia tlenku kwasowego" to "Balance the acidic oxide formation reaction",
            "Uzupełnij reakcję tlenku kwasowego z wodą" to "Complete the acidic oxide-water reaction",
            "Uzupełnij reakcję tlenku zasadowego z wodą" to "Complete the basic oxide-water reaction",

            "Siarczan baru wytrąca się jako biały osad" to "Barium sulfate precipitates as a white precipitate",
            "Metan — główny składnik gazu ziemnego" to "Methane — the main component of natural gas",
            "Rozkład wody utlenionej (katalizator: MnO₂)" to "Decomposition of hydrogen peroxide (catalyst: MnO₂)",
            "Historyczna reakcja Lavoisiera — rozkład tlenku rtęci(II)" to "Historic Lavoisier reaction — decomposition of mercury(II) oxide",
            "Ważna reakcja przemysłowa — produkcja wapna palonego" to "Important industrial reaction — production of quicklime",
            "Tlenek zasadowy + woda → zasada" to "Basic oxide + water → base",
            "Wapno palone + woda → wapno gaszone" to "Quicklime + water → slaked lime",

            "Alkohole — gr. -OH" to "Alcohols — group -OH",
            "Kwasy karboksylowe — gr. -COOH" to "Carboxylic acids — group -COOH",
            "Aminy — gr. -NH₂" to "Amines — group -NH₂",
            "Estry — z kwasu + alkoholu" to "Esters — from acid + alcohol",

            // Solubility and Base hints/items (chemia_3_3)
            "Rozpuszczalne zasady (mocne): NaOH, KOH, LiOH, Ca(OH)₂, Ba(OH)₂. Pozostałe są trudno rozpuszczalne." to "Soluble bases (strong): NaOH, KOH, LiOH, Ca(OH)₂, Ba(OH)₂. The rest are sparingly soluble.",
            "Inne wodorotlenki — trudno rozpuszczalne" to "Other hydroxides — sparingly soluble",
            "NaOH, KOH, LiOH — gr. 1" to "NaOH, KOH, LiOH — group 1",
            "Ca(OH)₂, Ba(OH)₂ — gr. 2" to "Ca(OH)₂, Ba(OH)₂ — group 2",

            // Salt naming hints/items (chemia_3_4)
            "Chlorki — od HCl (kwas chlorowodorowy)" to "Chlorides — from HCl (hydrochloric acid)",
            "Siarczany(VI) — od H₂SO₄" to "Sulfates(VI) — from H₂SO₄",
            "Azotany(V) — od HNO₃" to "Nitrates(V) — from HNO₃",
            "Węglany — od H₂CO₃" to "Carbonates — from H₂CO₃",
            "Fosforany(V) — od H₃PO₄" to "Phosphates(V) — from H₃PO₄",

            // Organic functional groups hints (chemia_5_2)
            "Alkohole zawierają grupę hydroksylową -OH." to "Alcohols contain a hydroxyl group -OH.",
            "Kwasy karboksylowe zawierają grupę karboksylową -COOH." to "Carboxylic acids contain a carboxyl group -COOH.",
            "Aminy zawierają grupę aminową -NH₂." to "Amines contain an amine group -NH₂.",
            "Estry powstają z reakcji kwasu karboksylowego z alkoholem." to "Esters are formed from the reaction of a carboxylic acid with an alcohol.",

            // Oxide classification hints/items (chemia_6_1)
            "Tlenki zasadowe = tlenki metali; tlenki kwasowe = tlenki niemetali. Amfoteryczne reagują zarówno z kwasami jak i zasadami." to "Basic oxides = metal oxides; acidic oxides = nonmetal oxides. Amphoteric oxides react with both acids and bases.",
            "zasadowy: tlenek metalu → reaguje z kwasami (→ sól + H₂O)" to "basic: metal oxide → reacts with acids (→ salt + H₂O)",
            "kwasowy: tlenek niemetalu → reaguje z zasadami (→ sól + H₂O) i z H₂O (→ kwas)" to "acidic: nonmetal oxide → reacts with bases (→ salt + H₂O) and with H₂O (→ acid)",
            "amfoteryczny: reaguje z kwasami i z zasadami (ZnO, Al₂O₃)" to "amphoteric: reacts with both acids and bases (ZnO, Al₂O₃)",
            "obojętny: nie reaguje z kwasami ani zasadami (CO, NO)" to "neutral: does not react with acids or bases (CO, NO)",

            // Real-world pH examples (chemia_4_1)
            "Sok cytrynowy (pH ≈ 2)" to "Lemon juice (pH ≈ 2)",
            "Ocet (pH ≈ 3)" to "Vinegar (pH ≈ 3)",
            "Kawa (pH ≈ 5)" to "Coffee (pH ≈ 5)",
            "Mleko (pH ≈ 6)" to "Milk (pH ≈ 6)",
            "Woda destylowana (pH = 7)" to "Distilled water (pH = 7)",
            "Krew ludzka (pH ≈ 7)" to "Human blood (pH ≈ 7)",
            "Woda morska (pH ≈ 8)" to "Sea water (pH ≈ 8)",
            "Soda oczyszczona w wodzie (pH ≈ 9)" to "Baking soda in water (pH ≈ 9)",
            "Mydło (pH ≈ 10)" to "Soap (pH ≈ 10)",
            "Mleko wapienne (pH ≈ 12)" to "Limewater (pH ≈ 12)",

            // Dissociation hints (chemia_4_2)
            "Kwasy dysocjują oddając jon H⁺." to "Acids dissociate by releasing H⁺ ions.",
            "Zasady dysocjują oddając jon OH⁻." to "Bases dissociate by releasing OH⁻ ions.",
            "Sole to produkty reakcji kwasu z zasadą — dysocjują na kationy metalu i aniony reszty kwasowej." to "Salts are products of the reaction of an acid with a base — they dissociate into metal cations and acid residue anions."
        )

        dictMap[trimmed]?.let { return it }
        dictMap[trimmed.lowercase()]?.let { return it }

        ELEMENTS.find { it.name.equals(trimmed, ignoreCase = true) }?.let {
            return it.localizedName(lang)
        }

        val lower = trimmed.lowercase()
        return when {
            trimmed.startsWith("Kwas o wzorze ") && trimmed.endsWith(" to:") -> {
                val formula = trimmed.removePrefix("Kwas o wzorze ").removeSuffix(" to:").trim()
                "The acid with formula $formula is:"
            }
            trimmed.startsWith("Wzór ") && trimmed.endsWith(" to:") -> {
                val name = trimmed.removePrefix("Wzór ").removeSuffix(" to:").trim()
                "The formula of ${translate(name, lang)} is:"
            }
            trimmed.startsWith("Wodorotlenek o wzorze ") && trimmed.endsWith(" to:") -> {
                val formula = trimmed.removePrefix("Wodorotlenek o wzorze ").removeSuffix(" to:").trim()
                "The hydroxide with formula $formula is:"
            }
            trimmed.startsWith("Sól o wzorze ") && trimmed.endsWith(" to:") -> {
                val formula = trimmed.removePrefix("Sól o wzorze ").removeSuffix(" to:").trim()
                "The salt with formula $formula is:"
            }
            trimmed.startsWith("Tlenek o wzorze ") && trimmed.endsWith(" to:") -> {
                val formula = trimmed.removePrefix("Tlenek o wzorze ").removeSuffix(" to:").trim()
                "The oxide with formula $formula is:"
            }
            trimmed.endsWith(" jest kwasem:") -> {
                val nameFormula = trimmed.removeSuffix(" jest kwasem:").trim()
                "${translateNameFormula(nameFormula, lang)} is an acid:"
            }
            trimmed.endsWith(" jest:") -> {
                val nameFormula = trimmed.removeSuffix(" jest:").trim()
                "${translateNameFormula(nameFormula, lang)} is:"
            }
            trimmed.endsWith(" jest tlenkiem:") -> {
                val nameFormula = trimmed.removeSuffix(" jest tlenkiem:").trim()
                "${translateNameFormula(nameFormula, lang)} is an oxide:"
            }
            trimmed.startsWith("Podczas dysocjacji ") && trimmed.endsWith(" powstają jony:") -> {
                val nameFormula = trimmed.removePrefix("Podczas dysocjacji ").removeSuffix(" powstają jony:").trim()
                "During the dissociation of ${translateNameFormula(nameFormula, lang)}, the following ions are formed:"
            }
            trimmed.startsWith("Który związek chemiczny dysocjuje dając jony ") && trimmed.endsWith("?") -> {
                val ions = trimmed.removePrefix("Który związek chemiczny dysocjuje dając jony ").removeSuffix("?").trim()
                "Which chemical compound dissociates to give ${translate(ions, lang)} ions?"
            }
            trimmed.startsWith("Jak nazywa się związek o wzorze ") && trimmed.endsWith("?") -> {
                val formula = trimmed.removePrefix("Jak nazywa się związek o wzorze ").removeSuffix("?").trim()
                "What is the name of the compound with the formula $formula?"
            }
            trimmed.startsWith("Jaki jest wzór sumaryczny ") && trimmed.endsWith("?") -> {
                val name = trimmed.removePrefix("Jaki jest wzór sumaryczny ").removeSuffix("?").trim()
                "What is the molecular formula of ${translate(name, lang)}?"
            }
            trimmed.startsWith("Jaki jest wzór chemiczny: ") && trimmed.endsWith("?") -> {
                val name = trimmed.removePrefix("Jaki jest wzór chemiczny: ").removeSuffix("?").trim()
                "What is the chemical formula of: ${translate(name, lang)}?"
            }
            trimmed.startsWith("Który z tych pierwiastków jest ") && trimmed.endsWith("?") -> {
                val label = trimmed.removePrefix("Który z tych pierwiastków jest ").removeSuffix("?").trim()
                "Which of these elements is ${translate(label, lang)}?"
            }
            trimmed.endsWith(" należy do szeregu:") -> {
                val nameFormula = trimmed.removeSuffix(" należy do szeregu:").trim()
                "${translateNameFormula(nameFormula, lang)} belongs to the series:"
            }
            trimmed.endsWith(" należy do grupy:") -> {
                val nameFormula = trimmed.removeSuffix(" należy do grupy:").trim()
                "${translateNameFormula(nameFormula, lang)} belongs to the group:"
            }
            trimmed.startsWith("Ile atomów węgla zawiera ") && trimmed.endsWith("?") -> {
                val nameFormula = trimmed.removePrefix("Ile atomów węgla zawiera ").removeSuffix("?").trim()
                "How many carbon atoms does ${translateNameFormula(nameFormula, lang)} contain?"
            }
            trimmed.startsWith("Jaką grupę funkcyjną zawiera ") && trimmed.endsWith("?") -> {
                val nameFormula = trimmed.removePrefix("Jaką grupę funkcyjną zawiera ").removeSuffix("?").trim()
                "Which functional group does ${translateNameFormula(nameFormula, lang)} contain?"
            }
            trimmed.endsWith(" pochodzi od:") -> {
                val nameFormula = trimmed.removeSuffix(" pochodzi od:").trim()
                "${translateNameFormula(nameFormula, lang)} is derived from:"
            }
            trimmed.startsWith("Roztwór o pH = ") && trimmed.endsWith(" ma odczyn:") -> {
                val ph = trimmed.removePrefix("Roztwór o pH = ").removeSuffix(" ma odczyn:").trim()
                "A solution with pH = $ph is:"
            }
            trimmed.endsWith(" ma odczyn:") -> {
                val desc = trimmed.removeSuffix(" ma odczyn:").trim()
                "${translate(desc, lang)} is:"
            }
            trimmed.contains(" → ") -> {
                trimmed.split(" → ").map { translate(it, lang) }.joinToString(" → ")
            }
            trimmed.endsWith(" to kwasowy.") -> {
                val ph = trimmed.substringBefore(" jest mniejsze od 7").trim()
                "$ph is less than 7 — acidic reaction."
            }
            trimmed.endsWith(" to obojętny.") -> {
                "pH = 7 — neutral reaction."
            }
            trimmed.endsWith(" to zasadowy.") -> {
                val ph = trimmed.substringBefore(" jest większe od 7").trim()
                "$ph is greater than 7 — basic reaction."
            }
            trimmed.startsWith("Grupa ") && trimmed.contains(" → ") -> {
                val group = trimmed.substringAfter("Grupa ").substringBefore(" → ").trim()
                val groupName = trimmed.substringAfter(" → ").trim()
                "Group $group → ${translate(groupName, lang)}"
            }
            trimmed.startsWith("Wzór ") && trimmed.contains(" to ") -> {
                val middle = trimmed.removePrefix("Wzór ").substringBefore(" to ").trim()
                val formula = trimmed.substringAfter(" to ").trim().removeSuffix(".")
                "The formula of ${translate(middle, lang)} is $formula."
            }
            trimmed.contains(" to ") -> {
                val parts = trimmed.split(" to ")
                val formula = parts[0].trim()
                val name = parts[1].trim().removeSuffix(".")
                "$formula is ${translate(name, lang)}."
            }
            trimmed.contains(" pochodzi od ") -> {
                val parts = trimmed.split(" pochodzi od ")
                val formula = parts[0].trim()
                val parent = parts[1].trim().removeSuffix(".")
                "$formula is derived from ${translate(parent, lang)}."
            }
            trimmed.startsWith("pH ") && trimmed.endsWith(" — odczyn kwasowy.") -> {
                val ph = trimmed.removePrefix("pH ").removeSuffix(" — odczyn kwasowy.").trim()
                "pH $ph is less than 7 — acidic reaction."
            }
            trimmed.startsWith("pH ") && trimmed.endsWith(" — odczyn zasadowy.") -> {
                val ph = trimmed.removePrefix("pH ").removeSuffix(" — odczyn zasadowy.").trim()
                "pH $ph is greater than 7 — basic reaction."
            }
            trimmed.contains(" i ") -> {
                val parts = trimmed.split(" i ")
                parts.map { translate(it, lang) }.joinToString(" and ")
            }

            else -> {
                // If no exact match is found, apply dynamic word/phrase translation mapping sequentially.
                var result = trimmed
                val replacements = mapOf(
                    // Specific full sentences/phrases
                    "Wszystkie współczynniki = 1" to "All coefficients = 1",
                    "Klasyczna reakcja syntezy wody" to "Classic water synthesis reaction",
                    "Tlenek kwasowy + woda → kwas" to "Acidic oxide + water → acid",
                    "Kwas siarkowy(IV) — przyczyna kwaśnych deszczy" to "Sulfurous acid — cause of acid rain",
                    "Przyczyna kwaśnych deszczy" to "Cause of acid rain",
                    "przyczyna kwaśnych deszczy" to "cause of acid rain",
                    "Tak powstaje kwas węglowy w napojach gazowanych" to "This is how carbonic acid is formed in carbonated drinks",
                    "Dwa atomy N → 2 cząsteczki HNO₃" to "Two N atoms → 2 HNO₃ molecules",
                    "Dwa atomy Cl → 2 cząsteczki kwasu" to "Two Cl atoms → 2 acid molecules",
                    "Metal + kwas → sól + wodór" to "Metal + acid → salt + hydrogen",
                    "Zasada + kwas → sól + woda" to "Base + acid → salt + water",
                    "BaSO₄ to biały osad — reakcja służy do wykrywania siarczanów" to "BaSO₄ is a white precipitate — reaction is used to detect sulfates",
                    "Spalanie węgla" to "Coal combustion",
                    "Spalanie siarki" to "Sulfur combustion",
                    "O₂ = 2 atomy O → 2 cząsteczki MgO" to "O₂ = 2 O atoms → 2 MgO molecules",
                    "Analogicznie jak spalanie magnezu" to "Analogous to magnesium combustion",
                    "Katalizator MnO₂ przyspiesza rozkład" to "Catalyst MnO₂ accelerates decomposition",
                    "Wapno palone (CaO) stosowane w budownictwie" to "Quicklime (CaO) used in construction",
                    "Elektroliza — odwrotność syntezy wody" to "Electrolysis — inverse of water synthesis",
                    "2 atomy Na → 2 cząsteczki NaOH" to "2 Na atoms → 2 NaOH molecules",
                    "2 atomy K → 2 cząsteczki KOH" to "2 K atoms → 2 KOH molecules",
                    "2 atomy Li → 2 cząsteczki LiOH" to "2 Li atoms → 2 LiOH molecules",
                    "2 atomy O → 2 cząsteczki MgO" to "2 O atoms → 2 MgO molecules",
                    "2 atomy Ca → 2 cząsteczki CaO" to "2 Ca atoms → 2 CaO molecules",
                    "Tlenek zasadowy + woda → zasada" to "Basic oxide + water → base",
                    "Wapno palone + woda → wapno gaszone" to "Quicklime + water → slaked lime",

                    "Na₂O ma 2Na → 2×NaOH" to "Na₂O has 2Na → 2×NaOH",
                    "H₂O daje grupę OH⁻ dla każdego Na" to "H₂O gives an OH⁻ group for each Na",
                    "H₂SO₄ ma 2H⁺ → potrzeba 2 NaOH" to "H₂SO₄ has 2H⁺ → needs 2 NaOH",
                    "2Na⁺ + SO₄²⁻ → Na₂SO₄" to "2Na⁺ + SO₄²⁻ → Na₂SO₄",
                    "2OH⁻ + 2H⁺ → 2H₂O" to "2OH⁻ + 2H⁺ → 2H₂O",
                    "H₃PO₄ ma 3H⁺ → potrzeba 3 NaOH" to "H₃PO₄ has 3H⁺ → needs 3 NaOH",
                    "3Na⁺ + PO₄³⁻ → Na₃PO₄" to "3Na⁺ + PO₄³⁻ → Na₃PO₄",
                    "3OH⁻ + 3H⁺ → 3H₂O" to "3OH⁻ + 3H⁺ → 3H₂O",
                    "O₂ dostarcza 2O" to "O₂ provides 2O",
                    "2O → 2×MgO" to "2O → 2×MgO",
                    "2×MgO wymaga 2×Mg" to "2×MgO requires 2×Mg",
                    "O₂ = 2O → 2×CaO → 2×Ca" to "O₂ = 2O → 2×CaO → 2×Ca",
                    "O₂ = 2O → 2×Na₂O" to "O₂ = 2O → 2×Na₂O",
                    "2×Na₂O ma 4Na → 4×Na" to "2×Na₂O has 4Na → 4×Na",

                    // Acids & Hydroxides & Compounds names (case-insensitive or specific forms)
                    "kwas azotowy(III)" to "nitrous acid",
                    "kwas azotowy(V)" to "nitric acid",
                    "kwas siarkowy(IV)" to "sulfurous acid",
                    "kwas siarkowy(VI)" to "sulfuric acid",
                    "kwas chlorowy(I)" to "hypochlorous acid",
                    "kwas chlorowy(III)" to "chlorous acid",
                    "kwas chlorowy(V)" to "chloric acid",
                    "kwas chlorowy(VII)" to "perchloric acid",
                    "kwas węglowy" to "carbonic acid",
                    "kwas fosforowy(V)" to "phosphoric acid",
                    "kwas borowy" to "boric acid",
                    "kwas fluorowodorowy" to "hydrofluoric acid",
                    "kwas chlorowodorowy" to "hydrochloric acid",
                    "kwas bromowodorowy" to "hydrobromic acid",
                    "kwas jodowodorowy" to "hydroiodic acid",
                    "kwas siarkowodorowy" to "hydrosulfuric acid",

                    "Kwas azotowy(III)" to "Nitrous acid",
                    "Kwas azotowy(V)" to "Nitric acid",
                    "Kwas siarkowy(IV)" to "Sulfurous acid",
                    "Kwas siarkowy(VI)" to "Sulfuric acid",
                    "Kwas chlorowy(I)" to "Hypochlorous acid",
                    "Kwas chlorowy(III)" to "Chlorous acid",
                    "Kwas chlorowy(V)" to "Chloric acid",
                    "Kwas chlorowy(VII)" to "Perchloric acid",
                    "Kwas węglowy" to "Carbonic acid",
                    "Kwas fosforowy(V)" to "Phosphoric acid",
                    "Kwas borowy" to "Boric acid",
                    "Kwas fluorowodorowy" to "Hydrofluoric acid",
                    "Kwas chlorowodorowy" to "Hydrochloric acid",
                    "Kwas bromowodorowy" to "Hydrobromic acid",
                    "Kwas jodowodorowy" to "Hydroiodic acid",
                    "Kwas siarkowodorowy" to "Hydrosulfuric acid",

                    "Tlenek kwasowy" to "Acidic oxide",
                    "Tlenek zasadowy" to "Basic oxide",
                    "tlenek kwasowy" to "acidic oxide",
                    "tlenek zasadowy" to "basic oxide",

                    "ma grupy" to "has groups",
                    "ma grupę" to "has group",
                    "potrzeba" to "needs",
                    "potrzebuje" to "needs",
                    "wymaga" to "requires",
                    "dostarcza" to "provides",
                    "ma " to "has ",
                    "atomy" to "atoms",
                    "atom" to "atom",
                    "cząsteczki" to "molecules",
                    "cząsteczka" to "molecule",
                    "cząsteczek" to "molecules",
                    "Sprawdź " to "Check ",
                    "Pozostałe " to "Remaining ",
                    "kwaśnych deszczy" to "acid rain",
                    "napojach gazowanych" to "carbonated drinks",
                    "Wszystkie współczynniki = 1" to "All coefficients = 1",
                    "Klasyczna reakcja syntezy wody" to "Classic water synthesis reaction",
                    "Katalizator MnO₂ przyspiesza rozkład" to "Catalyst MnO₂ accelerates decomposition",
                    "Elektroliza — odwrotność syntezy wody" to "Electrolysis — inverse of water synthesis",
                    "Wapno palone (CaO) stosowane w budownictwie" to "Quicklime (CaO) used in construction",
                    "Wapno palone + woda → wapno gaszone" to "Quicklime + water → slaked lime",
                    "Tlenek kwasowy + woda → kwas" to "Acidic oxide + water → acid",
                    "Tlenek zasadowy + woda → zasada" to "Basic oxide + water → base",
                    "Metal + kwas → sól + wodór" to "Metal + acid → salt + hydrogen",
                    "Zasada + kwas → sól + woda" to "Base + acid → salt + water",
                    "dla każdego" to "for each",
                    "daje" to "gives",
                    "odczyn kwasowy" to "acidic reaction",
                    "odczyn zasadowy" to "basic reaction",
                    "odczyn obojętny" to "neutral reaction",
                    "jest mniejsze od 7" to "is less than 7",
                    "jest większe od 7" to "is greater than 7",
                    "wynosi" to "is",
                    "woda" to "water",

                    "wodorotlenkiem" to "hydroxide",
                    "wodorotlenku" to "hydroxide",
                    "wodorotlenki" to "hydroxides",
                    "wodorotlenek" to "hydroxide",
                    "Wodorotlenkiem" to "Hydroxide",
                    "Wodorotlenku" to "Hydroxide",
                    "Wodorotlenki" to "Hydroxides",
                    "Wodorotlenek" to "Hydroxide",
                    "współczynników" to "coefficients",
                    "współczynniki" to "coefficients",
                    "współczynnik" to "coefficient",
                    "Współczynników" to "Coefficients",
                    "Współczynniki" to "Coefficients",
                    "Współczynnik" to "Coefficient",
                    "rozpuszczalny" to "soluble",
                    "rozpuszczalne" to "soluble",
                    "tlenkiem" to "oxide",
                    "tlenku" to "oxide",
                    "tlenki" to "oxides",
                    "tlenek" to "oxide",
                    "Tlenkiem" to "Oxide",
                    "Tlenku" to "Oxide",
                    "Tlenki" to "Oxides",
                    "Tlenek" to "Oxide",
                    "niemetale" to "nonmetals",
                    "niemetal" to "nonmetal",
                    "Niemetale" to "Nonmetals",
                    "Niemetal" to "Nonmetal",
                    "kwasem" to "acid",
                    "kwasu" to "acid",
                    "kwasy" to "acids",
                    "kwas" to "acid",
                    "Kwasem" to "Acid",
                    "Kwasu" to "Acid",
                    "Kwasy" to "Acids",
                    "Kwas" to "Acid",
                    "zasadą" to "base",
                    "zasadę" to "base",
                    "zasady" to "bases",
                    "zasad" to "base",
                    "zasada" to "base",
                    "Zasadą" to "Base",
                    "Zasadę" to "Base",
                    "Zasady" to "Bases",
                    "Zasad" to "Base",
                    "Zasada" to "Base",
                    "sole" to "salts",
                    "soli" to "salts",
                    "sól" to "salt",
                    "Sole" to "Salts",
                    "Soli" to "Salts",
                    "Sól" to "Salt",
                    "wodór" to "hydrogen",
                    "Wodór" to "Hydrogen",
                    "metalem" to "metal",
                    "metalu" to "metal",
                    "metal" to "metal",
                    "Metalem" to "Metal",
                    "Metalu" to "Metal",
                    "Metal" to "Metal",
                    "reakcją" to "reaction",
                    "reakcję" to "reaction",
                    "reakcje" to "reactions",
                    "reakcja" to "reaction",
                    "reakcji" to "reaction",
                    "Reakcją" to "Reaction",
                    "Reakcję" to "Reaction",
                    "Reakcje" to "Reactions",
                    "Reakcja" to "Reaction",
                    "Reakcji" to "Reaction",
                    "syntezę" to "synthesis",
                    "syntezy" to "synthesis",
                    "synteza" to "synthesis",
                    "Syntezę" to "Synthesis",
                    "Syntezy" to "Synthesis",
                    "Synteza" to "Synthesis",
                    "spalania" to "combustion",
                    "spalanie" to "combustion",
                    "Spalania" to "Combustion",
                    "Spalanie" to "Combustion",
                    "zbilansuj" to "balance",
                    "Zbilansuj" to "Balance",
                    "uzupełnij" to "complete",
                    "Uzupełnij" to "Complete",
                    "dobierz" to "choose",
                    "Dobierz" to "Choose",
                    "wodoru" to "hydrogen",
                    "Wodoru" to "Hydrogen",
                    "węgla" to "carbon",
                    "Węgla" to "Carbon",
                    "tlenu" to "oxygen",
                    "Tlenu" to "Oxygen",
                    "azotu" to "nitrogen",
                    "Azotu" to "Nitrogen",
                    "gazem" to "gas",
                    "gazy" to "gases",
                    "gaz" to "gas",
                    "Gazem" to "Gas",
                    "Gazy" to "Gases",
                    "Gaz" to "Gas",
                    "Razem" to "Total",
                    "razem" to "total",
                    "obojętny" to "neutral",
                    "zasadowy" to "basic",
                    "kwasowy" to "acidic",
                    "obojętna" to "neutral",
                    "zasadowa" to "basic",
                    "kwasowa" to "acidic",
                    "obojętne" to "neutral",
                    "zasadowe" to "basic",
                    "kwasowe" to "acidic",
                    "Obojętny" to "Neutral",
                    "Zasadowy" to "Basic",
                    "Kwasowy" to "Acidic",
                    "Obojętna" to "Neutral",
                    "Zasadowa" to "Basic",
                    "Kwasowa" to "Acidic",
                    "Obojętne" to "Neutral",
                    "Zasadowe" to "Basic",
                    "Kwasowe" to "Acidic"
                )

                for ((pl, en) in replacements) {
                    result = result.replace(pl, en)
                }
                result
            }
        }
    }


}
