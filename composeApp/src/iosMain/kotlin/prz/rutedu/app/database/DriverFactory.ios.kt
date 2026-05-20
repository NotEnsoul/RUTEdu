package prz.rutedu.app.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import prz.rutedu.app.Database

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val schema = Database.Schema.synchronous()
        return NativeSqliteDriver(
            schema = schema,
            name = "test.db"
        )
    }
}