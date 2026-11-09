#!/usr/bin/env bash
# Takes "beauty shots" of The Sift Unbound with shaders.
#
# Downloads Sodium, Iris and the Complementary Reimagined shader pack from Modrinth into photoshoot/ (git-ignored:
# these are third-party files and are never committed or shipped with the mod), checks their SHA-512 hashes, then runs
# the client GameTest in photoshoot mode (SiftPhotoshoot) on a virtual display.
#
# Usage:  tools/photoshoot.sh            (Linux; needs Java 25, xvfb-run and Mesa)
# Output: build/run/clientGameTest/screenshots/*.png
set -euo pipefail
cd "$(dirname "$0")/.."
GRADLE=${GRADLE:-./gradlew}
MC=26.3
UA="SiftUnbound-photoshoot/0.1 (github.com/MilkdromedaStudios)"
mkdir -p photoshoot/mods photoshoot/shaderpacks photoshoot/config

fetch() { # project, loaders-json, dest-dir, version-filter(python expr on v)
  python3 - "$1" "$2" "$3" "$4" "$UA" "$MC" <<'PY'
import hashlib, json, os, sys, urllib.parse, urllib.request
project, loaders, dest, flt, ua, mc = sys.argv[1:7]
q = urllib.parse.urlencode({"loaders": loaders, "game_versions": json.dumps([mc])})
req = urllib.request.Request(f"https://api.modrinth.com/v2/project/{project}/version?{q}", headers={"User-Agent": ua})
versions = [v for v in json.load(urllib.request.urlopen(req)) if eval(flt)]
if not versions:
    sys.exit(f"no {project} version for {mc}")
f = next(f for f in versions[0]["files"] if f["primary"]) if any(f["primary"] for f in versions[0]["files"]) else versions[0]["files"][0]
path = os.path.join(dest, f["filename"])
if not os.path.exists(path):
    data = urllib.request.urlopen(urllib.request.Request(f["url"], headers={"User-Agent": ua})).read()
    if hashlib.sha512(data).hexdigest() != f["hashes"]["sha512"]:
        sys.exit(f"hash mismatch for {f['filename']}")
    open(path, "wb").write(data)
print(f"{project}: {versions[0]['version_number']} -> {path}")
PY
}

fetch sodium '["fabric"]' photoshoot/mods 'v["version_number"].startswith("mc'$MC'-0.9")'
fetch iris '["fabric"]' photoshoot/mods 'True'
fetch complementary-reimagined '["iris"]' photoshoot/shaderpacks 'True'

pack=$(ls photoshoot/shaderpacks | grep -i '\.zip$' | head -1)
printf 'shaderPack=%s\nenableShaders=true\ndisableUpdateMessage=true\n' "$pack" > photoshoot/config/iris.properties

# Minecraft 26.x opens its window through SDL; on Xvfb the OpenGL backend (which shaders need) works through EGL.
export SDL_VIDEO_FORCE_EGL=1 LIBGL_ALWAYS_SOFTWARE=1
xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX" $GRADLE runClientGameTest -Pphotoshoot --console=plain
echo "Screenshots: build/run/clientGameTest/screenshots/"
