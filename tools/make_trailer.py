#!/usr/bin/env python3
"""Cuts the trailer for The Sift Unbound from the photo-shoot screenshots.

    python3 tools/make_trailer.py SHOTS_DIR MUSIC.mp3 OUT.mp4 [--format wide|short] [--mc-font auto|JAR|none]

SHOTS_DIR is build/run/clientGameTest/screenshots after `tools/photoshoot.sh` (or `runClientGameTest -Pphotoshoot`).
The music is not part of this repository: the trailer uses "Minecraft - Epic Trailer" by Kalamity Music, credited in
the end card. The edit is timed to that track (hit at 5.35 s, drop at 40.5 s, final hit at 64.85 s).

--format wide   1920x1080 teaser over the whole track (default).
--format short  1080x1920 YouTube Short (under 60 s): the same edit from 24 s into the track, each shot a full-height
                vertical slice of the screenshot, captions kept clear of the Shorts buttons and title.
--mc-font       titles in Minecraft's own bitmap font, read at render time from a Minecraft client jar ("auto" finds
                the one Loom downloaded for this project; nothing of Mojang's is stored in this repository). "none", or
                no jar found, falls back to DejaVu.

Every frame is drawn with Pillow and piped to ffmpeg (libx264 + AAC). The look is a slow, cinematic teaser: every
shot glides to the right at one constant speed (scaled up only as much as the glide needs, no push-ins or shake), with
crossfades, two flashes on the big hits, a vignette and a light grade (plus letterbox bars in the wide format).
The end card credits the music.
"""
import argparse
import glob
import io
import json
import os
import re
import subprocess
import sys
import zipfile

import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont

FPS = 30
W, H = 1920, 1080  # set by configure()
FORMAT = "wide"
BAR = 128  # cinematic letterbox height (wide only)
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ICON = os.path.join(ROOT, "src/main/resources/assets/sift_unbound/textures/item/echo_tuner.png")
SERIF = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
SANS = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
SANS_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
TEAL = (96, 213, 200)
SALMON = (241, 117, 130)
WHITE = (255, 255, 255)
SOFT = (230, 230, 230)
MC = None  # the Minecraft font, when available

# role -> (TrueType file, TrueType size, Minecraft font scale, bold); per format
ROLES = {
    "wide": {"title": (SERIF, 128, 13, True), "big": (SERIF, 92, 9, False), "card": (SERIF, 76, 8, False),
             "body": (SANS, 50, 6, False), "caption": (SANS_B, 54, 6, False), "sub": (SANS_B, 52, 6, False),
             "small": (SANS, 34, 5, False)},
    "short": {"title": (SERIF, 150, 15, True), "big": (SERIF, 96, 9, False), "card": (SERIF, 80, 8, False),
              "body": (SANS, 60, 6, False), "caption": (SANS_B, 62, 7, False), "sub": (SANS_B, 56, 6, False),
              "small": (SANS, 40, 5, False)},
}


def configure(fmt):
    global W, H, FORMAT
    FORMAT = fmt
    W, H = (1920, 1080) if fmt == "wide" else (1080, 1920)


# ----------------------------------------------------------------------------- Minecraft's font
class MinecraftFont:
    """Minecraft's default bitmap font (ascii.png + nonlatin_european.png), drawn the way the game draws it: glyphs
    8 px high, 1 px between letters, 4 px spaces, a drop shadow 1 px down-right at a quarter of the colour, and bold
    as the glyph drawn twice 1 px apart. Scaled up by whole pixels only, so it stays crisp."""

    SHEETS = ("ascii.png", "nonlatin_european.png")

    def __init__(self, jar):
        with zipfile.ZipFile(jar) as z:
            providers = json.loads(z.read("assets/minecraft/font/include/default.json"))["providers"]
            self.glyphs = {}
            for p in providers:
                file = p.get("file", "").split(":")[-1]
                if p.get("type") != "bitmap" or not file.endswith(self.SHEETS):
                    continue
                sheet = np.asarray(Image.open(io.BytesIO(z.read("assets/minecraft/textures/" + file))).convert("RGBA"))
                rows = p["chars"]
                ch_h, ch_w = sheet.shape[0] // len(rows), sheet.shape[1] // len(rows[0])
                if ch_h != 8:
                    continue
                for r, row in enumerate(rows):
                    for c, ch in enumerate(row):
                        if ch == "\x00" or ch in self.glyphs:
                            continue
                        alpha = sheet[r * ch_h:(r + 1) * ch_h, c * ch_w:(c + 1) * ch_w, 3] > 0
                        cols = np.where(alpha.any(axis=0))[0]
                        self.glyphs[ch] = (alpha, int(cols.max()) + 1 if len(cols) else 0)
        self.jar = jar

    def advance(self, ch, bold):
        if ch == " ":
            return 4
        alpha, w = self.glyphs.get(ch, self.glyphs["?"])
        return w + 1 + (1 if bold else 0)

    def width(self, text, bold=False):
        return sum(self.advance(ch, bold) for ch in text)

    def render(self, text, scale, colour, bold=False):
        w = self.width(text, bold) + 1
        canvas = np.zeros((9, w + 1, 4), np.uint8)
        shadow = tuple(int(v) >> 2 for v in colour)
        for layer, (dx, dy, rgb) in enumerate([(1, 1, shadow), (0, 0, colour)]):
            x = 0
            for ch in text:
                if ch != " ":
                    alpha, gw = self.glyphs.get(ch, self.glyphs["?"])
                    for extra in ((0, 1) if bold else (0,)):
                        ys, xs = np.nonzero(alpha)
                        canvas[ys + dy, xs + x + dx + extra, :3] = rgb
                        canvas[ys + dy, xs + x + dx + extra, 3] = 255
                x += self.advance(ch, bold)
        img = Image.fromarray(canvas, "RGBA")
        return img.resize((img.width * scale, img.height * scale), Image.NEAREST)


def find_minecraft_jar():
    props = open(os.path.join(ROOT, "gradle.properties"), encoding="utf-8").read()
    mc = re.search(r"^minecraft_version=(.+)$", props, re.M).group(1).strip()
    home = os.environ.get("GRADLE_USER_HOME", os.path.expanduser("~/.gradle"))
    for pattern in (f"caches/fabric-loom/{mc}/minecraft-client.jar", "caches/fabric-loom/*/minecraft-client.jar",
                    "caches/fabric-loom/**/minecraft-client*.jar"):
        hits = sorted(glob.glob(os.path.join(home, pattern), recursive=True))
        if hits:
            return hits[0]
    return None


# ----------------------------------------------------------------------------- text
def ease(t):
    t = min(1.0, max(0.0, t))
    return t * t * (3 - 2 * t)


def render_line(text, role, colour):
    """The text (word-wrapped to the frame) as tight RGBA images, one per visual line."""
    ttf, size, scale, bold = ROLES[FORMAT][role]
    max_w = W - (260 if FORMAT == "short" else 160)

    def draw(s):
        if MC:
            return MC.render(s, scale, colour, bold)
        f = ImageFont.truetype(ttf, size)
        box = ImageDraw.Draw(Image.new("L", (1, 1))).textbbox((0, 0), s, font=f)
        img = Image.new("RGBA", (box[2] - box[0] + 4, box[3] - box[1] + 4), (0, 0, 0, 0))
        ImageDraw.Draw(img).text((2 - box[0], 2 - box[1]), s, font=f, fill=colour + (255,))
        return img

    def wrap(limit):
        lines, cur = [], ""
        for word in text.split(" "):
            trial = (cur + " " + word).strip()
            if cur and draw(trial).width > limit:
                lines.append(cur)
                cur = word
            else:
                cur = trial
        return lines + [cur]

    lines = wrap(max_w)
    if len(lines) > 1:  # balance the lines: the narrowest width that still needs no more lines
        lo, hi = max(draw(word).width for word in text.split(" ")), max_w
        while lo < hi:
            mid = (lo + hi) // 2
            lo, hi = (lo, mid) if len(wrap(mid)) <= len(lines) else (mid + 1, hi)
        lines = wrap(lo)
    return [draw(s) for s in lines]


def text_layer(lines, y=None, glow=None):
    """lines: [(text, role, colour)] stacked and centred on y (default: the middle). Returns a full-frame RGBA layer."""
    images = [img for text, role, colour in lines for img in render_line(text, role, colour)]
    gap = 22 if MC else 26
    total = sum(i.height for i in images) + gap * (len(images) - 1)
    cy = (H // 2 if y is None else y) - total // 2
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for img in images:
        layer.alpha_composite(img, ((W - img.width) // 2, cy))
        cy += img.height + gap
    # a soft halo behind the letters: coloured glow for titles, dark for captions (readable on bright shots)
    halo = Image.new("RGBA", (W, H), (glow or (0, 0, 0)) + (0,))
    strength, radius = (235, 14) if glow else (170, 8)
    halo.putalpha(layer.getchannel("A").point(lambda a: strength if a else 0).filter(ImageFilter.GaussianBlur(radius)))
    return Image.alpha_composite(halo, layer)


def caption(text):
    return text_layer([(text, "caption", (245, 245, 245))], y=int(H * 0.70) if FORMAT == "short" else H - BAR - 90)


def card_text(text):
    return text_layer([(text, "card", WHITE)], y=H // 2 + (300 if FORMAT == "short" else 220), glow=(30, 120, 115))


# ----------------------------------------------------------------------------- look
def vignette():
    ys, xs = np.mgrid[0:H, 0:W]
    dx, dy = (xs - W / 2) / (W / 2), (ys - H / 2) / (H / 2)
    r = np.sqrt(dx * dx * 0.8 + dy * dy)
    a = np.clip((r - 0.55) / 0.75, 0, 1) ** 1.6 * 190
    img = np.zeros((H, W, 4), np.uint8)
    img[..., 3] = a.astype(np.uint8)
    return Image.fromarray(img, "RGBA")


def letterbox():
    bars = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    if FORMAT == "wide":
        ImageDraw.Draw(bars).rectangle([0, 0, W, BAR], fill=(0, 0, 0, 255))
        ImageDraw.Draw(bars).rectangle([0, H - BAR, W, H], fill=(0, 0, 0, 255))
    return bars


def grade(img):
    img = img.convert("RGB")
    img = img.point(lambda v: round(255 * (v / 255) ** 0.9))  # open the shadows a touch (shader shots run dark)
    img = ImageEnhance.Contrast(img).enhance(1.12)
    img = ImageEnhance.Color(img).enhance(1.18)
    return img


SPEED = 16.0  # px per second of output: every shot glides the same way (the camera trucks right) at the same speed
XF = 0.8  # crossfade length in seconds


def glide(img, t, g0, g1, focus=0.5):
    """Slow, constant-speed slide to the right over the glide window g0..g1 (+ the next crossfade).

    No push-ins and no direction changes. The window has the output's shape: in the wide format the image is scaled up
    only as much as the slide needs; in the short format a full-height vertical slice centred on `focus` (0..1 across
    the image) slides along the wide screenshot."""
    iw, ih = img.size
    travel = SPEED * (g1 - g0 + XF)  # output pixels covered over the whole glide
    aspect = W / H
    w = min(ih * aspect, iw / (1 + (travel + 6) / W))
    h = w / aspect
    src_travel = travel * w / W
    u = min(1.0, max(0.0, (t - g0) / (g1 - g0 + XF)))
    x_start = min(max(focus * iw - (w + src_travel) / 2, 0), iw - w - src_travel)
    x0 = x_start + u * src_travel
    y0 = (ih - h) / 2
    return img.resize((W, H), Image.BICUBIC, box=(x0, y0, x0 + w, y0 + h))


def tuner_card():
    """Dark card with the (original) Echo Tuner texture blown up and soul sparks; the recipe stays a secret."""
    rng = np.random.default_rng(7)
    ys = np.linspace(0, 1, H)[:, None]
    base = (np.array([10, 22, 30]) * (1 - ys)[..., None] + np.array([4, 40, 46]) * ys[..., None])
    arr = np.repeat(base, W, axis=1).astype(np.uint8)
    card = Image.fromarray(arr, "RGB").convert("RGBA")
    d = ImageDraw.Draw(card)
    for _ in range(160 * (W * H) // (1920 * 1080)):
        x, y, r = rng.integers(W), rng.integers(H), rng.integers(1, 4)
        d.ellipse([x - r, y - r, x + r, y + r], fill=(120, 230, 230, int(rng.integers(60, 200))))
    icon = Image.open(ICON).convert("RGBA").resize((400, 400), Image.NEAREST)
    glow = icon.filter(ImageFilter.GaussianBlur(28))
    top = H // 2 - (330 if FORMAT == "short" else 300)
    card.alpha_composite(glow, (W // 2 - 200, top))
    card.alpha_composite(icon, (W // 2 - 200, top))
    return card.convert("RGB")


# ----------------------------------------------------------------------------- the edit
SHOTS = ["01_warden_portal_dormant", "02_warden_portal_awakened", "03_warden_portal_close", "04_arrival_in_the_sift",
         "05_pallid_grove_flow_tide", "06_pallid_grove_thrive_tide", "07_pallid_grove_endure_tide",
         "09_soul_wetlands_thrive_tide", "11_lullaby_hills_thrive_tide", "12_blubs_by_the_soul_water",
         "13_sculk_prowler_endure_tide"]
FOCUS = {  # where the interesting part of each shot is (0..1 across), for the vertical slices of the short format
    "01_warden_portal_dormant": 0.49, "02_warden_portal_awakened": 0.49, "03_warden_portal_close": 0.62,
    "04_arrival_in_the_sift": 0.45, "05_pallid_grove_flow_tide": 0.66, "06_pallid_grove_thrive_tide": 0.62,
    "07_pallid_grove_endure_tide": 0.66, "09_soul_wetlands_thrive_tide": 0.6, "11_lullaby_hills_thrive_tide": 0.5,
    "12_blubs_by_the_soul_water": 0.52, "13_sculk_prowler_endure_tide": 0.56,
}


def build_timeline(shots):
    """(start, end, image, glide window, focus, overlays [(layer, t_in, t_out)], transition-in: cut|fade|flash).
    Times are seconds into the music track."""
    T = []
    shots, focus_of = dict(shots), dict(FOCUS)
    black = Image.new("RGB", (W, H), (0, 0, 0))

    def seg(a, b, name_or_img, over=(), trans="fade", glide_window=None):
        if isinstance(name_or_img, str):
            img, focus = shots[name_or_img], focus_of[name_or_img]
        else:
            img, focus = name_or_img, 0.5
        T.append(dict(a=a, b=b, img=img, g=glide_window or (a, b), focus=focus, over=list(over), trans=trans))

    def big(text):
        return text_layer([(text, "big", WHITE)], glow=(40, 140, 135))

    # A teaser, not a tour: no biome names, no recipe, only some of the places and creatures. The rest is left for
    # players to find (husklands, meadows, the Thrive Tide, most blub variants...). Every shot glides slowly to the
    # right at the same speed, so cuts and crossfades never change direction.
    if FORMAT == "wide":
        seg(0.0, 5.35, black, over=[(text_layer([("In 2027, Mojang opens a new dimension.", "body", SOFT)]), 0.9, 4.7)])
        seg(5.35, 12.0, "01_warden_portal_dormant", over=[(caption("Deep inside every ancient city, a frame waits."), 6.0, 11.4)])
        seg(12.0, 22.0, tuner_card(), over=[(card_text("Find the key."), 12.4, 15.8),
                                            (card_text("Play the melody that wakes it."), 16.3, 21.5)])
        seg(22.0, 28.5, black,
            over=[(text_layer([("The Sift arrives in Minecraft next year.", "body", SOFT)]), 22.4, 25.2),
                  (big("Why wait until next year…"), 25.4, 28.3)])
        # back to the frame; one unbroken glide from here through the moment it wakes (same camera, it just lights up)
        portal = (28.5, 42.1)
        seg(28.5, 40.5, "01_warden_portal_dormant", glide_window=portal,
            over=[(big("…when you can get it now?"), 29.0, 35.4)])
    else:
        # Short: starts 24 s into the track and opens on the frame itself, so the first second already shows something
        seg(24.0, 34.0, "01_warden_portal_dormant",
            over=[(text_layer([("The Sift arrives in Minecraft next year.", "body", SOFT)]), 24.2, 27.0),
                  (big("Why wait until next year…"), 27.2, 30.6),
                  (big("…when you can get it now?"), 30.8, 33.8)])
        seg(34.0, 37.2, tuner_card(), over=[(card_text("Find the key."), 34.2, 37.0)])
        portal = (37.2, 42.1)
        seg(37.2, 40.5, "01_warden_portal_dormant", glide_window=portal,
            over=[(caption("Play the melody that wakes it."), 37.4, 39.4)])
    seg(40.5, 42.1, "02_warden_portal_awakened", trans="flash", glide_window=portal)
    seg(42.1, 43.4, "03_warden_portal_close", trans="cut")
    seg(43.4, 46.05, "04_arrival_in_the_sift", over=[(caption("Step through."), 43.7, 45.9)])
    seg(46.05, 49.0, "05_pallid_grove_flow_tide")
    seg(49.0, 51.6, "09_soul_wetlands_thrive_tide", over=[(caption("Don't touch the water."), 49.3, 51.4)])
    seg(51.6, 54.2, "11_lullaby_hills_thrive_tide")
    seg(54.2, 56.8, "07_pallid_grove_endure_tide", over=[(caption("When the tide turns…"), 54.5, 56.6)])
    seg(56.8, 59.6, "12_blubs_by_the_soul_water", over=[(caption("Make some friends…"), 57.0, 59.4)])
    # the hunter stays in the dark
    shots["13_dark"] = ImageEnhance.Brightness(shots["13_sculk_prowler_endure_tide"]).enhance(0.5)
    focus_of["13_dark"] = focus_of["13_sculk_prowler_endure_tide"]
    seg(59.6, 63.4, "13_dark", over=[(caption("…and run from the rest."), 59.9, 62.8)])
    seg(63.4, 64.85, black)
    shots["title_bg"] = ImageEnhance.Brightness(shots["06_pallid_grove_thrive_tide"].filter(ImageFilter.GaussianBlur(6))).enhance(0.45)
    focus_of["title_bg"] = focus_of["06_pallid_grove_thrive_tide"]
    tagline = (["Why wait until next year? Get it now."] if FORMAT == "wide"
               else ["Why wait until next year?", "Get it now."])
    title = text_layer([("THE SIFT UNBOUND", "title", WHITE)] + [(line, "sub", SALMON) for line in tagline],
                       glow=(40, 160, 150))
    seg(64.85, 70.6, "title_bg", trans="flash", over=[(title, 64.95, 70.3)])
    song = (["“Minecraft – Epic Trailer” by Kalamity Music"] if FORMAT == "wide"
            else ["“Minecraft – Epic Trailer”", "by Kalamity Music"])
    credits = text_layer([("Music", "small", TEAL)] + [(line, "sub", WHITE) for line in song])
    seg(70.6, 99.0, black, over=[(credits, 71.0, 78.2)])
    return T


def overlays(s, t):
    img = glide(s["img"], t, *s["g"], focus=s["focus"]).convert("RGBA")
    for layer, ti, to in s["over"]:
        a = min(ease((t - ti) / 0.5), ease((to - t) / 0.5)) if ti <= t <= to else 0
        if a > 0:
            img = Image.blend(img, Image.alpha_composite(img, layer), a)
    return img


def compose(timeline, t, start, end, vig, bars):
    """The finished frame at track time t (the video runs from start to end)."""
    s = [s for s in timeline if s["a"] <= t < s["b"]][-1]
    img = overlays(s, t)
    since = t - s["a"]
    idx = timeline.index(s)
    if s["trans"] == "fade" and since < XF and idx > 0 and timeline[idx - 1]["b"] > start:
        img = Image.blend(overlays(timeline[idx - 1], t), img, ease(since / XF))
    if s["trans"] == "flash" and since < 0.7:
        img = Image.blend(img, Image.new("RGBA", (W, H), (235, 255, 252, 255)), (1 - since / 0.7) ** 2)
    if 39.3 <= t < 40.5:  # build-up into the drop: the frame starts to glow
        img = Image.blend(img, Image.new("RGBA", (W, H), (190, 250, 245, 255)), 0.55 * ease((t - 39.3) / 1.2))
    if t - start < 0.8:
        img = Image.blend(Image.new("RGBA", (W, H), (0, 0, 0, 255)), img, (t - start) / 0.8)
    if t > end - 1.5:
        img = Image.blend(img, Image.new("RGBA", (W, H), (0, 0, 0, 255)), min(1, (t - (end - 1.5)) / 1.5))
    img.alpha_composite(vig)
    img.alpha_composite(bars)
    return img


def render(shots_dir, music, out, fmt="wide", mc_font="auto"):
    global MC
    configure(fmt)
    jar = find_minecraft_jar() if mc_font == "auto" else (None if mc_font == "none" else mc_font)
    MC = MinecraftFont(jar) if jar else None
    print(f"{fmt} {W}x{H}, titles in " + (f"Minecraft's font ({jar})" if MC else "DejaVu"), flush=True)
    names = SHOTS
    missing = [n for n in names if not os.path.exists(os.path.join(shots_dir, n + ".png"))]
    if missing:
        sys.exit(f"missing screenshots in {shots_dir}: {', '.join(missing)} (run tools/photoshoot.sh first)")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    shots = {n: grade(Image.open(os.path.join(shots_dir, n + ".png"))) for n in names}
    timeline = build_timeline(shots)
    duration = float(subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", music],
                                    capture_output=True, text=True, check=True).stdout)
    start = 0.0 if fmt == "wide" else 24.0
    end = duration
    frames = int((end - start) * FPS)
    vig, bars = vignette(), letterbox()
    audio_fx = f"afade=t=out:st={end - start - 2.5:.2f}:d=2.5" + (",afade=t=in:st=0:d=0.6" if start else "")
    ff = subprocess.Popen(["ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}",
                           "-r", str(FPS), "-i", "-", "-ss", f"{start}", "-i", music, "-map", "0:v", "-map", "1:a",
                           "-c:v", "libx264", "-preset", "medium", "-crf", "19", "-pix_fmt", "yuv420p",
                           "-c:a", "aac", "-b:a", "192k", "-af", audio_fx,
                           "-movflags", "+faststart", "-shortest", out], stdin=subprocess.PIPE)
    for i in range(frames):
        t = start + i / FPS
        ff.stdin.write(compose(timeline, t, start, end, vig, bars).convert("RGB").tobytes())
        if i % 150 == 0:
            print(f"frame {i}/{frames}", flush=True)
    ff.stdin.close()
    ff.wait()
    print("wrote", out)


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("shots")
    ap.add_argument("music")
    ap.add_argument("out")
    ap.add_argument("--format", choices=["wide", "short"], default="wide")
    ap.add_argument("--mc-font", default="auto", help='"auto" (default), a Minecraft client jar, or "none"')
    a = ap.parse_args()
    render(a.shots, a.music, a.out, a.format, a.mc_font)
