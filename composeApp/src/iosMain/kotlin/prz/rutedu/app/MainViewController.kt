package prz.rutedu.app

import androidx.compose.ui.window.ComposeUIViewController
import prz.rutedu.app.database.DriverFactory

/**
 * iOS entry point for the app.
 *
 * Creates the platform [DriverFactory], initialises the SQLite driver, and returns a
 * [ComposeUIViewController] that hosts the [App] composable.
 */
fun MainViewController() = ComposeUIViewController {
    App(DriverFactory())
}