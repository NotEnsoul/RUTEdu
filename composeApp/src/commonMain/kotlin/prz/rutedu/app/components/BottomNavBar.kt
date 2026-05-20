package prz.rutedu.app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class NavTab(val route: String, val label: String) {
    START("home", "START"),
    NAUKA("nauka", "NAUKA"),
    CWICZENIA("cwiczenia", "ĆWICZENIA")
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (NavTab) -> Unit,
    activeColor: Color = Color(0xFFF47B20)
) {
    val inactiveColor = Color(0xFF8F9BB3)

    val activeTab: NavTab = when {
        currentRoute == NavTab.CWICZENIA.route ||
            currentRoute?.startsWith("lesson/") == true -> NavTab.CWICZENIA
        currentRoute == NavTab.NAUKA.route ||
            currentRoute?.startsWith("subject/") == true ||
            currentRoute?.startsWith("topic/") == true -> NavTab.NAUKA
        else -> NavTab.START
    }

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        NavTab.entries.forEach { tab ->
            val isSelected = tab == activeTab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            NavTab.START -> Icons.Default.Home
                            NavTab.NAUKA -> Icons.Default.MenuBook
                            NavTab.CWICZENIA -> Icons.Default.Assignment
                        },
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 10.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = activeColor,
                    selectedTextColor = activeColor,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = inactiveColor,
                    unselectedTextColor = inactiveColor
                )
            )
        }
    }
}
