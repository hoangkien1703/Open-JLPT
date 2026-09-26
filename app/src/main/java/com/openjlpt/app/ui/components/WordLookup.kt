@file:OptIn(ExperimentalMaterial3Api::class)

package com.openjlpt.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openjlpt.app.audio.ListeningPlayer
import com.openjlpt.app.data.AppContainer
import com.openjlpt.core.glossary.Glossary
import com.openjlpt.core.glossary.GlossaryWord
import com.openjlpt.core.model.JlptLevel

/** The loaded glossary and what to do when a word is tapped. */
class WordLookup(val glossary: Glossary, val onWord: (GlossaryWord) -> Unit)

val LocalWordLookup = staticCompositionLocalOf<WordLookup?> { null }

/** Whether Japanese text in this part of the screen can be tapped. Off unless a screen turns it on. */
val LocalWordTapEnabled = compositionLocalOf { false }

/**
 * Loads the level's glossary and shows the word pop-up. Screens turn tapping on for the parts
 * where it is allowed with [LocalWordTapEnabled].
 */
@Composable
fun WordLookupHost(
    container: AppContainer,
    level: JlptLevel?,
    player: ListeningPlayer,
    content: @Composable () -> Unit,
) {
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val glossary by produceState<Glossary?>(null, level) { value = level?.let { container.glossary.load(it) } }
    var word by remember { mutableStateOf<GlossaryWord?>(null) }
    val speak by rememberUpdatedState(settings.speakTappedWord)
    val lookup = remember(glossary, settings.tapWords) {
        glossary?.takeIf { settings.tapWords }?.let { g ->
            WordLookup(g) { tapped ->
                word = tapped
                if (speak) player.playWord(tapped)
            }
        }
    }
    CompositionLocalProvider(LocalWordLookup provides lookup) { content() }
    word?.let { current ->
        WordSheet(current, onPlay = { player.playWord(current) }, onDismiss = { word = null })
    }
}

/** Adds a tap target for every glossary word in [text] (already free of markup). */
fun withWordLinks(base: AnnotatedString, lookup: WordLookup, text: String, pressed: Color): AnnotatedString {
    val spans = lookup.glossary.spansFor(text)
    if (spans.isEmpty() || text.length != base.length) return base
    return buildAnnotatedString {
        append(base)
        spans.forEach { span ->
            addLink(
                LinkAnnotation.Clickable(
                    tag = "word:${span.start}",
                    styles = TextLinkStyles(pressedStyle = SpanStyle(background = pressed)),
                ) { lookup.onWord(span.word) },
                span.start,
                span.end,
            )
        }
    }
}

@Composable
private fun WordSheet(word: GlossaryWord, onPlay: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (word.reading.isNotEmpty()) {
                        Text(word.reading, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(word.headword, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
                FilledTonalIconButton(onClick = onPlay) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Say the word")
                }
            }
            word.meanings.forEachIndexed { i, meaning ->
                Row {
                    Text("${i + 1}.", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(meaning, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Text(
                "Meanings from JMdict (EDRDG). A word written in kana can have several meanings; check the sentence.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
