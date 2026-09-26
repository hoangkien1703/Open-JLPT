package com.openjlpt.app.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openjlpt.app.audio.ListeningPlayer
import com.openjlpt.app.audio.VoiceState
import com.openjlpt.app.ui.components.RichText
import com.openjlpt.app.ui.theme.CorrectGreen
import com.openjlpt.app.ui.theme.CorrectGreenContainer
import com.openjlpt.app.ui.theme.WrongRed
import com.openjlpt.app.ui.theme.WrongRedContainer
import com.openjlpt.core.model.Passage
import com.openjlpt.core.model.Question
import com.openjlpt.core.model.ScriptLine
import com.openjlpt.core.session.SessionQuestion
import kotlinx.coroutines.delay

@Composable
fun PassageCard(passage: Passage) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (passage.title.isNotBlank()) {
                RichText(passage.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.padding(top = 6.dp))
            }
            RichText(passage.text, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.7))
        }
    }
}

@Composable
fun ListeningCard(
    question: SessionQuestion,
    player: ListeningPlayer,
    showTranscript: Boolean,
    allowTranscript: Boolean,
) {
    val voice by player.voice.collectAsStateWithLifecycle()
    val playingId by player.playing.collectAsStateWithLifecycle()
    val id = question.question.id
    val playing = playingId == id
    val available = player.hasRecording(id) || voice == VoiceState.READY
    var transcriptOpen by rememberSaveable(id) { mutableStateOf(false) }
    var progress by remember(id) { mutableStateOf<Float?>(null) }
    LaunchedEffect(playing) {
        while (playing) {
            progress = player.progress()
            delay(200)
        }
        progress = null
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.question.situation?.takeIf { it.isNotBlank() }?.let {
                RichText(it, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { if (playing) player.stop() else player.play(question) },
                    enabled = playing || available,
                ) {
                    Icon(if (playing) Icons.Filled.Close else Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (playing) "Stop" else "Play audio")
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    when {
                        playing -> "Playing…"
                        available -> ""
                        voice == VoiceState.CHECKING -> "Preparing voice…"
                        else -> "No audio for this question"
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
            if (!available && voice == VoiceState.MISSING) {
                Text(
                    "This build has no recording for this question and your phone has no Japanese voice. You can read the transcript instead.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val canOpen = allowTranscript || (!available && voice == VoiceState.MISSING)
            if (showTranscript || transcriptOpen) {
                Transcript(question.question)
            } else if (canOpen) {
                TextButton(onClick = { transcriptOpen = true }) { Text("Show transcript") }
            }
        }
    }
}

@Composable
fun Transcript(question: Question) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Transcript", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        question.script.forEach { line -> TranscriptLine(line) }
    }
}

@Composable
private fun TranscriptLine(line: ScriptLine) {
    val label = when (line.speaker.uppercase()) {
        "M", "M1" -> "男"
        "M2" -> "男2"
        "F", "F1" -> "女"
        "F2" -> "女2"
        "N" -> ""
        else -> line.speaker
    }
    Row {
        if (label.isNotEmpty()) {
            Text("$label：", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
        RichText(line.text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ExplanationCard(correct: Boolean, explanation: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (correct) CorrectGreenContainer else WrongRedContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (correct) "Correct!" else "Not quite",
                color = if (correct) CorrectGreen else WrongRed,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            if (explanation.isNotBlank()) {
                Spacer(Modifier.padding(top = 4.dp))
                RichText(explanation, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF1B1B1B)))
            }
        }
    }
}
