"""Generates the store art and launcher icons, deterministically.

Everything here is drawn in the app's own design language: the flat warm
ground, the candy palette with each hue's darker outline, hard-edged facets
instead of gradients, and the ten vector shapes ported from ShapeArt.kt by
sampling the same quadratic paths. No stock clip-art, no gradients, no faces,
so the no-animate-beings rule holds on the store shelf too.

Run:  python tools/make_art.py

Outputs:
  play-store/feature-graphic-1024x500.png
  play-store/play-icon-512.png
  app/src/main/res/mipmap-*/ic_launcher.png            (legacy, rounded)
  app/src/main/res/mipmap-*/ic_launcher_foreground.png (adaptive, safe zone)
"""

import math
import os

from PIL import Image, ImageChops, ImageDraw, ImageFont

# -- The palette, verbatim from ui/Theme.kt ---------------------------------

GROUND = (255, 246, 227, 255)
INK = (34, 38, 46, 255)
BLUE = (28, 169, 232, 255)
LINER = (251, 251, 249, 255)
CHIP_BLUE = (39, 53, 122, 255)

APPLE_FILL = (227, 59, 44, 255)
APPLE_STROKE = (140, 29, 18, 255)
APPLE_FACET = (244, 105, 92, 255)
CARROT_FILL = (240, 106, 14, 255)
CARROT_STROKE = (138, 58, 5, 255)
BALL_FILL = (14, 160, 174, 255)
BALL_STROKE = (4, 82, 90, 255)
BALL_FACET = (68, 192, 204, 255)
GREEN = (51, 168, 82, 255)
LEAF_DARK = (27, 107, 55, 255)
STEM_BROWN = (122, 82, 51, 255)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STORE = os.path.join(ROOT, "play-store")
RES = os.path.join(ROOT, "app", "src", "main", "res")

FONT_PATHS = [
    r"C:\Windows\Fonts\ariblk.ttf",   # Arial Black, closest to ToyBlack
    r"C:\Windows\Fonts\seguibl.ttf",  # Segoe UI Black fallback
]


def font(size):
    for path in FONT_PATHS:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


# -- Vector shapes, ported from ui/ShapeArt.kt ------------------------------

def quad(p0, c, p1, n=28):
    return [
        (
            (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * c[0] + t * t * p1[0],
            (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * c[1] + t * t * p1[1],
        )
        for t in (i / n for i in range(n + 1))
    ]


def apple_body():
    """A circle with a dimple and a stalk, in the 100x100 design box."""
    pts = []
    pts += quad((50, 24), (58, 14), (70, 20))
    pts += quad((70, 20), (92, 32), (88, 58))[1:]
    pts += quad((88, 58), (84, 88), (50, 90))[1:]
    pts += quad((50, 90), (16, 88), (12, 58))[1:]
    pts += quad((12, 58), (8, 32), (30, 20))[1:]
    pts += quad((30, 20), (42, 14), (50, 24))[1:]
    return pts


def carrot_body():
    """The only downward-pointing wedge."""
    pts = [(26, 28), (74, 28)]
    pts += quad((74, 28), (70, 62), (54, 92))[1:]
    pts += quad((54, 92), (50, 97), (46, 92))[1:]
    pts += quad((46, 92), (30, 62), (26, 28))[1:]
    return pts


def ball_body(n=64):
    """The only clean circle."""
    return [
        (50 + 44 * math.cos(2 * math.pi * i / n), 50 + 44 * math.sin(2 * math.pi * i / n))
        for i in range(n + 1)
    ]


def facet_blob():
    """The lit facet, upper-left, shared by round shapes."""
    pts = []
    pts += quad((6, 62), (10, 16), (58, 8))
    pts += quad((58, 8), (30, 26), (30, 62))[1:]
    return pts


BODIES = {"apple": apple_body, "carrot": carrot_body, "ball": ball_body}
FACETS = {
    "apple": facet_blob(),
    "carrot": facet_blob(),
    "ball": facet_blob(),
}
PALETTE = {
    "apple": (APPLE_FILL, APPLE_STROKE, APPLE_FACET),
    "carrot": (CARROT_FILL, CARROT_STROKE, BALL_FACET and (255, 148, 64, 255)),
    "ball": (BALL_FILL, BALL_STROKE, BALL_FACET),
}


def draw_shape(img, kind, x, y, size, outline_scale=4.0):
    """One countable at (x, y), `size` px tall, straight from ShapeArt."""
    s = size / 100.0
    fill, stroke, facet = PALETTE[kind]
    body = [(x + px * s, y + py * s) for px, py in BODIES[kind]()]
    facet_pts = [(x + px * s, y + py * s) for px, py in FACETS[kind]]

    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.polygon(body, fill=fill)

    body_mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(body_mask).polygon(body, fill=255)
    facet_mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(facet_mask).polygon(facet_pts, fill=255)
    clipped = ImageChops.multiply(body_mask, facet_mask)
    solid = Image.new("RGBA", img.size, facet)
    # The facet shows only where it lies inside the body.
    facet_layer = Image.composite(solid, Image.new("RGBA", img.size, (0, 0, 0, 0)), clipped)
    layer.alpha_composite(facet_layer)

    w = max(2, int(outline_scale * s))
    d.line(body + [body[0]], fill=stroke, width=w, joint="curve")
    for pt in (body[0], body[-1]):
        d.ellipse([pt[0] - w / 2, pt[1] - w / 2, pt[0] + w / 2, pt[1] + w / 2], fill=stroke)
    img.alpha_composite(layer)


def draw_apple_trim(img, x, y, size):
    s = size / 100.0
    d = ImageDraw.Draw(img)
    a = (x + 50 * s, y + 26 * s)
    b = (x + 56 * s, y + 6 * s)
    w = max(2, int(5 * s))
    d.line([a, b], fill=STEM_BROWN, width=w)
    for pt in (a, b):
        d.ellipse([pt[0] - w / 2, pt[1] - w / 2, pt[0] + w / 2, pt[1] + w / 2], fill=STEM_BROWN)
    leaf = []
    leaf += quad((x + 54 * s, y + 14 * s), (x + 74 * s, y + 2 * s), (x + 84 * s, y + 14 * s))
    leaf += quad((x + 84 * s, y + 14 * s), (x + 70 * s, y + 22 * s), (x + 54 * s, y + 14 * s))[1:]
    d.polygon(leaf, fill=GREEN)
    d.line(leaf + [leaf[0]], fill=LEAF_DARK, width=max(1, int(2.6 * s)), joint="curve")


def draw_carrot_trim(img, x, y, size):
    s = size / 100.0
    d = ImageDraw.Draw(img)
    w = max(2, int(4.2 * s))
    # A narrow, short fan: wide fans read as arrows once strokes scale up.
    for fx in (-16, 0, 16):
        a = (x + 50 * s, y + 28 * s)
        b = (x + (50 + fx) * s, y + 8 * s)
        d.line([a, b], fill=GREEN, width=w)
        for pt in (a, b):
            d.ellipse([pt[0] - w / 2, pt[1] - w / 2, pt[0] + w / 2, pt[1] + w / 2], fill=GREEN)


def draw_ball_trim(img, x, y, size):
    s = size / 100.0
    d = ImageDraw.Draw(img)
    w = max(2, int(9 * s))
    for p0, c, p1 in (((9, 40), (50, 30), (91, 40)), ((12, 66), (50, 76), (88, 66))):
        pts = [(x + px * s, y + py * s) for px, py in quad(p0, c, p1)]
        d.line(pts, fill=LINER, width=w, joint="curve")


TRIMS = {"apple": draw_apple_trim, "carrot": draw_carrot_trim, "ball": draw_ball_trim}


def draw_countable(img, kind, x, y, size):
    draw_shape(img, kind, x, y, size)
    TRIMS[kind](img, x, y, size)


def draw_chip(img, cx, cy, dia, text):
    d = ImageDraw.Draw(img)
    d.ellipse([cx - dia / 2, cy - dia / 2, cx + dia / 2, cy + dia / 2], fill=CHIP_BLUE)
    f = font(int(dia * 0.56))
    box = d.textbbox((0, 0), text, font=f)
    d.text((cx - (box[2] - box[0]) / 2 - box[0], cy - (box[3] - box[1]) / 2 - box[1]), text, font=f, fill=(255, 255, 255, 255))


# -- The feature graphic ------------------------------------------------------

def feature_graphic():
    W, H = 1024, 500
    img = Image.new("RGBA", (W, H), GROUND)
    d = ImageDraw.Draw(img)

    title_f = font(96)
    d.text((W / 2, 108), "Count & Play", font=title_f, fill=INK, anchor="mm")

    # Two apples plus three carrots: the fact the app teaches, in its own art.
    fruit = 132
    gap = 22
    plus_w = 74
    widths = [fruit, fruit, plus_w, fruit, fruit, fruit]
    total = sum(widths) + gap * 5
    x = (W - total) / 2
    y = 196
    row_cy = y + fruit / 2
    chips = ["1", "2", None, "1", "2", "3"]
    kinds = ["apple", "apple", None, "carrot", "carrot", "carrot"]
    centers = []
    for kind, w in zip(kinds, widths):
        if kind is None:
            f = font(86)
            d.text((x + w / 2, row_cy), "+", font=f, fill=INK, anchor="mm")
            x += w + gap
            continue
        draw_countable(img, kind, x, y, fruit)
        centers.append((x, y))
        x += w + gap

    for (fx, fy), label in zip(centers, chips):
        if label:
            draw_chip(img, fx + fruit * 0.94, fy + fruit * 0.10, 46, label)

    sub_f = font(38)
    d.text((W / 2, 448), "See addition and subtraction happen", font=sub_f,
           fill=(34, 38, 46, 205), anchor="mm")

    path = os.path.join(STORE, "feature-graphic-1024x500.png")
    img.convert("RGB").save(path)
    print(path)


# -- The icon ------------------------------------------------------------------

def draw_icon_content(img, cx, cy, plate_r):
    """Plate circle plus the trio, centred at (cx, cy); plate_r in px."""
    d = ImageDraw.Draw(img)
    d.ellipse([cx - plate_r, cy - plate_r, cx + plate_r, cy + plate_r], fill=LINER)
    d.ellipse(
        [cx - plate_r, cy - plate_r, cx + plate_r, cy + plate_r],
        outline=(226, 216, 190, 255), width=max(2, int(plate_r * 8 / 150)),
    )
    fruit = plate_r * 118 / 150
    # Yellow ball up top, red apple lower left, orange carrot lower right,
    # spread so the three just kiss instead of crowding.
    draw_countable(img, "ball", cx - fruit / 2, cy - plate_r * 0.70, fruit)
    draw_countable(img, "apple", cx - plate_r * 0.74, cy - fruit * 0.12, fruit)
    draw_countable(img, "carrot", cx + plate_r * 0.74 - fruit / 2, cy - fruit * 0.12, fruit)


def rounded_bg(px, color, radius_ratio=0.20):
    img = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = px * radius_ratio
    d.rounded_rectangle([0, 0, px - 1, px - 1], radius=r, fill=color)
    return img


def store_icon():
    px = 512
    img = rounded_bg(px, BLUE)
    draw_icon_content(img, px / 2, px / 2 + 10, 186)
    path = os.path.join(STORE, "play-icon-512.png")
    img.save(path)
    print(path)


LAUNCHER_DENSITIES = [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]


def launcher_icons():
    for name, dp in LAUNCHER_DENSITIES:
        # Legacy: full rounded square on the candy blue.
        img = rounded_bg(dp, BLUE, radius_ratio=0.24)
        draw_icon_content(img, dp / 2, dp / 2 + dp * 0.02, dp * 0.36)
        path = os.path.join(RES, f"mipmap-{name}", "ic_launcher.png")
        img.save(path)
        print(path)

        # Adaptive foreground: transparent, content inside the 66/108 safe zone.
        canvas = int(dp * 108 / 48)
        fg = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
        safe = canvas * 66 / 108
        draw_icon_content(fg, canvas / 2, canvas / 2 + canvas * 0.01, safe * 0.45)
        path = os.path.join(RES, f"mipmap-{name}", "ic_launcher_foreground.png")
        fg.save(path)
        print(path)


if __name__ == "__main__":
    feature_graphic()
    store_icon()
    launcher_icons()
