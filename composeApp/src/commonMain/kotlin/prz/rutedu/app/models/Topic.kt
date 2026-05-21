package prz.rutedu.app.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A thematic grouping of [Lesson]s within a [Subject] (e.g. "Wyrażenia algebraiczne").
 *
 * Topics are displayed as cards on the subject-detail screen. A locked topic renders as
 * a greyed-out card and is not tappable.  Unlocking a topic is done by setting
 * [isLocked] = `false` in `SubjectRepository.kt`.
 *
 * **How to add a new topic:**
 * Open `SubjectRepository.kt`, find the parent [Subject], and append a `Topic(...)` to its
 * `topics` list. Topics without a `lessons` list (or with an empty list) will show no
 * lessons on the topic-detail screen.
 *
 * @property id          Unique string identifier used in navigation routes
 *                       (e.g. `"mat_1"`, `"chemia_3"`). Must be unique across all subjects.
 * @property name        Display name shown on topic cards (Polish).
 * @property description One-line subtitle shown below the topic name.
 * @property progress    Completion fraction `0f..1f`. Static placeholder - actual value
 *                       is computed from [prz.rutedu.app.data.LessonProgressStore].
 * @property isLocked    When `true` the card is rendered with a lock icon and cannot be tapped.
 * @property color       Accent colour used for the topic's icon background and decorative elements.
 * @property icon        Icon shown inside the coloured icon box on the topic card.
 * @property lessons     Ordered list of [Lesson]s that make up this topic. May be empty for
 *                       topics that are placeholders (coming soon).
 */
data class Topic(
    val id: String,
    val name: String,
    val description: String,
    val progress: Float,
    val isLocked: Boolean,
    val color: Color,
    val icon: ImageVector,
    val lessons: List<Lesson> = emptyList()
)
