# Open JLPT

An open-source Android app for JLPT practice, N5 to N1, in the spirit of apps like Migii JLPT.
It works fully offline: every question ships inside the app.

## Download

Get the latest APK from the [Releases page](https://github.com/hoangkien1703/Open-JLPT/releases/latest)
([direct link](https://github.com/hoangkien1703/Open-JLPT/releases/latest/download/open-jlpt.apk)).
Open the file on your phone and allow installing apps from your browser when Android asks.
Android 8.0 or newer is required. Every change merged into `main` publishes a new release, and a new
APK installs over the old one without losing your history.

## Features

- **All five levels.** Pick N5, N4, N3, N2 or N1 on the home screen; the app remembers your level.
- **Practice by skill.** Vocabulary (文字・語彙), grammar (文法), reading (読解) and listening (聴解),
  either mixed or by a single JLPT question type (漢字読み, 文脈規定, 文の組み立て, 情報検索, 即時応答 …).
  Each answer is checked on the spot with an explanation.
- **Full mock tests.** The real paper structure for each level: question types and counts from the
  official format, a separate timer per paper, and an estimated score per score section with a pass/fail
  verdict against the JLPT pass mark and sectional minimums.
- **Listening without audio files.** Listening scripts are read aloud by the phone's Japanese
  text-to-speech voice, with a different pitch for each speaker. Transcripts appear after you answer.
- **Progress.** Every attempt is saved. History shows past scores and accuracy per skill, and
  *Review mistakes* re-drills every question whose latest answer was wrong.

## About the questions

All questions are **original** practice material written to match the JLPT format and each level's
vocabulary and grammar scope. Official JLPT papers and other apps' question banks are copyrighted, so
nothing is copied from them. Scores are estimates: the real test uses scaled scoring.

## Project layout

| Path | What it holds |
| --- | --- |
| `core/` | Plain Kotlin module: data model, JLPT formats (timings, question counts, pass marks), session builder, scoring, and the tests that validate every question bank. |
| `app/` | The Android app: Jetpack Compose UI, Room history database, text-to-speech listening player. |
| `app/src/main/assets/questions/<level>/*.json` | The question banks. Every JSON file in a level's folder is loaded and merged. |

## Building

Requirements: JDK 17 and the Android SDK (API 35).

```bash
./gradlew :core:test          # engine tests + question bank validation
./gradlew :app:assembleDebug  # builds app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions runs both on every push and uploads the debug APK as a build artifact. Pushes to
`main` also build the release APK and publish it as a GitHub Release (`.github/workflows/release.yml`).

## Adding questions

See [docs/QUESTION_FORMAT.md](docs/QUESTION_FORMAT.md). In short: add entries to a JSON file under
`app/src/main/assets/questions/<level>/` and run `./gradlew :core:test`, which checks ids, choices,
answers, passages and listening scripts.
