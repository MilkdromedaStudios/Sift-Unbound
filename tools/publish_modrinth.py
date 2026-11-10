#!/usr/bin/env python3
"""Uploads a built jar to Modrinth as a new version of The Sift Unbound (used by the release workflows).

    python3 tools/publish_modrinth.py --version 0.2.0 --jar build/libs/sift-unbound-0.2.0+mc26.3.jar \
        --changelog release-notes.md [--dry-run]

Environment:
  MODRINTH_API_KEY   a Modrinth personal access token with the "Create versions" and "Read projects" scopes
                     (GitHub: Settings -> Secrets and variables -> Actions -> Secrets)
  MODRINTH_PROJECT   the Modrinth project's slug or ID (GitHub: ... -> Actions -> Variables)
If either is missing the upload is skipped with a notice, so releases still work without Modrinth. A version that is
already on Modrinth is skipped too, so re-running a release is safe.
"""
import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
import uuid

API = os.environ.get("MODRINTH_API", "https://api.modrinth.com/v2/")
UA = "MilkdromedaStudios/Sift-Unbound (release workflow; github.com/MilkdromedaStudios/Sift-Unbound)"
FABRIC_API = "P7dR8mSH"  # Modrinth project ID of Fabric API
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def notice(msg):
    print(f"::notice::{msg}" if os.environ.get("GITHUB_ACTIONS") else msg)


def request(method, path, key, body=None, content_type=None):
    headers = {"User-Agent": UA, "Authorization": key}
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(API + path, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", "replace")
        sys.exit(f"::error::Modrinth {method} {path} failed: HTTP {e.code} {detail}")


def multipart(fields, files):
    """fields: {name: str}; files: {name: (filename, bytes, content type)} -> (body, content type)."""
    boundary = uuid.uuid4().hex
    out = []
    for name, value in fields.items():
        out.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n'
                   f"Content-Type: application/json\r\n\r\n".encode() + value.encode() + b"\r\n")
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


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--version", required=True)
    ap.add_argument("--jar", required=True)
    ap.add_argument("--changelog", help="file with the release notes (Markdown)")
    ap.add_argument("--dry-run", action="store_true", help="print what would be uploaded, upload nothing")
    a = ap.parse_args()

    mc = gradle_property("minecraft_version")
    version_type = "alpha" if "alpha" in a.version else "beta" if re.search(r"beta|rc|pre", a.version) else "release"
    changelog = open(a.changelog, encoding="utf-8").read().strip() if a.changelog else ""
    data = {
        "name": f"The Sift Unbound {a.version}",
        "version_number": f"{a.version}+mc{mc}",
        "changelog": changelog,
        "dependencies": [{"project_id": FABRIC_API, "dependency_type": "required"}],
        "game_versions": [mc],
        "version_type": version_type,
        "loaders": ["fabric"],
        "featured": version_type == "release",
        "status": "listed",
        "file_parts": ["jar"],
        "primary_file": "jar",
    }
    if a.dry_run:
        print(json.dumps(data, indent=2))
        print(f"file: {a.jar} ({os.path.getsize(a.jar)} bytes)")
        return

    key = os.environ.get("MODRINTH_API_KEY", "").strip()
    project = os.environ.get("MODRINTH_PROJECT", "").strip()
    if not key:
        notice("MODRINTH_API_KEY is not set: skipping the Modrinth upload.")
        return
    if not project:
        notice("MODRINTH_PROJECT (the project's slug or ID) is not set: skipping the Modrinth upload.")
        return

    project_id = request("GET", f"project/{project}", key)["id"]
    existing = request("GET", f"project/{project_id}/version", key)
    if any(v["version_number"] == data["version_number"] for v in existing):
        notice(f"{data['version_number']} is already on Modrinth: nothing to upload.")
        return
    data["project_id"] = project_id
    with open(a.jar, "rb") as f:
        jar = f.read()
    body, ctype = multipart({"data": json.dumps(data)},
                            {"jar": (os.path.basename(a.jar), jar, "application/java-archive")})
    created = request("POST", "version", key, body, ctype)
    print(f"Published {data['version_number']} to Modrinth: https://modrinth.com/mod/{project}/version/{created['id']}")


if __name__ == "__main__":
    main()
