package com.openjlpt.core

import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.Passage
import com.openjlpt.core.model.Question
import com.openjlpt.core.model.QuestionBank
import com.openjlpt.core.model.QuestionType
import com.openjlpt.core.model.ScriptLine
import com.openjlpt.core.model.Section
import com.openjlpt.core.session.SessionBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SessionBuilderTest {

    private fun q(id: String, type: QuestionType, passage: String? = null, answer: Int = 0) =
        Question(id, type, "p $id", listOf("a$id", "b$id", "c$id", "d$id"), answer, "e", passageId = passage)

    private val bank = QuestionBank(
        JlptLevel.N5,
        passages = listOf(Passage("p1", text = "one"), Passage("p2", text = "two")),
        questions = (1..20).map { q("k$it", QuestionType.KANJI_READING, answer = it % 4) } +
            listOf(
                q("r1", QuestionType.MID_PASSAGE, "p1"), q("r2", QuestionType.MID_PASSAGE, "p1"),
                q("r3", QuestionType.MID_PASSAGE, "p2"), q("r4", QuestionType.MID_PASSAGE, "p2"),
            ),
    )

    @Test
    fun shuffledChoicesStillPointAtTheRightAnswer() {
        val session = SessionBuilder.practice(bank, Section.VOCABULARY, null, 20, Random(7))
        assertEquals(20, session.questions.size)
        for (sq in session.questions) {
            assertEquals(sq.question.choices[sq.question.answer], sq.displayedChoices[sq.correctDisplayedIndex])
            assertEquals(sq.question.answer, sq.originalIndex(sq.correctDisplayedIndex))
        }
    }

    @Test
    fun audioOnlyChoicesKeepTheirRecordedOrder() {
        val listening = QuestionBank(
            JlptLevel.N5, emptyList(),
            (1..5).map {
                Question("qr$it", QuestionType.QUICK_RESPONSE, "", listOf("a", "b", "c"), 1, "e", script = listOf(ScriptLine("M", "x")))
            },
        )
        repeat(10) { seed ->
            SessionBuilder.practice(listening, Section.LISTENING, null, 5, Random(seed)).questions.forEach {
                assertEquals(listOf(0, 1, 2), it.choiceOrder)
            }
        }
    }

    @Test
    fun passageQuestionsStayTogether() {
        repeat(20) { seed ->
            val picked = SessionBuilder.practice(bank, Section.READING, QuestionType.MID_PASSAGE, 2, Random(seed)).questions
            assertEquals(2, picked.size)
            assertEquals(1, picked.map { it.question.passageId }.toSet().size)
            assertTrue(picked.all { it.passage != null })
        }
    }

    @Test
    fun practiceCountIsCappedByThePool() {
        val session = SessionBuilder.practice(bank, Section.VOCABULARY, QuestionType.KANJI_READING, 50, Random(1))
        assertEquals(20, session.questions.size)
    }
}
