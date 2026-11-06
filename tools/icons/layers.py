"""Rebuild a generated mod icon from layers: brick background, block, text. See docs/icon-editing.md.

usage: python tools/icons/layers.py <source.png> <TITLE> <SUBTITLE> <out_dir>
"""
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

SIZE = 1024
BLOCK = np.array([(512, 108), (834, 281), (834, 671), (512, 845), (188, 671), (188, 281)], np.int32)
SHADOW = np.array([(124, 664), (512, 872), (900, 664), (512, 456)], np.int32)

# 5x7 pixel font, strokes two cells thick (same style as the Gemini lettering)
FONT = {
    "A": ["#####", "##.##", "##.##", "#####", "##.##", "##.##", "##.##"],
    "C": ["#####", "##...", "##...", "##...", "##...", "##...", "#####"],
    "D": ["####.", "##.##", "##.##", "##.##", "##.##", "##.##", "####."],
    "E": ["#####", "##...", "##...", "####.", "##...", "##...", "#####"],
    "F": ["#####", "##...", "##...", "####.", "##...", "##...", "##..."],
    "I": ["##"] * 7,
    "L": ["##..."] * 6 + ["#####"],
    "M": ["##...##", "###.###", "#######", "##.#.##", "##...##", "##...##", "##...##"],
    "N": ["##..##", "###.##", "######", "##.###", "##..##", "##..##", "##..##"],
    "O": ["#####", "##.##", "##.##", "##.##", "##.##", "##.##", "#####"],
    "P": ["#####", "##.##", "##.##", "#####", "##...", "##...", "##..."],
    "R": ["#####", "##.##", "##.##", "####.", "##.##", "##.##", "##.##"],
    "S": ["#####", "##...", "##...", "#####", "...##", "...##", "#####"],
    "T": ["######"] + ["..##.."] * 6,
    "V": ["##.##", "##.##", "##.##", "##.##", "##.##", ".###.", "..#.."],
}


def disk(r):
    return cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * r + 1, 2 * r + 1))


# ---------------------------------------------------------------- background
def background(src):
    """A full brick wall with the source's colours and vignette."""
    rng = np.random.default_rng(7)
    # low frequency colour of the wall, ignoring block, shadow and text
    hide = np.zeros((SIZE, SIZE), np.uint8)
    cv2.fillPoly(hide, [BLOCK], 255)
    cv2.fillPoly(hide, [SHADOW], 255)
    hide[600:990] = 255
    hide = cv2.dilate(hide, disk(30))
    small = cv2.resize(src, (128, 128), interpolation=cv2.INTER_AREA)
    small_hide = cv2.resize(hide, (128, 128), interpolation=cv2.INTER_NEAREST)
    base = cv2.inpaint(small, small_hide, 12, cv2.INPAINT_TELEA)
    base = cv2.GaussianBlur(base.astype(np.float32), (0, 0), 10)
    base = cv2.resize(base, (SIZE, SIZE), interpolation=cv2.INTER_CUBIC)

    row_h, brick_w, mortar = 172, 340, 16
    y_off = -15
    shade = np.full((SIZE, SIZE), 0.5, np.float32)  # mortar
    for r in range(-1, SIZE // row_h + 2):
        y0 = y_off + r * row_h
        x_off = -(r % 2) * brick_w // 2 - 60
        for c in range(-1, SIZE // brick_w + 2):
            x0 = x_off + c * brick_w
            tone = 1.0 + rng.uniform(-0.06, 0.06)
            ya, yb = max(y0 + mortar // 2, 0), min(y0 + row_h - mortar // 2, SIZE)
            xa, xb = max(x0 + mortar // 2, 0), min(x0 + brick_w - mortar // 2, SIZE)
            if ya < yb and xa < xb:
                shade[ya:yb, xa:xb] = tone
                # slightly lighter top edge, darker bottom edge, like the source
                shade[ya:min(ya + 14, yb), xa:xb] *= 1.07
                shade[max(yb - 18, ya):yb, xa:xb] *= 0.92
    noise = cv2.GaussianBlur(rng.normal(0, 0.035, (SIZE // 8, SIZE // 8)).astype(np.float32), (0, 0), 0.6)
    shade *= 1 + cv2.resize(noise, (SIZE, SIZE), interpolation=cv2.INTER_NEAREST)
    shade = cv2.GaussianBlur(shade, (0, 0), 7)
    yy, xx = np.mgrid[0:SIZE, 0:SIZE].astype(np.float32) / SIZE - 0.5
    vignette = 1.0 - 0.55 * np.clip(np.sqrt(xx ** 2 + yy ** 2) / 0.72, 0, 1) ** 2
    bg = base * shade[..., None] * vignette[..., None] * 1.05
    return np.clip(bg, 0, 255)


def ground_shadow(img):
    """The block's stepped shadow on the ground."""
    cell = 12
    small = np.zeros((SIZE // cell, SIZE // cell), np.uint8)
    cv2.fillPoly(small, [SHADOW // cell], 255)
    mask = cv2.resize(small, (SIZE, SIZE), interpolation=cv2.INTER_NEAREST).astype(np.float32) / 255
    return img * (1 - 0.5 * mask[..., None])


# --------------------------------------------------------------------- block
def block(src):
    """The block, cut along its outline; parts that were under the old text are filled in."""
    mask = np.zeros((SIZE, SIZE), np.uint8)
    cv2.fillPoly(mask, [BLOCK], 255)
    white = (src.min(axis=2) > 200).astype(np.uint8)
    white[:600] = 0
    # old text: white face, its outline (12px) and the grey depth + outline below it (~30px)
    text = cv2.dilate(white, disk(14))
    for dy in range(1, 31):
        text[dy:] |= text[:-dy] & 1
    fixed = cv2.inpaint(src, (text * 255).astype(np.uint8), 7, cv2.INPAINT_TELEA)
    alpha = cv2.GaussianBlur(mask.astype(np.float32) / 255, (3, 3), 0)
    return fixed.astype(np.float32), alpha


# ---------------------------------------------------------------------- text
def glyph_mask(text, cell, gap, word_gap):
    width = 0.0
    for ch in text:
        width += word_gap if ch == " " else len(FONT[ch][0]) * cell + gap
    width -= gap
    h = int(round(7 * cell))
    m = np.zeros((h, int(np.ceil(width)) + 2), np.uint8)
    x = 0.0
    for ch in text:
        if ch == " ":
            x += word_gap - gap
            continue
        rows = FONT[ch]
        for r, line in enumerate(rows):
            for c, px in enumerate(line):
                if px == "#":
                    xa, xb = int(round(x + c * cell)), int(round(x + (c + 1) * cell))
                    ya, yb = int(round(r * cell)), int(round((r + 1) * cell))
                    m[ya:yb, xa:xb] = 1
        x += len(rows[0]) * cell + gap
    return m, width


def text_layer(lines):
    """RGBA layer: white face, grey depth, black outline, dark drop shadow."""
    face = np.zeros((SIZE, SIZE), np.uint8)
    depth = np.zeros((SIZE, SIZE), np.uint8)
    outline = np.zeros((SIZE, SIZE), np.uint8)
    shadow = np.zeros((SIZE, SIZE), np.uint8)
    for text, cell, top, outline_px, depth_px, shadow_px in lines:
        m, width = glyph_mask(text, cell, gap=1.2 * cell, word_gap=3.2 * cell)
        f = np.zeros((SIZE, SIZE), np.uint8)
        x0 = int(round(SIZE / 2 - width / 2))
        f[top:top + m.shape[0], x0:x0 + m.shape[1]] = m
        d = np.zeros_like(f)
        for dy in range(1, depth_px + 1):
            d[dy:] |= f[:-dy]
        body = f | d
        o = cv2.dilate(body, disk(outline_px))
        s = np.zeros_like(o)
        s[shadow_px:] = o[:-shadow_px]
        face |= f
        depth |= d & ~f
        outline |= o
        shadow |= s
    rgba = np.zeros((SIZE, SIZE, 4), np.float32)
    rgba[shadow > 0] = (10, 18, 26, 150)
    rgba[outline > 0] = (2, 2, 3, 255)
    rgba[depth > 0] = (82, 82, 82, 255)
    rgba[face > 0] = (255, 255, 255, 255)
    return rgba


def over(dst, rgb, alpha):
    return dst * (1 - alpha[..., None]) + rgb * alpha[..., None]


def main():
    src_path, title, subtitle, out = sys.argv[1], sys.argv[2], sys.argv[3], Path(sys.argv[4])
    out.mkdir(parents=True, exist_ok=True)
    src = np.asarray(Image.open(src_path).convert("RGB").resize((SIZE, SIZE)))

    bg = background(src)
    Image.fromarray(bg.astype(np.uint8)).save(out / "1-background.png")
    img = ground_shadow(bg)

    block_rgb, block_a = block(src)
    Image.fromarray(np.dstack([block_rgb, block_a * 255]).astype(np.uint8)).save(out / "2-block.png")
    img = over(img, block_rgb, block_a)

    # (text, cell px, top y, outline px, depth px, shadow px), measured from the Gemini lettering
    text = text_layer([(title, 25.3, 642, 12, 16, 10), (subtitle, 7.6, 868, 8, 10, 8)])
    Image.fromarray(text.astype(np.uint8)).save(out / "3-text.png")
    img = over(img, text[..., :3], text[..., 3] / 255)

    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).save(out / "icon-1024.png")


if __name__ == "__main__":
    main()
