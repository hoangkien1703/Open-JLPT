package com.openjlpt.core.model

import kotlinx.serialization.Serializable

enum class JlptLevel(val label: String, val summary: String) {
    N5("N5", "Basic Japanese: hiragana, katakana and about 100 kanji"),
    N4("N4", "Everyday Japanese on familiar topics, about 300 kanji"),
    N3("N3", "Japanese used in everyday situations, about 650 kanji"),
    N2("N2", "Everyday and general-topic Japanese, about 1,000 kanji"),
    N1("N1", "Japanese across a broad range of situations, about 2,000 kanji");

    companion object {
        fun fromLabel(label: String): JlptLevel = entries.first { it.label.equals(label, ignoreCase = true) }
    }
}

enum class Section(val japanese: String, val english: String) {
    VOCABULARY("文字・語彙", "Vocabulary"),
    GRAMMAR("文法", "Grammar"),
    READING("読解", "Reading"),
    LISTENING("聴解", "Listening"),
}

/**
 * The question types (問題) used in the JLPT, grouped by section.
 * [instruction] is the Japanese rubric shown above each group of questions.
 */
enum class QuestionType(
    val section: Section,
    val japanese: String,
    val english: String,
    val instruction: String,
    /** In the real test these choices are only spoken, not printed. */
    val choicesAudioOnly: Boolean = false,
) {
    KANJI_READING(
        Section.VOCABULARY, "漢字読み", "Kanji reading",
        "＿＿＿のことばの読み方として最もよいものを、１・２・３・４から一つえらびなさい。",
    ),
    ORTHOGRAPHY(
        Section.VOCABULARY, "表記", "Orthography",
        "＿＿＿のことばを漢字で書くとき、最もよいものを、１・２・３・４から一つえらびなさい。",
    ),
    WORD_FORMATION(
        Section.VOCABULARY, "語形成", "Word formation",
        "（　　）に入れるのに最もよいものを、１・２・３・４から一つえらびなさい。",
    ),
    CONTEXT(
        Section.VOCABULARY, "文脈規定", "Contextual meaning",
        "（　　）に入れるのに最もよいものを、１・２・３・４から一つえらびなさい。",
    ),
    PARAPHRASE(
        Section.VOCABULARY, "言い換え類義", "Paraphrase",
        "＿＿＿の意味が最も近いものを、１・２・３・４から一つえらびなさい。",
    ),
    USAGE(
        Section.VOCABULARY, "用法", "Usage",
        "つぎのことばの使い方として最もよいものを、１・２・３・４から一つえらびなさい。",
    ),
    GRAMMAR_FORM(
        Section.GRAMMAR, "文法形式の判断", "Grammar form",
        "（　　）に入れるのに最もよいものを、１・２・３・４から一つえらびなさい。",
    ),
    SENTENCE_ORDER(
        Section.GRAMMAR, "文の組み立て", "Sentence composition",
        "つぎの文の ★ に入る最もよいものを、１・２・３・４から一つえらびなさい。",
    ),
    TEXT_GRAMMAR(
        Section.GRAMMAR, "文章の文法", "Text grammar",
        "つぎの文章を読んで、文章全体の内容を考えて、（　　）に入る最もよいものを、１・２・３・４から一つえらびなさい。",
    ),
    SHORT_PASSAGE(
        Section.READING, "内容理解（短文）", "Short passage",
        "つぎの文章を読んで、質問に答えなさい。答えは、１・２・３・４から最もよいものを一つえらびなさい。",
    ),
    MID_PASSAGE(
        Section.READING, "内容理解（中文）", "Mid-length passage",
        "つぎの文章を読んで、質問に答えなさい。答えは、１・２・３・４から最もよいものを一つえらびなさい。",
    ),
    LONG_PASSAGE(
        Section.READING, "内容理解（長文）", "Long passage",
        "つぎの文章を読んで、質問に答えなさい。答えは、１・２・３・４から最もよいものを一つえらびなさい。",
    ),
    INTEGRATED_READING(
        Section.READING, "統合理解", "Integrated comprehension",
        "つぎのＡとＢの文章を読んで、質問に答えなさい。答えは、１・２・３・４から最もよいものを一つえらびなさい。",
    ),
    THEMATIC(
        Section.READING, "主張理解（長文）", "Thematic comprehension",
        "つぎの文章を読んで、質問に答えなさい。答えは、１・２・３・４から最もよいものを一つえらびなさい。",
    ),
    INFO_RETRIEVAL(
        Section.READING, "情報検索", "Information retrieval",
        "右のページを見て、質問に答えなさい。答えは、１・２・３・４から最もよいものを一つえらびなさい。",
    ),
    TASK(
        Section.LISTENING, "課題理解", "Task-based comprehension",
        "まず質問を聞いてください。それから話を聞いて、問題用紙の１から４の中から、最もよいものを一つえらんでください。",
    ),
    POINT(
        Section.LISTENING, "ポイント理解", "Point comprehension",
        "まず質問を聞いてください。そのあと、問題用紙を見てください。それから話を聞いて、最もよいものを一つえらんでください。",
    ),
    SUMMARY(
        Section.LISTENING, "概要理解", "Summary comprehension",
        "問題用紙に何も印刷されていません。まず話を聞いてください。それから、質問とせんたくしを聞いて、最もよいものを一つえらんでください。",
        choicesAudioOnly = true,
    ),
    UTTERANCE(
        Section.LISTENING, "発話表現", "Verbal expressions",
        "絵を見ながら質問を聞いてください。やじるし（→）の人は何と言いますか。最もよいものを一つえらんでください。",
        choicesAudioOnly = true,
    ),
    QUICK_RESPONSE(
        Section.LISTENING, "即時応答", "Quick response",
        "問題用紙に何も印刷されていません。まず文を聞いてください。それから、その返事を聞いて、最もよいものを一つえらんでください。",
        choicesAudioOnly = true,
    ),
    INTEGRATED_LISTENING(
        Section.LISTENING, "統合理解", "Integrated listening",
        "長めの話を聞きます。問題用紙にメモをとってもかまいません。まず話を聞いてください。それから、質問とせんたくしを聞いて、最もよいものを一つえらんでください。",
    ),
}

@Serializable
data class ScriptLine(
    /** "M" (man), "F" (woman), "N" (narrator), or any label; used to vary the voice. */
    val speaker: String = "N",
    val text: String,
)

@Serializable
data class Passage(
    val id: String,
    val title: String = "",
    val text: String,
)

@Serializable
data class Question(
    val id: String,
    val type: QuestionType,
    /**
     * The question stem. Words wrapped in `<u>…</u>` are shown underlined,
     * the way the target word is marked on the real test paper.
     */
    val prompt: String,
    val choices: List<String>,
    /** Zero-based index into [choices]. */
    val answer: Int,
    val explanation: String = "",
    /** Reading and text-grammar questions point at a shared [Passage]. */
    val passageId: String? = null,
    /** Listening questions: the scene set-up read before the conversation. */
    val situation: String? = null,
    /** Listening questions: the conversation or monologue, read aloud with text-to-speech. */
    val script: List<ScriptLine> = emptyList(),
) {
    val section: Section get() = type.section
}

@Serializable
data class QuestionBankFile(
    val level: JlptLevel,
    val passages: List<Passage> = emptyList(),
    val questions: List<Question> = emptyList(),
)

/** All questions for one level, merged from every bank file for that level. */
class QuestionBank(
    val level: JlptLevel,
    passages: List<Passage>,
    val questions: List<Question>,
) {
    val passages: Map<String, Passage> = passages.associateBy { it.id }
    private val byId: Map<String, Question> = questions.associateBy { it.id }

    fun question(id: String): Question? = byId[id]
    fun passageFor(question: Question): Passage? = question.passageId?.let { passages[it] }
    fun bySection(section: Section): List<Question> = questions.filter { it.section == section }
    fun byType(type: QuestionType): List<Question> = questions.filter { it.type == type }

    companion object {
        fun merge(level: JlptLevel, files: List<QuestionBankFile>): QuestionBank {
            files.forEach { require(it.level == level) { "Bank file for ${it.level} passed as $level" } }
            return QuestionBank(
                level = level,
                passages = files.flatMap { it.passages },
                questions = files.flatMap { it.questions },
            )
        }
    }
}

enum class TestMode {
    /** Instant feedback after each answer. */
    PRACTICE,
    /** A timed, full-format test scored like the real JLPT. */
    MOCK,
    /** Re-practice of questions answered wrong before. */
    REVIEW,
}
