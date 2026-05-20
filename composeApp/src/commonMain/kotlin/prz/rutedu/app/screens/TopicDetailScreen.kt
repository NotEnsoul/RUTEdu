package prz.rutedu.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.cash.sqldelight.db.SqlDriver
import prz.rutedu.app.Screen
import prz.rutedu.app.components.LessonCard
import prz.rutedu.app.data.LessonProgressStore
import prz.rutedu.app.data.QuestionBank
import prz.rutedu.app.data.SubjectRepository

@Composable
fun TopicDetailScreen(
    subjectId: String,
    topicId: String,
    navController: NavController,
    driver: SqlDriver,
    bottomPadding: Dp = 0.dp
) {
    val topic = SubjectRepository.getTopicById(subjectId, topicId) ?: return

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var lessonProgresses by remember {
        mutableStateOf(
            topic.lessons.associate { lesson ->
                lesson.id to LessonProgressStore.lessonFraction(driver, lesson.id)
            }
        )
    }
    LaunchedEffect(navBackStackEntry) {
        lessonProgresses = topic.lessons.associate { lesson ->
            lesson.id to LessonProgressStore.lessonFraction(driver, lesson.id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Wróć", tint = Color(0xFF1A1A1A))
            }
            Text(text = topic.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(topic.lessons) { lesson ->
                // Use real progress from DB, keep hardcoded isLocked
                val realProgress = lessonProgresses[lesson.id] ?: lesson.progress
                LessonCard(
                    lesson = lesson.copy(progress = realProgress),
                    onClick = {
                        if (!lesson.isLocked && QuestionBank.questionsFor(lesson.id).isNotEmpty())
                            navController.navigate(Screen.LessonGame.createRoute(subjectId, topicId, lesson.id))
                    }
                )
            }
        }
    }
}
