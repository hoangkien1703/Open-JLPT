package com.openjlpt.core

import com.openjlpt.core.data.QuestionBankParser
import com.openjlpt.core.format.JlptFormat
import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.QuestionBank
import com.openjlpt.core.model.QuestionType
import com.openjlpt.core.model.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/** Checks every question bank the app ships with. */
class QuestionBankContentTest {

    private val root = File(System.getProperty("questionsDir") ?: "../app/src/main/assets/questions")

    /** Levels whose banks must hold enough questions for a full mock test. */
    private val completeLevels = setOf(JlptLevel.N5)

    private val banks: Map<JlptLevel, QuestionBank> by lazy {
        JlptLevel.entries.mapNotNull { level ->
            val dir = File(root, level.label.lowercase())
            val files = dir.listFiles { f -> f.extension == "json" }?.sortedBy { it.name }.orEmpty()
            if (level in completeLevels) assertTrue("No bank files in $dir", files.isNotEmpty())
            if (files.isEmpty()) null else level to QuestionBankParser.parseLevel(level, files.map { it.readText() })
        }.toMap()
    }

    private val passageTypes = setOf(
        QuestionType.TEXT_GRAMMAR, QuestionType.SHORT_PASSAGE, QuestionType.MID_PASSAGE,
        QuestionType.LONG_PASSAGE, QuestionType.INTEGRATED_READING, QuestionType.THEMATIC,
        QuestionType.INFO_RETRIEVAL,
    )
    private val threeChoiceTypes = setOf(QuestionType.UTTERANCE, QuestionType.QUICK_RESPONSE)
    private val underlinedTypes = setOf(QuestionType.KANJI_READING, QuestionType.ORTHOGRAPHY, QuestionType.PARAPHRASE)

    @Test
    fun everyQuestionIsWellFormed() {
        val problems = mutableListOf<String>()
        val seenIds = mutableSetOf<String>()
        for ((level, bank) in banks) {
            val allowedTypes = JlptFormat.typesFor(level).toSet()
            for (q in bank.questions) {
                val at = "${level.label} ${q.id}"
                if (!seenIds.add(q.id)) problems += "$at: duplicate id"
                if (q.type !in allowedTypes) problems += "$at: ${q.type} is not on the ${level.label} test"
                val expectedChoices = if (q.type in threeChoiceTypes) 3 else 4
                if (q.choices.size != expectedChoices) problems += "$at: expected $expectedChoices choices, got ${q.choices.size}"
                if (q.answer !in q.choices.indices) problems += "$at: answer index ${q.answer} out of range"
                if (q.choices.any { it.isBlank() }) problems += "$at: blank choice"
                if (q.choices.toSet().size != q.choices.size) problems += "$at: duplicate choices"
                if (q.prompt.isBlank() && q.type.section != Section.LISTENING) problems += "$at: blank prompt"
                if (q.explanation.isBlank()) problems += "$at: missing explanation"
                if (q.prompt.split("<u>").size != q.prompt.split("</u>").size) problems += "$at: unbalanced <u> tags"
                if (q.type in underlinedTypes && "<u>" !in q.prompt) problems += "$at: target word not underlined"
                if (q.type == QuestionType.SENTENCE_ORDER && "★" !in q.prompt) problems += "$at: missing ★"
                if (q.type in passageTypes) {
                    if (q.passageId == null) problems += "$at: ${q.type} needs a passage"
                    else if (bank.passages[q.passageId] == null) problems += "$at: unknown passage ${q.passageId}"
                } else if (q.passageId != null) {
                    problems += "$at: ${q.type} should not have a passage"
                }
                if (q.section == Section.LISTENING) {
                    if (q.script.isEmpty()) problems += "$at: listening question without a script"
                    if (q.script.any { it.text.isBlank() }) problems += "$at: blank script line"
                } else if (q.script.isNotEmpty()) {
                    problems += "$at: script on a non-listening question"
                }
            }
            val used = bank.questions.mapNotNull { it.passageId }.toSet()
            bank.passages.keys.filter { it !in used }.forEach { problems += "${level.label}: passage $it is unused" }
        }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun everyLevelCanBuildAFullMockTest() {
        val report = StringBuilder()
        var missing = false
        for ((level, bank) in banks.filterKeys { it in completeLevels }) {
            val format = JlptFormat.forLevel(level)
            for (part in format.parts) for ((type, needed) in part.items) {
                val have = bank.byType(type).size
                if (have < needed) {
                    missing = true
                    report.appendLine("${level.label} ${type.name}: $have of $needed")
                }
            }
        }
        if (missing) fail("Not enough questions for a full mock test:\n$report")
    }

    @Test
    fun answerPositionsAreSpreadOut() {
        // Choices are shuffled at runtime, but authored positions should still vary.
        for ((level, bank) in banks) {
            val counts = bank.questions.groupingBy { it.answer }.eachCount()
            val max = counts.values.maxOrNull() ?: 0
            assertTrue("${level.label}: answers bunched on one position $counts", max <= bank.questions.size * 0.5 + 2)
        }
    }

    @Test
    fun levelsMatchDirectories() {
        for ((level, bank) in banks) assertEquals(level, bank.level)
    }
}
