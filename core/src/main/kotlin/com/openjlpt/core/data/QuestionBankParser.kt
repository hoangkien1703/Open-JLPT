package com.openjlpt.core.data

import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.QuestionBank
import com.openjlpt.core.model.QuestionBankFile
import kotlinx.serialization.json.Json

object QuestionBankParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun parseFile(text: String): QuestionBankFile = json.decodeFromString(QuestionBankFile.serializer(), text)

    /** Parses and merges all bank files of one level. */
    fun parseLevel(level: JlptLevel, texts: List<String>): QuestionBank =
        QuestionBank.merge(level, texts.map(::parseFile))
}
