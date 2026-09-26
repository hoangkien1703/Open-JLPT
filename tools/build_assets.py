#!/usr/bin/env python3
"""Generates the app's built-in audio and word glossary from the question banks.

Outputs (under --out, default app/generated-assets, which Gradle packages as assets):

  audio/listening/<question id>.webm   the full listening track for each listening question
  audio/words/<key>.webm               the pronunciation of each word in the glossary
  glossary/<level>.json               tappable word spans for every Japanese text in the app

Audio is synthesized offline with Open JTalk: the nitech m001 male voice (apt package
hts-voice-nitech-jp-atr503-m001) and the tohoku-f01 female voice in tools/voices (CC BY 4.0).
Word meanings come from JMdict (EDRDG, CC BY-SA 4.0) via the jamdict-data package, and
sentences are split into words with SudachiPy.

The script is incremental: files whose inputs have not changed are kept, so CI can cache
the output folder and only synthesize new or edited questions.

Requirements: open_jtalk, ffmpeg, `pip install sudachipy sudachidict_core`, and a JMdict database
(`--jmdict path/to/jamdict.db`, or one is fetched from PyPI with pip download).
"""

from __future__ import annotations

import argparse
import concurrent.futures
import hashlib
import json
import lzma
import os
import re
import shutil
import sqlite3
import subprocess
import sys
import tarfile
import tempfile
import wave
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
QUESTIONS = ROOT / "app/src/main/assets/questions"
LEVELS = ["n5", "n4", "n3", "n2", "n1"]

# Bump when voices or audio settings change so every track is regenerated.
AUDIO_VERSION = "2"
GLOSSARY_VERSION = 1

DICT_DIR = "/var/lib/mecab/dic/open-jtalk/naist-jdic"
MALE_VOICE = "/usr/share/hts-voice/nitech-jp-atr503-m001/nitech_jp_atr503_m001.htsvoice"
FEMALE_VOICE = str(ROOT / "tools/voices/tohoku-f01-neutral.htsvoice")

# Open JTalk options per speaker: voice, pitch shift in semitones, speed.
VOICES = {
    "N": (FEMALE_VOICE, 0.0, 0.95),
    "F": (FEMALE_VOICE, 1.0, 1.0),
    "F2": (FEMALE_VOICE, 4.0, 1.02),
    "M": (MALE_VOICE, 0.0, 1.0),
    "M2": (MALE_VOICE, -3.0, 0.97),
}
WORD_VOICE = (FEMALE_VOICE, 0.0, 0.9)

SAMPLE_RATE = 48000
PAUSE_BETWEEN_LINES = 0.7
PAUSE_BEFORE_CHOICES = 1.2
PAUSE_BETWEEN_CHOICES = 1.0

AUDIO_ONLY_CHOICES = {"SUMMARY", "UTTERANCE", "QUICK_RESPONSE"}
LISTENING_TYPES = {"TASK", "POINT", "SUMMARY", "UTTERANCE", "QUICK_RESPONSE", "INTEGRATED_LISTENING"}

JAMDICT_DATA = "jamdict-data==1.5"


def sha(text: str, n: int = 16) -> str:
    return hashlib.sha1(text.encode("utf-8")).hexdigest()[:n]


def strip_markup(text: str) -> str:
    return text.replace("<u>", "").replace("</u>", "")


def load_level(level: str) -> dict:
    passages, questions = [], []
    for path in sorted((QUESTIONS / level).glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        passages += data.get("passages", [])
        questions += data.get("questions", [])
    return {"passages": passages, "questions": questions}


# ---------------------------------------------------------------- listening audio


def listening_lines(q: dict) -> list[tuple[str, str, float]]:
    """(speaker, text, pause after) in the order the app's player uses (see ListeningPlayer.linesFor)."""
    qtype = q["type"]
    prompt = q.get("prompt", "")
    lines: list[tuple[str, str, float]] = []
    situation = (q.get("situation") or "").strip()
    if qtype != "UTTERANCE" and situation:
        lines.append(("N", situation, PAUSE_BETWEEN_LINES))
    if qtype in ("TASK", "POINT") and prompt.strip():
        lines.append(("N", prompt, PAUSE_BETWEEN_LINES))
    for line in q.get("script", []):
        lines.append((line.get("speaker", "N"), line["text"], PAUSE_BETWEEN_LINES))
    if prompt.strip() and qtype != "QUICK_RESPONSE":
        lines.append(("N", prompt, PAUSE_BETWEEN_LINES))
    if qtype in AUDIO_ONLY_CHOICES:
        if lines:
            lines[-1] = (lines[-1][0], lines[-1][1], PAUSE_BEFORE_CHOICES)
        for i, choice in enumerate(q["choices"]):
            lines.append(("N", f"{i + 1}、{choice}", PAUSE_BETWEEN_CHOICES))
    return lines


def voice_for(speaker: str) -> tuple[str, float, float]:
    s = speaker.upper()
    if s in ("M1",):
        s = "M"
    if s in ("F1",):
        s = "F"
    return VOICES.get(s, VOICES["N"])


def synthesize(text: str, voice: tuple[str, float, float], out_wav: Path) -> None:
    model, pitch, speed = voice
    with tempfile.NamedTemporaryFile("w", suffix=".txt", encoding="utf-8", delete=False) as f:
        # Open JTalk reads one utterance per line.
        f.write(text.replace("\n", "、") + "\n")
        txt = f.name
    try:
        subprocess.run(
            ["open_jtalk", "-x", DICT_DIR, "-m", model, "-s", str(SAMPLE_RATE),
             "-fm", str(pitch), "-r", str(speed), "-ow", str(out_wav), txt],
            check=True, capture_output=True,
        )
    finally:
        os.unlink(txt)


TRIM = "silenceremove=start_periods=1:start_threshold=-45dB:stop_periods=1:stop_threshold=-45dB,adelay=60,apad=pad_dur=0.15"


def encode(wav: Path, out: Path, bitrate: str, trim: bool = False) -> None:
    """Opus in WebM: small for speech, and played by MediaPlayer on every supported Android version."""
    out.parent.mkdir(parents=True, exist_ok=True)
    tmp = out.with_suffix(".tmp.webm")
    filters = ["-af", TRIM] if trim else []
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), *filters, "-ac", "1",
         "-c:a", "libopus", "-b:a", bitrate, "-application", "voip", str(tmp)],
        check=True,
    )
    tmp.replace(out)


def build_track(job: tuple[str, list[tuple[str, str, float]], Path]) -> str:
    qid, lines, out = job
    with tempfile.TemporaryDirectory() as tmp:
        frames = bytearray()
        for i, (speaker, text, pause) in enumerate(lines):
            part = Path(tmp) / f"{i}.wav"
            synthesize(text, voice_for(speaker), part)
            with wave.open(str(part), "rb") as w:
                assert w.getframerate() == SAMPLE_RATE and w.getsampwidth() == 2 and w.getnchannels() == 1
                frames += w.readframes(w.getnframes())
            if i != len(lines) - 1:
                frames += b"\x00\x00" * int(SAMPLE_RATE * pause)
        full = Path(tmp) / "full.wav"
        with wave.open(str(full), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SAMPLE_RATE)
            w.writeframes(bytes(frames))
        encode(full, out, "24k")
    return qid


def build_word(job: tuple[str, str, Path]) -> str:
    key, reading, out = job
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "w.wav"
        synthesize(reading, WORD_VOICE, wav)
        encode(wav, out, "20k", trim=True)
    return key


def run_jobs(fn, jobs, workers: int, label: str) -> None:
    if not jobs:
        print(f"{label}: nothing to do")
        return
    done = 0
    with concurrent.futures.ProcessPoolExecutor(max_workers=workers) as pool:
        for _ in pool.map(fn, jobs, chunksize=4):
            done += 1
            if done % 100 == 0 or done == len(jobs):
                print(f"{label}: {done}/{len(jobs)}", flush=True)


def load_manifest(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def build_listening(banks: dict, out: Path, workers: int) -> None:
    folder = out / "audio/listening"
    folder.mkdir(parents=True, exist_ok=True)
    manifest_path = folder / "manifest.json"
    old = load_manifest(manifest_path)
    new, jobs = {}, []
    for level in LEVELS:
        for q in banks[level]["questions"]:
            if q["type"] not in LISTENING_TYPES:
                continue
            lines = listening_lines(q)
            digest = sha(AUDIO_VERSION + json.dumps(lines, ensure_ascii=False), 20)
            new[q["id"]] = digest
            track = folder / f"{q['id']}.webm"
            if old.get(q["id"]) != digest or not track.exists():
                jobs.append((q["id"], lines, track))
    for stale in set(old) - set(new):
        (folder / f"{stale}.webm").unlink(missing_ok=True)
    run_jobs(build_track, jobs, workers, "listening tracks")
    manifest_path.write_text(json.dumps(new, indent=0, sort_keys=True), encoding="utf-8")


# ---------------------------------------------------------------- glossary

KATA_TO_HIRA = {c: c - 0x60 for c in range(ord("ァ"), ord("ヶ") + 1)}
HAS_KANJI = re.compile(r"[\u3400-\u9fff\uf900-\ufaff々]")
JAPANESE = re.compile(r"[\u3040-\u30ff\u3400-\u9fff\uf900-\ufaff々ー]")

# Sudachi parts of speech that get a tappable span, with the JMdict part of speech they suggest.
POS_HINTS = {
    "名詞": ("noun",),
    "代名詞": ("pronoun",),
    "形状詞": ("adjectival nouns",),
    "動詞": ("verb",),
    "形容詞": ("adjective (keiyoushi)",),
    "副詞": ("adverb",),
    "連体詞": ("pre-noun adjectival",),
    "接続詞": ("conjunction",),
    "感動詞": ("interjection",),
}
USUALLY_KANA = "word usually written using kana alone"

# Kana spellings that are ambiguous out of context, mapped to the word the question banks mean.
# Beginner texts write these in kana, so without a hint the dictionary would guess.
KANA_OVERRIDES = {
    "こうえん": "公園",
    "かく": "書く",
    "かう": "買う",
    "きる": "着る",
    "はし": "橋",
    "あう": "会う",
    "きく": "聞く",
    "みる": "見る",
    "でる": "出る",
    "とる": "取る",
    "のる": "乗る",
    "かえる": "帰る",
    "つくる": "作る",
    "はいる": "入る",
    "あける": "開ける",
    "しめる": "閉める",
    "おもう": "思う",
    "かいしゃ": "会社",
    "きょうしつ": "教室",
}
# Readings to prefer when a spelling has several.
READING_OVERRIDES = {"日本": "にほん", "何": "なに"}


def hira(text: str) -> str:
    return text.translate(KATA_TO_HIRA)


def utf16_len(text: str) -> int:
    return len(text.encode("utf-16-le")) // 2


@dataclass
class Entry:
    idseq: int
    kanji: list[str]
    kana: list[str]
    priority: float
    usually_kana: bool
    senses: list[tuple[list[str], list[str]]]  # (parts of speech, glosses)


class Dictionary:
    def __init__(self, db_path: Path):
        self.db = sqlite3.connect(str(db_path))
        self.cache: dict[int, Entry] = {}
        self.by_form: dict[str, list[int]] = {}

    def candidates(self, form: str) -> list[int]:
        if form not in self.by_form:
            rows = self.db.execute(
                "SELECT idseq FROM Kanji WHERE text = ? UNION SELECT idseq FROM Kana WHERE text = ?", (form, form)
            ).fetchall()
            self.by_form[form] = [r[0] for r in rows]
        return self.by_form[form]

    def entry(self, idseq: int) -> Entry:
        if idseq not in self.cache:
            db = self.db
            kanji = [r[0] for r in db.execute("SELECT text FROM Kanji WHERE idseq = ? ORDER BY ID", (idseq,))]
            kana = [r[0] for r in db.execute("SELECT text FROM Kana WHERE idseq = ? ORDER BY ID", (idseq,))]
            tags = [r[0] for r in db.execute(
                "SELECT KJP.text FROM KJP JOIN Kanji ON KJP.kid = Kanji.ID WHERE Kanji.idseq = ? "
                "UNION ALL SELECT KNP.text FROM KNP JOIN Kana ON KNP.kid = Kana.ID WHERE Kana.idseq = ?",
                (idseq, idseq),
            )]
            priority = 0.0
            if any(t in ("ichi1", "news1", "spec1", "gai1") for t in tags):
                priority += 2
            elif tags:
                priority += 1
            ranks = [int(t[2:]) for t in tags if t.startswith("nf")]
            if ranks:
                priority += (49 - min(ranks)) / 48
            senses, usually_kana = [], False
            for i, (sid,) in enumerate(db.execute("SELECT ID FROM Sense WHERE idseq = ? ORDER BY ID", (idseq,)).fetchall()):
                pos = [r[0] for r in db.execute("SELECT text FROM pos WHERE sid = ?", (sid,))]
                glosses = [r[0] for r in db.execute("SELECT text FROM SenseGloss WHERE sid = ? AND lang = 'eng'", (sid,))]
                if i == 0:
                    usually_kana = db.execute("SELECT 1 FROM misc WHERE sid = ? AND text = ?", (sid, USUALLY_KANA)).fetchone() is not None
                if glosses:
                    senses.append((pos, glosses))
            self.cache[idseq] = Entry(idseq, kanji, kana, priority, usually_kana, senses)
        return self.cache[idseq]

    def lookup(self, forms: list[str], kana_form: str | None, reading: str | None, pos: str) -> tuple[Entry, str] | None:
        """Best entry for a word. forms: spellings to try (normalized first); kana_form: the word's
        dictionary form when written in kana; reading: the dictionary form's reading when known."""
        hints = POS_HINTS.get(pos, ())
        best, best_score, best_kana = None, float("-inf"), ""
        seen = set()
        for rank, form in enumerate(forms):
            for idseq in self.candidates(form):
                if idseq in seen:
                    continue
                seen.add(idseq)
                e = self.entry(idseq)
                if not e.senses:
                    continue
                score = e.priority - rank
                if form in e.kanji:
                    score += 3
                kana = None
                for target in (reading, kana_form):
                    if target:
                        kana = next((k for k in e.kana if hira(k) == hira(target)), None)
                        if kana:
                            score += 3
                            break
                if hints and any(h in p for p in e.senses[0][0] for h in hints):
                    score += 2
                if not HAS_KANJI.search(form) and e.kanji:
                    # Written in kana: likely if the word is usually written that way, and more likely
                    # when this is its main reading.
                    score += 1.5 if e.usually_kana and pos in ("名詞", "代名詞") else 0
                    score += 0.5 if kana and e.kana and kana == e.kana[0] else 0
                score += min(len(e.senses), 10) * 0.05  # well-used words have more senses
                if score > best_score:
                    best, best_score = e, score
                    best_kana = kana or (e.kana[0] if e.kana else form)
        return (best, best_kana) if best else None


class Glossary:
    def __init__(self, dictionary: Dictionary):
        from sudachipy import dictionary as sudachi_dictionary
        from sudachipy import tokenizer as sudachi_tokenizer

        self.tokenizer = sudachi_dictionary.Dictionary(dict="core").create()  # noqa: tokenizer() needs sudachipy>=0.6.8
        self.mode = sudachi_tokenizer.Tokenizer.SplitMode.B
        self.dictionary = dictionary
        self.words: dict[str, dict] = {}  # JMdict id.reading -> entry json

    def spans(self, text: str) -> list[list]:
        result = []
        after_number = False
        for m in self.tokenizer.tokenize(text, self.mode):
            pos = m.part_of_speech()
            counter = after_number and pos[2] == "助数詞可能"
            after_number = pos[1] == "数詞"
            if pos[0] not in POS_HINTS or pos[1] == "数詞" or counter:
                continue
            surface = m.surface()
            if not JAPANESE.search(surface):
                continue
            normalized, base = m.normalized_form(), m.dictionary_form()
            kana_form = base if not HAS_KANJI.search(base) else None
            # The surface reading is the dictionary form's reading when the word is not inflected.
            reading = hira(m.reading_form()) if surface == base else None
            reading = READING_OVERRIDES.get(base, reading)
            override = KANA_OVERRIDES.get(normalized) or KANA_OVERRIDES.get(base)
            forms = list(dict.fromkeys(f for f in (override, normalized, base, surface) if f))
            found = self.dictionary.lookup(forms, kana_form, reading, pos[0])
            if found is None:
                continue
            entry, kana = found
            key = f"{entry.idseq}.{entry.kana.index(kana)}" if kana in entry.kana else str(entry.idseq)
            if key not in self.words:
                if entry.usually_kana or not entry.kanji:
                    headword = kana
                elif base in entry.kanji:
                    headword = base
                elif normalized in entry.kanji:
                    headword = normalized
                else:
                    headword = entry.kanji[0]
                self.words[key] = {
                    "w": headword,
                    "r": hira(kana) if HAS_KANJI.search(headword) else "",
                    "m": ["; ".join(glosses[:4]) for _, glosses in entry.senses[:3]],
                    "a": sha(f"word{AUDIO_VERSION}:" + hira(kana), 12),
                    "_speak": hira(kana),
                }
            start, end = m.begin(), m.end()
            result.append([utf16_len(text[:start]), utf16_len(text[:end]), key])
        return result


def texts_for(bank: dict) -> set[str]:
    texts: set[str] = set()
    for p in bank["passages"]:
        texts.add(p.get("title", ""))
        texts.add(strip_markup(p["text"]))
    for q in bank["questions"]:
        texts.add(strip_markup(q.get("prompt", "")))
        texts.update(strip_markup(c) for c in q["choices"])
        texts.add(q.get("situation") or "")
        texts.update(line["text"] for line in q.get("script", []))
    return {t for t in texts if t and JAPANESE.search(t)}


def build_glossary(banks: dict, out: Path, dictionary: Dictionary, workers: int, audio: bool) -> None:
    glossary = Glossary(dictionary)
    folder = out / "glossary"
    folder.mkdir(parents=True, exist_ok=True)
    all_words: dict[str, str] = {}
    for level in LEVELS:
        glossary.words = {}
        spans = {}
        for text in sorted(texts_for(banks[level])):
            s = glossary.spans(text)
            if s:
                spans[sha(text)] = s
        words = {k: {kk: vv for kk, vv in v.items() if not kk.startswith("_")} for k, v in glossary.words.items()}
        for v in glossary.words.values():
            all_words[v["a"]] = v["_speak"]
        data = {"version": GLOSSARY_VERSION, "words": words, "texts": spans}
        (folder / f"{level}.json").write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        print(f"glossary {level}: {len(spans)} texts, {len(words)} words")
    if audio:
        words_dir = out / "audio/words"
        words_dir.mkdir(parents=True, exist_ok=True)
        existing = {p.stem for p in words_dir.glob("*.webm")}
        for stale in existing - set(all_words):
            (words_dir / f"{stale}.webm").unlink()
        jobs = [(k, r, words_dir / f"{k}.webm") for k, r in sorted(all_words.items()) if k not in existing]
        run_jobs(build_word, jobs, workers, "word audio")


# ---------------------------------------------------------------- JMdict


def fetch_jmdict(cache: Path) -> Path:
    db = cache / "jamdict.db"
    if db.exists():
        return db
    cache.mkdir(parents=True, exist_ok=True)
    print(f"Downloading JMdict ({JAMDICT_DATA}) from PyPI…", flush=True)
    subprocess.run(
        [sys.executable, "-m", "pip", "download", "--quiet", "--no-deps", "--no-binary", ":all:",
         "--dest", str(cache), JAMDICT_DATA],
        check=True,
    )
    archive = next(cache.glob("jamdict_data-*.tar.gz"))
    with tarfile.open(archive) as tar:
        member = next(m for m in tar.getmembers() if m.name.endswith("jamdict.db.xz"))
        with tar.extractfile(member) as src, lzma.open(src) as xz, open(db.with_suffix(".tmp"), "wb") as dst:
            shutil.copyfileobj(xz, dst)
    db.with_suffix(".tmp").replace(db)
    archive.unlink()
    return db


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", type=Path, default=ROOT / "app/generated-assets")
    parser.add_argument("--jmdict", type=Path, help="path to jamdict.db (downloaded when omitted)")
    parser.add_argument("--cache", type=Path, default=Path.home() / ".cache/open-jlpt")
    parser.add_argument("--no-audio", action="store_true", help="only build the glossary")
    parser.add_argument("--jobs", type=int, default=os.cpu_count() or 2)
    args = parser.parse_args()

    banks = {level: load_level(level) for level in LEVELS}
    if not args.no_audio:
        for tool in ("open_jtalk", "ffmpeg"):
            if shutil.which(tool) is None:
                sys.exit(f"{tool} is not installed (apt install open-jtalk open-jtalk-mecab-naist-jdic "
                         "hts-voice-nitech-jp-atr503-m001 ffmpeg)")
        build_listening(banks, args.out, args.jobs)
    dictionary = Dictionary(args.jmdict or fetch_jmdict(args.cache))
    build_glossary(banks, args.out, dictionary, args.jobs, audio=not args.no_audio)


if __name__ == "__main__":
    main()
