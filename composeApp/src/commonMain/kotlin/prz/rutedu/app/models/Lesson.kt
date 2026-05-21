package prz.rutedu.app.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A single interactive lesson within a [Topic].
 *
 * Each lesson has a unique [id] that acts as the key for both navigation and question
 * lookup.  When the user taps a lesson card, the app navigates to `LessonGameScreen`
 * which calls [com.example.myapplication.data.QuestionBank.questionsFor] with this [id].
 *
 * **Lesson ID naming convention:**
 * `{subject_prefix}_{topic_number}_{lesson_number}` - for example `"mat_1_1"`,
 * `"geo_4_3"`, `"chemia_3_1"`. Chemistry lessons must start with `"chemia_"` because
 * the question system uses that prefix to route them to
 * [com.example.myapplication.data.ChemistryQuestionGenerator] instead of the static bank.
 *
 * **How to add a new lesson:**
 * 1. Open `SubjectRepository.kt` and append a `Lesson(...)` to the desired [Topic]'s
 *    `lessons` list.
 * 2. Register questions for this lesson in `QuestionBank.kt` (static list) or implement
 *    a generator function in `ChemistryQuestionGenerator.kt` (dynamic / randomised).
 *
 * @property id          Unique identifier (e.g. `"mat_1_1"`). Used for navigation and as
 *                       the key when loading questions and persisting progress.
 * @property name        Lesson title shown on the lesson card (Polish).
 * @property description Short subtitle shown below the title on the card.
 * @property progress    renderCompletion fraction `0f..1f`. Static placeholder - actual value
 *                       is computed and persisted by
 *                       [com.example.myapplication.data.LessonProgressStore].
 * @property isLocked    When `true` the card renders with a lock icon and cannot be tapped.
 *                       Set to `false` to make the lesson accessible.
 * @property color       Accent colour for the icon box, text highlights, and progress bar.
 * @property icon        Icon displayed inside the coloured box on the lesson card.
 *                       Use `Icons.Default.*` from the `materialIconsExtended` dependency.
 */
data class Lesson(
    val id: String,
    val name: String,
    val description: String,
    val progress: Float,
    val isLocked: Boolean,
    val color: Color,
    val icon: ImageVector
)
