package com.openjlpt.app.data

import android.content.res.AssetManager
import com.openjlpt.core.glossary.Glossary
import com.openjlpt.core.model.JlptLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Loads the generated word glossary for a level (assets/glossary/<level>.json), if the build has one. */
class GlossaryRepository(private val assets: AssetManager) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<JlptLevel, Glossary?>()

    suspend fun load(level: JlptLevel): Glossary? = mutex.withLock {
        if (level in cache) return@withLock cache[level]
        val glossary = withContext(Dispatchers.IO) {
            runCatching {
                assets.open("glossary/${level.label.lowercase()}.json").bufferedReader().use { Glossary.parse(it.readText()) }
            }.getOrNull()
        }
        cache[level] = glossary
        glossary
    }
}
