# Open JLPT

An open-source Android app for JLPT practice, N5 to N1, in the spirit of apps like Migii JLPT.
It works fully offline: every question ships inside the app.

## Download

Get the latest APK from the [Releases page](https://github.com/hoangkien1703/Open-JLPT/releases/latest)
([direct link](https://github.com/hoangkien1703/Open-JLPT/releases/latest/download/open-jlpt.apk)).
Open the file on your phone and allow installing apps from your browser when Android asks.
Android 8.0 or newer is required. A new APK installs over the old one without losing your history.

Test builds of open pull requests are on the [preview release](https://github.com/hoangkien1703/Open-JLPT/releases/tag/preview).
They install as *Open JLPT Preview*, next to the normal app.

## Features

- **All five levels.** Pick N5, N4, N3, N2 or N1 on the home screen; the app remembers your level.
- **Practice by skill.** Vocabulary (文字・語彙), grammar (文法), reading (読解) and listening (聴解),
  either mixed or by a single JLPT question type (漢字読み, 文脈規定, 文の組み立て, 情報検索, 即時応答 …).
  Each answer is checked on the spot with an explanation.
- **Full mock tests.** The real paper structure for each level: question types and counts from the
  official format, a separate timer per paper, and an estimated score per score section with a pass/fail
  verdict against the JLPT pass mark and sectional minimums.
- **Built-in listening audio.** Every listening question comes with a recording (male and female
  voices), so it works on any phone. You can switch to the phone's own Japanese voice and change the
  speed in Settings. Transcripts appear after you answer.
- **Tap a word.** Tap any Japanese word to hear it and see its reading and English meaning. By
  default this opens only after you've answered a question and stays off in mock tests, so it never
  gives the answer away. Both can be changed in Settings.
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
| `app/` | The Android app: Jetpack Compose UI, Room history database, audio player, settings. |
| `app/src/main/assets/questions/<level>/*.json` | The question banks. Every JSON file in a level's folder is loaded and merged. |
| `tools/build_assets.py` | Generates the listening audio, word pronunciations and word glossary into `app/generated-assets/` (not committed). |

## Building

Requirements: JDK 17 and the Android SDK (API 35).

```bash
./gradlew :core:test          # engine tests + question bank validation
python3 tools/build_assets.py # optional: audio + glossary (see below)
./gradlew :app:assembleDebug  # builds app/build/outputs/apk/debug/app-debug.apk
```

`tools/build_assets.py` needs Open JTalk and ffmpeg
(`apt install open-jtalk open-jtalk-mecab-naist-jdic hts-voice-nitech-jp-atr503-m001 ffmpeg`) and
`pip install sudachipy sudachidict_core`; it downloads JMdict from PyPI on first run. Without it the app
still builds: listening falls back to the phone's voice and words can't be tapped.

### Continuous integration and releases

- **Pull requests** (`.github/workflows/android.yml`): run the tests, generate the assets, build the
  APK, and publish it to the rolling *preview* release as `open-jlpt-pr<N>-preview.apk`. The file is
  removed when the PR closes.
- **Merges into `main`** (`.github/workflows/release.yml`): publish a new GitHub Release with
  `open-jlpt.apk` when the app changed. The version goes up by a patch by default; label the PR
  `release:minor`, `release:major` or `release:skip`, or write `Release-Version: X.Y.Z` in its
  description. Merges that only touch docs or CI don't make a release. *Run workflow* on the Release
  APK action forces one.

## Credits

Listening audio is synthesized with [Open JTalk](https://open-jtalk.sourceforge.net/) using the
nitech-jp-atr503-m001 voice (Nagoya Institute of Technology) and the
[tohoku-f01](https://github.com/icn-lab/htsvoice-tohoku-f01) voice (Tohoku University, CC BY 4.0,
included in `tools/voices`). Word meanings come from [JMdict](https://www.edrdg.org/jmdict/j_jmdict.html)
by the Electronic Dictionary Research and Development Group (CC BY-SA 4.0), and sentences are split
into words with [Sudachi](https://github.com/WorksApplications/SudachiPy).

## Adding questions

See [docs/QUESTION_FORMAT.md](docs/QUESTION_FORMAT.md). In short: add entries to a JSON file under
`app/src/main/assets/questions/<level>/` and run `./gradlew :core:test`, which checks ids, choices,
answers, passages and listening scripts.
