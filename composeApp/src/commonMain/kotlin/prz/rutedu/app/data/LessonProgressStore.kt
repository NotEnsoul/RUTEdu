package prz.rutedu.app.data

import app.cash.sqldelight.db.SqlDriver

/**
 * Persists lesson progress using the existing appSettings key-value table.
 *
 * Key format:  lp_<lessonId>
 * Value format: "<currentIndex>/<correctCount>/<totalCount>"
 */
object LessonProgressStore {

    data class Progress(
        val currentIndex: Int,
        val correctCount: Int,
        val totalCount: Int
    ) {
        /** Single source of truth: questions completed / total */
        val fraction: Float
            get() = if (totalCount == 0) 0f else (currentIndex.toFloat() / totalCount).coerceIn(0f, 1f)
    }

    private fun key(lessonId: String) = "lp_$lessonId"

    fun load(driver: SqlDriver, lessonId: String): Progress? {
        val raw = driver.executeQuery(
            identifier = null,
            sql = "SELECT setting_value FROM appSettings WHERE setting_key = ?",
            mapper = { cursor ->
                app.cash.sqldelight.db.QueryResult.Value(
                    if (cursor.next().value) cursor.getString(0) else null
                )
            },
            parameters = 1,
            binders = { bindString(0, key(lessonId)) }
        ).value ?: return null

        val parts = raw.split("/")
        if (parts.size != 3) return null
        return runCatching {
            Progress(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }.getOrNull()
    }

    fun save(driver: SqlDriver, lessonId: String, progress: Progress) {
        val value = "${progress.currentIndex}/${progress.correctCount}/${progress.totalCount}"
        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO appSettings (setting_key, setting_value) VALUES (?, ?)",
            parameters = 2,
            binders = {
                bindString(0, key(lessonId))
                bindString(1, value)
            }
        )
    }

    /** Returns the saved completion fraction for a lesson, or 0f if not started. */
    fun lessonFraction(driver: SqlDriver, lessonId: String): Float =
        load(driver, lessonId)?.fraction ?: 0f

    /** Returns the average completion fraction across all lessons in a topic. */
    fun topicFraction(driver: SqlDriver, lessonIds: List<String>): Float =
        if (lessonIds.isEmpty()) 0f
        else lessonIds.map { lessonFraction(driver, it) }.average().toFloat()
}
