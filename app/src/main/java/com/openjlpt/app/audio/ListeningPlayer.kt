package com.openjlpt.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.openjlpt.core.model.QuestionType
import com.openjlpt.core.model.ScriptLine
import com.openjlpt.core.session.SessionQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class PlayerState { INITIALIZING, READY, PLAYING, UNAVAILABLE }

/**
 * Reads listening scripts aloud with the device's Japanese text-to-speech voice.
 * Speakers get different pitches so conversations are easier to follow.
 */
class ListeningPlayer(context: Context) {
    private val _state = MutableStateFlow(PlayerState.INITIALIZING)
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var playCount = 0
    private var lastUtteranceId: String? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status -> onInit(status) }

    private fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            _state.value = PlayerState.UNAVAILABLE
            return
        }
        val result = tts.setLanguage(Locale.JAPAN)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            _state.value = PlayerState.UNAVAILABLE
            return
        }
        tts.setSpeechRate(0.9f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                if (utteranceId == lastUtteranceId) _state.value = PlayerState.READY
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = PlayerState.READY
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                _state.value = PlayerState.READY
            }
        })
        _state.value = PlayerState.READY
    }

    fun play(lines: List<ScriptLine>) {
        if (_state.value == PlayerState.UNAVAILABLE || _state.value == PlayerState.INITIALIZING || lines.isEmpty()) return
        tts.stop()
        val run = ++playCount
        lastUtteranceId = "run$run-last"
        _state.value = PlayerState.PLAYING
        lines.forEachIndexed { index, line ->
            tts.setPitch(pitchFor(line.speaker))
            val isLast = index == lines.lastIndex
            val id = if (isLast) "run$run-last" else "run$run-$index"
            tts.speak(line.text, TextToSpeech.QUEUE_ADD, null, id)
            if (!isLast) tts.playSilentUtterance(PAUSE_MS, TextToSpeech.QUEUE_ADD, "run$run-pause$index")
        }
        tts.setPitch(1.0f)
    }

    fun stop() {
        tts.stop()
        if (_state.value == PlayerState.PLAYING) _state.value = PlayerState.READY
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    private fun pitchFor(speaker: String): Float = when (speaker.uppercase()) {
        "M", "M1" -> 0.8f
        "M2" -> 0.7f
        "F", "F1" -> 1.25f
        "F2" -> 1.4f
        else -> 1.0f
    }

    companion object {
        private const val PAUSE_MS = 600L

        /**
         * What is read aloud for a listening question, in the order used on the
         * real test: set-up, question, conversation, question again, then any
         * choices that exist only as audio.
         */
        fun linesFor(sq: SessionQuestion): List<ScriptLine> {
            val q = sq.question
            val lines = mutableListOf<ScriptLine>()
            // For 発話表現 the situation stands in for the picture, so it is shown but not read.
            if (q.type != QuestionType.UTTERANCE) {
                q.situation?.takeIf { it.isNotBlank() }?.let { lines += ScriptLine("N", it) }
            }
            val asksFirst = q.type == QuestionType.TASK || q.type == QuestionType.POINT
            if (asksFirst && q.prompt.isNotBlank()) lines += ScriptLine("N", q.prompt)
            lines += q.script
            if (q.prompt.isNotBlank() && q.type != QuestionType.QUICK_RESPONSE) lines += ScriptLine("N", q.prompt)
            if (q.type.choicesAudioOnly) {
                sq.displayedChoices.forEachIndexed { i, choice -> lines += ScriptLine("N", "${i + 1}、$choice") }
            }
            return lines
        }
    }
}
