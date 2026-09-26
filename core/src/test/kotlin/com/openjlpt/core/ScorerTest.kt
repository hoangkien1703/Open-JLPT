package com.openjlpt.core

import com.openjlpt.core.format.JlptFormat
import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.Question
import com.openjlpt.core.model.QuestionType
import com.openjlpt.core.scoring.GradedAnswer
import com.openjlpt.core.scoring.Scorer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScorerTest {

    private fun q(id: String, type: QuestionType) =
        Question(id = id, type = type, prompt = "p", choices = listOf("a", "b", "c", "d"), answer = 0, explanation = "e")

    private fun answers(type: QuestionType, correct: Int, total: Int) = (0 until total).map {
        GradedAnswer(q("$type-$it", type), if (it < correct) 0 else 1)
    }

    @Test
    fun n5CombinesLanguageAndReadingInOneSection() {
        val format = JlptFormat.forLevel(JlptLevel.N5)
        val result = Scorer.score(
            format,
            answers(QuestionType.KANJI_READING, 10, 10) + answers(QuestionType.TASK, 5, 10),
        )
        assertEquals(2, result.sections.size)
        assertEquals(120, result.sections[0].score)
        assertEquals(30, result.sections[1].score)
        assertEquals(150, result.total)
        assertTrue(result.passed)
    }

    @Test
    fun failingOneSectionMinimumFailsTheTest() {
        val format = JlptFormat.forLevel(JlptLevel.N3)
        val result = Scorer.score(
            format,
            answers(QuestionType.GRAMMAR_FORM, 10, 10) +
                answers(QuestionType.SHORT_PASSAGE, 10, 10) +
                answers(QuestionType.TASK, 2, 10),
        )
        assertEquals(60, result.sections[0].score)
        assertEquals(60, result.sections[1].score)
        assertEquals(12, result.sections[2].score)
        assertTrue(result.total >= format.passMark)
        assertFalse(result.allSectionsMeetMinimum)
        assertFalse(result.passed)
    }

    @Test
    fun unansweredCountsAsWrong() {
        val format = JlptFormat.forLevel(JlptLevel.N1)
        val result = Scorer.score(format, listOf(GradedAnswer(q("x", QuestionType.CONTEXT), null)))
        assertEquals(0, result.total)
        assertFalse(result.passed)
    }

    @Test
    fun formatsMatchPublishedTotals() {
        for (level in JlptLevel.entries) {
            assertEquals(180, JlptFormat.forLevel(level).maxScore)
        }
        assertEquals(90, JlptFormat.forLevel(JlptLevel.N5).totalMinutes)
        assertEquals(115, JlptFormat.forLevel(JlptLevel.N4).totalMinutes)
        assertEquals(140, JlptFormat.forLevel(JlptLevel.N3).totalMinutes)
        assertEquals(155, JlptFormat.forLevel(JlptLevel.N2).totalMinutes)
        assertEquals(165, JlptFormat.forLevel(JlptLevel.N1).totalMinutes)
    }
}
