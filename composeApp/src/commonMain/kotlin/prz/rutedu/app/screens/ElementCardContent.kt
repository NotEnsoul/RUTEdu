package prz.rutedu.app.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prz.rutedu.app.models.ELEMENTS
import prz.rutedu.app.models.Question
import kotlin.math.abs
import kotlin.math.round

/**
 * Formats a float to exactly 3 decimal places without using `String.format` (which is not
 * available in Kotlin/Native). Used for displaying atomic mass (e.g. `"12.011"`).
 *
 * The implementation multiplies by 1000, rounds to the nearest integer, then reconstructs the
 * decimal string with `padStart` to preserve leading zeros in the fractional part.
 */
private fun Float.to3dp(): String {
    val rounded = round(this * 1000).toLong()
    val intPart = rounded / 1000
    val decPart = abs(rounded % 1000)
    return "$intPart.${decPart.toString().padStart(3, '0')}"
}

// ─────────────────────────────────────────────────────────────────────────────

/**
 * Question content for [Question.ElementCardQuiz] - displays a styled element card and asks the
 * student to select one of four multiple-choice answers about that element.
 *
 * The element card mirrors the visual design of a real periodic-table cell:
 * - A circle border containing the chemical **symbol** in large type.
 * - The Polish element **name** and **atomic mass** (3 decimal places, KMP-safe formatter).
 * - An electron **configuration** row and **group** row when present.
 *
 * The four answer options are arranged in a 2x2 grid of tappable cards. Selection and wrong-answer
 * highlighting follow the same pattern as [SelectFromListContent] (accent border / red background).
 *
 * @param question      The question: atomic number, prompt, subtitle, 4 option strings, correct index, hint.
 * @param accentColor   Subject accent color applied to the element card background and selected option.
 * @param bottomPadding System navigation bar height padding.
 * @param onCorrect     Called when `selectedIndex == question.correctIndex`.
 * @param onWrong       Called when the selection is incorrect.
 */
@Composable
internal fun ElementCardContent(
    question: Question.ElementCardQuiz,
    accentColor: Color,
    bottomPadding: Dp,
    onCorrect: () -> Unit,
    onWrong: () -> Unit = {}
) {
    val element = remember(question.atomicNumber) {
        ELEMENTS.find { it.atomicNumber == question.atomicNumber }
    }

    var selectedIndex by remember(question.id) { mutableStateOf<Int?>(null) }
    var isWrong      by remember(question.id) { mutableStateOf(false) }
    var showHint     by remember(question.id) { mutableStateOf(false) }

    if (showHint) {
        HintBottomSheet(hint = question.hint, accentColor = accentColor, onDismiss = { showHint = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        // Question prompt
        Text(
            text = question.prompt,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Center,
            lineHeight = 30.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = question.subtitle,
            fontSize = 14.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Element card
        if (element != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.07f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Inner white card with symbol
                    Card(
                        modifier = Modifier.wrapContentSize(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 36.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Symbol in circle border
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .border(3.dp, Color(0xFF1A1A1A), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = element.symbol,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = element.namePL,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                text = element.atomicMass.to3dp(),
                                fontSize = 13.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = accentColor.copy(alpha = 0.25f), thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))

                    // Info rows
                    if (element.electronConfig.isNotEmpty()) {
                        ElementInfoRow("Konfiguracja:", element.electronConfig, accentColor)
                        Spacer(Modifier.height(6.dp))
                    }
                    if (element.groupName.isNotEmpty()) {
                        ElementInfoRow(
                            label = "Grupa:",
                            value = "${element.tableCol} (${element.groupName.lowercase()})",
                            accentColor = accentColor
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 2x2 answer grid
        question.options.chunked(2).forEachIndexed { rowIdx, rowOptions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowOptions.forEachIndexed { colIdx, option ->
                    val index = rowIdx * 2 + colIdx
                    val isSelected = selectedIndex == index
                    val borderColor = when {
                        isWrong && isSelected -> Color(0xFFE53935)
                        isSelected            -> accentColor
                        else                  -> Color(0xFFE8EAF0)
                    }
                    val bgColor = when {
                        isWrong && isSelected -> Color(0xFFFFEBEA)
                        isSelected            -> accentColor.copy(alpha = 0.10f)
                        else                  -> Color.White
                    }

                    Card(
                        onClick = { selectedIndex = index; isWrong = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.5.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
                        )
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = option,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected && !isWrong) accentColor else Color(0xFF1A1A1A)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))

        BottomButtons(
            accentColor = accentColor,
            onHint = { showHint = true },
            onCheck = {
                if (selectedIndex == question.correctIndex) onCorrect()
                else onWrong()
            },
            checkEnabled = selectedIndex != null
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** A two-column label/value row used inside the element information card. */
@Composable
private fun ElementInfoRow(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF1A1A1A))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
    }
}
