package com.openjlpt.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openjlpt.app.data.AppContainer
import com.openjlpt.app.data.AudioSource
import com.openjlpt.app.ui.components.BackTopBar

private val SPEEDS = listOf(0.75f, 0.9f, 1.0f, 1.25f)

@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val store = container.settings
    val settings by store.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(topBar = { BackTopBar("Settings", onBack) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SectionTitle("Listening audio")
            RadioRow(
                title = "Built-in recordings",
                subtitle = "Recorded voices that come with the app. Works on every phone.",
                selected = settings.audioSource == AudioSource.BUILT_IN,
                onSelect = { store.update { it.copy(audioSource = AudioSource.BUILT_IN) } },
            )
            RadioRow(
                title = "Phone's Japanese voice",
                subtitle = "Uses your phone's text-to-speech. Needs a Japanese voice installed.",
                selected = settings.audioSource == AudioSource.PHONE_VOICE,
                onSelect = { store.update { it.copy(audioSource = AudioSource.PHONE_VOICE) } },
            )
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                } catch (_: ActivityNotFoundException) {
                    runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
                }
            }) { Text("Install a Japanese voice on this phone") }

            Text("Playback speed", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SPEEDS.forEach { speed ->
                    FilterChip(
                        selected = settings.playbackSpeed == speed,
                        onClick = { store.update { it.copy(playbackSpeed = speed) } },
                        label = { Text("${speed}×") },
                    )
                }
            }
            SwitchRow(
                title = "Play automatically",
                subtitle = "Start the audio as soon as a listening question opens.",
                checked = settings.autoPlayListening,
                onChange = { v -> store.update { it.copy(autoPlayListening = v) } },
            )

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionTitle("Tap a word")
            SwitchRow(
                title = "Tap Japanese words for meaning and sound",
                subtitle = "Shows the reading and English meaning in a pop-up.",
                checked = settings.tapWords,
                onChange = { v -> store.update { it.copy(tapWords = v) } },
            )
            SwitchRow(
                title = "Also before I answer",
                subtitle = "Off: words open only after you check your answer, so they don't give it away.",
                checked = settings.tapWordsBeforeAnswer,
                enabled = settings.tapWords,
                onChange = { v -> store.update { it.copy(tapWordsBeforeAnswer = v) } },
            )
            SwitchRow(
                title = "Also during mock tests",
                subtitle = "Off: mock tests stay like the real exam. You can still tap words in the results.",
                checked = settings.tapWordsInMock,
                enabled = settings.tapWords,
                onChange = { v -> store.update { it.copy(tapWordsInMock = v) } },
            )
            SwitchRow(
                title = "Say the word when tapped",
                checked = settings.speakTappedWord,
                enabled = settings.tapWords,
                onChange = { v -> store.update { it.copy(speakTappedWord = v) } },
            )

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionTitle("Credits")
            Text(
                "Voices: HTS voice nitech-jp-atr503-m001 (Nagoya Institute of Technology) and " +
                    "tohoku-f01 (Tohoku University, CC BY 4.0), synthesized with Open JTalk.\n" +
                    "Dictionary: JMdict by the Electronic Dictionary Research and Development Group, CC BY-SA 4.0. " +
                    "Word splitting: Sudachi (Works Applications).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun RadioRow(title: String, subtitle: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            Text(title, style = MaterialTheme.typography.bodyLarge, color = color)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
