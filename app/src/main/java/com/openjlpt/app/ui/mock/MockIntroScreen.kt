package com.openjlpt.app.ui.mock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openjlpt.app.data.AppContainer
import com.openjlpt.app.ui.components.BackTopBar
import com.openjlpt.app.ui.components.LoadingBox
import com.openjlpt.app.ui.components.rememberBank
import com.openjlpt.core.format.JlptFormat
import com.openjlpt.core.model.JlptLevel

@Composable
fun MockIntroScreen(container: AppContainer, level: JlptLevel, onBack: () -> Unit, onStart: () -> Unit) {
    val bank by rememberBank(container, level)
    val format = JlptFormat.forLevel(level)

    Scaffold(topBar = { BackTopBar("${level.label} mock test", onBack) }) { padding ->
        val loaded = bank
        if (loaded == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Test papers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    format.parts.forEachIndexed { i, part ->
                        if (i > 0) HorizontalDivider()
                        val available = part.items.sumOf { (type, needed) -> minOf(needed, loaded.byType(type).size) }
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text(part.japanese, fontWeight = FontWeight.SemiBold)
                                Text(part.english, style = MaterialTheme.typography.bodySmall)
                            }
                            Column {
                                Text("${part.minutes} min", fontWeight = FontWeight.SemiBold)
                                Text("~$available questions", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            Text("Scoring", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    format.scoreSections.forEach { s ->
                        Row {
                            Text(s.japanese, Modifier.weight(1f))
                            Text("0–${s.maxScore} (min ${s.minimum})")
                        }
                    }
                    HorizontalDivider()
                    Row {
                        Text("Pass mark", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("${format.passMark} / ${format.maxScore}", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                "Each paper has its own timer, like the real test. When time runs out the test moves to the next paper. " +
                    "Unanswered questions count as wrong. Listening questions are read aloud by your phone's Japanese voice. " +
                    "Scores are an estimate of the official scaled score.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth(), enabled = loaded.questions.isNotEmpty()) {
                Text("Start mock test (${format.totalMinutes} min)")
            }
        }
    }
}
