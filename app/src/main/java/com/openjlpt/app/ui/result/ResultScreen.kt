package com.openjlpt.app.ui.result

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openjlpt.app.data.AppContainer
import com.openjlpt.app.data.db.AttemptEntity
import com.openjlpt.app.ui.appViewModel
import com.openjlpt.app.ui.components.BackTopBar
import com.openjlpt.app.ui.components.LoadingBox
import com.openjlpt.app.ui.components.RichText
import com.openjlpt.app.ui.components.stripMarkup
import com.openjlpt.app.ui.quiz.ChoiceRow
import com.openjlpt.app.ui.quiz.ChoiceState
import com.openjlpt.app.ui.quiz.ExplanationCard
import com.openjlpt.app.ui.quiz.PassageCard
import com.openjlpt.app.ui.quiz.Transcript
import com.openjlpt.app.ui.theme.CorrectGreen
import com.openjlpt.app.ui.theme.WrongRed
import com.openjlpt.core.model.Section
import com.openjlpt.core.scoring.Scorer
import com.openjlpt.core.scoring.TestScore

@Composable
fun ResultScreen(container: AppContainer, attemptId: Long, onBack: () -> Unit) {
    val vm = appViewModel { ResultViewModel(container, attemptId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var onlyWrong by rememberSaveable { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(setOf<String>()) }

    Scaffold(topBar = { BackTopBar("Results", onBack) }) { padding ->
        val attempt = state.attempt
        if (state.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        if (attempt == null) {
            Text("This result could not be found.", Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val shown = if (onlyWrong) state.items.filter { !it.isCorrect } else state.items
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val score = state.score
                if (score != null) MockSummary(attempt, score) else PracticeSummary(attempt)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Answers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    FilterChip(selected = !onlyWrong, onClick = { onlyWrong = false }, label = { Text("All") })
                    FilterChip(selected = onlyWrong, onClick = { onlyWrong = true }, label = { Text("Wrong only") })
                }
            }
            if (shown.isEmpty()) {
                item { Text(if (onlyWrong) "No wrong answers. Great work!" else "No answers were recorded.") }
            }
            items(shown, key = { it.question.id }) { item ->
                val open = item.question.id in expanded
                ReviewCard(item, open) {
                    expanded = if (open) expanded - item.question.id else expanded + item.question.id
                }
            }
        }
    }
}

@Composable
private fun MockSummary(attempt: AttemptEntity, score: TestScore) {
    val passed = score.passed
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${attempt.level} mock test", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${score.total}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text(" / ${score.maxScore}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    if (passed) "PASS" else "FAIL",
                    color = if (passed) CorrectGreen else WrongRed,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                "Pass mark ${score.passMark}, and every section needs its minimum. ${score.correct} of ${score.questionCount} correct.",
                style = MaterialTheme.typography.bodySmall,
            )
            score.sections.forEach { s ->
                Column {
                    Row {
                        Text(s.section.japanese, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            "${s.score}/${s.section.maxScore}",
                            fontWeight = FontWeight.Bold,
                            color = if (s.meetsMinimum) MaterialTheme.colorScheme.onPrimaryContainer else WrongRed,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { s.score / s.section.maxScore.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "${s.correct}/${s.total} correct · minimum ${s.section.minimum}" + if (!s.meetsMinimum) " (below minimum)" else "",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Text(
                "Scores are estimates. The official test uses scaled scoring.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PracticeSummary(attempt: AttemptEntity) {
    val pct = Scorer.accuracyPercent(attempt.correct, attempt.total)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("${attempt.level} · ${attempt.title}", style = MaterialTheme.typography.titleMedium)
            Text("$pct%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text("${attempt.correct} of ${attempt.total} correct", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ReviewCard(item: ReviewItem, open: Boolean, onToggle: () -> Unit) {
    val q = item.question
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (item.isCorrect) "○" else "×",
                    color = if (item.isCorrect) CorrectGreen else WrongRed,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("${item.number}. ${q.type.japanese}", style = MaterialTheme.typography.labelMedium)
                    Text(
                        stripMarkup(q.prompt.ifBlank { q.script.firstOrNull()?.text ?: "" }),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (open) Int.MAX_VALUE else 2,
                    )
                }
            }
            if (open) {
                item.passage?.let { PassageCard(it) }
                if (q.section == Section.LISTENING) {
                    q.situation?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Transcript(q)
                }
                if (q.prompt.isNotBlank()) RichText(q.prompt, style = MaterialTheme.typography.titleSmall)
                q.choices.forEachIndexed { i, choice ->
                    ChoiceRow(
                        number = i + 1,
                        text = choice,
                        state = when {
                            i == q.answer -> ChoiceState.CORRECT
                            i == item.selected -> ChoiceState.WRONG
                            else -> ChoiceState.NORMAL
                        },
                        onClick = null,
                    )
                }
                if (item.selected == null) Text("Not answered", color = WrongRed, style = MaterialTheme.typography.labelMedium)
                ExplanationCard(item.isCorrect, q.explanation)
                TextButton(onClick = onToggle) { Text("Hide") }
            }
        }
    }
}
