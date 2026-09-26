package com.openjlpt.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.openjlpt.app.data.AppContainer
import com.openjlpt.app.data.db.AttemptEntity
import com.openjlpt.app.data.db.SectionStat
import com.openjlpt.app.ui.appViewModel
import com.openjlpt.app.ui.components.BackTopBar
import com.openjlpt.app.ui.theme.CorrectGreen
import com.openjlpt.app.ui.theme.WrongRed
import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.Section
import com.openjlpt.core.model.TestMode
import com.openjlpt.core.scoring.Scorer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class HistoryViewModel(private val container: AppContainer, private val level: JlptLevel) : ViewModel() {
    val attempts: StateFlow<List<AttemptEntity>> = container.history.attempts(level)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val stats: StateFlow<Map<Section, SectionStat>> = container.history.sectionStats(level)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun clear() {
        viewModelScope.launch { container.history.clear(level) }
    }
}

@Composable
fun HistoryScreen(container: AppContainer, level: JlptLevel, onBack: () -> Unit, onOpenAttempt: (Long) -> Unit) {
    val vm = appViewModel { HistoryViewModel(container, level) }
    val attempts by vm.attempts.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    Scaffold(
        topBar = {
            BackTopBar("${level.label} history", onBack) {
                if (attempts.isNotEmpty()) {
                    IconButton(onClick = { confirmClear = true }) { Icon(Icons.Filled.Delete, contentDescription = "Clear history") }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Accuracy by skill", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Section.entries.forEach { section ->
                            val stat = stats[section]
                            val pct = stat?.let { Scorer.accuracyPercent(it.correct, it.total) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(section.english, Modifier.weight(0.35f), style = MaterialTheme.typography.bodyMedium)
                                LinearProgressIndicator(progress = { (pct ?: 0) / 100f }, modifier = Modifier.weight(0.45f))
                                Text(
                                    pct?.let { "$it%" } ?: "–",
                                    Modifier.weight(0.2f).padding(start = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }
            if (attempts.isEmpty()) {
                item { Text("No attempts yet. Finish a practice set or mock test and it will show up here.") }
            }
            items(attempts, key = { it.id }) { attempt ->
                Card(onClick = { onOpenAttempt(attempt.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(attempt.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(dateFormat.format(Date(attempt.finishedAt)), style = MaterialTheme.typography.labelSmall)
                        }
                        if (attempt.mode == TestMode.MOCK.name) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${attempt.score ?: 0}/${attempt.maxScore ?: 180}", fontWeight = FontWeight.Bold)
                                Text(
                                    if (attempt.passed == true) "Passed" else "Not passed",
                                    color = if (attempt.passed == true) CorrectGreen else WrongRed,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        } else {
                            Text(
                                "${attempt.correct}/${attempt.total} · ${Scorer.accuracyPercent(attempt.correct, attempt.total)}%",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear ${level.label} history?") },
            text = { Text("This deletes every attempt and your mistake list for this level.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clear() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}
