#!/usr/bin/env python3
"""Uploads a built jar to CurseForge as a new file of The Sift Unbound (used by the release workflow).

    python3 tools/publish_curseforge.py --version 0.3.2 --jar build/libs/sift-unbound-0.3.2+mc26.3.jar \
        --changelog release-notes.md [--dry-run]

Environment:
  CURSEFORGE_API_KEY   a CurseForge API token (https://legacy.curseforge.com/account/api-tokens)
                       (GitHub: Settings -> Secrets and variables -> Actions -> Secrets)
  CURSEFORGE_PROJECT   the CurseForge project ID (default 1798861, The Sift Unbound)
Without the key the upload is skipped with a notice, so releases still work without CurseForge. If CurseForge doesn't
list the Minecraft version yet, the upload is skipped with a warning instead of failing the release.
"""
import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
import uuid

API = os.environ.get("CURSEFORGE_API", "https://minecraft.curseforge.com/api/")
UA = "MilkdromedaStudios/Sift-Unbound (release workflow; github.com/MilkdromedaStudios/Sift-Unbound)"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def notice(msg, level="notice"):
    print(f"::{level}::{msg}" if os.environ.get("GITHUB_ACTIONS") else msg)


def request(method, path, key, body=None, content_type=None):
    headers = {"User-Agent": UA, "X-Api-Token": key}
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(API + path, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", "replace")
        sys.exit(f"::error::CurseForge {method} {path} failed: HTTP {e.code} {detail}")


def multipart(fields, files):
    boundary = uuid.uuid4().hex
    out = []
    for name, value in fields.items():
        out.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n'.encode() + value.encode() + b"\r\n")
    for name, (filename, data, ctype) in files.items():
        out.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename="{filename}"\r\n'
                   f"Content-Type: {ctype}\r\n\r\n".encode() + data + b"\r\n")
    out.append(f"--{boundary}--\r\n".encode())
    return b"".join(out), f"multipart/form-data; boundary={boundary}"


def gradle_property(name):
    with open(os.path.join(ROOT, "gradle.properties"), encoding="utf-8") as f:
        for line in f:
            if line.startswith(name + "="):
                return line.split("=", 1)[1].strip()
    sys.exit(f"::error::{name} missing from gradle.properties")


def game_version_ids(key, mc):
    """CurseForge's numeric IDs for the Minecraft version, Fabric, Java 25 and client + server."""
    types = {t["id"]: t["slug"] for t in request("GET", "game/version-types", key)}
    versions = request("GET", "game/versions", key)

    def find(type_test, name):
        return [v["id"] for v in versions if v["name"] == name and type_test(types.get(v["gameVersionTypeID"], ""))]

    minecraft = find(lambda s: s.startswith("minecraft"), mc)
    if not minecraft:
        return None
    ids = minecraft[:1]
    ids += find(lambda s: s == "modloader", "Fabric")[:1]
    ids += find(lambda s: s == "java", "Java 25")[:1]
    ids += find(lambda s: s == "environment", "Client")[:1] + find(lambda s: s == "environment", "Server")[:1]
    return ids


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--version", required=True)
    ap.add_argument("--jar", required=True)
    ap.add_argument("--changelog", help="file with the release notes (Markdown)")
    ap.add_argument("--dry-run", action="store_true", help="print what would be uploaded, upload nothing")
    a = ap.parse_args()

    mc = gradle_property("minecraft_version")
    release_type = "alpha" if "alpha" in a.version else "beta" if re.search(r"beta|rc|pre", a.version) else "release"
    changelog = open(a.changelog, encoding="utf-8").read().strip() if a.changelog else ""
    metadata = {
        "changelog": changelog or f"The Sift Unbound {a.version}",
        "changelogType": "markdown",
        "displayName": f"The Sift Unbound {a.version} (Minecraft {mc})",
        "releaseType": release_type,
        "relations": {"projects": [{"slug": "fabric-api", "type": "requiredDependency"}]},
    }
    if a.dry_run:
        print(json.dumps(metadata, indent=2))
        print(f"file: {a.jar} ({os.path.getsize(a.jar)} bytes), Minecraft {mc}, Fabric")
        return

    key = os.environ.get("CURSEFORGE_API_KEY", "").strip()
    project = os.environ.get("CURSEFORGE_PROJECT", "").strip() or "1798861"
    if not key:
        notice("CURSEFORGE_API_KEY is not set: skipping the CurseForge upload.")
        return
    ids = game_version_ids(key, mc)
    if ids is None:
        notice(f"CurseForge doesn't list Minecraft {mc} yet: skipping the CurseForge upload.", "warning")
        return
    metadata["gameVersions"] = ids
    with open(a.jar, "rb") as f:
        jar = f.read()
    body, ctype = multipart({"metadata": json.dumps(metadata)},
                            {"file": (os.path.basename(a.jar), jar, "application/java-archive")})
    created = request("POST", f"projects/{project}/upload-file", key, body, ctype)
    print(f"Published {a.version} to CurseForge project {project} (file {created.get('id')}).")


if __name__ == "__main__":
    main()
