package com.openjlpt.app.data

import android.content.res.AssetManager
import com.openjlpt.core.data.QuestionBankParser
import com.openjlpt.core.model.JlptLevel
import com.openjlpt.core.model.QuestionBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Loads the bundled question banks from `assets/questions/<level>/`.
 * Every JSON file in a level's folder is merged, so new banks can be added
 * by dropping in another file.
 */
class QuestionRepository(private val assets: AssetManager) {
    private val cache = mutableMapOf<JlptLevel, QuestionBank>()
    private val mutex = Mutex()

    suspend fun bank(level: JlptLevel): QuestionBank = mutex.withLock {
        cache[level] ?: withContext(Dispatchers.IO) { load(level) }.also { cache[level] = it }
    }

    private fun load(level: JlptLevel): QuestionBank {
        val dir = "questions/${level.label.lowercase()}"
        val texts = assets.list(dir).orEmpty()
            .filter { it.endsWith(".json") }
            .sorted()
            .map { name -> assets.open("$dir/$name").bufferedReader(Charsets.UTF_8).use { it.readText() } }
        return QuestionBankParser.parseLevel(level, texts)
    }
}
