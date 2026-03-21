package com.example.myapplication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.models.Hint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HintBottomSheet(
    hint: Hint,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Podpowiedź",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Zamknij", tint = Color(0xFF9E9E9E))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main text with left border and optional bold
            if (hint.mainText.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.07f))
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                    val annotated = buildAnnotatedString {
                        val text = hint.mainText
                        val bold = hint.boldPart
                        if (bold != null) {
                            val idx = text.indexOf(bold)
                            if (idx >= 0) {
                                append(text.substring(0, idx))
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                                append(text.substring(idx + bold.length))
                            } else append(text)
                        } else append(text)
                    }
                    Text(
                        text = annotated,
                        fontSize = 14.sp,
                        color = Color(0xFF1A1A1A),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Section title + items
            if (hint.sectionTitle != null) {
                Text(
                    text = hint.sectionTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9E9E9E),
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
            }
            if (hint.items.isNotEmpty()) {
                hint.items.forEachIndexed { index, item ->
                    val dotColor = when (index % 3) {
                        0 -> Color(0xFFBCC1CA)
                        1 -> accentColor
                        else -> accentColor.copy(alpha = 0.5f)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (index == 1) accentColor.copy(alpha = 0.08f) else Color(0xFFF5F6FA))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(text = item, fontSize = 14.sp, color = Color(0xFF1A1A1A))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Steps
            if (hint.steps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F6FA))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Krok po kroku:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        hint.steps.forEach { step ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "•",
                                    fontSize = 14.sp,
                                    color = Color(0xFF9E9E9E),
                                    modifier = Modifier.padding(end = 6.dp, top = 1.dp)
                                )
                                Text(text = step, fontSize = 14.sp, color = Color(0xFF4A4A4A), lineHeight = 20.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Dismiss button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Rozumiem, wracam do zadania", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
internal fun EquationText(text: String, color: Color, fontSize: TextUnit = 48.sp) {
    Text(text = text, fontSize = fontSize, fontWeight = FontWeight.ExtraBold, color = color)
}

@Composable
internal fun BottomButtons(
    accentColor: Color,
    onHint: () -> Unit,
    onCheck: () -> Unit,
    checkEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onHint,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(26.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor)
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Podpowiedź", color = accentColor, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onCheck,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            enabled = checkEnabled
        ) {
            Text("Sprawdź", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
internal fun Numpad(
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫")
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable {
                                when (key) {
                                    "C" -> onClear()
                                    "⌫" -> onBackspace()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = key, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                    }
                }
            }
        }
    }
}
