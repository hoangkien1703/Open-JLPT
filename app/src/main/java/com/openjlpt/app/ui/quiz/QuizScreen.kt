@file:OptIn(ExperimentalMaterial3Api::class)

package com.openjlpt.app.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import com.openjlpt.app.audio.ListeningPlayer
import com.openjlpt.app.data.AppContainer
import com.openjlpt.app.ui.appViewModel
import com.openjlpt.app.ui.components.LoadingBox
import com.openjlpt.app.ui.components.RichText
import com.openjlpt.app.ui.theme.CorrectGreen
import com.openjlpt.app.ui.theme.CorrectGreenContainer
import com.openjlpt.app.ui.theme.WrongRed
import com.openjlpt.app.ui.theme.WrongRedContainer
import com.openjlpt.core.model.Section
import com.openjlpt.core.session.SessionQuestion

@Composable
fun QuizScreen(container: AppContainer, onExit: () -> Unit, onFinished: (Long) -> Unit) {
    val vm = appViewModel { QuizViewModel(createSavedStateHandle(), container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val player = remember { ListeningPlayer(context) }
    DisposableEffect(player) { onDispose { player.shutdown() } }

    var confirmExit by remember { mutableStateOf(false) }
    var confirmSubmit by remember { mutableStateOf(false) }
    var showNavigator by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedAttemptId) { state.savedAttemptId?.let(onFinished) }
    // Stop any audio when moving to another question.
    LaunchedEffect(state.partIndex, state.index) { player.stop() }

    val requestExit: () -> Unit = {
        if (state.answeredCount > 0 && state.savedAttemptId == null) {
            confirmExit = true
        } else {
            onExit()
        }
    }
    BackHandler(onBack = requestExit)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.session?.title ?: "", style = MaterialTheme.typography.titleMedium)
                        state.part?.let { part ->
                            if (state.isMock) Text(part.japanese, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = requestExit) { Icon(Icons.Filled.Close, contentDescription = "Exit") }
                },
                actions = {
                    state.remainingSeconds?.let { TimerText(it) }
                    if (state.isMock && !state.awaitingPartStart) {
                        IconButton(onClick = { showNavigator = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "All questions")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!state.loading && !state.awaitingPartStart && state.current != null) {
                QuizBottomBar(
                    state = state,
                    onPrevious = vm::previous,
                    onNext = vm::next,
                    onFinishPractice = vm::finish,
                    onSubmitPart = { confirmSubmit = true },
                )
            }
        },
    ) { padding ->
        val current = state.current
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.session?.questions.isNullOrEmpty() -> EmptyState(Modifier.padding(padding), onExit)
            state.awaitingPartStart -> PartIntro(state, vm::startPart, Modifier.padding(padding))
            current != null -> Column(Modifier.padding(padding)) {
                val size = state.part?.questions?.size ?: 1
                LinearProgressIndicator(
                    progress = { (state.index + 1f) / size },
                    modifier = Modifier.fillMaxWidth(),
                )
                QuestionView(
                    question = current,
                    number = state.index + 1,
                    total = size,
                    selected = state.selections[current.question.id],
                    revealed = !state.isMock && current.question.id in state.checked,
                    allowTranscript = !state.isMock,
                    player = player,
                    onSelect = vm::select,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Leave this test?") },
            text = {
                Text(
                    if (state.isMock) "Your answers will be lost." else "You can save the questions you've answered so far, or leave without saving.",
                )
            },
            confirmButton = {
                if (state.isMock) {
                    TextButton(onClick = { confirmExit = false; onExit() }) { Text("Leave") }
                } else {
                    TextButton(onClick = { confirmExit = false; vm.finish() }) { Text("Save and finish") }
                }
            },
            dismissButton = {
                Row {
                    if (!state.isMock) TextButton(onClick = { confirmExit = false; onExit() }) { Text("Leave") }
                    TextButton(onClick = { confirmExit = false }) { Text("Stay") }
                }
            },
        )
    }

    if (confirmSubmit) {
        val unanswered = state.unansweredInPart
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text(if (state.isLastPart) "Submit the test?" else "Submit this paper?") },
            text = {
                Text(
                    buildString {
                        if (unanswered > 0) append("$unanswered questions are unanswered. ")
                        append(if (state.isLastPart) "Your score will be calculated." else "You can't come back to this paper.")
                    },
                )
            },
            confirmButton = { TextButton(onClick = { confirmSubmit = false; vm.submitPart() }) { Text("Submit") } },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("Keep working") } },
        )
    }

    if (state.timeUp) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Time's up") },
            text = { Text(if (state.isLastPart) "The test is over. Let's see your score." else "This paper is over. The next one starts when you're ready.") },
            confirmButton = { TextButton(onClick = vm::acknowledgeTimeUp) { Text("Continue") } },
        )
    }

    if (showNavigator) {
        AlertDialog(
            onDismissRequest = { showNavigator = false },
            title = { Text("Questions") },
            text = {
                val questions = state.part?.questions.orEmpty()
                LazyVerticalGrid(columns = GridCells.Adaptive(48.dp), modifier = Modifier.heightIn(max = 360.dp)) {
                    itemsIndexed(questions) { i, q ->
                        val answered = q.question.id in state.selections
                        Surface(
                            shape = CircleShape,
                            color = if (answered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (i == state.index) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(40.dp)
                                .clickable { vm.goTo(i); showNavigator = false },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "${i + 1}",
                                    color = if (answered) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNavigator = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun TimerText(seconds: Int) {
    val color = if (seconds < 60) WrongRed else MaterialTheme.colorScheme.onSurface
    Text(
        "%d:%02d".format(seconds / 60, seconds % 60),
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

@Composable
private fun QuizBottomBar(
    state: QuizUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinishPractice: () -> Unit,
    onSubmitPart: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onPrevious, enabled = state.index > 0, modifier = Modifier.weight(1f)) {
                Text("Previous")
            }
            when {
                state.isMock && state.isLastInPart -> Button(onClick = onSubmitPart, modifier = Modifier.weight(1f)) {
                    Text(if (state.isLastPart) "Submit test" else "Submit paper")
                }
                state.isMock -> Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text("Next") }
                state.isLastInPart -> Button(onClick = onFinishPractice, enabled = !state.saving, modifier = Modifier.weight(1f)) {
                    Text("Finish")
                }
                else -> {
                    val answered = state.current?.question?.id in state.checked
                    Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text(if (answered) "Next" else "Skip") }
                }
            }
        }
    }
}

@Composable
private fun PartIntro(state: QuizUiState, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val part = state.part ?: return
    val first = part.questions.firstOrNull()?.question?.type
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Paper ${state.partIndex + 1} of ${state.session?.parts?.size ?: 1}", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Text(part.japanese, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(part.english, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Text("${part.questions.size} questions · ${part.minutes ?: 0} minutes", style = MaterialTheme.typography.bodyLarge)
        if (first?.section == Section.LISTENING) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Turn up your volume. Each question is read aloud by your phone's Japanese voice; tap Play to hear it.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStart) { Text("Start") }
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onExit: () -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No questions here yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Try another section or level.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onExit) { Text("Back") }
    }
}

@Composable
private fun QuestionView(
    question: SessionQuestion,
    number: Int,
    total: Int,
    selected: Int?,
    revealed: Boolean,
    allowTranscript: Boolean,
    player: ListeningPlayer,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val q = question.question
    var showChoiceText by rememberSaveable(q.id) { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Question $number / $total", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("${q.type.japanese} · ${q.type.english}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(q.type.instruction, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        question.passage?.let { PassageCard(it) }

        if (q.section == Section.LISTENING) {
            ListeningCard(question, player, showTranscript = revealed, allowTranscript = allowTranscript)
        }

        val showPrompt = q.prompt.isNotBlank() &&
            (q.section != Section.LISTENING || !q.type.choicesAudioOnly || revealed)
        if (showPrompt) {
            RichText(q.prompt, style = MaterialTheme.typography.titleMedium)
        }

        val hideChoices = q.type.choicesAudioOnly && !showChoiceText && !revealed
        question.displayedChoices.forEachIndexed { i, choice ->
            ChoiceRow(
                number = i + 1,
                text = if (hideChoices) null else choice,
                state = when {
                    revealed && i == question.correctDisplayedIndex -> ChoiceState.CORRECT
                    revealed && i == selected -> ChoiceState.WRONG
                    i == selected -> ChoiceState.SELECTED
                    else -> ChoiceState.NORMAL
                },
                onClick = { onSelect(i) },
            )
        }
        if (q.type.choicesAudioOnly && !revealed) {
            TextButton(onClick = { showChoiceText = !showChoiceText }) {
                Text(if (showChoiceText) "Hide choice text" else "Show choice text (the real test only speaks them)")
            }
        }

        if (revealed) {
            ExplanationCard(correct = selected == question.correctDisplayedIndex, explanation = q.explanation)
        }
        Spacer(Modifier.height(8.dp))
    }
}

enum class ChoiceState { NORMAL, SELECTED, CORRECT, WRONG }

@Composable
fun ChoiceRow(number: Int, text: String?, state: ChoiceState, onClick: (() -> Unit)?) {
    val (container, border) = when (state) {
        ChoiceState.NORMAL -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outlineVariant
        ChoiceState.SELECTED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        ChoiceState.CORRECT -> CorrectGreenContainer to CorrectGreen
        ChoiceState.WRONG -> WrongRedContainer to WrongRed
    }
    val textColor = if (state == ChoiceState.CORRECT || state == ChoiceState.WRONG) Color(0xFF1B1B1B) else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = BorderStroke(if (state == ChoiceState.NORMAL) 1.dp else 2.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = border, modifier = Modifier.size(28.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$number", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            RichText(
                text ?: "…",
                style = MaterialTheme.typography.bodyLarge.copy(color = textColor),
            )
        }
    }
}
