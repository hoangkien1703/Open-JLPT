package com.openjlpt.app.data

import android.content.Context
import com.openjlpt.core.model.JlptLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioSource { BUILT_IN, PHONE_VOICE }

data class AppSettings(
    /** Built-in recordings ship with the app; the phone voice needs Japanese text-to-speech installed. */
    val audioSource: AudioSource = AudioSource.BUILT_IN,
    val playbackSpeed: Float = 1.0f,
    val autoPlayListening: Boolean = false,
    /** Tap a Japanese word to see its meaning and hear it. */
    val tapWords: Boolean = true,
    /** Allow tapping words before the question is answered (off: no hints while you think). */
    val tapWordsBeforeAnswer: Boolean = false,
    /** Allow tapping words during mock tests (off: like the real exam). */
    val tapWordsInMock: Boolean = false,
    val speakTappedWord: Boolean = true,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _level = MutableStateFlow(
        prefs.getString(KEY_LEVEL, null)?.let { runCatching { JlptLevel.valueOf(it) }.getOrNull() } ?: JlptLevel.N5,
    )
    val level: StateFlow<JlptLevel> = _level.asStateFlow()

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setLevel(level: JlptLevel) {
        prefs.edit().putString(KEY_LEVEL, level.name).apply()
        _level.value = level
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putString(KEY_AUDIO_SOURCE, next.audioSource.name)
            .putFloat(KEY_SPEED, next.playbackSpeed)
            .putBoolean(KEY_AUTO_PLAY, next.autoPlayListening)
            .putBoolean(KEY_TAP_WORDS, next.tapWords)
            .putBoolean(KEY_TAP_BEFORE, next.tapWordsBeforeAnswer)
            .putBoolean(KEY_TAP_MOCK, next.tapWordsInMock)
            .putBoolean(KEY_SPEAK_WORD, next.speakTappedWord)
            .apply()
        _settings.value = next
    }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            audioSource = prefs.getString(KEY_AUDIO_SOURCE, null)
                ?.let { runCatching { AudioSource.valueOf(it) }.getOrNull() } ?: defaults.audioSource,
            playbackSpeed = prefs.getFloat(KEY_SPEED, defaults.playbackSpeed),
            autoPlayListening = prefs.getBoolean(KEY_AUTO_PLAY, defaults.autoPlayListening),
            tapWords = prefs.getBoolean(KEY_TAP_WORDS, defaults.tapWords),
            tapWordsBeforeAnswer = prefs.getBoolean(KEY_TAP_BEFORE, defaults.tapWordsBeforeAnswer),
            tapWordsInMock = prefs.getBoolean(KEY_TAP_MOCK, defaults.tapWordsInMock),
            speakTappedWord = prefs.getBoolean(KEY_SPEAK_WORD, defaults.speakTappedWord),
        )
    }

    private companion object {
        const val KEY_LEVEL = "level"
        const val KEY_AUDIO_SOURCE = "audio_source"
        const val KEY_SPEED = "playback_speed"
        const val KEY_AUTO_PLAY = "auto_play_listening"
        const val KEY_TAP_WORDS = "tap_words"
        const val KEY_TAP_BEFORE = "tap_words_before_answer"
        const val KEY_TAP_MOCK = "tap_words_in_mock"
        const val KEY_SPEAK_WORD = "speak_tapped_word"
    }
}
