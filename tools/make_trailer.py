#!/usr/bin/env python3
"""Cuts the launch trailer for The Sift Unbound from the photo-shoot screenshots.

    python3 tools/make_trailer.py SHOTS_DIR MUSIC.mp3 OUT.mp4

SHOTS_DIR is build/run/clientGameTest/screenshots after `tools/photoshoot.sh` (or `runClientGameTest -Pphotoshoot`).
The music is not part of this repository: the launch trailer uses "Minecraft - Epic Trailer" by Kalamity Music,
credited in the end card. The edit is timed to that track (hit at 5.35 s, drop at 40.5 s, final hit at 64.85 s).

Every frame is drawn with Pillow (Ken Burns moves, crossfades, flashes, camera shake, letterbox, vignette and
titles) and piped to ffmpeg (libx264 + AAC).
"""
import math
import os
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont

W, H, FPS = 1920, 1080, 30
BAR = 128  # cinematic letterbox height
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ICON = os.path.join(ROOT, "src/main/resources/assets/sift_unbound/textures/item/echo_tuner.png")
SERIF = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
SANS = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
SANS_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
TEAL = (96, 213, 200)
SALMON = (241, 117, 130)


# ----------------------------------------------------------------------------- helpers
def ease(t):
    t = min(1.0, max(0.0, t))
    return t * t * (3 - 2 * t)


def font(path, size):
    return ImageFont.truetype(path, size)


def text_layer(lines, anchor="mm", y=None, glow=None):
    """lines: [(text, fontpath, size, colour)] stacked vertically, centred. Returns an RGBA layer."""
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    sizes = []
    for text, fp, size, _ in lines:
        f = font(fp, size)
        box = d.textbbox((0, 0), text, font=f)
        sizes.append((f, box[2] - box[0], box[3] - box[1], box[1]))
    gap = 26
    total = sum(h for _, _, h, _ in sizes) + gap * (len(lines) - 1)
    cy = (H // 2 if y is None else y) - total // 2
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    for (text, _, _, colour), (f, w, h, top) in zip(lines, sizes):
        x = (W - w) // 2
        y = cy - top  # textbbox's top offset: draw so the glyphs start exactly at cy
        sd.text((x + 3, y + 4), text, font=f, fill=(0, 0, 0, 200))
        if glow:
            sd.text((x, y), text, font=f, fill=glow + (255,))
        d.text((x, y), text, font=f, fill=colour + (255,))
        cy += h + gap
    shadow = shadow.filter(ImageFilter.GaussianBlur(10 if glow else 6))
    return Image.alpha_composite(shadow, layer)


def caption(text, size=54, colour=(245, 245, 245)):
    return text_layer([(text, SANS_B, size, colour)], y=H - BAR - 90)


def lower_third(title, sub=None):
    lines = [(title, SERIF, 64, (255, 255, 255))]
    if sub:
        lines.append((sub, SANS, 34, TEAL))
    return text_layer(lines, y=H - BAR - 110, glow=(30, 120, 115))


def vignette():
    ys, xs = np.mgrid[0:H, 0:W]
    dx, dy = (xs - W / 2) / (W / 2), (ys - H / 2) / (H / 2)
    r = np.sqrt(dx * dx * 0.8 + dy * dy)
    a = np.clip((r - 0.55) / 0.75, 0, 1) ** 1.6 * 190
    img = np.zeros((H, W, 4), np.uint8)
    img[..., 3] = a.astype(np.uint8)
    return Image.fromarray(img, "RGBA")


def grade(img):
    img = img.convert("RGB")
    img = ImageEnhance.Contrast(img).enhance(1.12)
    img = ImageEnhance.Color(img).enhance(1.18)
    return img


def kenburns(img, t, z0, z1, c0=(0.5, 0.5), c1=(0.5, 0.5), shake=0.0):
    """Crop a moving, zooming window out of img (t in 0..1) and scale it to W x H."""
    z = z0 + (z1 - z0) * ease(t)
    cx = c0[0] + (c1[0] - c0[0]) * ease(t)
    cy = c0[1] + (c1[1] - c0[1]) * ease(t)
    iw, ih = img.size
    w, h = iw / z, ih / z
    x0 = min(max(cx * iw - w / 2, 0), iw - w) + shake * math.sin(t * 90) * 6
    y0 = min(max(cy * ih - h / 2, 0), ih - h) + shake * math.cos(t * 77) * 6
    x0 = min(max(x0, 0), iw - w)
    y0 = min(max(y0, 0), ih - h)
    return img.resize((W, H), Image.BICUBIC, box=(x0, y0, x0 + w, y0 + h))


def tuner_card():
    """Dark card with the (original) Echo Tuner texture blown up and soul sparks; the recipe stays a secret."""
    rng = np.random.default_rng(7)
    ys = np.linspace(0, 1, H)[:, None]
    base = (np.array([10, 22, 30]) * (1 - ys)[..., None] + np.array([4, 40, 46]) * ys[..., None])
    arr = np.repeat(base, W, axis=1).astype(np.uint8)
    card = Image.fromarray(arr, "RGB").convert("RGBA")
    d = ImageDraw.Draw(card)
    for _ in range(160):
        x, y, r = rng.integers(W), rng.integers(H), rng.integers(1, 4)
        d.ellipse([x - r, y - r, x + r, y + r], fill=(120, 230, 230, int(rng.integers(60, 200))))
    icon = Image.open(ICON).convert("RGBA").resize((400, 400), Image.NEAREST)
    glow = icon.filter(ImageFilter.GaussianBlur(28))
    card.alpha_composite(glow, (W // 2 - 200, H // 2 - 300))
    card.alpha_composite(icon, (W // 2 - 200, H // 2 - 300))
    card.alpha_composite(text_layer([("Find the key.", SERIF, 76, (255, 255, 255))], y=H // 2 + 220, glow=(30, 120, 115)))
    return card.convert("RGB")


# ----------------------------------------------------------------------------- the edit
def build_timeline(shots):
    S = lambda n: shots[n]  # noqa: E731
    black = Image.new("RGB", (W, H), (0, 0, 0))
    title_bg = grade(S("06_pallid_grove_thrive_tide")).filter(ImageFilter.GaussianBlur(6))
    title_bg = ImageEnhance.Brightness(title_bg).enhance(0.45)
    # (start, end, image, kenburns args, overlays [(layer, t_in, t_out)], transition-in: cut|fade|flash)
    T = []

    def seg(a, b, img, kb=(1.0, 1.0), c0=(0.5, 0.5), c1=(0.5, 0.5), over=(), trans="fade", shake=0.0):
        T.append(dict(a=a, b=b, img=img, kb=kb, c0=c0, c1=c1, over=list(over), trans=trans, shake=shake))

    # A teaser, not a tour: no biome names, no recipe, only some of the places and creatures. The rest is
    # left for players to find (husklands, meadows, the Thrive Tide, soul wisps, most blub variants...).
    seg(0.0, 5.35, black, over=[(text_layer([("In 2027, Mojang opens a new dimension.", SANS, 50, (230, 230, 230))]), 0.9, 4.7)])
    seg(5.35, 12.0, S("01_warden_portal_dormant"), (1.0, 1.08), (0.5, 0.45), (0.5, 0.42), trans="flash",
        over=[(caption("Deep inside every ancient city, a frame waits."), 6.0, 11.4)])
    seg(12.0, 16.0, tuner_card(), (1.0, 1.04), trans="fade")
    seg(15.95, 22.0, S("01_warden_portal_dormant"), (1.7, 1.9), (0.36, 0.42), (0.46, 0.42), trans="fade",
        over=[(caption("Play the melody that wakes it."), 16.6, 21.4)])
    seg(22.0, 28.5, black, trans="fade",
        over=[(text_layer([("The Sift arrives in Minecraft next year.", SANS, 52, (230, 230, 230))]), 22.4, 25.2),
              (text_layer([("Why wait until next year…", SERIF, 92, (255, 255, 255))], glow=(40, 140, 135)), 25.4, 28.3)])
    seg(28.5, 36.0, S("01_warden_portal_dormant"), (1.1, 1.3), (0.5, 0.45), (0.5, 0.43), trans="fade",
        over=[(text_layer([("…when you can get it now?", SERIF, 92, (255, 255, 255))], glow=(40, 140, 135)), 29.0, 35.4)])
    seg(36.0, 40.5, S("01_warden_portal_dormant"), (1.3, 2.1), (0.5, 0.43), (0.5, 0.42), trans="cut", shake=0.6)
    seg(40.5, 42.1, S("02_warden_portal_awakened"), (1.25, 1.0), (0.5, 0.42), (0.5, 0.45), trans="flash", shake=1.0)
    seg(42.1, 43.4, S("03_warden_portal_close"), (1.0, 1.08), trans="cut")
    seg(43.4, 46.05, S("04_arrival_in_the_sift"), (1.0, 1.1), trans="fade", over=[(caption("Step through."), 43.7, 45.9)])
    seg(46.05, 49.0, S("05_pallid_grove_flow_tide"), (1.0, 1.12), (0.4, 0.5), (0.6, 0.47), trans="fade")
    seg(49.0, 51.6, S("09_soul_wetlands_thrive_tide"), (1.05, 1.15), (0.5, 0.55), (0.5, 0.5), trans="cut",
        over=[(caption("Don't touch the water."), 49.3, 51.4)])
    seg(51.6, 54.2, S("11_lullaby_hills_thrive_tide"), (1.0, 1.1), (0.6, 0.5), (0.4, 0.5), trans="fade")
    seg(54.2, 56.8, S("07_pallid_grove_endure_tide"), (1.08, 1.16), trans="fade",
        over=[(caption("When the tide turns…"), 54.5, 56.6)])
    seg(56.8, 59.6, S("12_blubs_by_the_soul_water"), (1.7, 1.9), (0.5, 0.66), (0.52, 0.65), trans="cut",
        over=[(caption("Make some friends…"), 57.0, 59.4)])
    # the hunter itself is never shown: just the dark Endure Tide horizon
    seg(59.6, 62.0, ImageEnhance.Brightness(S("13_sculk_prowler_endure_tide")).enhance(0.6), (1.6, 1.85), (0.55, 0.3), (0.5, 0.32),
        trans="cut", shake=0.4,
        over=[(caption("…and run from the rest."), 59.8, 61.8)])
    # rapid detail flashes, no context
    seg(62.0, 62.95, S("12_blubs_by_the_soul_water"), (2.6, 2.8), (0.22, 0.72), (0.25, 0.72), trans="cut")
    seg(62.95, 63.9, S("05_pallid_grove_flow_tide"), (2.2, 2.4), (0.62, 0.5), (0.6, 0.5), trans="cut")
    seg(63.9, 64.85, S("02_warden_portal_awakened"), (2.6, 3.0), (0.5, 0.45), (0.5, 0.45), trans="cut")
    title = text_layer([("THE SIFT UNBOUND", SERIF, 128, (255, 255, 255)),
                        ("Why wait until next year? Get it now.", SANS_B, 52, SALMON),
                        ("Free  ·  Fabric  ·  Minecraft Java 26.3", SANS, 40, TEAL)], glow=(40, 160, 150))
    seg(64.85, 70.6, title_bg, (1.0, 1.05), trans="flash", shake=0.5, over=[(title, 64.95, 70.3)])
    credits = text_layer([("Music", SANS, 34, TEAL),
                          ("“Minecraft – Epic Trailer” by Kalamity Music", SANS_B, 52, (255, 255, 255)),
                          (" ", SANS, 20, (0, 0, 0)),
                          ("The Sift Unbound is an unofficial fan mod by MilkdromedaStudios.", SANS, 34, (220, 220, 220)),
                          ("Not affiliated with Mojang Studios or Microsoft. Minecraft and the Sift © Mojang Studios.", SANS, 30, (180, 180, 180)),
                          ("github.com/MilkdromedaStudios/The-Rift", SANS_B, 36, SALMON)])
    seg(70.6, 99.0, black, trans="fade", over=[(credits, 71.0, 78.2)])
    return T


def render(shots_dir, music, out):
    names = ["01_warden_portal_dormant", "02_warden_portal_awakened", "03_warden_portal_close", "04_arrival_in_the_sift",
             "05_pallid_grove_flow_tide", "06_pallid_grove_thrive_tide", "07_pallid_grove_endure_tide",
             "09_soul_wetlands_thrive_tide", "11_lullaby_hills_thrive_tide", "12_blubs_by_the_soul_water",
             "13_sculk_prowler_endure_tide"]
    shots = {n: grade(Image.open(os.path.join(shots_dir, n + ".png"))) for n in names}
    timeline = build_timeline(shots)
    duration = float(subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", music],
                                    capture_output=True, text=True, check=True).stdout)
    frames = int(duration * FPS)
    vig = vignette()
    bars = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(bars).rectangle([0, 0, W, BAR], fill=(0, 0, 0, 255))
    ImageDraw.Draw(bars).rectangle([0, H - BAR, W, H], fill=(0, 0, 0, 255))
    ff = subprocess.Popen(["ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}",
                           "-r", str(FPS), "-i", "-", "-i", music, "-map", "0:v", "-map", "1:a",
                           "-c:v", "libx264", "-preset", "medium", "-crf", "19", "-pix_fmt", "yuv420p",
                           "-c:a", "aac", "-b:a", "192k", "-af", f"afade=t=out:st={duration - 2.5:.2f}:d=2.5",
                           "-movflags", "+faststart", "-shortest", out], stdin=subprocess.PIPE)

    def frame_of(s, t):
        u = (t - s["a"]) / max(0.001, s["b"] - s["a"])
        img = kenburns(s["img"], u, s["kb"][0], s["kb"][1], s["c0"], s["c1"], s["shake"] * max(0, 1 - (t - s["a"]) * 2))
        img = img.convert("RGBA")
        for layer, ti, to in s["over"]:
            a = min(ease((t - ti) / 0.5), ease((to - t) / 0.5)) if ti <= t <= to else 0
            if a > 0:
                img = Image.blend(img, Image.alpha_composite(img, layer), a)
        return img

    for i in range(frames):
        t = i / FPS
        cur = [s for s in timeline if s["a"] <= t < s["b"]]
        s = cur[-1]
        img = frame_of(s, t)
        since = t - s["a"]
        idx = timeline.index(s)
        if s["trans"] == "fade" and since < 0.6 and idx > 0:
            prev = timeline[idx - 1]
            img = Image.blend(frame_of(prev, t), img, ease(since / 0.6))
        if s["trans"] == "flash" and since < 0.7:
            img = Image.blend(img, Image.new("RGBA", (W, H), (235, 255, 252, 255)), (1 - since / 0.7) ** 2)
        if s["a"] == 36.0:  # build-up into the drop (36-40.5 s): glow creeping in
            img = Image.blend(img, Image.new("RGBA", (W, H), (190, 250, 245, 255)), 0.55 * ease((t - 39.3) / 1.2))
        if t < 0.8:
            img = Image.blend(Image.new("RGBA", (W, H), (0, 0, 0, 255)), img, t / 0.8)
        if t > duration - 1.5:
            img = Image.blend(img, Image.new("RGBA", (W, H), (0, 0, 0, 255)), min(1, (t - (duration - 1.5)) / 1.5))
        img.alpha_composite(vig)
        img.alpha_composite(bars)
        ff.stdin.write(img.convert("RGB").tobytes())
        if i % 150 == 0:
            print(f"frame {i}/{frames}", flush=True)
    ff.stdin.close()
    ff.wait()
    print("wrote", out)


if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    render(*sys.argv[1:])
