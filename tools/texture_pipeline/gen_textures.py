#!/usr/bin/env python3
"""Generates every texture shipped with The Sift Unbound.

All art is ORIGINAL: it is drawn procedurally (seeded noise, shapes and
hand-placed pixel patterns) using only the colour palettes stored in
palettes.json (produced by extract_palette.py from reference screenshots).
No pixels from Mojang's images are copied.

Run from the repository root:
    python3 tools/texture_pipeline/gen_textures.py
Outputs go to src/main/resources/assets/sift_unbound/textures/ and the mod icon.
A preview sheet is written to build/texture_preview.png.
"""
import json
import os

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "sift_unbound")
TEX = os.path.join(ASSETS, "textures")
PAL = json.load(open(os.path.join(HERE, "palettes.json")))

GENERATED = []


# --------------------------------------------------------------------------- helpers
def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def pal(name):
    """Palette colours of a material sorted dark -> light."""
    return [hexrgb(c) for c in PAL[name]["by_brightness"]]


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


def ramp(colors, n):
    """Interpolate a list of colours into n evenly spaced shades."""
    out = []
    for i in range(n):
        t = i / max(1, n - 1) * (len(colors) - 1)
        j = min(int(t), len(colors) - 2)
        out.append(mix(colors[j], colors[j + 1], t - j))
    return out


def rng(seed):
    return np.random.default_rng(seed)


def value_noise(w, h, seed, scale=4, octaves=2):
    """Tileable value noise in [0,1]."""
    r = rng(seed)
    acc = np.zeros((h, w))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        s = max(1, scale * (2 ** o))
        grid = r.random((s, s))
        ys = np.arange(h) * s / h
        xs = np.arange(w) * s / w
        y0 = np.floor(ys).astype(int); x0 = np.floor(xs).astype(int)
        fy = ys - y0; fx = xs - x0
        fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
        y1 = (y0 + 1) % s; x1 = (x0 + 1) % s
        a = grid[np.ix_(y0, x0)]; b = grid[np.ix_(y0, x1)]
        c = grid[np.ix_(y1, x0)]; d = grid[np.ix_(y1, x1)]
        top = a + (b - a) * fx[None, :]; bot = c + (d - c) * fx[None, :]
        acc += amp * (top + (bot - top) * fy[:, None])
        tot += amp; amp *= 0.5
    acc /= tot
    return (acc - acc.min()) / max(1e-9, acc.max() - acc.min())


def from_levels(levels, shades, alpha=None):
    """Map an array of float levels [0,1] onto a list of shades."""
    h, w = levels.shape
    idx = np.clip((levels * len(shades)).astype(int), 0, len(shades) - 1)
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            a = 255 if alpha is None else int(alpha[y, x])
            px[x, y] = shades[idx[y, x]] + (a,)
    return img


def save(img, rel):
    path = os.path.join(TEX, rel + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    GENERATED.append((rel, img))
    return img


def save_mcmeta(rel, frametime, interpolate=False):
    path = os.path.join(TEX, rel + ".png.mcmeta")
    with open(path, "w") as f:
        json.dump({"animation": {"frametime": frametime, "interpolate": interpolate}}, f, indent=2)
        f.write("\n")


def speckle(img, seed, color, count):
    r = rng(seed)
    px = img.load()
    w, h = img.size
    for _ in range(count):
        x, y = int(r.integers(w)), int(r.integers(h))
        if px[x, y][3] > 0:
            px[x, y] = color + (255,)
    return img


def noisy_block(seed, shades, scale=4, octaves=2, bias=0.0):
    lv = np.clip(value_noise(16, 16, seed, scale, octaves) + bias, 0, 1)
    return from_levels(lv, shades)


# --------------------------------------------------------------------------- palettes
SALMON = ramp(pal("sift_ground_vanilla"), 5)                 # vanilla Sift grass/ground salmon
GRASS = ramp([hexrgb("#c9444f")] + [c for c in pal("sift_grass_vanilla") if c[0] > c[1] + 40], 6)  # salmon only (drop teal sky bleed)
SKY = hexrgb(PAL["sift_sky_vanilla"]["colors"][0]["hex"])
RED = ramp(pal("red_terrain")[:4], 5)                         # Dungeons II red "healthy sculk"
TEAL = ramp(pal("teal_foliage")[2:], 5)
SAND = ramp(pal("huskland_sand")[2:], 5)
LEAF = ramp(pal("pale_tree_leaves")[2:], 5)                   # pale blue canopy
ROCK = ramp(pal("lullaby_rock")[:5], 5)
PORTAL = ramp(pal("portal_glow")[1:], 5)
BLUB = ramp(pal("blub_soul_engorged")[1:], 5)
MEADOW = ramp(pal("blub_meadows"), 6)
CARAPACE = pal("blub_carapace")
ICHOR = [hexrgb(c["hex"]) for c in PAL["ichor"]["colors"] + PAL["ichor_pond"]["colors"]]
NIGHT = ramp(pal("endure_tide_night"), 6)
# Soulshale: dark teal stone (the 26.4 snapshot's chiseled soulshale is dark teal; colours only)
SHALE = ramp([hexrgb("#0f2b35"), hexrgb("#1a434a"), hexrgb("#24565a"), hexrgb("#2f6a66"), hexrgb("#3b7c74")], 5)
SOIL = ramp([hexrgb("#5c2f37"), hexrgb("#7a3f45"), hexrgb("#97535a"), hexrgb("#b0686b")], 5)
PALE_BARK = ramp([hexrgb("#6f86a3"), hexrgb("#93abc4"), hexrgb("#b9cde0"), hexrgb("#dbe7f2")], 5)
PALE_WOOD = ramp([hexrgb("#9fb5c9"), hexrgb("#bccfdd"), hexrgb("#d3e1ea"), hexrgb("#e8f0f5")], 5)
HUSK = ramp([hexrgb("#8f8063"), hexrgb("#b3a582"), hexrgb("#d2c7a7"), hexrgb("#ebe3cc")], 5)
SOUL_CYAN = ramp([hexrgb("#0c4f5c"), hexrgb("#1c9591"), hexrgb("#4fcdd5"), hexrgb("#bef0f3")], 6)


# --------------------------------------------------------------------------- terrain blocks
def gen_terrain():
    # Sift soil: dusky rose dirt
    soil = noisy_block(11, SOIL, 5, 2)
    speckle(soil, 12, SOIL[0], 14); speckle(soil, 13, SOIL[4], 8)
    save(soil, "block/sift_soil")

    # Sift grass block top: soft salmon tufts
    top = noisy_block(21, GRASS[1:], 6, 2)
    speckle(top, 22, GRASS[0], 18); speckle(top, 23, GRASS[5], 16)
    save(top, "block/sift_grass_block_top")

    # side: soil with a ragged salmon fringe
    side = soil.copy()
    sp = side.load(); tp = top.load()
    r = rng(24)
    for x in range(16):
        depth = 3 + int(r.integers(0, 3))
        for y in range(depth):
            sp[x, y] = tp[x, y]
        if r.random() < 0.5:
            sp[x, depth] = GRASS[1] + (255,)
    save(side, "block/sift_grass_block_side")

    # red "healthy" sift sculk with glowing specks
    sculk = noisy_block(31, RED, 4, 3)
    px = sculk.load(); r = rng(32)
    for _ in range(7):
        x, y = int(r.integers(1, 15)), int(r.integers(1, 15))
        px[x, y] = hexrgb("#ff9a7a") + (255,)
    save(sculk, "block/sift_sculk")

    # dark sift sculk (husklands)
    dark = noisy_block(33, ramp([NIGHT[0], hexrgb("#2b2d3f"), RED[0], hexrgb("#3d2a3a")], 5), 4, 3)
    px = dark.load(); r = rng(34)
    for _ in range(6):
        x, y = int(r.integers(1, 15)), int(r.integers(1, 15))
        px[x, y] = SOUL_CYAN[3] + (255,)
    save(dark, "block/dark_sift_sculk")

    # sift sand
    sand = noisy_block(41, SAND, 6, 2)
    speckle(sand, 42, SAND[0], 10); speckle(sand, 43, hexrgb("#f1ecd9"), 10)
    save(sand, "block/sift_sand")

    # husk shell: layered bone-like plates
    husk = Image.new("RGBA", (16, 16)); px = husk.load(); n = value_noise(16, 16, 51, 4, 2)
    for y in range(16):
        band = (y // 4) % 2
        for x in range(16):
            lv = n[y, x] * 0.6 + (0.4 if band else 0.15)
            if y % 4 == 3:
                lv -= 0.45
            px[x, y] = HUSK[int(np.clip(lv, 0, 0.999) * 5)] + (255,)
    save(husk, "block/husk_shell")
    top = noisy_block(52, HUSK[1:], 3, 2)
    d = ImageDraw.Draw(top); d.ellipse([3, 3, 12, 12], outline=HUSK[0] + (255,)); d.ellipse([6, 6, 9, 9], outline=HUSK[1] + (255,))
    save(top, "block/husk_shell_top")


def stone_like(seed, shades):
    return noisy_block(seed, shades, 4, 3)


def gen_soulshale():
    base = stone_like(61, SHALE)
    # thin strata lines
    px = base.load(); r = rng(62)
    for y in (4, 10, 14):
        for x in range(16):
            if r.random() < 0.6:
                px[x, y] = SHALE[0] + (255,)
    save(base, "block/soulshale")

    # cobbled: rounded stones with dark mortar
    cob = Image.new("RGBA", (16, 16), SHALE[0] + (255,)); d = ImageDraw.Draw(cob); r = rng(63)
    stones = [(0, 0, 6, 5), (7, 0, 15, 4), (0, 6, 4, 11), (5, 5, 10, 10), (11, 5, 15, 11), (0, 12, 7, 15), (8, 11, 15, 15)]
    for i, (x0, y0, x1, y1) in enumerate(stones):
        c = SHALE[1 + (i % 3)]
        d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=c + (255,))
        d.line([x0, y0, x1 - 1, y0], fill=SHALE[min(4, 2 + i % 3)] + (255,))
    speckle(cob, 64, SHALE[4], 6)
    save(cob, "block/cobbled_soulshale")

    # polished: smooth with bevel
    pol = noisy_block(65, SHALE[1:4], 2, 1)
    d = ImageDraw.Draw(pol)
    d.rectangle([0, 0, 15, 15], outline=SHALE[0] + (255,))
    d.line([1, 1, 14, 1], fill=SHALE[4] + (255,)); d.line([1, 1, 1, 14], fill=SHALE[4] + (255,))
    save(pol, "block/polished_soulshale")

    # bricks
    br = Image.new("RGBA", (16, 16), SHALE[0] + (255,)); d = ImageDraw.Draw(br); n = value_noise(16, 16, 66, 4, 2)
    rows = [(0, 3, 0), (4, 7, 4), (8, 11, 0), (12, 15, 4)]
    for y0, y1, off in rows:
        for bx in range(-8 + off, 16, 8):
            x0, x1 = max(0, bx), min(15, bx + 6)
            for y in range(y0, y1):
                for x in range(x0, x1 + 1):
                    br.putpixel((x, y), SHALE[1 + int(n[y, x] * 2.99)] + (255,))
            for x in range(x0, x1 + 1):
                br.putpixel((x, y0), SHALE[4] + (255,))
    save(br, "block/soulshale_bricks")

    # chiseled: a carved singer (vanilla 26.4: chiseled soulshale "depicts a singer"). A healthy warden with curling horns,
    # an open singing mouth and raised arms, a soul glowing in its chest, and the song rising around it.
    ch = pol.copy(); d = ImageDraw.Draw(ch)
    d.rectangle([1, 1, 14, 14], outline=SHALE[0] + (255,))
    panel = value_noise(16, 16, 67, 4, 1)
    for y in range(2, 14):  # a lighter recessed panel, so the carved figure reads as a silhouette
        for x in range(2, 14):
            ch.putpixel((x, y), SHALE[3 if panel[y, x] < 0.6 else 4] + (255,))
    singer = [
        ".#........#.",
        ".##......##.",
        "..##.##.##..",
        "...######...",
        "...#.##.#...",
        "...######...",
        "....#..#....",
        "#..######..#",
        ".#.##OO##.#.",
        "..###OO###..",
        "....####....",
        "...##..##...",
    ]
    for y, row in enumerate(singer):
        for x, c in enumerate(row):
            px, py = x + 2, y + 2
            if c == "#":
                ch.putpixel((px, py), SHALE[0] + (255,))
            elif c == "O":
                ch.putpixel((px, py), SOUL_CYAN[3] + (255,))
    for x, y in ((2, 9), (13, 9)):  # the eyes catch the light
        pass
    for x, y in ((1, 4), (14, 4), (1, 6), (14, 6)):  # the song rising on both sides, cut into the frame
        ch.putpixel((x, y), SOUL_CYAN[2] + (255,))
    save(ch, "block/chiseled_soulshale")

    # soul shard ore
    ore = base.copy(); px = ore.load()
    for cx, cy in ((4, 4), (11, 6), (6, 11), (12, 12)):
        for dx, dy, c in ((0, 0, 4), (1, 0, 3), (0, 1, 2), (-1, 0, 2), (0, -1, 3)):
            x, y = cx + dx, cy + dy
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = SOUL_CYAN[c] + (255,)
    save(ore, "block/soul_shard_ore")

    blk = Image.new("RGBA", (16, 16)); px = blk.load(); n = value_noise(16, 16, 67, 3, 2)
    for y in range(16):
        for x in range(16):
            facet = ((x + y) % 6 < 3)
            px[x, y] = SOUL_CYAN[int(np.clip(n[y, x] * 0.5 + (0.45 if facet else 0.2), 0, 0.999) * 6)] + (255,)
    ImageDraw.Draw(blk).rectangle([0, 0, 15, 15], outline=SOUL_CYAN[0] + (255,))
    save(blk, "block/soul_shard_block")


# --------------------------------------------------------------------------- plants
def blade_plant(seed, shades, tall_top=None, tall_bottom=None, density=11, max_h=14):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = img.load(); r = rng(seed)
    for _ in range(density):
        x = int(r.integers(1, 15)); h = int(r.integers(5, max_h + 1)); lean = r.choice([-1, 0, 0, 1])
        for i in range(h):
            y = 15 - i
            xx = int(np.clip(x + (lean if i > h * 0.6 else 0), 0, 15))
            c = shades[min(len(shades) - 1, int(i / h * len(shades)))]
            px[xx, y] = c + (255,)
            if i < h * 0.4 and xx + 1 < 16 and r.random() < 0.5:
                px[xx + 1, y] = shades[0] + (255,)
    return img


def gen_plants():
    save(blade_plant(71, GRASS[:5], density=12, max_h=12), "block/short_sift_grass")
    # tall: two halves
    bottom = blade_plant(72, GRASS[:4], density=14, max_h=16)
    top = blade_plant(73, GRASS[1:6], density=10, max_h=13)
    save(bottom, "block/tall_sift_grass_bottom"); save(top, "block/tall_sift_grass_top")
    save(blade_plant(74, TEAL, density=9, max_h=10), "block/teal_sprouts")

    # lullaby bloom: tall stem with drooping pale-blue bell flowers that glow
    fl = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(fl)
    d.line([8, 15, 8, 6], fill=TEAL[1] + (255,)); d.line([8, 11, 5, 9], fill=TEAL[2] + (255,)); d.line([8, 9, 11, 7], fill=TEAL[2] + (255,))
    for (cx, cy) in ((8, 3), (4, 7), (12, 5)):
        d.rectangle([cx - 1, cy - 1, cx + 1, cy + 1], fill=LEAF[3] + (255,))
        fl.putpixel((cx, cy + 2), SOUL_CYAN[4] + (255,))
        fl.putpixel((cx, cy - 1), LEAF[4] + (255,))
    save(fl, "block/lullaby_bloom")

    # pallid sapling
    sp = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(sp)
    d.line([8, 15, 8, 8], fill=PALE_BARK[1] + (255,))
    d.ellipse([3, 2, 13, 9], fill=LEAF[2] + (255,)); d.ellipse([5, 3, 11, 7], fill=LEAF[4] + (255,))
    for x in (4, 7, 10, 12):
        sp.putpixel((x, 10), LEAF[1] + (255,))
    save(sp, "block/pallid_sapling")


# --------------------------------------------------------------------------- pallid wood
def gen_wood():
    # bark: vertical streaks
    bark = Image.new("RGBA", (16, 16)); px = bark.load(); n = value_noise(16, 16, 81, 8, 2); r = rng(82)
    cols = r.random(16)
    for y in range(16):
        for x in range(16):
            lv = n[y, x] * 0.4 + cols[x] * 0.6
            px[x, y] = PALE_BARK[int(np.clip(lv, 0, 0.999) * 5)] + (255,)
    for x in (3, 9, 13):
        for y in range(16):
            if r.random() < 0.7:
                px[x, y] = PALE_BARK[0] + (255,)
    save(bark, "block/pallid_log")

    def rings(seed, outer, inner):
        img = Image.new("RGBA", (16, 16)); px = img.load()
        for y in range(16):
            for x in range(16):
                dist = max(abs(x - 7.5), abs(y - 7.5))
                if dist > 6.5:
                    px[x, y] = outer[int((x * 7 + y * 3) % 3)] + (255,)
                else:
                    px[x, y] = inner[int(dist) % 2 + (1 if int(dist) % 3 == 0 else 0)] + (255,)
        return img

    save(rings(83, PALE_BARK[0:3], PALE_WOOD[1:4]), "block/pallid_log_top")
    stripped = noisy_block(84, PALE_WOOD[1:], 8, 1)
    px = stripped.load()
    for x in (2, 7, 12):
        for y in range(16):
            px[x, y] = PALE_WOOD[1] + (255,)
    save(stripped, "block/stripped_pallid_log")
    save(rings(85, PALE_WOOD[1:4], PALE_WOOD[0:3]), "block/stripped_pallid_log_top")

    planks = Image.new("RGBA", (16, 16)); px = planks.load(); n = value_noise(16, 16, 86, 8, 1)
    for y in range(16):
        for x in range(16):
            row = y // 4
            lv = n[y, x] * 0.5 + 0.35
            if y % 4 == 3:
                lv = 0.02
            if (x == (row * 5 + 3) % 16) and y % 4 != 3:
                lv = 0.15
            px[x, y] = PALE_WOOD[int(np.clip(lv, 0, 0.999) * 5)] + (255,)
    save(planks, "block/pallid_planks")

    # leaves: pale blue canopy with drips at the bottom edge and see-through holes
    lv = value_noise(16, 16, 87, 4, 3)
    alpha = np.where(lv < 0.18, 0, 255)
    leaves = from_levels(lv, LEAF, alpha)
    px = leaves.load(); r = rng(88)
    for x in range(0, 16, 3):
        if r.random() < 0.7:
            px[x, 15] = LEAF[3] + (255,)
    save(leaves, "block/pallid_leaves")

    # door + trapdoor
    door_b = planks.copy(); d = ImageDraw.Draw(door_b)
    d.rectangle([0, 0, 15, 15], outline=PALE_BARK[0] + (255,)); d.rectangle([3, 2, 12, 13], outline=PALE_BARK[1] + (255,))
    d.point([(12, 1)], fill=SOUL_CYAN[3] + (255,))
    save(door_b, "block/pallid_door_bottom")
    door_t = planks.copy(); d = ImageDraw.Draw(door_t)
    d.rectangle([0, 0, 15, 15], outline=PALE_BARK[0] + (255,))
    d.rectangle([3, 3, 12, 12], fill=(0, 0, 0, 0)); d.rectangle([3, 3, 12, 12], outline=PALE_BARK[1] + (255,))
    d.line([7, 3, 7, 12], fill=PALE_BARK[1] + (255,)); d.line([3, 7, 12, 7], fill=PALE_BARK[1] + (255,))
    save(door_t, "block/pallid_door_top")
    trap = planks.copy(); d = ImageDraw.Draw(trap)
    d.rectangle([0, 0, 15, 15], outline=PALE_BARK[0] + (255,))
    for (x0, y0) in ((2, 2), (9, 2), (2, 9), (9, 9)):
        d.rectangle([x0, y0, x0 + 4, y0 + 4], fill=(0, 0, 0, 0))
    save(trap, "block/pallid_trapdoor")

    # item icon for the door (2D)
    di = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    di.paste(door_t.resize((8, 8), Image.NEAREST), (4, 0)); di.paste(door_b.resize((8, 8), Image.NEAREST), (4, 8))
    save(di, "item/pallid_door")


# --------------------------------------------------------------------------- soul water + portal (animated)
def ichor_frame(t, w, seed, flow=False):
    """One frame of shimmering, oil-on-water style soul water."""
    ys, xs = np.mgrid[0:w, 0:w] / w
    shift = t / 32.0
    if flow:
        ys = (ys - shift) % 1.0
    n1 = value_noise(w, w, seed, 3, 2)
    n2 = value_noise(w, w, seed + 1, 2, 2)
    phase = (n1 * 1.6 + n2 * 0.8 + xs * 0.5 + ys * 0.7 + shift * (1.0 if not flow else 0.3)) % 1.0
    # rainbow ring of hues taken from the ichor palettes: teal, blue, pink, gold, green
    stops = [hexrgb("#2fc8d8"), hexrgb("#4a8fe0"), hexrgb("#c45cc0"), hexrgb("#f09ad0"), hexrgb("#e8e070"), hexrgb("#4cc88a"), hexrgb("#2fc8d8")]
    img = Image.new("RGBA", (w, w)); px = img.load()
    for y in range(w):
        for x in range(w):
            p = phase[y, x] * (len(stops) - 1)
            i = int(p)
            c = mix(stops[i], stops[i + 1], p - i)
            c = shade(c, 0.85 + 0.25 * n2[y, x])
            px[x, y] = c + (215,)
    # tiny soul sparkles
    r = rng(seed * 31 + t)
    for _ in range(3):
        x, y = int(r.integers(w)), int(r.integers(w))
        px[x, y] = (240, 255, 255, 230)
    return img


def gen_fluids():
    frames = 32
    still = Image.new("RGBA", (16, 16 * frames))
    for t in range(frames):
        still.paste(ichor_frame(t, 16, 91), (0, 16 * t))
    save(still, "block/soul_water_still"); save_mcmeta("block/soul_water_still", 3, True)
    flow = Image.new("RGBA", (32, 32 * frames))
    for t in range(frames):
        flow.paste(ichor_frame(t, 32, 93, flow=True), (0, 32 * t))
    save(flow, "block/soul_water_flow"); save_mcmeta("block/soul_water_flow", 2, True)
    # overlay used when looking through the fluid side
    save(ichor_frame(0, 16, 95), "block/soul_water_overlay")


def gen_portal():
    frames = 16
    sheet = Image.new("RGBA", (16, 16 * frames))
    for t in range(frames):
        img = Image.new("RGBA", (16, 16)); px = img.load()
        n = value_noise(16, 16, 101 + t % 4, 4, 2)
        for y in range(16):
            for x in range(16):
                wave = 0.5 + 0.5 * np.sin((x + y * 0.5) / 2.2 + t / frames * 2 * np.pi)
                lv = np.clip(n[y, x] * 0.55 + wave * 0.45, 0, 0.999)
                px[x, y] = SOUL_CYAN[1 + int(lv * 5)] + (190 + int(lv * 60),)
        sheet.paste(img, (0, 16 * t))
    save(sheet, "block/sift_portal"); save_mcmeta("block/sift_portal", 2, True)


# --------------------------------------------------------------------------- items
def outline(img, color):
    """Add a 1px dark outline around the opaque shape (item style)."""
    src = img.copy(); px = img.load(); sp = src.load(); w, h = img.size
    for y in range(h):
        for x in range(w):
            if sp[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < w and 0 <= yy < h and sp[xx, yy][3] > 0:
                        px[x, y] = color + (255,)
                        break
    return img


def gen_items():
    # Echo tuner: tuning-fork shaped resonator made of echo shards
    it = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(it)
    d.line([7, 14, 7, 9], fill=hexrgb("#2b2350") + (255,)); d.line([8, 14, 8, 9], fill=hexrgb("#3c3270") + (255,))
    d.line([5, 9, 10, 9], fill=hexrgb("#3c3270") + (255,))
    d.line([5, 8, 5, 2], fill=SOUL_CYAN[2] + (255,)); d.line([10, 8, 10, 2], fill=SOUL_CYAN[2] + (255,))
    d.point([(5, 2), (10, 2)], fill=SOUL_CYAN[4] + (255,))
    d.point([(3, 4), (12, 4), (2, 6), (13, 6)], fill=SOUL_CYAN[3] + (255,))
    save(outline(it, (16, 20, 40)), "item/echo_tuner")

    # Soul shard: faceted crystal
    sh = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(sh)
    d.polygon([(8, 1), (13, 7), (9, 15), (4, 9)], fill=SOUL_CYAN[2] + (255,))
    d.polygon([(8, 1), (9, 15), (4, 9)], fill=SOUL_CYAN[3] + (255,))
    d.line([8, 3, 8, 12], fill=SOUL_CYAN[5] + (255,))
    save(outline(sh, SOUL_CYAN[0]), "item/soul_shard")

    # Blub jelly: wobbly glowing cube treat
    bj = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(bj)
    d.rounded_rectangle([3, 5, 12, 13], radius=3, fill=BLUB[2] + (255,))
    d.rounded_rectangle([4, 6, 9, 9], radius=2, fill=BLUB[4] + (255,))
    d.point([(10, 11), (6, 12)], fill=BLUB[1] + (255,))
    save(outline(bj, BLUB[0]), "item/blub_jelly")

    # Soul water bucket: bucket shape + ichor surface
    bk = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(bk)
    metal = [hexrgb("#5d5d5d"), hexrgb("#8b8b8b"), hexrgb("#c3c3c3"), hexrgb("#e8e8e8")]
    d.polygon([(3, 5), (12, 5), (11, 14), (4, 14)], fill=metal[1] + (255,))
    d.line([4, 6, 4, 13], fill=metal[2] + (255,)); d.line([11, 6, 10, 13], fill=metal[0] + (255,))
    d.line([3, 4, 12, 4], fill=metal[3] + (255,))
    surf = ichor_frame(4, 16, 91)
    for x in range(4, 12):
        bk.putpixel((x, 5), surf.getpixel((x, 3))[:3] + (255,))
        bk.putpixel((x, 6), surf.getpixel((x, 9))[:3] + (255,))
    d.arc([2, 0, 13, 9], 180, 360, fill=metal[0] + (255,))
    save(outline(bk, (40, 40, 40)), "item/soul_water_bucket")

    # Bucket of blub
    bb = bk.copy(); d = ImageDraw.Draw(bb)
    d.rectangle([6, 3, 9, 6], fill=BLUB[3] + (255,)); d.point([(6, 2), (9, 2)], fill=BLUB[3] + (255,))
    d.point([(7, 5), (8, 5)], fill=hexrgb("#704350") + (255,))
    save(bb, "item/blub_bucket")

    # spawn eggs (one texture per entity, 26.x style)
    def egg(base, spots, seed):
        e = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(e)
        d.ellipse([3, 1, 12, 14], fill=base + (255,))
        r = rng(seed)
        for _ in range(7):
            x, y = int(r.integers(5, 11)), int(r.integers(3, 13))
            d.rectangle([x, y, x + 1, y], fill=spots + (255,))
        e.putpixel((5, 4), shade(base, 1.3) + (255,))
        return outline(e, shade(base, 0.45))
    save(egg(BLUB[3], hexrgb("#704350"), 111), "item/blub_spawn_egg")
    save(egg(NIGHT[1], SOUL_CYAN[3], 112), "item/sculk_prowler_spawn_egg")
    save(egg(SOUL_CYAN[4], SOUL_CYAN[1], 113), "item/soul_wisp_spawn_egg")


# --------------------------------------------------------------------------- entities
def paint_box(img, u, v, w, h, d, faces):
    """Paint a Minecraft box-UV cube. faces: dict side -> callable(img_region_size)->Image or RGB colour."""
    regions = {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "east": (u, v + d, d, h), "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h),
    }
    for side, (x, y, rw, rh) in regions.items():
        f = faces.get(side, faces.get("default"))
        if f is None:
            continue
        if callable(f):
            patch = f(rw, rh, side)
        else:
            patch = Image.new("RGBA", (rw, rh), f + (255,))
        img.paste(patch, (x, y))


def textured(shades, seed, scale=3):
    def f(w, h, side):
        lv = value_noise(max(w, 1), max(h, 1), seed + hash(side) % 97, scale, 2)
        return from_levels(lv, shades)
    return f


def gen_blub():
    """Blub texture (64x32). Model in BlubModel.java:
    body  texOffs(0,0)   box 8x7x8
    ears  texOffs(32,0)  box 2x4x1 (x2, second at texOffs(38,0))
    legs  texOffs(0,15)  box 2x2x2 (x4)
    """
    variants = {
        "blub": (BLUB, hexrgb("#704350")),
        "blub_meadows": (ramp([MEADOW[1], MEADOW[2], MEADOW[3], MEADOW[5]], 4), hexrgb("#24414a")),
        "blub_carapace": (ramp([hexrgb("#8e7f6d"), hexrgb("#cbb89c"), hexrgb("#e6d6b8")], 4), hexrgb("#0b4559")),
    }
    for name, (shades, eye) in variants.items():
        img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

        def body(w, h, side, shades=shades, eye=eye, name=name):
            lv = value_noise(w, h, 121 + len(side), 3, 2) * 0.6 + 0.3
            p = from_levels(lv, shades)
            pp = p.load()
            if side == "north":  # face: two sleepy slit eyes + mouth
                for x in (1, 2, 5, 6):
                    pp[x, 3] = eye + (255,)
                pp[3, 5] = shade(eye, 1.4) + (255,); pp[4, 5] = shade(eye, 1.4) + (255,)
            if side == "top" and name == "blub_meadows":
                r = rng(130)
                for _ in range(10):
                    pp[int(r.integers(w)), int(r.integers(h))] = MEADOW[0] + (255,)
            if side == "top" and name == "blub_carapace":
                d = ImageDraw.Draw(p); d.rectangle([1, 1, w - 2, h - 2], outline=hexrgb("#b35055") + (255,))
                d.rectangle([3, 3, w - 4, h - 4], fill=hexrgb("#9dcdad") + (255,))
            return p
        paint_box(img, 0, 0, 8, 7, 8, {"default": body})
        ear_col = shades[-1] if name != "blub_carapace" else hexrgb("#b35055")
        paint_box(img, 32, 0, 2, 4, 1, {"default": ear_col, "north": shade(ear_col, 0.85)})
        paint_box(img, 38, 0, 2, 4, 1, {"default": ear_col, "north": shade(ear_col, 0.85)})
        leg_col = shades[1] if name == "blub" else hexrgb("#d9c2a5")
        paint_box(img, 0, 15, 2, 2, 2, {"default": leg_col})
        if name == "blub_meadows":  # antenna light
            paint_box(img, 44, 0, 1, 5, 1, {"default": MEADOW[1]})
            paint_box(img, 48, 0, 2, 2, 2, {"default": SOUL_CYAN[3]})
        save(img, f"entity/blub/{name}")

        # emissive glow layer: only the bright parts glow
        glow = Image.new("RGBA", (64, 32), (0, 0, 0, 0)); gp = glow.load(); ip = img.load()
        for y in range(32):
            for x in range(64):
                c = ip[x, y]
                if c[3] and (name == "blub" and sum(c[:3]) > 520 or (name == "blub_meadows" and 48 <= x < 56)):
                    gp[x, y] = c
        save(glow, f"entity/blub/{name}_glow")


def gen_wisp():
    """Soul wisp (32x32): core 4x4x4 at (0,0), halo 6x6x6 at (0,8)."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    paint_box(img, 0, 0, 4, 4, 4, {"default": textured(SOUL_CYAN[3:], 140, 2)})

    def halo(w, h, side):
        lv = value_noise(w, h, 141 + len(side), 2, 2)
        a = np.where(lv > 0.35, 120, 0)
        return from_levels(lv, SOUL_CYAN[2:5], a)
    paint_box(img, 0, 8, 6, 6, 6, {"default": halo})
    save(img, "entity/soul_wisp")


def gen_prowler():
    """Sculk prowler (64x64). Model in SculkProwlerModel.java:
    body texOffs(0,0)  box 10x8x16
    head texOffs(0,24) box 8x7x7
    legs texOffs(30,24) box 3x8x3 (x4)
    tendrils texOffs(44,24) box 1x5x1 (x2)
    """
    dark = ramp([hexrgb("#0b1a26"), hexrgb("#11283a"), hexrgb("#183a4c"), hexrgb("#1f4b5b")], 4)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def skin(seed, face=False):
        def f(w, h, side):
            p = from_levels(value_noise(w, h, seed + len(side), 4, 2), dark)
            pp = p.load(); r = rng(seed * 7 + len(side))
            for _ in range(max(1, w * h // 22)):
                pp[int(r.integers(w)), int(r.integers(h))] = SOUL_CYAN[2] + (255,)
            if face and side == "north":
                for x in (1, 2, w - 3, w - 2):
                    pp[x, 2] = SOUL_CYAN[5] + (255,)
                for x in range(2, w - 2):
                    pp[x, h - 2] = hexrgb("#0a0f14") + (255,)
            return p
        return f
    paint_box(img, 0, 0, 10, 8, 16, {"default": skin(150)})
    paint_box(img, 0, 24, 8, 7, 7, {"default": skin(151, face=True)})
    paint_box(img, 30, 24, 3, 8, 3, {"default": skin(152)})
    paint_box(img, 44, 24, 1, 5, 1, {"default": SOUL_CYAN[3]})
    save(img, "entity/sculk_prowler")
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); gp = glow.load(); ip = img.load()
    for y in range(64):
        for x in range(64):
            c = ip[x, y]
            if c[3] and c[2] > 150:
                gp[x, y] = c
    save(glow, "entity/sculk_prowler_glow")


# --------------------------------------------------------------------------- misc
def gen_misc():
    # mob effect icon (18x18): a cracked soul heart
    ic = Image.new("RGBA", (18, 18), (0, 0, 0, 0)); d = ImageDraw.Draw(ic)
    d.polygon([(9, 15), (2, 7), (4, 3), (7, 3), (9, 5), (11, 3), (14, 3), (16, 7)], fill=SOUL_CYAN[2] + (255,))
    d.line([9, 5, 8, 8, 10, 10, 9, 13], fill=SOUL_CYAN[0] + (255,))
    d.point([(5, 5), (6, 4)], fill=SOUL_CYAN[5] + (255,))
    save(outline(ic, SOUL_CYAN[0]), "mob_effect/soulsick")

    # mod icon (128x128): teal sky, salmon hills, a blub silhouette, portal glow
    icon = Image.new("RGBA", (128, 128), SKY + (255,)); d = ImageDraw.Draw(icon)
    for i in range(0, 128, 8):
        d.rectangle([i, 92 - (i * 7 % 20), i + 7, 127], fill=SALMON[(i // 8) % 4] + (255,))
    d.rectangle([12, 22, 52, 86], outline=hexrgb("#1c2a33") + (255,), width=6)
    d.rectangle([18, 28, 46, 80], fill=SOUL_CYAN[3] + (255,))
    d.rectangle([64, 64, 104, 96], fill=BLUB[3] + (255,))
    d.rectangle([68, 50, 74, 64], fill=BLUB[4] + (255,)); d.rectangle([94, 50, 100, 64], fill=BLUB[4] + (255,))
    for x in (72, 76, 90, 94):
        d.rectangle([x, 76, x + 3, 78], fill=hexrgb("#704350") + (255,))
    d.rectangle([66, 96, 72, 102], fill=BLUB[2] + (255,)); d.rectangle([96, 96, 102, 102], fill=BLUB[2] + (255,))
    path = os.path.join(ASSETS, "icon.png"); icon.save(path); GENERATED.append(("icon", icon))


def preview():
    tiles = [(n, im) for n, im in GENERATED]
    cell = 72
    cols = 10
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * (cell + 12)), (40, 40, 48, 255)); d = ImageDraw.Draw(sheet)
    for i, (n, im) in enumerate(tiles):
        x, y = (i % cols) * cell, (i // cols) * (cell + 12)
        w, h = im.size
        if h > w * 1.5:  # animated strip: show first frame
            im = im.crop((0, 0, w, w))
        t = im.copy(); t.thumbnail((64, 64), Image.NEAREST) if max(im.size) > 64 else None
        t = im.resize((64, int(64 * im.size[1] / im.size[0])) if im.size[0] >= im.size[1] else (int(64 * im.size[0] / im.size[1]), 64), Image.NEAREST)
        sheet.alpha_composite(t, (x + 4, y + 12))
        d.text((x + 2, y), n.split("/")[-1][:12], fill=(230, 230, 230, 255))
    os.makedirs(os.path.join(ROOT, "build"), exist_ok=True)
    sheet.save(os.path.join(ROOT, "build", "texture_preview.png"))


if __name__ == "__main__":
    gen_terrain(); gen_soulshale(); gen_plants(); gen_wood(); gen_fluids(); gen_portal()
    gen_items(); gen_blub(); gen_wisp(); gen_prowler(); gen_misc()
    preview()
    print(f"generated {len(GENERATED)} textures")
