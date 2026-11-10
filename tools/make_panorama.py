#!/usr/bin/env python3
"""Turns the photo shoot's panorama shots into the title-screen panorama of The Sift Unbound.

    python3 tools/make_panorama.py SCREENSHOTS_DIR NAME [--preview preview.png]

SCREENSHOTS_DIR is build/run/clientGameTest/screenshots after `tools/photoshoot.sh`; NAME picks one of the panoramas
SiftPhotoshoot takes (pallid_grove or lullaby_hills). Each of the six cube faces was shot at a 100-degree field of view;
this crops the middle 90 degrees (so screen-edge effects of the shader pack never reach a seam), scales it to 1024 x 1024
and writes src/main/resources/assets/minecraft/textures/gui/title/background/panorama_0..5.png, which replace
Minecraft's title-screen panorama while the mod is installed. --preview also writes the faces as an unfolded cube.
"""
import argparse
import math
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "minecraft", "textures", "gui", "title", "background")
CAPTURE_FOV = 100.0  # SiftPhotoshoot.panorama()
SIZE = 1024


def face(path):
    img = Image.open(path).convert("RGB")
    w, h = img.size
    if w != h:
        raise SystemExit(f"{path} is {w}x{h}, expected a square shot")
    keep = math.tan(math.radians(45)) / math.tan(math.radians(CAPTURE_FOV / 2)) * w
    box = ((w - keep) / 2, (h - keep) / 2, (w + keep) / 2, (h + keep) / 2)
    return img.resize((SIZE, SIZE), Image.LANCZOS, box=box)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("shots")
    ap.add_argument("name")
    ap.add_argument("--preview")
    a = ap.parse_args()
    faces = [face(os.path.join(a.shots, f"panorama_{a.name}_{i}.png")) for i in range(6)]
    os.makedirs(OUT, exist_ok=True)
    for i, img in enumerate(faces):
        img.save(os.path.join(OUT, f"panorama_{i}.png"), optimize=True)
    print(f"wrote {OUT}/panorama_0..5.png from the '{a.name}' panorama")
    if a.preview:
        # unfolded cube: up above front, then front / right / back / left, down below front
        s = 256
        sheet = Image.new("RGB", (4 * s, 3 * s))
        for i, (col, row) in enumerate([(0, 1), (1, 1), (2, 1), (3, 1), (0, 0), (0, 2)]):
            sheet.paste(faces[i].resize((s, s)), (col * s, row * s))
        sheet.save(a.preview)
        print("preview:", a.preview)


if __name__ == "__main__":
    main()
