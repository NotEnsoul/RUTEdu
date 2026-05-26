package prz.rutedu.app.data

import app.cash.sqldelight.db.SqlDriver
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import rutedu.composeapp.generated.resources.Res
import prz.rutedu.app.Database
import prz.rutedu.app.models.Question
import prz.rutedu.app.locale.getCurrentLanguage
import prz.rutedu.app.math.mathEngineAvailable

/**
 * Central registry of all quiz questions.
 *
 * For all static lessons, questions are fetched from the SQLite database
 * (seeded from `questions_pl.json` and `questions_en.json` assets) and cached in memory
 * under [cachedQuestions] for the current active language. This design ensures that
 * layout composition can access questions synchronously without blocking the UI thread.
 *
 * Chemistry lessons (except chemistry_3_1 and chemistry_3_2) generate questions dynamically
 * via [ChemistryQuestionGenerator].
 */
object QuestionBank {

    /**
     * List of lesson identifiers that contain static questions.
     */
    val staticLessons = listOf(
        "mat_1_1", "mat_1_2", "mat_1_3", "mat_2_1", "mat_2_2",
        "mat_3_1", "mat_4_1", "mat_5_1",
        "geo_1_1", "geo_4_1", "geo_4_2", "geo_4_3", "geo_4_4",
        "chemia_3_1", "chemia_3_2",
        "algebra_1_1", "algebra_1_2", "algebra_1_3", "algebra_2_1", "algebra_2_2"
    )

    private var cachedQuestions: Map<String, List<Question>> = emptyMap()

    /**
     * Seeds the questions database if it has not been seeded yet.
     * Loads localized JSON files and upserts questions into the database in a transaction.
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun seedDatabaseIfNeeded(driver: SqlDriver) {
        val db = Database(driver)
        
        // Version constant to force database re-seeding when question JSON files are updated.
        val targetVersion = "2"
        val dbVersion = try {
            db.databaseQueries.getSetting("questions_db_version").executeAsOneOrNull()
        } catch (_: Exception) {
            null
        }

        if (dbVersion == targetVersion) {
            return
        }

        db.transaction {
            try {
                db.databaseQueries.clearStoredQuestions()
            } catch (_: Exception) {}
            seedLanguage(db, "pl")
            seedLanguage(db, "en")
            db.databaseQueries.upsertSetting("questions_db_version", targetVersion)
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun seedLanguage(db: Database, lang: String) {
        try {
            val bytes = Res.readBytes("files/questions_$lang.json")
            val jsonText = bytes.decodeToString()
            val format = Json { ignoreUnknownKeys = true }
            val lessonsList = format.decodeFromString<List<LessonQuestionsDto>>(jsonText)

            for (lessonQuestions in lessonsList) {
                for (qDto in lessonQuestions.questions) {
                    val qJson = format.encodeToString(QuestionDto.serializer(), qDto)
                    db.databaseQueries.upsertQuestion(
                        lesson_id = lessonQuestions.lessonId,
                        question_id = qDto.id.toLong(),
                        language = lang,
                        type = qDto::class.simpleName ?: "Unknown",
                        data_json = qJson
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads the questions for the given language code from the database into the in-memory cache.
     * Fallbacks to English ("en") if the language code is not supported.
     */
    fun loadQuestions(driver: SqlDriver, languageCode: String) {
        val db = Database(driver)
        val lang = if (languageCode == "pl" || languageCode == "en") languageCode else "en"

        val newCache = mutableMapOf<String, List<Question>>()
        val format = Json { ignoreUnknownKeys = true }

        for (lessonId in staticLessons) {
            val dbRows = db.databaseQueries.getQuestionsForLesson(lessonId, lang).executeAsList()
            val models = dbRows.mapNotNull { row ->
                try {
                    format.decodeFromString<QuestionDto>(row.data_json).toModel()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            newCache[lessonId] = models
        }
        cachedQuestions = newCache
    }

    /**
     * Returns the ordered list of questions for the given lesson.
     *
     * Routing:
     * - Static lessons (e.g. math, geography, algebra, static chemistry) are loaded from the database cache.
     * - Chemistry lessons (except chemia_3_1 and chemia_3_2) are generated dynamically.
     *
     * Dynamic questions are localized to the active language via [getCurrentLanguage] if needed.
     */
    fun questionsFor(lessonId: String, seed: Long = 0L, excludeIds: Set<Int> = emptySet()): List<Question> {
        val rawQuestions = when {
            lessonId in staticLessons -> {
                val cached = cachedQuestions[lessonId] ?: emptyList()
                val platformFiltered = if (lessonId.startsWith("algebra_")) {
                    if (mathEngineAvailable) {
                        cached.filterIsInstance<Question.ExpressionTypeAnswer>()
                    } else {
                        cached.filterIsInstance<Question.SelectFromList>()
                    }
                } else {
                    cached
                }
                val shuffled = if (seed != 0L) {
                    platformFiltered.shuffled(kotlin.random.Random(seed))
                } else {
                    platformFiltered
                }
                if (excludeIds.isEmpty()) shuffled else shuffled.filter { it.id !in excludeIds }
            }
            lessonId.startsWith("chemia_") ->
                ChemistryQuestionGenerator.generateFor(lessonId, seed, excludeIds)
            else ->
                emptyList()
        }

        val lang = getCurrentLanguage()
        if (lang == "pl") return rawQuestions

        return rawQuestions.map { question ->
            when {
                lessonId.startsWith("chemia_") && lessonId !in staticLessons ->
                    ChemistryQuestionGenerator.localize(question, lang)
                else -> question
            }
        }
    }
}
