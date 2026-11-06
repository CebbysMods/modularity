# Icon editing: rebuilding a generated icon from layers

The mvd and mpd icons were generated with Gemini. The first mpd image had a spelling mistake
("PLATFDRM DETETOR"). This page explains how that image was repaired by splitting it into layers,
why that worked when quick fixes did not, and why the shipped icon is a later Gemini image instead.

Script: [`tools/icons/layers.py`](../tools/icons/layers.py)

```
python tools/icons/layers.py <source.png> MPD "PLATFORM DETECTOR" <out-dir>
```

It writes `1-background.png`, `2-block.png`, `3-text.png` (the last two with transparency) and the
combined `icon-1024.png`.

## What did not work, and why

Two quick fixes were tried first:

1. **Redrawing only the outline.** The subtitle letters were taken from the image and a new black
   outline was drawn around them. The new outline had a different thickness and shape than the
   lettering it was mixed with, so it looked like a sticker on top of the picture.
2. **Copying letters between images.** The correct letters already existed: "DETECTOR" and an "O"
   in the mvd image, the rest in the mpd image. They were cut out as vertical strips and placed
   next to each other. Every pixel was original, but the strips came from two images with slightly
   different backgrounds, the black outlines of neighbouring letters overlap, and the cuts left
   faint seams. Each fix created a new seam somewhere else.

Both failed for the same reason: in a flat image the text, its outline, the block behind it and
the background are mixed into the same pixels. Any change to one of them disturbs the others.

## Why layers

An icon like this is really three things stacked on top of each other:

| Layer | Contains |
|---|---|
| Background | the brick wall and the block's shadow on the ground |
| Block | the isometric block with its frame, glow lines and front symbols |
| Text | the title and subtitle with their face, depth, outline and shadow |

Once they are separate, each one can be repaired without touching the others, and putting them
back together never leaves a seam, because each layer is complete on its own.

## How each layer was made

All numbers were measured from the Gemini image, not guessed.

### Background

* The bricks are about 172 px tall and 340 px wide, in a running bond (every second row shifted by
  half a brick). Found from the dark mortar lines in the visible parts of the wall.
* The colour and the darker corners come from the source: the visible wall was shrunk, the hidden
  parts (behind the block and text) filled in, and the result heavily blurred. That gives the
  overall light and colour without any brick detail.
* A new, complete brick pattern was drawn on top of that colour, with slightly different shades
  per brick, a lighter top edge, a darker bottom edge and darker mortar, then blurred to match the
  out-of-focus look of the original. A vignette darkens the corners.
* The block's stepped shadow on the ground is a diamond drawn on a 12 px grid, so its edges have
  the same pixel-art steps as the original.

Result: a wall made of whole bricks everywhere, including where the block and text used to hide it.

### Block

* The block is a regular isometric cube, so it can be cut out exactly along its six corners:
  (512,108), (834,281), (834,671), (512,845), (188,671), (188,281). They were found from the frame
  colour: the iron frame is neutral grey while the wall is bluish.
* The lower part of the block was hidden by the old title. Only the area the old text covered
  (its white letters plus the outline and grey depth below them) was filled in from the
  surrounding block pixels. A first attempt filled a larger area and smeared the amber dial's
  glow downwards; limiting the area to the old text fixed it.
* The new title covers the same spot, so the filled part is mostly hidden again.

### Text

* The Gemini lettering is a 5x7 pixel font with strokes two cells thick. The subtitle cell is about
  8.8 px, the title cell about 25.3 px. This was checked by sampling the letters on that grid and
  printing them as text.
* The generated letters do not follow the grid exactly (uneven gaps, a broken "N" and "V"), so
  instead of tracing them, each letter was drawn on a clean 5x7 grid in the same style.
* The look of the lettering was measured through a letter stroke:

  | | Title | Subtitle |
  |---|---|---|
  | Black outline | 12 px | 8 px |
  | Grey depth below the face, colour (82, 82, 82) | 16 px | 10 px |
  | Dark drop shadow | 10 px | 8 px |

* Every letter is rendered with exactly these sizes: white face, grey depth (the face shifted
  down), a rounded black outline around both, and a semi-transparent shadow below. Because every
  letter is drawn the same way, the outlines are even everywhere.
* The subtitle uses a smaller cell (7.6 px) so "PLATFORM DETECTOR" fits the width; it is two
  letters longer than "VERSION DETECTOR".

### Combining

Background, then the ground shadow, then the block (with a soft 1-2 px edge), then the text.

## Why the shipped icon is the Gemini image anyway

The layered version was clean and correctly spelled, but it no longer matched the mvd icon:
the bricks were redrawn, the letter shapes were redrawn rather than Gemini's, and the subtitle
size changed. The mvd icon is an unedited Gemini image, so the two icons would have looked like
they came from different sets.

Matching them would have meant running the same script on the mvd icon too. Instead a new mpd
image was generated in Gemini with the correct spelling, so both icons come from the same
generator and keep the same style. That image is shipped as-is, scaled to 256x256.

## When to use which

* **Ask the image generator** for new artwork: a different object, symbol, texture or mood.
* **Use the layer script** when the artwork is right but something exact is wrong: spelling, text
  size or position, or when several icons must share identical text and background. To keep a
  set consistent, run it on every icon of the set, not just one.
