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
