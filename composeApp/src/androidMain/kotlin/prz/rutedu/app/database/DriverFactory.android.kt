package prz.rutedu.app.database

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import prz.rutedu.app.Database
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val schema = Database.Schema.synchronous()
        return AndroidSqliteDriver(
            schema = schema,
            context = context,
            name = "test.db",
            callback = AndroidSqliteDriver.Callback(
                schema = schema
            )
        )
    }
}