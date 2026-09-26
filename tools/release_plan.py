#!/usr/bin/env python3
"""Decides whether a push to main needs a new release, and which version it gets.

Rules (the same idea as hoangkien1703/dual-sub-replay):
- The merged PR's label picks the bump: release:major, release:minor, release:patch (default)
  or release:skip. A `Release-Version: X.Y.Z` line in the PR description sets it exactly.
- A merge that only touches docs, CI or tooling (see IGNORED) does not need a release.
- The version continues from the highest vX.Y.Z tag. versionCode is derived from the version,
  so it always grows.

Writes needed/version/version_code/reason to $GITHUB_OUTPUT (or prints them).
"""

from __future__ import annotations

import fnmatch
import json
import os
import re
import subprocess
import sys
import urllib.request

VERSION = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")
LABELS = {"release:major": "major", "release:minor": "minor", "release:patch": "patch", "release:skip": "skip"}

# Changes to these paths alone don't change the app.
IGNORED = ["*.md", "docs/*", ".github/*", "tools/tests/*", "tools/release_plan.py", ".gitignore", "LICENSE*"]


def parse_version(text: str) -> tuple[int, int, int]:
    match = VERSION.match(text)
    if not match:
        raise ValueError(f"Invalid version {text!r}; expected X.Y.Z")
    return tuple(int(x) for x in match.groups())  # type: ignore[return-value]


def latest_version(tags: list[str]) -> tuple[int, int, int]:
    versions = [parse_version(t[1:]) for t in tags if t.startswith("v") and VERSION.match(t[1:])]
    return max(versions, default=(0, 1, 0))


def requested_bump(labels: list[str], body: str) -> str:
    body = re.sub(r"<!--[\s\S]*?-->", "", body or "")
    explicit = re.findall(r"^[ \t]*Release-Version:[ \t]*(\S+)[ \t]*$", body, re.MULTILINE)
    modes = [LABELS[label] for label in labels if label in LABELS]
    if len(modes) + len(explicit) > 1:
        raise ValueError("Use only one release label or one Release-Version line")
    if explicit:
        parse_version(explicit[0])
        return explicit[0]
    return modes[0] if modes else "patch"


def next_version(previous: tuple[int, int, int], bump: str) -> tuple[int, int, int]:
    major, minor, patch = previous
    if bump == "major":
        result = (major + 1, 0, 0)
    elif bump == "minor":
        result = (major, minor + 1, 0)
    elif bump == "patch":
        result = (major, minor, patch + 1)
    else:
        result = parse_version(bump)
    if result <= previous:
        raise ValueError(f"Version {result} must be higher than {previous}")
    return result


def version_code(version: tuple[int, int, int]) -> int:
    major, minor, patch = version
    if minor > 99 or patch > 99:
        raise ValueError("minor and patch must stay below 100")
    return major * 10000 + minor * 100 + patch


def app_changed(files: list[str]) -> bool:
    return any(not any(fnmatch.fnmatch(f, pattern) for pattern in IGNORED) for f in files)


def plan(labels: list[str], body: str, files: list[str], tags: list[str], forced_bump: str | None = None) -> dict:
    previous = latest_version(tags)
    bump = forced_bump or requested_bump(labels, body)
    if not forced_bump:
        if bump == "skip":
            return {"needed": False, "reason": "The PR is labelled release:skip."}
        if files and not app_changed(files):
            return {"needed": False, "reason": "Only docs, CI or tooling changed."}
    version = next_version(previous, bump)
    return {
        "needed": True,
        "version": ".".join(map(str, version)),
        "version_code": version_code(version),
        "reason": f"{bump} release after v{'.'.join(map(str, previous))}",
    }


def github(path: str) -> object:
    request = urllib.request.Request(
        f"https://api.github.com/repos/{os.environ['GITHUB_REPOSITORY']}{path}",
        headers={"Authorization": f"Bearer {os.environ['GH_TOKEN']}", "Accept": "application/vnd.github+json"},
    )
    with urllib.request.urlopen(request) as response:
        return json.load(response)


def main() -> None:
    sha = os.environ["GITHUB_SHA"]
    forced = os.environ.get("FORCE_BUMP") or None
    tags = subprocess.run(["git", "tag", "--list", "v*"], capture_output=True, text=True, check=True).stdout.split()

    labels, body, files = [], "", []
    pulls = github(f"/commits/{sha}/pulls")
    merged = [p for p in pulls if p.get("merged_at")] if isinstance(pulls, list) else []
    if merged:
        pr = merged[0]
        labels = [label["name"] for label in pr.get("labels", [])]
        body = pr.get("body") or ""
        page = 1
        while True:
            batch = github(f"/pulls/{pr['number']}/files?per_page=100&page={page}")
            files += [f["filename"] for f in batch]
            if len(batch) < 100:
                break
            page += 1
        print(f"Merged PR #{pr['number']}: labels={labels}, {len(files)} files")
    else:
        # A direct push: compare with the previous commit.
        diff = subprocess.run(["git", "diff", "--name-only", f"{sha}~1", sha], capture_output=True, text=True)
        files = diff.stdout.split() if diff.returncode == 0 else []
        print(f"Direct push: {len(files)} files")

    result = plan(labels, body, files, tags, forced)
    print(json.dumps(result, indent=2))
    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as f:
            for key, value in result.items():
                f.write(f"{key}={str(value).lower() if isinstance(value, bool) else value}\n")


if __name__ == "__main__":
    try:
        main()
    except ValueError as error:
        sys.exit(f"::error::{error}")
