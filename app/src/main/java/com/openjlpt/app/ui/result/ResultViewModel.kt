package com.openjlpt.app.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openjlpt.app.data.AppContainer
import com.openjlpt.app.data.db.AttemptEntity
import com.openjlpt.core.format.JlptFormat
import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.Passage
import com.openjlpt.core.model.Question
import com.openjlpt.core.model.TestMode
import com.openjlpt.core.scoring.GradedAnswer
import com.openjlpt.core.scoring.Scorer
import com.openjlpt.core.scoring.TestScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewItem(
    val number: Int,
    val question: Question,
    val passage: Passage?,
    /** Index into question.choices, or null if unanswered. */
    val selected: Int?,
) {
    val isCorrect: Boolean get() = selected == question.answer
}

data class ResultUiState(
    val loading: Boolean = true,
    val attempt: AttemptEntity? = null,
    val score: TestScore? = null,
    val items: List<ReviewItem> = emptyList(),
)

class ResultViewModel(private val container: AppContainer, private val attemptId: Long) : ViewModel() {
    private val _state = MutableStateFlow(ResultUiState())
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val attempt = container.history.attempt(attemptId)
            if (attempt == null) {
                _state.value = ResultUiState(loading = false)
                return@launch
            }
            val level = JlptLevel.valueOf(attempt.level)
            val bank = container.questions.bank(level)
            val items = container.history.answers(attemptId).mapIndexedNotNull { i, answer ->
                // A question removed from a later version of the bank is skipped.
                bank.question(answer.questionId)?.let { q ->
                    ReviewItem(i + 1, q, bank.passageFor(q), answer.selected.takeIf { it >= 0 })
                }
            }
            val score = if (attempt.mode == TestMode.MOCK.name) {
                Scorer.score(JlptFormat.forLevel(level), items.map { GradedAnswer(it.question, it.selected) })
            } else {
                null
            }
            _state.value = ResultUiState(loading = false, attempt = attempt, score = score, items = items)
        }
    }
}
