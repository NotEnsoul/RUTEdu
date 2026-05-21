package prz.rutedu.app

import android.os.Build

/** Android implementation of [Platform]. Reports `"Android <SDK_INT>"` as the platform name. */
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

/**
 * Instantiates the Android platform info provider.
 *
 * @return An instance of [AndroidPlatform] reporting the current Android SDK version.
 */
actual fun getPlatform(): Platform = AndroidPlatform()