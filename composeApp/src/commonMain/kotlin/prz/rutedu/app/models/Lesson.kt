package prz.rutedu.app.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a single lesson/exercise within a topic.
 *
 * HOW TO ADD A LESSON:
 * Open SubjectRepository.kt and find the Topic you want to add a lesson to.
 * Add a new Lesson(...) entry to its `lessons` list.
 *
 * @param id         Unique identifier (e.g. "mat_1_1"). Used for navigation.
 * @param name       Lesson title shown on the card.
 * @param description Short subtitle shown below the title.
 * @param progress   Completion 0f–1f (e.g. 0.8f = 80%).
 * @param isLocked   True = grayed-out lock card. Unlock by setting false.
 * @param color      Accent color for icon, text and progress bar.
 * @param icon       Icon shown inside the colored box. Use Icons.Default.* from materialIconsExtended.
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
