"""Generates the app's six sound effects as 16-bit mono WAVs.

They are synthesized rather than sampled so they are exactly to spec, tiny, and
free of any licensing question.

The religious constraint is a design input here, not an afterthought: there is
no music in this app. Five of the six sounds are deliberately *inharmonic*:
noise bursts and damped non-integer partials, so they read as physical events
(wood, cloth, a landing) rather than as notes. Only `chime` has a pitch, it is a
single struck bell, and the app never plays it twice inside 1200 ms, because two
pitched notes in sequence make an interval and intervals are where melody starts.

Run:  python tools/make_sounds.py            regenerate the assets in place
      python tools/make_sounds.py --check    verify the committed assets match
"""

import math
import os
import random
import struct
import sys
import tempfile
import wave

RATE = 44100
OUT = os.path.join("app", "src", "main", "res", "raw")

random.seed(20260727)  # deterministic: regenerating must not change the assets


def envelope(n, attack, decay, curve=3.0):
    """Percussive envelope: near-instant attack, exponential decay."""
    out = []
    a = max(1, int(attack * RATE))
    for i in range(n):
        if i < a:
            amp = i / a
        else:
            t = (i - a) / max(1, decay * RATE)
            amp = math.exp(-curve * t)
        out.append(amp)
    return out


def noise(n):
    return [random.uniform(-1.0, 1.0) for _ in range(n)]


def lowpass(samples, cutoff):
    """One-pole low-pass; enough to turn white noise into something wooden."""
    alpha = 1.0 - math.exp(-2.0 * math.pi * cutoff / RATE)
    out, prev = [], 0.0
    for s in samples:
        prev += alpha * (s - prev)
        out.append(prev)
    return out


def highpass(samples, cutoff):
    out = lowpass(samples, cutoff)
    return [s - l for s, l in zip(samples, out)]


def partials(n, freqs, decays, gains):
    """Sum of damped sinusoids. Non-integer ratios keep it inharmonic."""
    out = [0.0] * n
    for f, d, g in zip(freqs, decays, gains):
        for i in range(n):
            t = i / RATE
            out[i] += g * math.sin(2 * math.pi * f * t) * math.exp(-t / d)
    return out


def mix(*layers):
    n = max(len(l) for l in layers)
    out = [0.0] * n
    for layer in layers:
        for i, s in enumerate(layer):
            out[i] += s
    return out


def apply_env(samples, env):
    return [s * e for s, e in zip(samples, env)]


def write(name, samples, peak=0.82, outdir=None):
    n = len(samples)
    high = max(abs(s) for s in samples) or 1.0
    scale = peak / high
    # A short fade-out guarantees no click at the tail on any device.
    fade = min(int(0.006 * RATE), n)
    frames = bytearray()
    for i, s in enumerate(samples):
        v = s * scale
        if i >= n - fade:
            v *= (n - i) / fade
        frames += struct.pack("<h", max(-32767, min(32767, int(v * 32767))))

    path = os.path.join(outdir or OUT, name + ".wav")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(bytes(frames))
    print(f"{path}  {n / RATE * 1000:.0f} ms  {os.path.getsize(path) / 1024:.1f} KB")


def generate_all(outdir=None):
    """Regenerates every asset, in one fixed order, into [outdir] (default OUT)."""
    os.makedirs(outdir or OUT, exist_ok=True)
    random.seed(20260727)
    write("sfx_tick", tick(), peak=0.55, outdir=outdir)
    write("sfx_thud", thud(), peak=0.75, outdir=outdir)
    write("sfx_rustle", rustle(), peak=0.55, outdir=outdir)
    write("sfx_hollow", hollow(), peak=0.50, outdir=outdir)
    write("sfx_clink", clink(), peak=0.62, outdir=outdir)
    write("sfx_chime", chime(), peak=0.70, outdir=outdir)


def check():
    """Regenerates into a temp dir and byte-compares against the committed wavs.

    Exits non-zero if any committed asset would change on regeneration, so an
    accidental binary edit cannot ride along unnoticed until release.
    """
    with tempfile.TemporaryDirectory() as tmp:
        generate_all(outdir=tmp)
        bad = []
        for name in ("sfx_tick", "sfx_thud", "sfx_rustle", "sfx_hollow", "sfx_clink", "sfx_chime"):
            fresh = open(os.path.join(tmp, name + ".wav"), "rb").read()
            committed_path = os.path.join(OUT, name + ".wav")
            if not os.path.exists(committed_path):
                bad.append(f"{committed_path} is missing")
                continue
            committed = open(committed_path, "rb").read()
            if fresh != committed:
                bad.append(f"{committed_path} differs from a fresh regeneration")
    if bad:
        for line in bad:
            print("MISMATCH:", line)
        sys.exit(1)
    print("All six sound assets match a fresh regeneration.")


def tick():
    """60 ms: a soft dry wooden tap. Every count-tap. Must feel instant."""
    n = int(0.060 * RATE)
    body = lowpass(noise(n), 2600)
    knock = partials(n, [420, 690], [0.012, 0.008], [0.6, 0.3])
    return apply_env(mix(body, knock), envelope(n, 0.0006, 0.020, curve=5))


def thud():
    """120 ms: an object landing in a tray. Weight, no ring."""
    n = int(0.120 * RATE)
    low = partials(n, [95, 141], [0.045, 0.030], [1.0, 0.45])
    grit = lowpass(noise(n), 900)
    return apply_env(mix(low, [g * 0.35 for g in grit]), envelope(n, 0.001, 0.045, curve=4))


def rustle():
    """400 ms: many things settling at once. The pour."""
    n = int(0.400 * RATE)
    layer = highpass(lowpass(noise(n), 5200), 700)
    # Amplitude wobble so it sounds like many small events, not one hiss.
    out = []
    for i, s in enumerate(layer):
        t = i / RATE
        wobble = 0.65 + 0.35 * abs(math.sin(2 * math.pi * 11.3 * t + math.sin(2 * math.pi * 3.1 * t)))
        out.append(s * wobble)
    return apply_env(out, envelope(n, 0.010, 0.150, curve=2.5))


def hollow():
    """80 ms: a knock on an empty box. The sound of 'that did nothing'."""
    n = int(0.080 * RATE)
    box = partials(n, [232, 351, 508], [0.030, 0.018, 0.010], [1.0, 0.5, 0.22])
    return apply_env(mix(box, [s * 0.18 for s in lowpass(noise(n), 1800)]),
                     envelope(n, 0.0008, 0.028, curve=4))


def clink():
    """200 ms: one pebble into a glass jar. Bright, but deliberately inharmonic."""
    n = int(0.200 * RATE)
    # Ratios 1 : 2.76 : 5.40 are the classic inharmonic bar partials: bright
    # and glassy, but not a pitch you could hum.
    glass = partials(n, [1180, 3257, 6372], [0.055, 0.030, 0.016], [1.0, 0.42, 0.16])
    return apply_env(mix(glass, [s * 0.12 for s in highpass(noise(n), 3000)]),
                     envelope(n, 0.0005, 0.060, curve=3.5))


def chime():
    """450 ms: one soft struck bell. The only pitched sound in the app."""
    n = int(0.450 * RATE)
    f = 587.33  # a single note, struck once, never followed by another
    bell = partials(
        n,
        [f, f * 2.0, f * 3.01, f * 4.17],
        [0.230, 0.150, 0.090, 0.055],
        [1.0, 0.34, 0.15, 0.07],
    )
    return apply_env(bell, envelope(n, 0.004, 0.190, curve=2.2))

if __name__ == "__main__":
    if "--check" in sys.argv:
        check()
    else:
        generate_all()
