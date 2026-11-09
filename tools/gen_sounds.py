#!/usr/bin/env python3
"""Synthesizes every sound used by The Rift (all original, no samples).

Requires numpy and ffmpeg (with libvorbis). Run from the repository root:
    python3 tools/gen_sounds.py
Writes .ogg files to src/main/resources/assets/the_rift/sounds/.
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "the_rift", "sounds")
rng = np.random.default_rng(2026)


def t(dur):
    return np.arange(int(SR * dur)) / SR


def env(n, a=0.01, r=0.1, curve=2.0):
    """attack/release envelope over n samples."""
    e = np.ones(n)
    na, nr = min(n, max(1, int(a * SR))), min(n, max(1, int(r * SR)))
    e[:na] = np.linspace(0, 1, na)
    e[-nr:] *= np.linspace(1, 0, nr) ** curve
    return e


def chirp(f0, f1, dur, shape="sin"):
    tt = t(dur)
    f = np.geomspace(f0, f1, len(tt))
    ph = 2 * np.pi * np.cumsum(f) / SR
    if shape == "tri":
        return 2 / np.pi * np.arcsin(np.sin(ph))
    return np.sin(ph)


def noise(dur, color=0.0):
    n = rng.standard_normal(int(SR * dur))
    if color > 0:  # simple one-pole low-pass for "darker" noise
        out = np.zeros_like(n); a = color
        for i in range(1, len(n)):
            out[i] = a * out[i - 1] + (1 - a) * n[i]
        n = out / (np.abs(out).max() + 1e-9)
    return n


def lowpass(x, a):
    y = np.zeros_like(x)
    for i in range(1, len(x)):
        y[i] = a * y[i - 1] + (1 - a) * x[i]
    return y


def layer(dur, *parts):
    """Mix (signal, start_seconds, gain) parts into a buffer of `dur` seconds, clipping anything that runs past the end."""
    x = np.zeros(int(SR * dur))
    for sig, start, gain in parts:
        s = int(SR * start)
        n = max(0, min(len(sig), len(x) - s))
        x[s:s + n] += gain * sig[:n]
    return x


def norm(x, peak=0.8):
    return x / (np.abs(x).max() + 1e-9) * peak


def save(name, x, peak=0.8, mono=True):
    path = os.path.join(OUT, name + ".ogg")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    x = norm(x, peak)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        with wave.open(tmp.name, "wb") as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
            w.writeframes((x * 32767).astype(np.int16).tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp.name, "-c:a", "libvorbis", "-q:a", "4", path], check=True)
        os.unlink(tmp.name)
    print("wrote", os.path.relpath(path, ROOT))


# ------------------------------------------------------------------ blub: squishy, jelly-like squeaks
def squeak(f0, f1, dur, wobble=18.0):
    tt = t(dur)
    base = chirp(f0, f1, dur)
    vib = 1 + 0.3 * np.sin(2 * np.pi * wobble * tt)
    body = base * vib + 0.35 * chirp(f0 * 2.01, f1 * 2.01, dur)
    return body * env(len(tt), 0.008, dur * 0.6)


def gen_blub():
    save("blub/squeak1", squeak(900, 1500, 0.18))
    save("blub/squeak2", np.concatenate([squeak(1100, 1400, 0.09), np.zeros(int(SR * 0.04)), squeak(1200, 1700, 0.11)]))
    save("blub/squeak3", squeak(1400, 950, 0.22, wobble=24))
    save("blub/hurt1", squeak(1600, 700, 0.16, wobble=40))
    save("blub/hurt2", squeak(1800, 900, 0.12, wobble=50))
    save("blub/death", np.concatenate([squeak(1500, 600, 0.25, wobble=30), squeak(600, 300, 0.3, wobble=12) * 0.6]))
    for i, f in enumerate((180, 220), 1):  # soft "boing" hops
        tt = t(0.14)
        x = np.sin(2 * np.pi * np.geomspace(f, f * 1.8, len(tt)) * tt) * env(len(tt), 0.003, 0.1)
        x += 0.3 * lowpass(noise(0.14), 0.7) * env(len(tt), 0.001, 0.05)
        save(f"blub/hop{i}", x, 0.6)


# ------------------------------------------------------------------ soul wisp: glassy chimes
def bell(f, dur, partials=((1, 1), (2.76, 0.4), (5.4, 0.2), (8.9, 0.08))):
    tt = t(dur)
    x = sum(a * np.sin(2 * np.pi * f * m * tt) * np.exp(-tt * (3 + m * 1.5)) for m, a in partials)
    return x * env(len(tt), 0.002, 0.05)


def gen_wisp():
    save("soul_wisp/chime1", layer(1.2, (bell(1318, 1.2), 0, 1.0), (bell(1975, 1.0), 0.12, 0.6)), 0.5)
    save("soul_wisp/chime2", layer(1.3, (bell(1567, 1.3), 0, 1.0), (bell(2093, 1.0), 0.18, 0.5)), 0.5)
    save("soul_wisp/hurt", bell(880, 0.4) * (1 + 0.5 * np.sin(2 * np.pi * 30 * t(0.4))), 0.6)
    tt = t(1.2)
    save("soul_wisp/death", bell(1200, 1.2) * np.linspace(1, 0, len(tt)) + 0.2 * lowpass(noise(1.2), 0.9) * env(len(tt), 0.3, 0.8), 0.6)


# ------------------------------------------------------------------ sculk prowler: clicks + low growl
def clicks(n, spacing, dur):
    x = np.zeros(int(SR * dur))
    for i in range(n):
        start = int(SR * (i * spacing + rng.uniform(0, 0.02)))
        c = noise(0.012) * np.exp(-np.arange(int(SR * 0.012)) / 80)
        x[start:start + len(c)] += c[:len(x) - start]
    return x


def growl(dur, f=55):
    tt = t(dur)
    saw = 2 * ((f * tt * (1 + 0.05 * np.sin(2 * np.pi * 3 * tt))) % 1) - 1
    return lowpass(saw + 0.4 * noise(dur), 0.85) * env(len(tt), 0.08, 0.3)


def gen_prowler():
    save("sculk_prowler/click1", clicks(5, 0.06, 0.45) + 0.6 * growl(0.45, 60), 0.7)
    save("sculk_prowler/click2", clicks(8, 0.045, 0.55) + 0.5 * growl(0.55, 48), 0.7)
    save("sculk_prowler/hurt", clicks(3, 0.03, 0.25) + growl(0.25, 90), 0.8)
    save("sculk_prowler/death", np.concatenate([growl(0.6, 70), growl(0.6, 40) * 0.7]) + 0.4 * clicks(10, 0.1, 1.2), 0.8)


# ------------------------------------------------------------------ soul water: thick bubbles + soul hiss
def bubble(f, dur=0.12):
    tt = t(dur)
    return np.sin(2 * np.pi * np.geomspace(f, f * 2.2, len(tt)) * tt) * np.exp(-tt * 30)


def gen_water():
    for i in (1, 2):
        x = np.zeros(int(SR * 1.0))
        for _ in range(6 + i * 2):
            s = int(rng.uniform(0, 0.85) * SR)
            b = bubble(rng.uniform(250, 600))
            x[s:s + len(b)] += b * rng.uniform(0.3, 1.0)
        save(f"soul_water/bubble{i}", x, 0.5)
    tt = t(0.6)
    hiss = lowpass(noise(0.6), 0.3) * env(len(tt), 0.02, 0.4) + 0.5 * chirp(700, 300, 0.6) * env(len(tt), 0.05, 0.4)
    save("soul_water/burn", hiss, 0.6)


# ------------------------------------------------------------------ portal
def gen_portal():
    tt = t(4.0)
    hum = sum(np.sin(2 * np.pi * f * tt + p) * a for f, a, p in ((110, 1, 0), (165, 0.5, 1), (220.5, 0.35, 2), (331, 0.15, 3)))
    hum *= 0.7 + 0.3 * np.sin(2 * np.pi * 0.5 * tt)
    save("portal/hum", hum * env(len(tt), 0.5, 0.8), 0.5)
    tt = t(3.5)
    sweep = chirp(80, 900, 3.5) * env(len(tt), 1.5, 1.0) + 0.5 * lowpass(noise(3.5), 0.6) * env(len(tt), 2.0, 1.0)
    save("portal/travel", sweep, 0.6)
    # activation: rising arpeggio of glassy bells over a swell (the "melody" that wakes the portal)
    notes = [523.25, 659.25, 783.99, 1046.5, 1318.5]
    x = np.zeros(int(SR * 3.0))
    for i, f in enumerate(notes):
        b = bell(f, 1.6)
        s = int(SR * i * 0.22)
        x[s:s + len(b)] += b[:len(x) - s]
    x += 0.4 * chirp(60, 240, 3.0) * env(len(x), 0.8, 1.2)
    save("portal/activate", x, 0.7)


# ------------------------------------------------------------------ ambience & music
def pad(freqs, dur, amp=1.0, detune=0.3):
    tt = t(dur)
    x = np.zeros(len(tt))
    for f in freqs:
        for d in (-detune, 0, detune):
            x += np.sin(2 * np.pi * (f + d) * tt + rng.uniform(0, 6.28))
    return x * amp


def gen_ambient():
    dur = 20.0
    tt = t(dur)
    loop = pad([55, 82.4, 110, 164.8], dur, detune=0.15) * (0.75 + 0.25 * np.sin(2 * np.pi * tt / dur))
    loop += 0.25 * lowpass(noise(dur), 0.97)
    # cross-fade ends so it loops seamlessly
    n = int(SR * 2)
    loop[:n] = loop[:n] * np.linspace(0, 1, n) + loop[-n:] * np.linspace(1, 0, n)
    save("ambient/loop", loop[:-n], 0.35)
    for i, f in enumerate((987.8, 1174.7, 1318.5), 1):
        save(f"ambient/addition{i}", layer(2.5, (bell(f, 2.5), 0, 1.0), (bell(f * 1.5, 2.0), 0.3, 0.5)), 0.35)
    tt = t(6.0)
    mood = pad([41.2, 61.7], 6.0, detune=0.6) * env(len(tt), 2.0, 2.5) + 0.2 * clicks(4, 1.2, 6.0)
    save("ambient/mood", mood, 0.5)


def gen_music():
    """'Sift Lullaby': a slow, original pentatonic piece (~64 s) of bells over drones."""
    bpm = 60
    beat = 60 / bpm
    dur = 64.0
    x = np.zeros(int(SR * dur))
    scale = [261.63, 293.66, 329.63, 392.0, 440.0, 523.25, 587.33, 659.25]
    melody = [4, 3, 2, 3, 4, 4, 4, None, 3, 3, 3, None, 4, 6, 6, None,
              4, 3, 2, 3, 4, 4, 4, 4, 3, 3, 4, 3, 2, None, None, None,
              5, 7, 6, 5, 4, None, 3, 4, 5, None, 2, 3, 4, 3, 2, None,
              0, 2, 4, 2, 3, None, 1, 2, 0, None, None, None, None, None, None, None]
    for i, n in enumerate(melody):
        if n is None:
            continue
        s = int(SR * i * beat)
        b = bell(scale[n], 2.8) * 0.6
        x[s:s + len(b)] += b[:len(x) - s]
    chords = [[130.81, 196.0], [110.0, 164.81], [87.31, 130.81], [98.0, 146.83]]
    seg = dur / 8
    for k in range(8):
        c = chords[k % 4]
        p = pad(c, seg + 1.0, 0.18, 0.2) * env(int(SR * (seg + 1.0)), 1.0, 1.5)
        s = int(SR * k * seg)
        x[s:s + len(p)] += p[:len(x) - s]
    x *= env(len(x), 2.0, 4.0)
    save("music/sift_lullaby", x, 0.6)


if __name__ == "__main__":
    gen_blub(); gen_wisp(); gen_prowler(); gen_water(); gen_portal(); gen_ambient(); gen_music()
