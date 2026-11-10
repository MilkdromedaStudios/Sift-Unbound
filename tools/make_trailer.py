#!/usr/bin/env python3
"""Cuts the launch trailer for The Sift Unbound from the photo-shoot screenshots.

    python3 tools/make_trailer.py SHOTS_DIR MUSIC.mp3 OUT.mp4

SHOTS_DIR is build/run/clientGameTest/screenshots after `tools/photoshoot.sh` (or `runClientGameTest -Pphotoshoot`).
The music is not part of this repository: the trailer uses "Minecraft - Epic Trailer" by Kalamity Music, credited in
the end card. The edit is timed to that track (hit at 5.35 s, drop at 40.5 s, final hit at 64.85 s).

Every frame is drawn with Pillow and piped to ffmpeg (libx264 + AAC). The look is a slow, cinematic teaser: every
shot glides to the right at one constant speed (scaled up only as much as the glide needs, no push-ins or shake), with
crossfades, two flashes on the big hits, letterbox bars, a vignette and a light grade. The end card credits the music.
"""
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
    img = img.point(lambda v: round(255 * (v / 255) ** 0.9))  # open the shadows a touch (shader shots run dark)
    img = ImageEnhance.Contrast(img).enhance(1.12)
    img = ImageEnhance.Color(img).enhance(1.18)
    return img


SPEED = 16.0  # px per second of output: every shot glides the same way (the camera trucks right) at the same speed
XF = 0.8  # crossfade length in seconds


def glide(img, t, g0, g1):
    """Slow, constant-speed slide to the right over the glide window g0..g1 (+ the next crossfade).

    No push-ins and no direction changes: the image is scaled up only as much as the slide needs to stay inside it."""
    travel = SPEED * (g1 - g0 + XF)  # output pixels covered over the whole glide
    z = 1.0 + (travel + 6) / W
    iw, ih = img.size
    w, h = iw / z, ih / z
    src_travel = travel * w / W
    u = min(1.0, max(0.0, (t - g0) / (g1 - g0 + XF)))
    x0 = (iw - w - src_travel) / 2 + u * src_travel
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
    for _ in range(160):
        x, y, r = rng.integers(W), rng.integers(H), rng.integers(1, 4)
        d.ellipse([x - r, y - r, x + r, y + r], fill=(120, 230, 230, int(rng.integers(60, 200))))
    icon = Image.open(ICON).convert("RGBA").resize((400, 400), Image.NEAREST)
    glow = icon.filter(ImageFilter.GaussianBlur(28))
    card.alpha_composite(glow, (W // 2 - 200, H // 2 - 300))
    card.alpha_composite(icon, (W // 2 - 200, H // 2 - 300))
    return card.convert("RGB")


def card_text(text):
    return text_layer([(text, SERIF, 76, (255, 255, 255))], y=H // 2 + 220, glow=(30, 120, 115))


# ----------------------------------------------------------------------------- the edit
def build_timeline(shots):
    S = lambda n: shots[n]  # noqa: E731
    black = Image.new("RGB", (W, H), (0, 0, 0))
    title_bg = S("06_pallid_grove_thrive_tide").filter(ImageFilter.GaussianBlur(6))
    title_bg = ImageEnhance.Brightness(title_bg).enhance(0.45)
    # (start, end, image, glide window, overlays [(layer, t_in, t_out)], transition-in: cut|fade|flash)
    T = []

    def seg(a, b, img, over=(), trans="fade", glide_window=None):
        T.append(dict(a=a, b=b, img=img, g=glide_window or (a, b), over=list(over), trans=trans))

    # A teaser, not a tour: no biome names, no recipe, only some of the places and creatures. The rest is left for
    # players to find (husklands, meadows, the Thrive Tide, most blub variants...). Every shot glides slowly to the
    # right at the same speed, so cuts and crossfades never change direction.
    seg(0.0, 5.35, black, over=[(text_layer([("In 2027, Mojang opens a new dimension.", SANS, 50, (230, 230, 230))]), 0.9, 4.7)])
    seg(5.35, 12.0, S("01_warden_portal_dormant"), over=[(caption("Deep inside every ancient city, a frame waits."), 6.0, 11.4)])
    seg(12.0, 22.0, tuner_card(), over=[(card_text("Find the key."), 12.4, 15.8),
                                        (card_text("Play the melody that wakes it."), 16.3, 21.5)])
    seg(22.0, 28.5, black,
        over=[(text_layer([("The Sift arrives in Minecraft next year.", SANS, 52, (230, 230, 230))]), 22.4, 25.2),
              (text_layer([("Why wait until next year…", SERIF, 92, (255, 255, 255))], glow=(40, 140, 135)), 25.4, 28.3)])
    # back to the frame; one unbroken glide from here through the moment it wakes (same camera, it just lights up)
    portal = (28.5, 42.1)
    seg(28.5, 40.5, S("01_warden_portal_dormant"), glide_window=portal,
        over=[(text_layer([("…when you can get it now?", SERIF, 92, (255, 255, 255))], glow=(40, 140, 135)), 29.0, 35.4)])
    seg(40.5, 42.1, S("02_warden_portal_awakened"), trans="flash", glide_window=portal)
    seg(42.1, 43.4, S("03_warden_portal_close"), trans="cut")
    seg(43.4, 46.05, S("04_arrival_in_the_sift"), over=[(caption("Step through."), 43.7, 45.9)])
    seg(46.05, 49.0, S("05_pallid_grove_flow_tide"))
    seg(49.0, 51.6, S("09_soul_wetlands_thrive_tide"), over=[(caption("Don't touch the water."), 49.3, 51.4)])
    seg(51.6, 54.2, S("11_lullaby_hills_thrive_tide"))
    seg(54.2, 56.8, S("07_pallid_grove_endure_tide"), over=[(caption("When the tide turns…"), 54.5, 56.6)])
    seg(56.8, 59.6, S("12_blubs_by_the_soul_water"), over=[(caption("Make some friends…"), 57.0, 59.4)])
    # the hunter stays in the dark
    seg(59.6, 63.4, ImageEnhance.Brightness(S("13_sculk_prowler_endure_tide")).enhance(0.5),
        over=[(caption("…and run from the rest."), 59.9, 62.8)])
    seg(63.4, 64.85, black)
    title = text_layer([("THE SIFT UNBOUND", SERIF, 128, (255, 255, 255)),
                        (" ", SANS, 14, (0, 0, 0)),
                        ("Why wait until next year? Get it now.", SANS_B, 52, SALMON)], glow=(40, 160, 150))
    seg(64.85, 70.6, title_bg, trans="flash", over=[(title, 64.95, 70.3)])
    credits = text_layer([("Music", SANS, 34, TEAL),
                          ("“Minecraft – Epic Trailer” by Kalamity Music", SANS_B, 52, (255, 255, 255))])
    seg(70.6, 99.0, black, over=[(credits, 71.0, 78.2)])
    return T


def overlays(s, t):
    img = glide(s["img"], t, *s["g"]).convert("RGBA")
    for layer, ti, to in s["over"]:
        a = min(ease((t - ti) / 0.5), ease((to - t) / 0.5)) if ti <= t <= to else 0
        if a > 0:
            img = Image.blend(img, Image.alpha_composite(img, layer), a)
    return img


def compose(timeline, t, duration, vig, bars):
    """The finished frame at time t."""
    s = [s for s in timeline if s["a"] <= t < s["b"]][-1]
    img = overlays(s, t)
    since = t - s["a"]
    idx = timeline.index(s)
    if s["trans"] == "fade" and since < XF and idx > 0:
        img = Image.blend(overlays(timeline[idx - 1], t), img, ease(since / XF))
    if s["trans"] == "flash" and since < 0.7:
        img = Image.blend(img, Image.new("RGBA", (W, H), (235, 255, 252, 255)), (1 - since / 0.7) ** 2)
    if 39.3 <= t < 40.5:  # build-up into the drop: the frame starts to glow
        img = Image.blend(img, Image.new("RGBA", (W, H), (190, 250, 245, 255)), 0.55 * ease((t - 39.3) / 1.2))
    if t < 0.8:
        img = Image.blend(Image.new("RGBA", (W, H), (0, 0, 0, 255)), img, t / 0.8)
    if t > duration - 1.5:
        img = Image.blend(img, Image.new("RGBA", (W, H), (0, 0, 0, 255)), min(1, (t - (duration - 1.5)) / 1.5))
    img.alpha_composite(vig)
    img.alpha_composite(bars)
    return img


def letterbox():
    bars = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(bars).rectangle([0, 0, W, BAR], fill=(0, 0, 0, 255))
    ImageDraw.Draw(bars).rectangle([0, H - BAR, W, H], fill=(0, 0, 0, 255))
    return bars


def render(shots_dir, music, out):
    names = ["01_warden_portal_dormant", "02_warden_portal_awakened", "03_warden_portal_close", "04_arrival_in_the_sift",
             "05_pallid_grove_flow_tide", "06_pallid_grove_thrive_tide", "07_pallid_grove_endure_tide",
             "09_soul_wetlands_thrive_tide", "11_lullaby_hills_thrive_tide", "12_blubs_by_the_soul_water",
             "13_sculk_prowler_endure_tide"]
    missing = [n for n in names if not os.path.exists(os.path.join(shots_dir, n + ".png"))]
    if missing:
        sys.exit(f"missing screenshots in {shots_dir}: {', '.join(missing)} (run tools/photoshoot.sh first)")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    shots = {n: grade(Image.open(os.path.join(shots_dir, n + ".png"))) for n in names}
    timeline = build_timeline(shots)
    duration = float(subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", music],
                                    capture_output=True, text=True, check=True).stdout)
    frames = int(duration * FPS)
    vig = vignette()
    bars = letterbox()
    ff = subprocess.Popen(["ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}",
                           "-r", str(FPS), "-i", "-", "-i", music, "-map", "0:v", "-map", "1:a",
                           "-c:v", "libx264", "-preset", "medium", "-crf", "19", "-pix_fmt", "yuv420p",
                           "-c:a", "aac", "-b:a", "192k", "-af", f"afade=t=out:st={duration - 2.5:.2f}:d=2.5",
                           "-movflags", "+faststart", "-shortest", out], stdin=subprocess.PIPE)

    for i in range(frames):
        t = i / FPS
        img = compose(timeline, t, duration, vig, bars)
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
