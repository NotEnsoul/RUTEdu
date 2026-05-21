package prz.rutedu.app.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.InternalComposeUiApi
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

/**
 * iOS implementation of the [LocalAppLocale] expect object.
 *
 * Resolves standard iOS locale identifiers through the NSBundle/Foundation APIs.
 */
@OptIn(InternalComposeUiApi::class)
actual object LocalAppLocale {
    private const val LANG_KEY = "AppleLanguages"
    private val default = (NSLocale.preferredLanguages.firstOrNull() as? String) ?: "en"
    private val LocalAppLocaleComposition = staticCompositionLocalOf { default }
    
    actual val current: String
        @Composable get() = LocalAppLocaleComposition.current

    /**
     * Injects a local context override for iOS subtrees.
     *
     * For iOS, this translates the language code string into the local composition context.
     *
     * @param value Language code override (e.g. `"en"`) or `null` to default to system.
     * @return [ProvidedValue] representing the injected Compose locale state.
     */
    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val new = value ?: default
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(LANG_KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(listOf(new), LANG_KEY)
        }
        return LocalAppLocaleComposition.provides(new)
    }
}
