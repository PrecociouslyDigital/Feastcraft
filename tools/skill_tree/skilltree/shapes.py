"""The cluster shapes. Each one carries exactly as many notables as it has places for.

A shape is drawn in its own frame: `forward` points from where the cluster hangs to its centre, and
`side` is a quarter turn from that. Every shape but the fork can be walked from entry to exit
without buying a notable.
"""

import math
from dataclasses import dataclass

from .model import Notable


@dataclass(frozen=True)
class Point:
    x: float
    y: float

    def __add__(self, other: "Point") -> "Point":
        return Point(self.x + other.x, self.y + other.y)

    def __sub__(self, other: "Point") -> "Point":
        return Point(self.x - other.x, self.y - other.y)

    def __mul__(self, k: float) -> "Point":
        return Point(self.x * k, self.y * k)

    def length(self) -> float:
        return math.hypot(self.x, self.y)

    def distance(self, other: "Point") -> float:
        return (self - other).length()


def polar(radius: float, degrees: float) -> Point:
    """Degrees clockwise from straight up, which is where the first region points."""
    a = math.radians(degrees)
    return Point(radius * math.sin(a), -radius * math.cos(a))


@dataclass(frozen=True)
class Ring6:
    """Six smalls round a notable."""

    notable: Notable


@dataclass(frozen=True)
class Wheel6:
    """A ring of six with a small hub. The notables sit on the far side of the ring."""

    first: Notable
    second: Notable


@dataclass(frozen=True)
class Wheel8:
    first: Notable
    second: Notable


@dataclass(frozen=True)
class Wheel10:
    first: Notable
    second: Notable


@dataclass(frozen=True)
class Spine:
    """Four smalls in a line, with a notable off each of the middle two."""

    first: Notable
    second: Notable


@dataclass(frozen=True)
class Fork:
    """Two smalls that split into two branches, each ending in a notable. It has no exit."""

    first: Notable
    second: Notable


@dataclass(frozen=True)
class Kite:
    """Four smalls in a diamond round a notable."""

    notable: Notable


Shape = Ring6 | Wheel6 | Wheel8 | Wheel10 | Spine | Fork | Kite


@dataclass(frozen=True)
class Drawn:
    """A shape placed on the map. `notables[i]` is None for a small node."""

    points: tuple[Point, ...]
    notables: tuple[Notable | None, ...]
    edges: tuple[tuple[int, int], ...]
    entry: int
    exit: int | None


class _Pen:
    def __init__(self, centre: Point, attach: Point) -> None:
        forward = centre - attach
        self.u = forward * (1 / forward.length())
        self.v = Point(-self.u.y, self.u.x)
        self.centre = centre
        self.points: list[Point] = []
        self.notables: list[Notable | None] = []
        self.edges: list[tuple[int, int]] = []

    def node(self, forward: float, side: float, notable: Notable | None = None) -> int:
        self.points.append(self.centre + self.u * forward + self.v * side)
        self.notables.append(notable)
        return len(self.points) - 1

    def link(self, a: int, b: int) -> None:
        self.edges.append((a, b))

    def chain(self, nodes: list[int]) -> None:
        for a, b in zip(nodes, nodes[1:]):
            self.link(a, b)

    def done(self, entry: int, exit: int | None) -> Drawn:
        return Drawn(tuple(self.points), tuple(self.notables), tuple(self.edges), entry, exit)


def _wheel(pen: _Pen, n: int, radius: float, notables: dict[int, Notable], centre: Notable | None) -> Drawn:
    ring = []
    for j in range(n):
        a = math.pi + j * 2 * math.pi / n
        ring.append(pen.node(radius * math.cos(a), radius * math.sin(a), notables.get(j)))
    for j in range(n):
        pen.link(ring[j], ring[(j + 1) % n])
    half = n // 2
    if centre is not None:
        hub = pen.node(0, 0, centre)
        pen.link(hub, ring[half - 1])
        pen.link(hub, ring[half + 1])
    else:
        hub = pen.node(0, 0)
        pen.link(hub, ring[0])
        pen.link(hub, ring[half])
    return pen.done(ring[0], ring[half])


def draw(shape: Shape, centre: Point, attach: Point) -> Drawn:
    pen = _Pen(centre, attach)
    match shape:
        case Ring6(notable):
            return _wheel(pen, 6, 44, {}, notable)
        case Wheel6(first, second):
            return _wheel(pen, 6, 42, {2: first, 4: second}, None)
        case Wheel8(first, second):
            return _wheel(pen, 8, 46, {2: first, 6: second}, None)
        case Wheel10(first, second):
            return _wheel(pen, 10, 58, {3: first, 7: second}, None)
        case Spine(first, second):
            line = [pen.node(f, 0) for f in (-60, -20, 20, 60)]
            pen.chain(line)
            pen.link(pen.node(-20, 42, first), line[1])
            pen.link(pen.node(20, -42, second), line[2])
            return pen.done(line[0], line[3])
        case Fork(first, second):
            stem = pen.node(-52, 0)
            split = pen.node(-12, 0)
            pen.link(stem, split)
            for g, notable in ((1, first), (-1, second)):
                pen.chain([split, pen.node(17.7, g * 26.8), pen.node(55.3, g * 40.4), pen.node(94.8, g * 54.8, notable)])
            return pen.done(stem, None)
        case Kite(notable):
            entry = pen.node(-42, 0)
            left = pen.node(0, -36)
            right = pen.node(0, 36)
            exit = pen.node(42, 0)
            middle = pen.node(0, 0, notable)
            for a, b in ((entry, left), (entry, right), (left, exit), (right, exit), (middle, left), (middle, right)):
                pen.link(a, b)
            return pen.done(entry, exit)
