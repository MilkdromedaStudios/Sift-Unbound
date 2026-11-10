#!/usr/bin/env python3
"""Decides and prepares automatic releases (used by .github/workflows/auto-release.yml).

    python3 tools/auto_release.py plan
        Prints release=true|false, version=X.Y.Z and previous=vA.B.C (also to $GITHUB_OUTPUT on GitHub Actions).
    python3 tools/auto_release.py prepare VERSION PREVIOUS_TAG
        Sets mod_version in gradle.properties and turns CHANGELOG.md's [Unreleased] section into [VERSION] - today.

A release happens only when something that goes into the mod jar changed since the last v* tag (MOD_PATHS below).
Docs, the wiki, screenshots, tools, tests and workflows never trigger one.

The version: if gradle.properties already has a mod_version newer than the last tag (bumped by hand), that one;
otherwise the next minor version when the [Unreleased] changelog section has "### Added" or "### Removed"
(new content), else the next patch version.
"""
import datetime
import os
import re
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MOD_PATHS = ["src/main", "src/client", "build.gradle", "settings.gradle", "gradle.properties", "gradle"]
SEMVER = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")


def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True, check=True).stdout


def parse(v):
    m = SEMVER.match(v)
    return tuple(int(x) for x in m.groups()) if m else None


def last_tag():
    tags = [t for t in git("tag", "--list", "v*").split() if parse(t[1:])]
    return max(tags, key=lambda t: parse(t[1:])) if tags else None


def mod_version():
    m = re.search(r"^mod_version=(.+)$", open(os.path.join(ROOT, "gradle.properties"), encoding="utf-8").read(), re.M)
    return m.group(1).strip()


def unreleased(changelog):
    m = re.search(r"^## \[Unreleased\]\s*\n(.*?)(?=^## |\Z)", changelog, re.M | re.S)
    return m.group(1).strip() if m else ""


def output(**values):
    for k, v in values.items():
        print(f"{k}={v}")
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as f:
            for k, v in values.items():
                f.write(f"{k}={v}\n")


def plan():
    tag = last_tag()
    if tag is None:
        output(release="true", version=mod_version(), previous="")
        return
    changed = subprocess.run(["git", "diff", "--quiet", tag, "HEAD", "--", *MOD_PATHS], cwd=ROOT).returncode != 0
    if not changed:
        print(f"Nothing in the mod changed since {tag}: no release.", file=sys.stderr)
        output(release="false", version="", previous=tag)
        return
    last, current = parse(tag[1:]), parse(mod_version())
    if current and current > last:
        version = mod_version()
    else:
        notes = unreleased(open(os.path.join(ROOT, "CHANGELOG.md"), encoding="utf-8").read())
        minor = re.search(r"^### (Added|Removed)\b", notes, re.M) is not None
        version = f"{last[0]}.{last[1] + 1}.0" if minor else f"{last[0]}.{last[1]}.{last[2] + 1}"
    print(f"The mod changed since {tag}: releasing {version}.", file=sys.stderr)
    output(release="true", version=version, previous=tag)


def mod_commit_titles(previous):
    """Titles of the changes to the mod since the previous tag (PR titles for merge commits)."""
    log = git("log", "--first-parent", "--format=%s%x00%b%x1e", f"{previous}..HEAD", "--", *MOD_PATHS)
    titles = []
    for entry in filter(None, (e.strip() for e in log.split("\x1e"))):
        subject, _, body = entry.partition("\x00")
        if subject.startswith("Merge pull request"):
            subject = next((line.strip() for line in body.splitlines() if line.strip()), subject)
        titles.append(subject)
    return titles


def prepare(version, previous):
    if not parse(version):
        sys.exit(f"not a version: {version}")
    path = os.path.join(ROOT, "gradle.properties")
    props = open(path, encoding="utf-8").read()
    open(path, "w", encoding="utf-8").write(re.sub(r"^mod_version=.*$", f"mod_version={version}", props, flags=re.M))

    path = os.path.join(ROOT, "CHANGELOG.md")
    changelog = open(path, encoding="utf-8").read()
    today = datetime.date.today().isoformat()
    body = unreleased(changelog)
    if not body:
        titles = mod_commit_titles(previous) if previous else []
        body = "### Changed\n" + "\n".join(f"- {t}" for t in titles or ["Maintenance release."])
    section = f"## [Unreleased]\n\n## [{version}] - {today}\n\n{body}\n\n"
    if re.search(r"^## \[Unreleased\]", changelog, re.M):
        changelog = re.sub(r"^## \[Unreleased\]\s*\n.*?(?=^## |\Z)", lambda _: section, changelog, count=1, flags=re.M | re.S)
    else:
        first = re.search(r"^## ", changelog, re.M)
        at = first.start() if first else len(changelog)
        changelog = changelog[:at] + section + changelog[at:]
    open(path, "w", encoding="utf-8").write(changelog)
    print(f"Prepared {version}")


if __name__ == "__main__":
    if sys.argv[1:2] == ["plan"]:
        plan()
    elif sys.argv[1:2] == ["prepare"] and len(sys.argv) in (3, 4):
        prepare(sys.argv[2], sys.argv[3] if len(sys.argv) == 4 else "")
    else:
        sys.exit(__doc__)
