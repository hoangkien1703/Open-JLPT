package com.openjlpt.core.session

import com.openjlpt.core.format.LevelFormat
import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.Passage
import com.openjlpt.core.model.Question
import com.openjlpt.core.model.QuestionBank
import com.openjlpt.core.model.QuestionType
import com.openjlpt.core.model.Section
import com.openjlpt.core.model.TestMode
import kotlin.random.Random

/** A question as shown in a session, with its choices in display order. */
data class SessionQuestion(
    val question: Question,
    val passage: Passage?,
    /** choiceOrder[displayedIndex] = index into question.choices. */
    val choiceOrder: List<Int>,
) {
    val displayedChoices: List<String> get() = choiceOrder.map { question.choices[it] }
    val correctDisplayedIndex: Int get() = choiceOrder.indexOf(question.answer)
    fun originalIndex(displayedIndex: Int): Int = choiceOrder[displayedIndex]
}

data class SessionPart(
    val japanese: String,
    val english: String,
    /** Time limit for the part, or null for untimed practice. */
    val minutes: Int?,
    val questions: List<SessionQuestion>,
)

data class TestSession(
    val level: JlptLevel,
    val mode: TestMode,
    val title: String,
    val section: Section?,
    val parts: List<SessionPart>,
) {
    val questions: List<SessionQuestion> get() = parts.flatMap { it.questions }
}

object SessionBuilder {

    fun practice(
        bank: QuestionBank,
        section: Section,
        type: QuestionType?,
        count: Int,
        random: Random = Random.Default,
    ): TestSession {
        val pool = if (type != null) bank.byType(type) else bank.bySection(section)
        val picked = pick(pool, count, random)
        val title = type?.english ?: section.english
        return TestSession(
            level = bank.level,
            mode = TestMode.PRACTICE,
            title = title,
            section = section,
            parts = listOf(SessionPart(section.japanese, section.english, null, picked.map { toSession(bank, it, random) })),
        )
    }

    /**
     * A full-format mock test: every paper of the level with its official
     * timing, and questions drawn per question type up to the official count.
     */
    fun mock(bank: QuestionBank, format: LevelFormat, random: Random = Random.Default): TestSession {
        val parts = format.parts.map { part ->
            val questions = part.items.flatMap { (type, officialCount) ->
                pick(bank.byType(type), officialCount, random)
            }
            SessionPart(part.japanese, part.english, part.minutes, questions.map { toSession(bank, it, random) })
        }
        return TestSession(bank.level, TestMode.MOCK, "${bank.level.label} mock test", null, parts)
    }

    fun review(bank: QuestionBank, questionIds: List<String>, random: Random = Random.Default): TestSession {
        val questions = questionIds.mapNotNull(bank::question)
            .sortedBy { it.passageId ?: it.id }
        return TestSession(
            level = bank.level,
            mode = TestMode.REVIEW,
            title = "Review mistakes",
            section = null,
            parts = listOf(SessionPart("復習", "Review", null, questions.map { toSession(bank, it, random) })),
        )
    }

    private fun toSession(bank: QuestionBank, question: Question, random: Random) = SessionQuestion(
        question = question,
        passage = bank.passageFor(question),
        choiceOrder = question.choices.indices.shuffled(random),
    )

    /**
     * Picks up to [count] questions at random. Questions that share a passage
     * are kept together and in their authored order, as on the real paper.
     */
    internal fun pick(pool: List<Question>, count: Int, random: Random): List<Question> {
        if (count <= 0 || pool.isEmpty()) return emptyList()
        val groups = pool.groupBy { it.passageId ?: "#${it.id}" }.values.shuffled(random)
        val picked = mutableListOf<Question>()
        for (group in groups) {
            if (picked.size + group.size <= count) picked += group
            if (picked.size == count) break
        }
        // Every passage is longer than the count: take one whole passage anyway.
        if (picked.isEmpty()) picked += groups.first()
        return picked
    }
}
