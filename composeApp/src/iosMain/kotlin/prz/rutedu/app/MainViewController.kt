package prz.rutedu.app

import androidx.compose.ui.window.ComposeUIViewController
import prz.rutedu.app.database.DriverFactory

fun MainViewController() = ComposeUIViewController {
    val driver = DriverFactory().createDriver();
    App(driver)
}