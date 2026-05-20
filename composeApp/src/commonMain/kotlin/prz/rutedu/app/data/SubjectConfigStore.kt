package prz.rutedu.app.data

import app.cash.sqldelight.db.SqlDriver

/**
 * Persists per-lesson configuration (e.g. question count per session).
 *
 * Keys:  cfg_qcount_<lessonId>
 */
object SubjectConfigStore {

    data class Config(val questionCount: Int)

    private fun keyCount(lessonId: String) = "cfg_qcount_$lessonId"

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
