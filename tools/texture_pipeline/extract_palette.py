#!/usr/bin/env python3
"""Texture/colour extraction for The Rift.

Reads reference screenshots of the Sift (kept OUTSIDE this repository, because
they are Mojang's artwork), crops the regions listed in regions.json, and for
each material:

  * runs k-means colour clustering and writes the palette to palettes.json
    (plain hex colours - these are committed and drive gen_textures.py);
  * saves a 16x16 and a 64x64 "observed swatch" (a straight downscale of the
    crop) into --swatch-dir, for side-by-side comparison only;
  * renders a contact sheet (crop + swatch + palette strip) for review.

The swatches and contact sheet are reference material derived from Mojang's
images, so they are written to --swatch-dir (outside the repo) and must not
be committed or shipped. Only palettes.json is used by the mod.

Usage:
  python3 tools/texture_pipeline/extract_palette.py REFERENCE_DIR \
      --swatch-dir /some/private/folder [--out tools/texture_pipeline/palettes.json]
"""
import argparse
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))


def kmeans(pixels, k, iters=25, seed=1234):
    """Tiny deterministic k-means in RGB space. Returns (centres, counts)."""
    rng = np.random.default_rng(seed)
    pixels = pixels.astype(np.float64)
    if len(pixels) > 60000:
        pixels = pixels[rng.choice(len(pixels), 60000, replace=False)]
    # k-means++ initialisation
    centres = [pixels[rng.integers(len(pixels))]]
    for _ in range(1, k):
        d = np.min([((pixels - c) ** 2).sum(1) for c in centres], axis=0)
        centres.append(pixels[rng.choice(len(pixels), p=d / d.sum())])
    centres = np.array(centres)
    for _ in range(iters):
        labels = np.argmin(((pixels[:, None, :] - centres[None]) ** 2).sum(2), axis=1)
        new = np.array([pixels[labels == i].mean(0) if np.any(labels == i) else centres[i] for i in range(k)])
        if np.allclose(new, centres):
            break
        centres = new
    counts = np.bincount(labels, minlength=k)
    order = np.argsort(-counts)
    return centres[order], counts[order]


def to_hex(c):
    return "#%02x%02x%02x" % tuple(int(round(v)) for v in c)


def luminance(c):
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]


def crop_boxes(im, boxes):
    w, h = im.size
    return [im.crop((int(x0 * w), int(y0 * h), int(x1 * w), int(y1 * h))) for x0, y0, x1, y1 in boxes]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("reference_dir")
    ap.add_argument("--regions", default=os.path.join(HERE, "regions.json"))
    ap.add_argument("--out", default=os.path.join(HERE, "palettes.json"))
    ap.add_argument("--swatch-dir", required=True, help="private output folder for swatches + contact sheet (never commit)")
    args = ap.parse_args()

    regions = {k: v for k, v in json.load(open(args.regions)).items() if not k.startswith("_")}
    os.makedirs(args.swatch_dir, exist_ok=True)
    palettes = {}
    rows = []
    for name, spec in regions.items():
        path = os.path.join(args.reference_dir, spec["image"])
        if not os.path.exists(path):
            print(f"skip {name}: missing {spec['image']}", file=sys.stderr)
            continue
        im = Image.open(path).convert("RGB")
        crops = crop_boxes(im, spec["boxes"])
        px = np.concatenate([np.asarray(c).reshape(-1, 3) for c in crops])
        if spec.get("drop_dark"):
            px = px[px.sum(1) > 90]  # drop the black render background
        centres, counts = kmeans(px, spec.get("k", 6))
        total = counts.sum()
        entries = [{"hex": to_hex(c), "share": round(float(n) / total, 4)} for c, n in zip(centres, counts)]
        palettes[name] = {
            "source": spec["image"],
            "colors": entries,
            "by_brightness": [e["hex"] for e in sorted(entries, key=lambda e: luminance(bytes.fromhex(e["hex"][1:])))],
        }
        # observed swatches (reference only)
        first = crops[0]
        side = min(first.size)
        sq = first.crop((0, 0, side, side))
        sq.resize((16, 16), Image.BOX).save(os.path.join(args.swatch_dir, f"{name}_16.png"))
        sq.resize((64, 64), Image.BOX).resize((128, 128), Image.NEAREST).save(os.path.join(args.swatch_dir, f"{name}_64.png"))
        rows.append((name, first, sq.resize((16, 16), Image.BOX), entries))
        print(f"{name:22s} " + " ".join(e["hex"] for e in entries))

    with open(args.out, "w") as f:
        json.dump(palettes, f, indent=2)
        f.write("\n")

    # contact sheet
    rh = 140
    sheet = Image.new("RGB", (900, rh * len(rows)), "white")
    d = ImageDraw.Draw(sheet)
    for i, (name, crop, sw, entries) in enumerate(rows):
        y = i * rh
        c = crop.copy(); c.thumbnail((240, rh - 20))
        sheet.paste(c, (0, y + 18))
        sheet.paste(sw.resize((112, 112), Image.NEAREST), (250, y + 18))
        x = 380
        for e in entries:
            w = max(12, int(500 * e["share"]))
            d.rectangle([x, y + 18, x + w, y + 130], fill=e["hex"])
            x += w
        d.text((2, y + 2), name, fill="black")
    sheet.save(os.path.join(args.swatch_dir, "contact_sheet.png"))
    print(f"wrote {args.out} and {args.swatch_dir}/contact_sheet.png")


if __name__ == "__main__":
    main()
