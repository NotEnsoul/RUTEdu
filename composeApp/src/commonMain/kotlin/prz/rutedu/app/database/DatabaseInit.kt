package prz.rutedu.app.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Ensures all required tables exist in the database.
 * This is a workaround for when migrations don't run properly.
 */
fun ensureTablesExist(driver: SqlDriver) {
    // Create appSettings table if it doesn't exist
    driver.execute(
        identifier = null,
        sql = """
            CREATE TABLE IF NOT EXISTS appSettings (
                setting_key TEXT PRIMARY KEY NOT NULL,
                setting_value TEXT NOT NULL
            )
        """.trimIndent(),
        parameters = 0
    )
    
    // Insert default language if not exists
    driver.execute(
        identifier = null,
        sql = """
            INSERT OR IGNORE INTO appSettings (setting_key, setting_value)
            VALUES ('language', 'en')
        """.trimIndent(),
        parameters = 0
    )
    
    // Create player table if it doesn't exist
    driver.execute(
        identifier = null,
        sql = """
            CREATE TABLE IF NOT EXISTS player (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nickname TEXT UNIQUE NOT NULL,
                high_score INTEGER NOT NULL DEFAULT 0,
                games_played INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
        """.trimIndent(),
        parameters = 0
    )
}
