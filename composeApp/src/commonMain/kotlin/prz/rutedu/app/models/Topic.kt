package prz.rutedu.app.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Topic(
    val id: String,
    val name: String,
    val description: String,
    val progress: Float, // 0f to 1f
    val isLocked: Boolean,
    val color: Color,
    val icon: ImageVector,
    val lessons: List<Lesson> = emptyList()
)
