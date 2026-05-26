package prz.rutedu.app

/**
 * Abstraction over the target platform's identity.
 *
 * Currently used for debug/diagnostic purposes only. Each platform's `actual`
 * implementation (in `androidMain` / `iosMain`) returns a human-readable name
 * such as `"Android 16"` or `"iOS 26.5"`.
 */
interface Platform {
    /** Human-readable platform name and version string. */
    val name: String
}

/**
 * Returns the [Platform] instance for the currently running platform.
 *
 * This is an `expect` function - each target provides its own `actual` implementation:
 * - **Android:** `Platform.android.kt` -> `AndroidPlatform`
 * - **iOS:** `Platform.ios.kt` -> `IOSPlatform`
 */
expect fun getPlatform(): Platform

/**
 * Helper to write text to a local file path. No-op on platforms without local file access (e.g. iOS simulator sandbox).
 */
expect fun writeTextToFile(path: String, text: String)

/**
 * Helper to read text from a local file path. Returns empty string if the file doesn't exist or is unreadable.
 */
expect fun readTextFromFile(path: String): String

/**
 * Exits the application process.
 */
expect fun exitApp()

/**
 * Returns the two-letter ISO language code of the system (e.g. "pl", "en").
 */
expect fun getSystemLanguage(): String


