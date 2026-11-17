"""Writes the arsenal mod's hand-drawn sprites from the pixel maps below.

Tinkers tints its tool and part sprites per material, so these are greyscale.

    python3 mods/arsenal/art/draw.py
"""

import struct
import zlib
from pathlib import Path

TEXTURES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/feastcraft_arsenal/textures/item"

# Greys from Tinkers' own sprites, dark to light.
GREYS = {"1": 0x3F, "2": 0x66, "3": 0x8C, "4": 0xB2, "5": 0xD8, "6": 0xFF}


def png(rows: list[tuple[int, int, int, int]]) -> bytes:
    raw = b"".join(b"\x00" + bytes(channel for pixel in row for channel in pixel) for row in rows)

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


def grey(art: str) -> list[list[tuple[int, int, int, int]]]:
    lines = art.strip("\n").splitlines()
    assert len(lines) == 16 and all(len(line) == 16 for line in lines), art
    return [[(0, 0, 0, 0) if c == "." else (GREYS[c],) * 3 + (255,) for c in line] for line in lines]


# A flanged head on a metal neck. The neck meets the top of Tinkers' sword handle, which the mace reuses.
MACE_HEAD = """
...........2....
........2.111.2.
.........14541..
........1465431.
.......215544312
........1444321.
.........13321..
.......241111.2.
......241.......
.....241........
................
................
................
................
................
................
"""
NECK = {(7, 7), (8, 7), (9, 7), (6, 8), (7, 8), (8, 8), (5, 9), (6, 9), (7, 9)}


def broken(head: list[list[tuple[int, int, int, int]]]) -> list[list[tuple[int, int, int, int]]]:
    """The head with its top-right corner sheared off, outlined along the break."""
    dark = (GREYS["1"],) * 3 + (255,)
    return [[(0, 0, 0, 0) if x - y > 8 else dark if x - y == 8 and pixel[3] else pixel
             for x, pixel in enumerate(row)] for y, row in enumerate(head)]


def part(head: list[list[tuple[int, int, int, int]]]) -> list[list[tuple[int, int, int, int]]]:
    """The head alone, centred, as the part sits in a chest."""
    dx, dy = -4, 4
    empty = (0, 0, 0, 0)
    moved = [[empty] * 16 for _ in range(16)]
    for y, row in enumerate(head):
        for x, pixel in enumerate(row):
            if pixel[3] and (x, y) not in NECK and 0 <= x + dx < 16 and 0 <= y + dy < 16:
                moved[y + dy][x + dx] = pixel
    return moved


# Dense dark iron around a glowing core.
HEAVY_CORE = """
................
................
.....122221.....
....12333321....
...1233443321...
...1234554321...
..123456654321..
..123456654321..
..123455554321..
...1234554321...
...1233443321...
....12333321....
.....122221.....
................
................
................
"""


def heavy_core() -> list[list[tuple[int, int, int, int]]]:
    """Cold iron greys outside, and a teal glow where the core shows through."""
    pixels = grey(HEAVY_CORE)
    for row in pixels:
        for x, (r, g, b, a) in enumerate(row):
            if a and r >= GREYS["5"]:
                row[x] = (r // 3, r, (r * 9) // 10, 255)
            elif a:
                row[x] = (r * 3 // 4, r * 3 // 4, r * 4 // 5, 255)
    return pixels


SPRITES = {
    "tool/mace/head.png": grey(MACE_HEAD),
    "tool/mace/head_broken.png": broken(grey(MACE_HEAD)),
    "tool/parts/mace_head.png": part(grey(MACE_HEAD)),
    "heavy_core.png": heavy_core(),
}

if __name__ == "__main__":
    for name, pixels in SPRITES.items():
        path = TEXTURES / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(png([tuple(row) for row in pixels]))
        print(path)
