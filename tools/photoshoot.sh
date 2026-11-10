#!/usr/bin/env bash
# Takes "beauty shots" of The Sift Unbound with shaders.
#
# Downloads Iris, the Sodium build that Iris asks for and a shader pack (Complementary Unbound unless
# SHADER=<modrinth slug>) from Modrinth into photoshoot/, checking their SHA-512 hashes. photoshoot/ is git-ignored:
# these are third-party files and are never committed or shipped with the mod. Then runs the client GameTest in
# photoshoot mode (SiftPhotoshoot) on a virtual display, with the OpenGL backend that Iris needs.
#
# Usage:  tools/photoshoot.sh            (Linux; needs Java 25, xvfb-run and Mesa)
#         SHADER=bsl-shaders tools/photoshoot.sh
# Then:   python3 tools/make_trailer.py build/run/clientGameTest/screenshots <music.mp3> trailer.mp4
# Output: build/run/clientGameTest/screenshots/*.png
set -euo pipefail
cd "$(dirname "$0")/.."
GRADLE=${GRADLE:-./gradlew}
MC=26.3
UA="SiftUnbound-photoshoot/0.1 (github.com/MilkdromedaStudios/Sift-Unbound)"
mkdir -p photoshoot/mods photoshoot/shaderpacks photoshoot/config

python3 -I - "$UA" "$MC" "${SHADER:-complementary-unbound}" <<'PY'
import hashlib, json, os, sys, urllib.parse, urllib.request
ua, mc, shader = sys.argv[1:4]
def api(path, **q):
    url = "https://api.modrinth.com/v2/" + path + ("?" + urllib.parse.urlencode(q) if q else "")
    return json.load(urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": ua})))
def newest(slug, loaders):
    vs = api(f"project/{slug}/version", loaders=json.dumps(loaders), game_versions=json.dumps([mc]))
    if not vs:
        sys.exit(f"no {slug} version for {mc}")
    return ([v for v in vs if v["version_type"] == "release"] or vs)[0]
def download(v, dest):
    f = next((f for f in v["files"] if f["primary"]), v["files"][0])
    path = os.path.join(dest, f["filename"])
    if not os.path.exists(path):
        data = urllib.request.urlopen(urllib.request.Request(f["url"], headers={"User-Agent": ua})).read()
        if hashlib.sha512(data).hexdigest() != f["hashes"]["sha512"]:
            sys.exit(f"hash mismatch for {f['filename']}")
        open(path, "wb").write(data)
    print(f"{v['version_number']} -> {path}")
    return path
iris = newest("iris", ["fabric"])
# the exact Sodium build this Iris release asks for (the newest Sodium is often a beta Iris doesn't support yet)
dep = next((d for d in iris["dependencies"] if d.get("project_id") == "AANobbMI" and d.get("version_id")), None)
sodium = api(f"version/{dep['version_id']}") if dep else newest("sodium", ["fabric"])
keep = {download(iris, "photoshoot/mods"), download(sodium, "photoshoot/mods")}
for old in os.listdir("photoshoot/mods"):  # never leave two Sodiums on the classpath
    if os.path.join("photoshoot/mods", old) not in keep:
        os.remove(os.path.join("photoshoot/mods", old))
pack = download(newest(shader, ["iris", "optifine"]), "photoshoot/shaderpacks")
open("photoshoot/shaderpack.txt", "w").write(os.path.basename(pack))
PY

pack=$(cat photoshoot/shaderpack.txt)
printf 'shaderPack=%s\nenableShaders=true\ndisableUpdateMessage=true\n' "$pack" > photoshoot/config/iris.properties

# Minecraft 26.x opens its window through SDL; on Xvfb the OpenGL backend (which shaders need) works through EGL.
export SDL_VIDEO_FORCE_EGL=1 LIBGL_ALWAYS_SOFTWARE=1
xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX" $GRADLE runClientGameTest -Pphotoshoot --console=plain
echo "Screenshots: build/run/clientGameTest/screenshots/"
