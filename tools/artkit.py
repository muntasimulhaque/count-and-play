"""Rendering toolkit for Count & Play store art: candy materials with real light.

Everything the art needs to look manufactured rather than drawn: multi-stop
gradients, soft contact shadows, glossy speculars, rounded capsules and a
confetti scatter. Pure Pillow + numpy, fully deterministic when callers seed
their own Random.
"""

import math

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

INK = (34, 38, 46)
SHADOW_WARM = (150, 112, 58)
WHITE = (255, 255, 255)


# -- colour -------------------------------------------------------------------

def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def lighten(c, t):
    return mix(c, WHITE, t)


def darken(c, t):
    return mix(c, (0, 0, 0), t)


# -- gradients ----------------------------------------------------------------

def _interp(t, stops):
    """Map scalar field t through multi-stop colour stops; returns h x w x 3."""
    t = np.asarray(t, dtype=float)
    out = np.zeros(t.shape + (3,), dtype=float)
    out[:] = stops[0][1]
    for i in range(len(stops) - 1):
        p0, c0 = stops[i]
        p1, c1 = stops[i + 1]
        m = (t >= p0) & (t <= p1) if i < len(stops) - 2 else t >= p0
        u = np.clip((t[m] - p0) / max(p1 - p0, 1e-6), 0, 1)
        for ch in range(3):
            out[m, ch] = c0[ch] + (c1[ch] - c0[ch]) * u
    return out


def vgrad(w, h, stops):
    """Vertical multi-stop gradient as an RGBA image."""
    rows = _interp(np.linspace(0.0, 1.0, h), stops)
    arr = np.repeat(rows[:, None, :], w, axis=1)
    return Image.fromarray(arr.astype("uint8"), "RGB").convert("RGBA")


def rgrad(w, h, cx, cy, rx, ry, stops):
    """Elliptical radial gradient (t = 0 at the centre, 1 at the ellipse)."""
    yy, xx = np.mgrid[0:h, 0:w]
    t = np.clip(np.sqrt(((xx - cx) / max(rx, 1e-6)) ** 2 + ((yy - cy) / max(ry, 1e-6)) ** 2), 0, 1)
    arr = _interp(t, stops)
    return Image.fromarray(arr.astype("uint8"), "RGB").convert("RGBA")


# -- masks and soft shapes ------------------------------------------------------

def ellipse_mask(size, box):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).ellipse(box, fill=255)
    return m


def soft_ellipse(img, cx, cy, rx, ry, color, alpha, blur):
    """A blurred ellipse composited at (cx, cy): shadows, glows, bokeh."""
    pad = int(blur * 3 + 4)
    w, h = int(rx * 2) + pad * 2, int(ry * 2) + pad * 2
    tile = Image.new("RGBA", (max(w, 2), max(h, 2)), (0, 0, 0, 0))
    ImageDraw.Draw(tile).ellipse(
        [pad, pad, pad + rx * 2, pad + ry * 2], fill=color + (int(alpha),)
    )
    tile = tile.filter(ImageFilter.GaussianBlur(blur))
    img.alpha_composite(tile, (int(cx - w / 2), int(cy - h / 2)))


def place_tile(img, tile, cx, cy):
    img.alpha_composite(tile, (int(round(cx - tile.width / 2)), int(round(cy - tile.height / 2))))


# -- primitives -----------------------------------------------------------------

def capsule(d, p0, p1, r, fill):
    """A stadium-shaped stroke: line with round caps."""
    d.line([p0, p1], fill=fill, width=max(1, int(r * 2)))
    for x, y in (p0, p1):
        d.ellipse([x - r, y - r, x + r, y + r], fill=fill)


def star4(cx, cy, r, color, alpha, ratio=0.34, rot=0.0):
    """A four-point twinkle as an RGBA tile."""
    pad = int(r * 2 + 6)
    tile = Image.new("RGBA", (pad, pad), (0, 0, 0, 0))
    pts = []
    for i in range(8):
        ang = math.pi / 4 * i + rot
        rad = r if i % 2 == 0 else r * ratio
        pts.append((pad / 2 + rad * math.cos(ang), pad / 2 + rad * math.sin(ang)))
    ImageDraw.Draw(tile).polygon(pts, fill=color + (alpha,))
    return tile


def confetti(img, rng, n, xbox, ybox, colors, amax=60, smin=9, smax=24):
    """Scatter small circles, pills and triangles, gently rotated."""
    for _ in range(n):
        s = rng.uniform(smin, smax)
        c = rng.choice(colors)
        a = rng.randint(26, amax)
        pad = int(s * 2.6)
        tile = Image.new("RGBA", (pad, pad), (0, 0, 0, 0))
        td = ImageDraw.Draw(tile)
        cxp = cyp = pad / 2
        k = rng.random()
        if k < 0.35:
            td.ellipse([cxp - s * 0.5, cyp - s * 0.5, cxp + s * 0.5, cyp + s * 0.5], fill=c + (a,))
        elif k < 0.7:
            td.rounded_rectangle(
                [cxp - s * 0.8, cyp - s * 0.3, cxp + s * 0.8, cyp + s * 0.3],
                radius=s * 0.3, fill=c + (a,),
            )
        else:
            td.polygon(
                [(cxp, cyp - s * 0.65), (cxp + s * 0.62, cyp + s * 0.5), (cxp - s * 0.62, cyp + s * 0.5)],
                fill=c + (a,),
            )
        tile = tile.rotate(rng.uniform(0, 360), resample=Image.BICUBIC)
        img.alpha_composite(tile, (int(rng.uniform(*xbox)) - pad // 2, int(rng.uniform(*ybox)) - pad // 2))


# -- materials ------------------------------------------------------------------

def _radial_alpha(dia, cx, cy, radius, peak, power):
    """Soft radial alpha field, normalised to the tile: highlight geometry."""
    yy, xx = np.mgrid[0:dia, 0:dia]
    d = np.sqrt((xx - dia * cx) ** 2 + (yy - dia * cy) ** 2) / (dia * radius)
    return (np.clip(1 - d, 0, 1) ** power * peak).astype("uint8")


def ball_tile(dia, base):
    """A glossy toy ball: shaded sphere, soft sheen, one hot specular."""
    dia = max(4, int(dia))
    tile = Image.new("RGBA", (dia, dia), (0, 0, 0, 0))
    mask = ellipse_mask((dia, dia), (0, 0, dia - 1, dia - 1))
    grad = vgrad(dia, dia, [(0, lighten(base, 0.40)), (0.55, base), (1, darken(base, 0.24))])
    tile.paste(grad, (0, 0), mask)

    for cx, cy, rad, peak, power in (
        (0.40, 0.34, 0.46, 140, 1.7),   # wide quiet sheen
        (0.345, 0.27, 0.17, 235, 2.6),  # the hot spot
    ):
        layer = Image.new("RGBA", (dia, dia), (255, 255, 255, 0))
        layer.putalpha(Image.fromarray(_radial_alpha(dia, cx, cy, rad, peak, power), "L"))
        tile.alpha_composite(layer)

    shifted = ellipse_mask((dia, dia), (0, -round(dia * 0.05), dia - 1, dia - 1 - round(dia * 0.05)))
    rim = ImageChops.subtract(mask, shifted)
    lower = Image.new("L", (dia, dia), 0)
    ImageDraw.Draw(lower).rectangle((0, round(dia * 0.74), dia, dia), fill=255)
    rim = ImageChops.multiply(rim, lower).point(lambda v: int(v * 0.20))
    tint = Image.new("RGBA", (dia, dia), lighten(base, 0.45) + (0,))
    tint.putalpha(rim)
    tile.alpha_composite(tint)
    return tile


def plate_tile(rx, ry):
    """A white plate seen from a child's angle: lit top, shaded well, warm rim."""
    w, h = int(rx * 2), int(ry * 2)
    tile = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    mask = ellipse_mask((w, h), (0, 0, w - 1, h - 1))
    tile.paste(vgrad(w, h, [(0, WHITE), (0.6, (252, 250, 245)), (1, (233, 225, 207))]), (0, 0), mask)

    inner = ImageChops.subtract(mask, ellipse_mask((w, h), (0, -round(ry * 0.16), w - 1, h - 1 - round(ry * 0.16))))
    shade = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    shade.paste(Image.new("RGB", (w, h), (198, 188, 166)), (0, 0), inner.point(lambda v: int(v * 0.6)))
    tile.alpha_composite(shade.filter(ImageFilter.GaussianBlur(max(2, ry * 0.06))))

    lifted = ellipse_mask((w, h), (0, round(ry * 0.15), w - 1, h - 1 + round(ry * 0.15)))
    top = ImageChops.subtract(mask, lifted)
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    glow.paste(Image.new("RGB", (w, h), WHITE), (0, 0), top.point(lambda v: int(v * 0.85)))
    tile.alpha_composite(glow.filter(ImageFilter.GaussianBlur(max(1, ry * 0.03))))
    return tile


def corner_shade(img, alpha=24, color=(24, 18, 10)):
    """Gently darken toward the corners so the ground has depth."""
    w, h = img.size
    yy, xx = np.mgrid[0:h, 0:w]
    t = np.sqrt(((xx - w / 2) / (w * 0.72)) ** 2 + ((yy - h / 2) / (h * 0.80)) ** 2)
    a = (np.clip(t - 0.55, 0, 1) / 0.45 * alpha).astype("uint8")
    layer = Image.new("RGBA", (w, h), color + (0,))
    layer.putalpha(Image.fromarray(a, "L"))
    img.alpha_composite(layer)


def sticker_text(img, xy, text, fnt, fill, stroke=0, stroke_fill=None,
                 shadow=((0, 14), 60, 9), tint=None, tint_off=(9, 11), anchor="mm"):
    """Chunky type with a soft drop and an optional offset tint for depth."""
    sx, sy, salpha, sblur = shadow[0][0], shadow[0][1], shadow[1], shadow[2]
    lay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(lay)
    ld.text((xy[0] + sx, xy[1] + sy), text, font=fnt, fill=INK + (salpha,), anchor=anchor,
            stroke_width=stroke, stroke_fill=INK + (salpha,))
    img.alpha_composite(lay.filter(ImageFilter.GaussianBlur(sblur)))
    d = ImageDraw.Draw(img)
    sf = stroke_fill if stroke_fill is not None else WHITE + (255,)
    if tint is not None:
        d.text((xy[0] + tint_off[0], xy[1] + tint_off[1]), text, font=fnt, fill=tint,
               anchor=anchor, stroke_width=stroke, stroke_fill=sf)
    d.text(xy, text, font=fnt, fill=fill, anchor=anchor, stroke_width=stroke, stroke_fill=sf)
