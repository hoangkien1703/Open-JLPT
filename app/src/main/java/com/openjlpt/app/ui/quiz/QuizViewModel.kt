package com.openjlpt.app.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openjlpt.app.data.AppContainer
import com.openjlpt.core.format.JlptFormat
import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.QuestionType
import com.openjlpt.core.model.Section
import com.openjlpt.core.model.TestMode
import com.openjlpt.core.session.SessionBuilder
import com.openjlpt.core.session.SessionPart
import com.openjlpt.core.session.SessionQuestion
import com.openjlpt.core.session.TestSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class QuizUiState(
    val session: TestSession? = null,
    val loading: Boolean = true,
    val partIndex: Int = 0,
    val index: Int = 0,
    /** Question id to the displayed index of the chosen answer. */
    val selections: Map<String, Int> = emptyMap(),
    /** Practice: questions whose answer has been checked and locked. */
    val checked: Set<String> = emptySet(),
    val remainingSeconds: Int? = null,
    /** Mock test: showing the intro page before a paper starts. */
    val awaitingPartStart: Boolean = false,
    val timeUp: Boolean = false,
    val saving: Boolean = false,
    val savedAttemptId: Long? = null,
) {
    val isMock: Boolean get() = session?.mode == TestMode.MOCK
    val part: SessionPart? get() = session?.parts?.getOrNull(partIndex)
    val current: SessionQuestion? get() = part?.questions?.getOrNull(index)
    val isLastInPart: Boolean get() = part?.let { index == it.questions.lastIndex } ?: true
    val isLastPart: Boolean get() = session?.let { partIndex == it.parts.lastIndex } ?: true
    val unansweredInPart: Int get() = part?.questions?.count { it.question.id !in selections } ?: 0
    val answeredCount: Int get() = selections.size
    val questionCount: Int get() = session?.questions?.size ?: 0
}

class QuizViewModel(
    handle: SavedStateHandle,
    private val container: AppContainer,
) : ViewModel() {

    private val mode = TestMode.valueOf(handle.get<String>("mode") ?: TestMode.PRACTICE.name)
    private val level = JlptLevel.valueOf(handle.get<String>("level") ?: JlptLevel.N5.name)
    private val section = handle.get<String>("section")?.takeIf { it.isNotEmpty() }?.let(Section::valueOf)
    private val type = handle.get<String>("type")?.takeIf { it.isNotEmpty() }?.let(QuestionType::valueOf)
    private val count = handle.get<Int>("count")?.takeIf { it > 0 } ?: DEFAULT_COUNT

    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private val startedAt = System.currentTimeMillis()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val bank = container.questions.bank(level)
            val session = when (mode) {
                TestMode.MOCK -> SessionBuilder.mock(bank, JlptFormat.forLevel(level))
                TestMode.REVIEW -> SessionBuilder.review(bank, container.history.mistakeIds(level).first())
                TestMode.PRACTICE -> SessionBuilder.practice(bank, section ?: Section.VOCABULARY, type, count)
            }
            // Drop papers with no questions so an incomplete bank never shows an empty page.
            val trimmed = session.copy(parts = session.parts.filter { it.questions.isNotEmpty() })
            _state.update {
                it.copy(session = trimmed, loading = false, awaitingPartStart = mode == TestMode.MOCK)
            }
        }
    }

    fun select(displayedIndex: Int) {
        val s = _state.value
        val q = s.current ?: return
        val id = q.question.id
        if (!s.isMock && id in s.checked) return
        _state.update {
            it.copy(
                selections = it.selections + (id to displayedIndex),
                // Practice gives instant feedback, like Migii's practice mode.
                checked = if (it.isMock) it.checked else it.checked + id,
            )
        }
    }

    fun next() = _state.update { s ->
        val size = s.part?.questions?.size ?: 0
        if (s.index < size - 1) s.copy(index = s.index + 1) else s
    }

    fun previous() = _state.update { s -> if (s.index > 0) s.copy(index = s.index - 1) else s }

    fun goTo(index: Int) = _state.update { s ->
        val size = s.part?.questions?.size ?: 0
        if (index in 0 until size) s.copy(index = index) else s
    }

    fun startPart() {
        val minutes = _state.value.part?.minutes
        _state.update { it.copy(awaitingPartStart = false, remainingSeconds = minutes?.times(60)) }
        if (minutes != null) startTimer()
    }

    /** Mock test: hands in the current paper and moves to the next, or finishes. */
    fun submitPart() {
        timerJob?.cancel()
        val s = _state.value
        if (s.isLastPart) {
            finish()
        } else {
            _state.update {
                it.copy(partIndex = it.partIndex + 1, index = 0, awaitingPartStart = true, remainingSeconds = null, timeUp = false)
            }
        }
    }

    fun acknowledgeTimeUp() {
        _state.update { it.copy(timeUp = false) }
        submitPart()
    }

    fun finish() {
        val s = _state.value
        val session = s.session ?: return
        if (s.saving || s.savedAttemptId != null) return
        timerJob?.cancel()
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val byId = session.questions.associateBy { it.question.id }
            val original = s.selections.mapNotNull { (id, displayed) ->
                byId[id]?.let { id to it.originalIndex(displayed) }
            }.toMap()
            val attemptId = container.history.save(session, original, startedAt, System.currentTimeMillis())
            _state.update { it.copy(saving = false, savedAttemptId = attemptId) }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                val remaining = (_state.value.remainingSeconds ?: return@launch) - 1
                if (remaining <= 0) {
                    _state.update { it.copy(remainingSeconds = 0, timeUp = true) }
                    return@launch
                }
                _state.update { it.copy(remainingSeconds = remaining) }
            }
        }
    }

    private companion object {
        const val DEFAULT_COUNT = 20
    }
}
