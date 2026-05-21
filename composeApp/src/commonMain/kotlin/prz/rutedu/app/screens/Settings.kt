package prz.rutedu.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import prz.rutedu.app.Database
import prz.rutedu.app.locale.customAppLocale
import kotlinx.coroutines.launch
import rutedu.composeapp.generated.resources.Res
import rutedu.composeapp.generated.resources.menu_settings
import rutedu.composeapp.generated.resources.fragment_settings_select_language
import rutedu.composeapp.generated.resources.back
import org.jetbrains.compose.resources.stringResource

/**
 * A selectable locale in the language picker on the [Settings] screen.
 *
 * @property code        BCP-47 language code stored in the database (e.g. `"pl"`, `"en"`).
 * @property displayName Human-readable name shown in the dropdown (e.g. `"Polski"`, `"English"`).
 */
data class LanguageOption(
    val code: String,
    val displayName: String
)

/**
 * Language and app settings screen.
 *
 * Displays a dropdown listing all supported [LanguageOption]s. Selecting a language immediately
 * updates `customAppLocale` (triggering a UI re-render) and persists the choice to the database.
 * The current language is restored from the database on first composition.
 *
 * @param navController Navigation controller for the back-stack pop on "Back".
 * @param database      [Database] instance used to read/write the persisted language preference.
 */
@Composable
fun Settings(navController: NavController, database: Database) {
    val coroutineScope = rememberCoroutineScope()

    val languageOptions = listOf(
        LanguageOption("en", "English"),
        LanguageOption("pl", "Polski"),
        LanguageOption("fr", "Français"),
        LanguageOption("pt", "Português"),
        LanguageOption("es", "Español"),
        LanguageOption("el", "Ελληνικά"),
        LanguageOption("nl", "Nederlands"),
        LanguageOption("sk", "Slovenčina"),
        LanguageOption("it", "Italiano"),
        LanguageOption("de", "Deutsch"),
        LanguageOption("hu", "Magyar"),
        LanguageOption("cs", "Čeština")
    )

    var selectedLanguage by remember { mutableStateOf(languageOptions[0]) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    val settingsTitle = stringResource(Res.string.menu_settings)
    val selectLanguageText = stringResource(Res.string.fragment_settings_select_language)
    val backText = stringResource(Res.string.back)

    // Load current language from database on first composition
    LaunchedEffect(Unit) {
        try {
            val currentLang = database.databaseQueries.getLanguage().executeAsOneOrNull()
            if (currentLang != null) {
                selectedLanguage = languageOptions.find { it.code == currentLang } ?: languageOptions[0]
            }
        } catch (e: Exception) {
            // Table might not exist yet
        }
    }

    // Respect safe area and reuse MenuButton for consistent look
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeContentPadding() // ensure not under iOS status bar / notch
            .padding(16.dp),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top

    ) {
        Text(
            text = settingsTitle,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(24.dp))

        // Language Selection Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = selectLanguageText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                Box {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDropdownExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedLanguage.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "▼",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        languageOptions.forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = language.displayName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                onClick = {
                                    selectedLanguage = language
                                    isDropdownExpanded = false
                                    // Update the app locale
                                    customAppLocale = language.code
                                    // Save to database
                                    coroutineScope.launch {
                                        try {
                                            database.databaseQueries.setLanguage(language.code)
                                        } catch (e: Exception) {
                                            // Ignore if table doesn't exist
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        Button(onClick = {
            navController.popBackStack()
        }) {
            Text(backText)
        }
    }
}