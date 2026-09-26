package com.openjlpt.core.format

import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.QuestionType
import com.openjlpt.core.model.QuestionType.CONTEXT
import com.openjlpt.core.model.QuestionType.GRAMMAR_FORM
import com.openjlpt.core.model.QuestionType.INFO_RETRIEVAL
import com.openjlpt.core.model.QuestionType.INTEGRATED_LISTENING
import com.openjlpt.core.model.QuestionType.INTEGRATED_READING
import com.openjlpt.core.model.QuestionType.KANJI_READING
import com.openjlpt.core.model.QuestionType.LONG_PASSAGE
import com.openjlpt.core.model.QuestionType.MID_PASSAGE
import com.openjlpt.core.model.QuestionType.ORTHOGRAPHY
import com.openjlpt.core.model.QuestionType.PARAPHRASE
import com.openjlpt.core.model.QuestionType.POINT
import com.openjlpt.core.model.QuestionType.QUICK_RESPONSE
import com.openjlpt.core.model.QuestionType.SENTENCE_ORDER
import com.openjlpt.core.model.QuestionType.SHORT_PASSAGE
import com.openjlpt.core.model.QuestionType.SUMMARY
import com.openjlpt.core.model.QuestionType.TASK
import com.openjlpt.core.model.QuestionType.TEXT_GRAMMAR
import com.openjlpt.core.model.QuestionType.THEMATIC
import com.openjlpt.core.model.QuestionType.USAGE
import com.openjlpt.core.model.QuestionType.UTTERANCE
import com.openjlpt.core.model.QuestionType.WORD_FORMATION
import com.openjlpt.core.model.Section

/** One timed paper of the test, e.g. 言語知識（文字・語彙）. */
data class TestPart(
    val japanese: String,
    val english: String,
    val minutes: Int,
    /** Question types in the order they appear on the paper, with the official question count. */
    val items: List<Pair<QuestionType, Int>>,
) {
    val types: List<QuestionType> get() = items.map { it.first }
    val officialQuestionCount: Int get() = items.sumOf { it.second }
}

/** One of the score sections (得点区分) reported on the JLPT score report. */
data class ScoreSection(
    val japanese: String,
    val english: String,
    val sections: Set<Section>,
    val maxScore: Int,
    /** Sectional pass mark (基準点). */
    val minimum: Int,
)

data class LevelFormat(
    val level: JlptLevel,
    val parts: List<TestPart>,
    val scoreSections: List<ScoreSection>,
    /** Overall pass mark (合格点) out of 180. */
    val passMark: Int,
) {
    val totalMinutes: Int get() = parts.sumOf { it.minutes }
    val maxScore: Int get() = scoreSections.sumOf { it.maxScore }
}

/**
 * The structure of each level's test: papers, timings, question counts per
 * question type, score sections and pass marks, following the published
 * JLPT test format (2020 revision onward).
 */
object JlptFormat {

    private val languageAndReadingN5N4 = ScoreSection(
        "言語知識（文字・語彙・文法）・読解", "Language knowledge & reading",
        setOf(Section.VOCABULARY, Section.GRAMMAR, Section.READING), maxScore = 120, minimum = 38,
    )
    private val listening = ScoreSection("聴解", "Listening", setOf(Section.LISTENING), maxScore = 60, minimum = 19)
    private val languageKnowledge = ScoreSection(
        "言語知識（文字・語彙・文法）", "Language knowledge",
        setOf(Section.VOCABULARY, Section.GRAMMAR), maxScore = 60, minimum = 19,
    )
    private val reading = ScoreSection("読解", "Reading", setOf(Section.READING), maxScore = 60, minimum = 19)

    private val upperScoreSections = listOf(languageKnowledge, reading, listening)

    fun forLevel(level: JlptLevel): LevelFormat = when (level) {
        JlptLevel.N5 -> LevelFormat(
            level,
            parts = listOf(
                TestPart(
                    "言語知識（文字・語彙）", "Vocabulary", 20,
                    listOf(KANJI_READING to 7, ORTHOGRAPHY to 5, CONTEXT to 6, PARAPHRASE to 3),
                ),
                TestPart(
                    "言語知識（文法）・読解", "Grammar & reading", 40,
                    listOf(
                        GRAMMAR_FORM to 9, SENTENCE_ORDER to 4, TEXT_GRAMMAR to 4,
                        SHORT_PASSAGE to 2, MID_PASSAGE to 2, INFO_RETRIEVAL to 1,
                    ),
                ),
                TestPart("聴解", "Listening", 30, listOf(TASK to 7, POINT to 6, UTTERANCE to 5, QUICK_RESPONSE to 6)),
            ),
            scoreSections = listOf(languageAndReadingN5N4, listening),
            passMark = 80,
        )

        JlptLevel.N4 -> LevelFormat(
            level,
            parts = listOf(
                TestPart(
                    "言語知識（文字・語彙）", "Vocabulary", 25,
                    listOf(KANJI_READING to 7, ORTHOGRAPHY to 5, CONTEXT to 8, PARAPHRASE to 4, USAGE to 4),
                ),
                TestPart(
                    "言語知識（文法）・読解", "Grammar & reading", 55,
                    listOf(
                        GRAMMAR_FORM to 13, SENTENCE_ORDER to 4, TEXT_GRAMMAR to 4,
                        SHORT_PASSAGE to 3, MID_PASSAGE to 3, INFO_RETRIEVAL to 2,
                    ),
                ),
                TestPart("聴解", "Listening", 35, listOf(TASK to 8, POINT to 7, UTTERANCE to 5, QUICK_RESPONSE to 8)),
            ),
            scoreSections = listOf(languageAndReadingN5N4, listening),
            passMark = 90,
        )

        JlptLevel.N3 -> LevelFormat(
            level,
            parts = listOf(
                TestPart(
                    "言語知識（文字・語彙）", "Vocabulary", 30,
                    listOf(KANJI_READING to 8, ORTHOGRAPHY to 6, CONTEXT to 11, PARAPHRASE to 5, USAGE to 5),
                ),
                TestPart(
                    "言語知識（文法）・読解", "Grammar & reading", 70,
                    listOf(
                        GRAMMAR_FORM to 13, SENTENCE_ORDER to 5, TEXT_GRAMMAR to 5,
                        SHORT_PASSAGE to 4, MID_PASSAGE to 6, LONG_PASSAGE to 4, INFO_RETRIEVAL to 2,
                    ),
                ),
                TestPart(
                    "聴解", "Listening", 40,
                    listOf(TASK to 6, POINT to 6, SUMMARY to 3, UTTERANCE to 4, QUICK_RESPONSE to 9),
                ),
            ),
            scoreSections = upperScoreSections,
            passMark = 95,
        )

        JlptLevel.N2 -> LevelFormat(
            level,
            parts = listOf(
                TestPart(
                    "言語知識（文字・語彙・文法）・読解", "Language knowledge & reading", 105,
                    listOf(
                        KANJI_READING to 5, ORTHOGRAPHY to 5, WORD_FORMATION to 3, CONTEXT to 7,
                        PARAPHRASE to 5, USAGE to 5,
                        GRAMMAR_FORM to 12, SENTENCE_ORDER to 5, TEXT_GRAMMAR to 5,
                        SHORT_PASSAGE to 5, MID_PASSAGE to 9, INTEGRATED_READING to 2, THEMATIC to 3,
                        INFO_RETRIEVAL to 2,
                    ),
                ),
                TestPart(
                    "聴解", "Listening", 50,
                    listOf(TASK to 5, POINT to 6, SUMMARY to 5, QUICK_RESPONSE to 12, INTEGRATED_LISTENING to 4),
                ),
            ),
            scoreSections = upperScoreSections,
            passMark = 90,
        )

        JlptLevel.N1 -> LevelFormat(
            level,
            parts = listOf(
                TestPart(
                    "言語知識（文字・語彙・文法）・読解", "Language knowledge & reading", 110,
                    listOf(
                        KANJI_READING to 6, CONTEXT to 7, PARAPHRASE to 6, USAGE to 6,
                        GRAMMAR_FORM to 10, SENTENCE_ORDER to 5, TEXT_GRAMMAR to 5,
                        SHORT_PASSAGE to 4, MID_PASSAGE to 9, LONG_PASSAGE to 4, INTEGRATED_READING to 2,
                        THEMATIC to 4, INFO_RETRIEVAL to 2,
                    ),
                ),
                TestPart(
                    "聴解", "Listening", 55,
                    listOf(TASK to 5, POINT to 6, SUMMARY to 5, QUICK_RESPONSE to 11, INTEGRATED_LISTENING to 3),
                ),
            ),
            scoreSections = upperScoreSections,
            passMark = 100,
        )
    }

    /** Question types that appear on the given level's test, in paper order. */
    fun typesFor(level: JlptLevel): List<QuestionType> = forLevel(level).parts.flatMap { it.types }
}
