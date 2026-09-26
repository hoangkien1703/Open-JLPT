package com.openjlpt.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openjlpt.app.data.AppContainer
import com.openjlpt.app.data.db.AttemptEntity
import com.openjlpt.app.data.db.SectionStat
import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.QuestionBank
import com.openjlpt.core.model.Section
import com.openjlpt.core.model.TestMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val level: JlptLevel = JlptLevel.N5,
    val bank: QuestionBank? = null,
    val stats: Map<Section, SectionStat> = emptyMap(),
    val mistakeCount: Int = 0,
    val lastMock: AttemptEntity? = null,
    val attemptCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<HomeUiState> = container.settings.level.flatMapLatest { level ->
        combine(
            flow { emit(container.questions.bank(level)) },
            container.history.sectionStats(level),
            container.history.mistakeIds(level),
            container.history.attempts(level),
        ) { bank, stats, mistakes, attempts ->
            HomeUiState(
                level = level,
                bank = bank,
                stats = stats,
                mistakeCount = mistakes.size,
                lastMock = attempts.firstOrNull { it.mode == TestMode.MOCK.name },
                attemptCount = attempts.size,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(level = container.settings.level.value))

    fun selectLevel(level: JlptLevel) = container.settings.setLevel(level)
}
