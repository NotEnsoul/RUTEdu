package prz.rutedu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import prz.rutedu.app.database.DriverFactory


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val context = applicationContext

        val driver = DriverFactory(context)


        setContent {
            App(driver.createDriver())
        }
    }
}

//@Preview
//@Composable
//fun AppAndroidPreview() {
//
//
//
//    val driver = DriverFactory(context)
//
//    App(driver.createDriver())
//}