@file:OptIn(ExperimentalMaterial3Api::class)

package com.openjlpt.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openjlpt.app.data.AppContainer
import com.openjlpt.app.ui.appViewModel
import com.openjlpt.core.format.JlptFormat
import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.Section
import com.openjlpt.core.scoring.Scorer

@Composable
fun HomeScreen(
    container: AppContainer,
    onPractice: (JlptLevel, Section) -> Unit,
    onMock: (JlptLevel) -> Unit,
    onReview: (JlptLevel) -> Unit,
    onHistory: (JlptLevel) -> Unit,
) {
    val vm = appViewModel { HomeViewModel(container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val level = state.level
    val format = JlptFormat.forLevel(level)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open JLPT", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    JlptLevel.entries.forEach { l ->
                        FilterChip(
                            selected = l == level,
                            onClick = { vm.selectLevel(l) },
                            label = { Text(l.label, fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("JLPT ${level.label}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(level.summary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${state.bank?.questions?.size ?: 0} practice questions · test time ${format.totalMinutes} min · pass ${format.passMark}/${format.maxScore}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            item { SectionHeader("Practice by skill") }
            item {
                val sections = Section.entries
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    sections.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { section ->
                                val stat = state.stats[section]
                                SkillCard(
                                    section = section,
                                    questionCount = state.bank?.bySection(section)?.size ?: 0,
                                    accuracy = stat?.let { Scorer.accuracyPercent(it.correct, it.total) },
                                    answered = stat?.total ?: 0,
                                    onClick = { onPractice(level, section) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
            item { SectionHeader("Exam") }
            item {
                Card(
                    onClick = { onMock(level) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${level.label} mock test", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Full exam format, timed per paper: " + format.parts.joinToString(" · ") { "${it.english} ${it.minutes} min" },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        state.lastMock?.let { last ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Last result: ${last.score ?: 0}/${last.maxScore ?: 180} · " +
                                    if (last.passed == true) "Passed" else "Not passed",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionCard(
                        icon = Icons.Filled.Refresh,
                        title = "Review mistakes",
                        subtitle = if (state.mistakeCount == 0) "Nothing to review yet" else "${state.mistakeCount} questions",
                        enabled = state.mistakeCount > 0,
                        onClick = { onReview(level) },
                        modifier = Modifier.weight(1f),
                    )
                    ActionCard(
                        icon = Icons.Filled.DateRange,
                        title = "History",
                        subtitle = if (state.attemptCount == 0) "No attempts yet" else "${state.attemptCount} attempts",
                        enabled = true,
                        onClick = { onHistory(level) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Text(
                    "All questions are original practice material written in the JLPT format. " +
                        "They are not taken from official JLPT papers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SkillCard(
    section: Section,
    questionCount: Int,
    accuracy: Int?,
    answered: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(section.japanese, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(section.english, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("$questionCount questions", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { (accuracy ?: 0) / 100f }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(
                if (accuracy == null) "Not started" else "$accuracy% correct of $answered",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, enabled = enabled, modifier = modifier) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.Start) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
