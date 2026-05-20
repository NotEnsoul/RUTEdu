package prz.rutedu.app.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Subject(
    val id: String,
    val name: String,
    val lessonCount: Int,
    val progress: Float, // 0f to 1f – overall subject progress
    val color: Color,
    val backgroundColor: Color,
    val icon: ImageVector,
    val topics: List<Topic> = emptyList()
)
