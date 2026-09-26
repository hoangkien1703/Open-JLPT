# Question bank format

Question banks live in `app/src/main/assets/questions/<level>/`, where `<level>` is `n5` … `n1`.
Every `.json` file in that folder is loaded and merged, so you can split a level into as many files as
you like (the app ships `vocabulary.json`, `grammar.json`, `reading.json` and `listening.json`).

```json
{
  "level": "N5",
  "passages": [
    { "id": "n5-sp-p1", "title": "optional heading", "text": "passage text, \n for new lines" }
  ],
  "questions": [
    {
      "id": "n5-kr-001",
      "type": "KANJI_READING",
      "prompt": "<u>山</u>に のぼりました。",
      "choices": ["かわ", "うみ", "やま", "そら"],
      "answer": 2,
      "explanation": "山 is read やま (mountain)."
    }
  ]
}
```

## Fields

| Field | Required | Notes |
| --- | --- | --- |
| `id` | yes | Unique across all levels. Convention: `<level>-<type code>-<number>`. Don't reuse ids: history refers to them. |
| `type` | yes | One of the question types below. It must appear on that level's test. |
| `prompt` | yes* | The question stem. Wrap the target word in `<u>…</u>` to underline it. Blanks are written `（　　）`, and sentence-order questions use `＿＿＿` with a `★`. *May be empty for 即時応答 and 発話表現, where the question is only spoken. |
| `choices` | yes | 4 choices, or 3 for `UTTERANCE` and `QUICK_RESPONSE`. The app shuffles them at run time. |
| `answer` | yes | Zero-based index of the correct choice. |
| `explanation` | yes | Shown after answering. English, with Japanese where useful. |
| `passageId` | for passage types | Required for `TEXT_GRAMMAR` and all reading types. Several questions can share one passage; they are always kept together, in file order. |
| `situation` | listening | The scene set-up read before the conversation (e.g. 男の人と女の人が話しています。). For `UTTERANCE` it describes the picture instead and is shown, not read. |
| `script` | listening | List of `{ "speaker": "M" \| "F" \| "M2" \| "F2" \| "N", "text": "…" }`. Read aloud by text-to-speech; each speaker gets a different pitch. |

## Question types

| Section | `type` | Test paper name | Levels |
| --- | --- | --- | --- |
| Vocabulary | `KANJI_READING` | 漢字読み | all |
| | `ORTHOGRAPHY` | 表記 | N5–N2 |
| | `WORD_FORMATION` | 語形成 | N2 |
| | `CONTEXT` | 文脈規定 | all |
| | `PARAPHRASE` | 言い換え類義 | all |
| | `USAGE` | 用法 | N4–N1 |
| Grammar | `GRAMMAR_FORM` | 文法形式の判断 | all |
| | `SENTENCE_ORDER` | 文の組み立て | all |
| | `TEXT_GRAMMAR` | 文章の文法 | all |
| Reading | `SHORT_PASSAGE` | 内容理解（短文） | all |
| | `MID_PASSAGE` | 内容理解（中文） | all |
| | `LONG_PASSAGE` | 内容理解（長文） | N3, N1 |
| | `INTEGRATED_READING` | 統合理解 | N2, N1 |
| | `THEMATIC` | 主張理解（長文） | N2, N1 |
| | `INFO_RETRIEVAL` | 情報検索 | all |
| Listening | `TASK` | 課題理解 | all |
| | `POINT` | ポイント理解 | all |
| | `SUMMARY` | 概要理解 | N3–N1 |
| | `UTTERANCE` | 発話表現 | N5–N3 |
| | `QUICK_RESPONSE` | 即時応答 | all |
| | `INTEGRATED_LISTENING` | 統合理解 | N2, N1 |

The official number of questions per type for each level's mock test is defined in
`core/src/main/kotlin/com/openjlpt/core/format/JlptFormat.kt`.

## Content rules

- Write original questions. Do not copy official JLPT papers, published workbooks or other apps.
- Keep vocabulary and grammar within the level (a question may use easier material, not harder).
- Each question must have exactly one defensible answer. For sentence-order questions, check that the
  fragments can only be arranged one way.
- Run `./gradlew :core:test` before sending a change; it validates every bank.
