package com.openjlpt.app.data

import com.openjlpt.app.data.db.AnswerEntity
import com.openjlpt.app.data.db.AttemptEntity
import com.openjlpt.app.data.db.HistoryDao
import com.openjlpt.app.data.db.SectionStat
import com.openjlpt.core.format.JlptFormat
import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.Section
import com.openjlpt.core.model.TestMode
import com.openjlpt.core.scoring.GradedAnswer
import com.openjlpt.core.scoring.Scorer
import com.openjlpt.core.session.TestSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val dao: HistoryDao) {

    fun attempts(level: JlptLevel): Flow<List<AttemptEntity>> = dao.attempts(level.name)

    fun sectionStats(level: JlptLevel): Flow<Map<Section, SectionStat>> =
        dao.sectionStats(level.name).map { stats ->
            stats.mapNotNull { stat -> runCatching { Section.valueOf(stat.section) }.getOrNull()?.let { it to stat } }.toMap()
        }

    fun mistakeIds(level: JlptLevel): Flow<List<String>> = dao.mistakeIds(level.name)

    suspend fun attempt(id: Long): AttemptEntity? = dao.attempt(id)

    suspend fun answers(attemptId: Long): List<AnswerEntity> = dao.answers(attemptId)

    suspend fun clear(level: JlptLevel) = dao.clear(level.name)

    /**
     * Saves a finished session.
     * [selections] maps question id to the chosen index in the question's authored choices.
     * Mock tests record every question (unanswered ones count as wrong); practice
     * sessions record only the questions the learner answered.
     */
    suspend fun save(session: TestSession, selections: Map<String, Int>, startedAt: Long, finishedAt: Long): Long {
        val recorded = session.questions.filter { session.mode == TestMode.MOCK || it.question.id in selections }
        val graded = recorded.map { GradedAnswer(it.question, selections[it.question.id]) }
        val score = if (session.mode == TestMode.MOCK) Scorer.score(JlptFormat.forLevel(session.level), graded) else null
        val attempt = AttemptEntity(
            level = session.level.name,
            mode = session.mode.name,
            title = session.title,
            section = session.section?.name,
            startedAt = startedAt,
            finishedAt = finishedAt,
            correct = graded.count { it.isCorrect },
            total = graded.size,
            score = score?.total,
            maxScore = score?.maxScore,
            passed = score?.passed,
        )
        val answers = graded.mapIndexed { index, answer ->
            AnswerEntity(
                attemptId = 0,
                level = session.level.name,
                questionId = answer.question.id,
                section = answer.question.section.name,
                position = index,
                selected = answer.selected ?: -1,
                isCorrect = answer.isCorrect,
                answeredAt = finishedAt,
            )
        }
        return dao.insertAttemptWithAnswers(attempt, answers)
    }
}
