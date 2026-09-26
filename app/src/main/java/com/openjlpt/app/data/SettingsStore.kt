package com.openjlpt.app.data

import android.content.Context
import com.openjlpt.core.model.JlptLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _level = MutableStateFlow(
        prefs.getString(KEY_LEVEL, null)?.let { runCatching { JlptLevel.valueOf(it) }.getOrNull() } ?: JlptLevel.N5,
    )
    val level: StateFlow<JlptLevel> = _level.asStateFlow()

    fun setLevel(level: JlptLevel) {
        prefs.edit().putString(KEY_LEVEL, level.name).apply()
        _level.value = level
    }

    private companion object {
        const val KEY_LEVEL = "level"
    }
}
