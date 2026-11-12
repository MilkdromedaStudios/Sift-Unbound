#!/usr/bin/env python3
"""Fast static checks on the mod's resources (runs in CI before the Gradle build).

* every model / texture referenced from blockstates, models and item definitions exists
* every sound file referenced by sounds.json exists
* every block/item has an English name
* every registered block has a blockstate, and every block item has an item definition
* JSON files parse

Exit code 1 with a list of problems if anything is wrong.
"""
import glob
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
A = os.path.join(RES, "assets", "sift_unbound")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "milkdromedastudios", "siftunbound", "registry")

problems = []

for f in glob.glob(f"{RES}/**/*.json", recursive=True) + glob.glob(f"{RES}/**/*.mcmeta", recursive=True):
    try:
        json.load(open(f))
    except Exception as e:  # noqa: BLE001
        problems.append(f"invalid JSON: {os.path.relpath(f, ROOT)}: {e}")

for f in glob.glob(f"{A}/**/*.json", recursive=True):
    text = open(f).read()
    for ref in re.findall(r'"sift_unbound:((?:block|item|entity)/[a-z0-9_/]+)"', text):
        if not (os.path.exists(f"{A}/models/{ref}.json") or os.path.exists(f"{A}/textures/{ref}.png")):
            problems.append(f"missing model/texture sift_unbound:{ref} (from {os.path.relpath(f, ROOT)})")

sounds = json.load(open(f"{A}/sounds.json"))
for event, entry in sounds.items():
    for s in entry["sounds"]:
        name = s["name"] if isinstance(s, dict) else s
        path = f"{A}/sounds/{name.split(':', 1)[1]}.ogg"
        if not os.path.exists(path):
            problems.append(f"missing sound file for {event}: {os.path.relpath(path, ROOT)}")

lang = json.load(open(f"{A}/lang/en_us.json"))
blocks = re.findall(r'reg\("([a-z_]+)",.*?, (true|false)\);', open(f"{JAVA}/ModBlocks.java").read(), re.S)
blocks += [(m, "true") for m in re.findall(r'stairs\("([a-z_]+)"', open(f"{JAVA}/ModBlocks.java").read())]
for name, has_item in blocks:
    if f"block.sift_unbound.{name}" not in lang:
        problems.append(f"no English name for block {name}")
    if not os.path.exists(f"{A}/blockstates/{name}.json"):
        problems.append(f"no blockstate for block {name}")
    if has_item == "true" and not os.path.exists(f"{A}/items/{name}.json"):
        problems.append(f"no item definition for block item {name}")
for name in re.findall(r'(?:register|armor)\("([a-z_]+)"', open(f"{JAVA}/ModItems.java").read()):
    if f"item.sift_unbound.{name}" not in lang:
        problems.append(f"no English name for item {name}")
    if not os.path.exists(f"{A}/items/{name}.json"):
        problems.append(f"no item definition for item {name}")
for name in re.findall(r'register\("([a-z_]+)",\s*EntityType', open(f"{JAVA}/ModEntities.java").read()):
    if f"entity.sift_unbound.{name}" not in lang:
        problems.append(f"no English name for entity {name}")

# Every equipment asset must reference real armored texture layers, including baby humanoids.
for equipment in glob.glob(f"{A}/equipment/*.json"):
    definition = json.load(open(equipment))
    for layer in ("humanoid", "humanoid_leggings", "humanoid_baby"):
        entries = definition.get("layers", {}).get(layer, [])
        if not entries:
            problems.append(f"missing {layer} layer in {os.path.relpath(equipment, ROOT)}")
        for entry in entries:
            texture = entry.get("texture", "")
            if texture.startswith("sift_unbound:"):
                png = f"{A}/textures/entity/equipment/{layer}/{texture.split(':', 1)[1]}.png"
                if not os.path.isfile(png):
                    problems.append(f"missing equipment texture: {os.path.relpath(png, ROOT)}")


# Verify Sift equipment completeness and ensure leggings and baby texture atlases do
# not accidentally reuse the adult atlas. A 64x32 PNG can be syntactically valid yet
# render nearly invisible if only a couple of pixels have armor artwork.
import struct
armor_names = re.findall(r'armor\("([a-z_]+)"', open(f"{JAVA}/ModItems.java").read())
armor_sets = ("sifter", "humbler", "monarch")

def png_dimensions(path):
    with open(path, "rb") as image:
        head = image.read(24)
    if len(head) != 24 or head[:8] != bytes.fromhex('89504e470d0a1a0a') or head[12:16] != b"IHDR":
        raise ValueError("not a PNG")
    return struct.unpack(">II", head[16:24])

for name in armor_names:
    icon = f"{A}/textures/item/{name}.png"
    model = f"{A}/models/item/{name}.json"
    definition = f"{A}/items/{name}.json"
    recipe = f"{RES}/data/sift_unbound/recipe/{name}.json"
    if not all(os.path.isfile(p) for p in (icon, model, definition, recipe)):
        problems.append(f"incomplete armor assets or recipe: {name}")
        continue
    try:
        if png_dimensions(icon) != (16, 16):
            problems.append(f"armor icon {name}: expected 16x16")
        obj = json.load(open(recipe))
        if obj.get("type") != "minecraft:crafting_shaped" or obj.get("result", {}).get("id") != f"sift_unbound:{name}":
            problems.append(f"incorrect survival recipe: {name}")
        if not obj.get("pattern") or not obj.get("key"):
            problems.append(f"empty survival recipe: {name}")
    except (OSError, ValueError) as exc:
        problems.append(f"armor {name}: {exc}")

for name in armor_sets:
    images = {}
    for layer in ("humanoid", "humanoid_leggings", "humanoid_baby"):
        location = f"{A}/textures/entity/equipment/{layer}/{name}.png"
        try:
            if png_dimensions(location) != (64, 32):
                problems.append(f"armor atlas {name}/{layer}: expected 64x32")
            images[layer] = open(location, "rb").read()
        except (OSError, ValueError) as exc:
            problems.append(f"armor atlas {name}/{layer}: {exc}")
    if images.get("humanoid") == images.get("humanoid_leggings"):
        problems.append(f"{name}: leggings atlas duplicates adult armor (likely invisible leggings)")
    if images.get("humanoid") == images.get("humanoid_baby"):
        problems.append(f"{name}: baby armor atlas duplicates adult armor")

if problems:
    print("\n".join(problems))
    print(f"\n{len(problems)} problem(s)")
    sys.exit(1)
print(f"assets OK ({len(blocks)} blocks checked, {sum(len(e['sounds']) for e in sounds.values())} sounds)")
