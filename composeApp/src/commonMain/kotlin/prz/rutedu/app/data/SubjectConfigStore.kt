package prz.rutedu.app.data

import app.cash.sqldelight.db.SqlDriver

/**
 * Persists per-lesson configuration set by the student in `SubjectConfigScreen`.
 *
 * Currently only [Config.questionCount] is stored - it lets the student choose how many
 * questions to answer per session (the default is "all questions in the pool").
 *
 * Data is stored in the `appSettings` key-value table using the key scheme:
 * - **Key:** `cfg_qcount_<lessonId>` (e.g. `"cfg_qcount_mat_1_1"`)
 * - **Value:** Integer as string (e.g. `"10"`)
 */
object SubjectConfigStore {

    /**
     * Configurable settings for a single lesson session.
     *
     * @property questionCount How many questions to present per session. `LessonGameScreen`
     *                         calls [List.take] on the full question pool with this value,
     *                         so it is automatically clamped to the pool size at runtime.
     */
    data class Config(val questionCount: Int)

    private fun keyCount(lessonId: String) = "cfg_qcount_$lessonId"

    /**
     * Loads the saved [Config] for [lessonId].
     *
     * @param driver   Platform SQLite driver.
     * @param lessonId The lesson identifier (e.g. `"mat_1_1"`).
     * @return The persisted [Config], or `null` if no configuration has been saved
     *         (in which case `LessonGameScreen` uses the full question pool).
     */
    fun load(driver: SqlDriver, lessonId: String): Config? {
        val raw = driver.executeQuery(
            identifier = null,
            sql = "SELECT setting_value FROM appSettings WHERE setting_key = ?",
            mapper = { cursor ->
                app.cash.sqldelight.db.QueryResult.Value(
                    if (cursor.next().value) cursor.getString(0) else null
                )
            },
            parameters = 1,
            binders = { bindString(0, keyCount(lessonId)) }
        ).value ?: return null
        return runCatching { Config(raw.toInt()) }.getOrNull()
    }

    /**
     * Saves [config] for [lessonId], overwriting any previously stored value.
     *
     * @param driver   Platform SQLite driver.
     * @param lessonId The lesson identifier.
     * @param config   The configuration to persist.
     */
    fun save(driver: SqlDriver, lessonId: String, config: Config) {
        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO appSettings (setting_key, setting_value) VALUES (?, ?)",
            parameters = 2,
            binders = {
                bindString(0, keyCount(lessonId))
                bindString(1, config.questionCount.toString())
            }
        )
    }
}
