"""Generates the store art and launcher icons, deterministically.

The 2025 redesign. Flat clip-art is gone; this is the app's candy palette
rendered with real light: soft contact shadows, shaded materials, one crisp
specular per object, a warm ground with a quiet vignette and confetti.

The icon is the app's whole idea in one glance: two balls and three balls
seated on a plate, the parts you can count inside the whole they make.
The feature graphic stages that same fact as its hero, 2 + 3 with a giant
glossy 5 as the payoff, beside a title set like toy lettering.

Run:  python tools/make_art.py

Outputs:
  play-store/feature-graphic-1024x500.png
  play-store/play-icon-512.png
  app/src/main/res/mipmap-*/ic_launcher.png            (legacy, rounded)
  app/src/main/res/mipmap-*/ic_launcher_foreground.png (adaptive, full bleed)
"""

import os
import random

from PIL import Image, ImageDraw, ImageFilter, ImageFont

from artkit import (INK, SHADOW_WARM, WHITE, ball_tile, capsule, confetti,
                    corner_shade, darken, place_tile, plate_tile, rgrad,
                    soft_ellipse, star4, sticker_text, vgrad)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STORE = os.path.join(ROOT, "play-store")
RES = os.path.join(ROOT, "app", "src", "main", "res")

FONTS = {
    "black": [r"C:\Windows\Fonts\ariblk.ttf", r"C:\Windows\Fonts\seguibl.ttf"],
    "bold": [r"C:\Windows\Fonts\arialbd.ttf", r"C:\Windows\Fonts\seguibl.ttf"],
}

CREAM = (255, 246, 227)
CREAM_DEEP = (255, 233, 189)
BLUE = (28, 169, 232)
BLUE_LIGHT = (99, 203, 246)
BLUE_DEEP = (8, 110, 180)
CHIP_BLUE = (39, 53, 122)
RED = (227, 59, 44)
ORANGE = (240, 106, 14)
YELLOW = (250, 184, 5)
GREEN = (51, 168, 82)
TEAL = (14, 160, 174)
CONFETTI = (BLUE, RED, YELLOW, GREEN, TEAL, ORANGE)

# The icon scene lives in a 1024 design box: five balls packed two over
# three, tangent to each other, centred on the canvas. Two and three make
# five, and nothing else in the frame competes with that.
SPARKLES = ((168, 196, 30, WHITE, 170), (872, 300, 22, YELLOW, 180), (140, 800, 16, WHITE, 125))


def font(kind, size):
    for path in FONTS[kind]:
        if os.path.exists(path):
            return ImageFont.truetype(path, int(size))
    raise SystemExit("no usable font found")


def fit_size(text, budget, start, kind="black"):
    size = start
    while size > 20 and font(kind, size).getlength(text) > budget:
        size -= 4
    return size


# -- the shared scene -----------------------------------------------------------

def icon_bg(size):
    """Candy blue, lit from the top left and falling into deep blue."""
    return rgrad(size, size, size * 0.36, size * 0.24, size * 0.75, size * 0.86,
                 [(0, BLUE_LIGHT), (0.55, BLUE), (1, BLUE_DEEP)])


def paint_scene(img, cx, cy, s, sparkles=True):
    """Five balls packed two over three, tangent, one pooled shadow beneath."""
    r = 134 * s
    dy = 1.732 * r
    seats = [
        (cx - r, cy - dy / 2, YELLOW), (cx + r, cy - dy / 2, RED),
        (cx - 2 * r, cy + dy / 2, ORANGE), (cx, cy + dy / 2, GREEN),
        (cx + 2 * r, cy + dy / 2, TEAL),
    ]
    base = dy / 2 + r * 1.27
    soft_ellipse(img, cx, cy + base, 3.3 * r, 0.65 * r, (7, 74, 132), 120, 0.35 * r)
    soft_ellipse(img, cx, cy + base - 8 * s, 2.4 * r, 0.45 * r, (6, 60, 110), 85, 0.17 * r)
    for bx, by, col in seats:
        place_tile(img, ball_tile(r * 2, col), bx, by)
    if sparkles:
        for sx, sy, sr, scol, sa in SPARKLES:
            px, py = cx + (sx - 512) * s, cy + (sy - 512) * s
            place_tile(img, star4(px, py, sr * s, scol, sa), px, py)


def rounded_mask(size, ratio):
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).rounded_rectangle(
        [0, 0, size - 1, size - 1], radius=int(size * ratio), fill=255
    )
    return m


# -- the icons --------------------------------------------------------------------

def gel_edge(img, ratio=0.205):
    """A quiet inner highlight along the rounded edge: weight without chrome."""
    px = img.width
    bez = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    ImageDraw.Draw(bez).rounded_rectangle(
        [px * 0.014] * 2 + [px * 0.986] * 2, radius=int(px * ratio),
        outline=(255, 255, 255, 85), width=max(3, int(px * 0.007)),
    )
    img.alpha_composite(bez.filter(ImageFilter.GaussianBlur(max(2, px * 0.003))))


def store_icon():
    px = 1024
    img = icon_bg(px)
    paint_scene(img, px / 2, px / 2, 1.0)
    gel_edge(img)
    img.putalpha(rounded_mask(px, 0.21))
    out = img.resize((512, 512), Image.LANCZOS).filter(
        ImageFilter.UnsharpMask(radius=1.5, percent=50, threshold=2)
    )
    path = os.path.join(STORE, "play-icon-512.png")
    out.save(path)
    print(path)


LAUNCHER_DENSITIES = [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]


def launcher_icons():
    for name, dp in LAUNCHER_DENSITIES:
        img = icon_bg(dp)
        paint_scene(img, dp * 0.5, dp * 0.5, dp / 1024 * 1.06, sparkles=dp >= 96)
        gel_edge(img, ratio=0.215)
        img.putalpha(rounded_mask(dp, 0.22))
        path = os.path.join(RES, f"mipmap-{name}", "ic_launcher.png")
        img.save(path)
        print(path)

        # Adaptive foreground: full-bleed gradient; the scene must survive any
        # mask, so it keeps inside the 66/108 safe circle.
        canvas = int(dp * 108 / 48)
        fg = icon_bg(canvas)
        safe_r = canvas * 66 / 108 / 2
        paint_scene(fg, canvas / 2, canvas / 2, safe_r * 0.94 / 474, sparkles=False)
        path = os.path.join(RES, f"mipmap-{name}", "ic_launcher_foreground.png")
        fg.save(path)
        print(path)


# -- the feature graphic ------------------------------------------------------------

def feature_ground():
    W, H = 2048, 1000
    img = vgrad(W, H, [(0, CREAM), (1, CREAM_DEEP)])
    soft_ellipse(img, 300, 130, 260, 200, WHITE, 26, 90)
    soft_ellipse(img, 1960, 900, 300, 240, WHITE, 22, 100)
    corner_shade(img, 26)
    rng = random.Random(11)
    confetti(img, rng, 14, (60, 1990), (40, 150), CONFETTI, amax=58)
    confetti(img, rng, 18, (1080, 2010), (80, 950), CONFETTI, amax=64)
    confetti(img, rng, 10, (60, 1000), (880, 970), CONFETTI, amax=46)
    return img


def feature_title(img):
    x = 112
    size = fit_size("Count", 700, 196)
    f_black = font("black", size)
    d = ImageDraw.Draw(img)
    for i, word in enumerate(("Count", "& Play")):
        cy = 320 + i * 208
        sticker_text(img, (x, cy), word, f_black, INK, shadow=((6, 18), 55, 10), anchor="lm")
        bb = d.textbbox((x, cy), word, font=f_black, anchor="lm")
        bar_col = RED if i == 0 else TEAL
        capsule(d, (bb[0], bb[3] + 30), (bb[2], bb[3] + 30), 13, bar_col)
    sub = font("bold", 50)
    d.text((x + 2, 766), "See addition and subtraction happen",
           font=sub, fill=INK + (225,), anchor="lm")


def hero_ball(img, cx, cy, r, col):
    soft_ellipse(img, cx, cy + r * 0.95, r * 1.02, r * 0.30, SHADOW_WARM, 65, r * 0.16)
    place_tile(img, ball_tile(r * 2, col), cx, cy)


def plus_sign(img, cx, cy, r, t, color):
    soft_ellipse(img, cx + 5, cy + t * 0.35, r * 1.35, r * 0.95, SHADOW_WARM, 60, t * 0.85)
    d = ImageDraw.Draw(img)
    capsule(d, (cx - r, cy), (cx + r, cy), t / 2, color)
    capsule(d, (cx, cy - r), (cx, cy + r), t / 2, color)


def equals_sign(img, cx, cy, w, t, gap, color):
    soft_ellipse(img, cx + 5, cy + t * 0.4, w * 0.72, gap * 0.95, SHADOW_WARM, 55, t * 0.9)
    d = ImageDraw.Draw(img)
    capsule(d, (cx - w / 2, cy - gap / 2), (cx + w / 2, cy - gap / 2), t / 2, color)
    capsule(d, (cx - w / 2, cy + gap / 2), (cx + w / 2, cy + gap / 2), t / 2, color)


def count_chip(img, cx, cy, dia, text):
    """The game's counting chip: navy, white numeral, tiny drop."""
    soft_ellipse(img, cx + dia * 0.05, cy + dia * 0.12, dia * 0.52, dia * 0.38, SHADOW_WARM, 80, dia * 0.10)
    d = ImageDraw.Draw(img)
    d.ellipse([cx - dia / 2, cy - dia / 2, cx + dia / 2, cy + dia / 2], fill=CHIP_BLUE)
    d.text((cx, cy - dia * 0.04), text, font=font("black", dia * 0.58), fill=WHITE, anchor="mm")


def feature_hero(img):
    row_y, r = 330, 68
    red1, red2 = 1164, 1314
    teal1, teal2, teal3 = 1586, 1736, 1886
    for n, x in enumerate((red1, red2), 1):
        hero_ball(img, x, row_y, r, RED)
        count_chip(img, x + r * 0.62, row_y - r * 0.62, 88, str(n))
    plus_sign(img, 1450, row_y, 56, 34, BLUE)
    for n, x in enumerate((teal1, teal2, teal3), 1):
        hero_ball(img, x, row_y, r, TEAL)
        count_chip(img, x + r * 0.62, row_y - r * 0.62, 88, str(n))

    equals_sign(img, 1400, 660, 130, 34, 92, BLUE)
    f5 = font("black", 360)
    sticker_text(img, (1640, 654), "5", f5, RED, stroke=18,
                 shadow=((8, 24), 70, 13), tint=darken(RED, 0.32), tint_off=(13, 15))

    for sx, sy, sr, scol, sa in ((1100, 148, 26, WHITE, 165), (1956, 500, 20, YELLOW, 185),
                                 (1200, 852, 17, WHITE, 120)):
        place_tile(img, star4(sx, sy, sr, scol, sa), sx, sy)


def feature_graphic():
    img = feature_ground()
    feature_title(img)
    feature_hero(img)
    out = img.resize((1024, 500), Image.LANCZOS).filter(
        ImageFilter.UnsharpMask(radius=1.8, percent=55, threshold=2)
    )
    path = os.path.join(STORE, "feature-graphic-1024x500.png")
    out.convert("RGB").save(path)
    print(path)


if __name__ == "__main__":
    feature_graphic()
    store_icon()
    launcher_icons()
