package com.openjlpt.core.scoring

import com.openjlpt.core.format.LevelFormat
import com.openjlpt.core.format.ScoreSection
import com.openjlpt.core.model.Question
import kotlin.math.roundToInt

/** A question and the choice the learner picked (index into question.choices), or null if unanswered. */
data class GradedAnswer(val question: Question, val selected: Int?) {
    val isCorrect: Boolean get() = selected == question.answer
}

data class SectionScore(
    val section: ScoreSection,
    val correct: Int,
    val total: Int,
    /** Scaled score out of section.maxScore. */
    val score: Int,
) {
    val meetsMinimum: Boolean get() = score >= section.minimum
}

data class TestScore(
    val sections: List<SectionScore>,
    val passMark: Int,
) {
    val total: Int get() = sections.sumOf { it.score }
    val maxScore: Int get() = sections.sumOf { it.section.maxScore }
    val correct: Int get() = sections.sumOf { it.correct }
    val questionCount: Int get() = sections.sumOf { it.total }
    val allSectionsMeetMinimum: Boolean get() = sections.all { it.meetsMinimum }
    val passed: Boolean get() = total >= passMark && allSectionsMeetMinimum
}

object Scorer {
    /**
     * Estimates JLPT scaled scores. The real test uses item-response equating;
     * here each score section is the share of correct answers scaled to the
     * section's maximum, which is close enough to judge readiness.
     */
    fun score(format: LevelFormat, answers: List<GradedAnswer>): TestScore {
        val sections = format.scoreSections.map { scoreSection ->
            val inSection = answers.filter { it.question.section in scoreSection.sections }
            val correct = inSection.count { it.isCorrect }
            val scaled = if (inSection.isEmpty()) 0 else (correct.toDouble() / inSection.size * scoreSection.maxScore).roundToInt()
            SectionScore(scoreSection, correct, inSection.size, scaled)
        }
        return TestScore(sections, format.passMark)
    }

    fun accuracyPercent(correct: Int, total: Int): Int =
        if (total == 0) 0 else (correct * 100.0 / total).roundToInt()
}
