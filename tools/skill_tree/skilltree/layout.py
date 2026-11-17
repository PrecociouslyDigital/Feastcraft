"""Turns hand placements into a tree of positioned, connected nodes.

The skeleton is the only regular part: eight regions of 45°, a spoke of travel nodes along each
border, and three ring roads per region. The far ring lies past every region's tips, and some
border spurs run on out to it. Everything else hangs off it where a region file says.

Skill ids come from names, never from position or order, because Pufferfish stores what a player
owns by skill id. Road nodes are numbered from the end they leave, so a road that grows or shrinks
keeps the ids of the nodes it still has.
"""

import itertools
import math
import re
import unicodedata
from collections.abc import Iterator
from dataclasses import dataclass, field

from .model import Content, Keystone, Notable, Origin, Reward, Small, Start, Travel
from .shapes import Point, Shape, draw, polar

ROAD_STEP = 48
SPOKE_LENGTH = 8
INNER_RING = (192, (-11.25, 0, 11.25))
OUTER_RING = (384, (-15, -7.5, 0, 7.5, 15))
FAR_RING = (880, (-18, -13.5, -9, -4.5, 0, 4.5, 9, 13.5, 18))
KEYSTONE_PAD = 26


def slug(name: str) -> str:
    plain = unicodedata.normalize("NFKD", name.replace("'", "")).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z]+", "_", plain.lower()).strip("_")


@dataclass(frozen=True)
class Inner:
    """The inner ring road's j-th node, counted clockwise."""

    j: int


@dataclass(frozen=True)
class Outer:
    j: int


@dataclass(frozen=True)
class Far:
    j: int


@dataclass(frozen=True, eq=False)
class Cluster:
    name: str
    shape: Shape
    smalls: tuple[Reward, ...]
    """Handed out to the shape's small nodes in turn."""
    icon: str
    radius: float
    angle: float
    """Degrees off the region's centre line, clockwise."""
    hangs_from: "Hang"
    joins: "Hang | None" = None
    """A second road, from the cluster's exit."""


Hang = Inner | Outer | Far | Cluster


@dataclass(frozen=True)
class KeystoneAt:
    keystone: Keystone | Origin
    radius: float
    angle: float
    hangs_from: Hang


@dataclass(frozen=True)
class Region:
    key: str
    name: str
    road: Reward
    road_icon: str
    inner: Cluster
    """Sits between the ring roads, and also joins the outer ring."""
    clusters: tuple[Cluster, ...]
    keystones: tuple[KeystoneAt, ...]

    def all_clusters(self) -> tuple[Cluster, ...]:
        return (self.inner, *self.clusters)

    def travel(self) -> Travel:
        return Travel(f"{self.name} Road", (self.road,), self.road_icon)


@dataclass(frozen=True)
class BorderCluster:
    """Two smalls and a notable mixing the two neighbouring regions."""

    name: str
    smalls: tuple[Reward, Reward]
    notable: Notable
    icon: str
    # Whether spoke travel runs on from the notable to the far ring.
    reaches_far: bool = False


Border = BorderCluster | Keystone


@dataclass(frozen=True)
class Node:
    id: str
    at: Point
    content: Content
    region: int | None
    """None for the start, spokes and border spurs."""


@dataclass
class Tree:
    nodes: dict[str, Node] = field(default_factory=dict)
    edges: list[tuple[str, str]] = field(default_factory=list)
    exclusive: list[tuple[str, str]] = field(default_factory=list)
    clusters: dict[str, list[str]] = field(default_factory=dict)
    """Cluster id to the ids of its own nodes."""

    def neighbours(self) -> dict[str, list[str]]:
        adj: dict[str, list[str]] = {n: [] for n in self.nodes}
        for a, b in self.edges:
            adj[a].append(b)
            adj[b].append(a)
        return adj


class _Builder:
    def __init__(self, regions: tuple[Region, ...], spokes: tuple[Travel, ...]) -> None:
        self.tree = Tree()
        self.regions = regions
        self.spokes = spokes
        self.rings: dict[tuple[str, type, int], str] = {}
        self.cluster_ids: dict[Cluster, str] = {}
        self.cluster_region: dict[Cluster, int] = {}
        self.cluster_exits: dict[Cluster, str | None] = {}

    def add(self, id: str, at: Point, content: Content, region: int | None) -> str:
        if id in self.tree.nodes:
            raise ValueError(f"two nodes would be called {id}")
        self.tree.nodes[id] = Node(id, at, content, region)
        return id

    def link(self, a: str, b: str) -> None:
        self.tree.edges.append((a, b))

    def spoke(self, s: int, k: int) -> Travel:
        """The k-th node out along spoke s. Neighbouring spokes start at different places in the cycle."""
        return self.spokes[(s + k) % len(self.spokes)]

    def at(self, id: str) -> Point:
        return self.tree.nodes[id].at

    def road(self, a: str, b: str, prefix: str, travel: Travel, region: int | None, pad: float = 0) -> None:
        """Travel nodes every ROAD_STEP or so from a to b, keeping `pad` clear of b."""
        pa, pb = self.at(a), self.at(b)
        d = pa.distance(pb)
        usable = d - pad
        count = max(0, math.floor(usable / ROAD_STEP + 0.5) - 1)
        prev = a
        for i in range(1, count + 1):
            t = i / (count + 1) * usable / d
            n = self.add(f"{prefix}.{i}", pa + (pb - pa) * t, travel, region)
            self.link(prev, n)
            prev = n
        self.link(prev, b)

    def smalls_of(self, cluster: Cluster) -> list[str]:
        ids = self.tree.clusters[self.cluster_ids[cluster]]
        return [n for n in ids if isinstance(self.tree.nodes[n].content, Small)]

    def nearest_small(self, cluster: Cluster, to: Point) -> str:
        return min(self.smalls_of(cluster), key=lambda n: self.at(n).distance(to))

    def resolve(self, region: Region, hang: Hang, to: Point) -> str:
        match hang:
            case Inner(j):
                return self.rings[(region.key, Inner, j)]
            case Outer(j):
                return self.rings[(region.key, Outer, j)]
            case Far(j):
                return self.rings[(region.key, Far, j)]
            case Cluster():
                if hang not in self.cluster_ids:
                    raise ValueError(f"{hang.name} must be placed before what hangs from it")
                return self.nearest_small(hang, to)

    def cluster(self, region: Region, i: int, cluster: Cluster) -> None:
        cid = f"{region.key}.{slug(cluster.name)}"
        centre = polar(cluster.radius, 45 * i + cluster.angle)
        attach = self.resolve(region, cluster.hangs_from, centre)
        drawn = draw(cluster.shape, centre, self.at(attach))
        smalls = itertools.count()
        ids: list[str] = []
        for point, notable in zip(drawn.points, drawn.notables):
            if notable is None:
                k = next(smalls)
                reward = cluster.smalls[k % len(cluster.smalls)]
                ids.append(self.add(f"{cid}.{k + 1}", point, Small(cluster.name, (reward,), cluster.icon), i))
            else:
                ids.append(self.add(f"{cid}.{slug(notable.title)}", point, notable, i))
        for a, b in drawn.edges:
            self.link(ids[a], ids[b])
        self.tree.clusters[cid] = ids
        self.cluster_ids[cluster] = cid
        self.cluster_region[cluster] = i
        self.cluster_exits[cluster] = None if drawn.exit is None else ids[drawn.exit]
        self.road(attach, ids[drawn.entry], f"{cid}.in", region.travel(), i)
        if cluster.joins is not None:
            exit = self._exit(cluster)
            self.road(exit, self.resolve(region, cluster.joins, self.at(exit)), f"{cid}.join", region.travel(), i)

    def _border_name(self, s: int) -> str:
        return f"{self.regions[s].key}_{self.regions[(s + 1) % len(self.regions)].key}"

    def spoke_road(self, a: str, far: str, s: int) -> None:
        """Spoke travel from a border spur's notable out to the far ring."""
        pa, pb = self.at(a), self.at(far)
        count = max(0, math.floor(pa.distance(pb) / ROAD_STEP + 0.5) - 1)
        prev = a
        for k in range(1, count + 1):
            content = self.spoke(s, SPOKE_LENGTH + 2 + k)
            n = self.add(f"far.{self._border_name(s)}.in.{k}", pa + (pb - pa) * (k / (count + 1)), content, None)
            self.link(prev, n)
            prev = n
        self.link(prev, far)

    def _exit(self, cluster: Cluster) -> str:
        exit = self.cluster_exits[cluster]
        if exit is None:
            raise ValueError(f"{cluster.name} has no exit to join from")
        return exit

    def keystone(self, id: str, at: Point, content: Keystone | Origin, region: int | None) -> str:
        if isinstance(content, Origin):
            others = [n for n, node in self.tree.nodes.items() if isinstance(node.content, Origin)]
            self.tree.exclusive.extend((o, id) for o in others)
        return self.add(id, at, content, region)

    def build(self, borders: tuple[Border, ...], roads: tuple[tuple[Cluster, Cluster], ...]) -> Tree:
        count = len(self.regions)
        if len(borders) != count:
            raise ValueError(f"{count} regions need {count} borders, got {len(borders)}")
        self.add("start", Point(0, 0), Start(), None)

        spokes = []
        for s in range(count):
            name = f"spoke.{self._border_name(s)}"
            prev, column = "start", []
            for k in range(1, SPOKE_LENGTH + 1):
                n = self.add(f"{name}.{k}", polar(ROAD_STEP * k, 22.5 + 45 * s), self.spoke(s, k), None)
                self.link(prev, n)
                prev = n
                column.append(n)
            spokes.append(column)
        # Where the far ring crosses each border.
        far = [
            self.add(f"far.{self._border_name(s)}", polar(FAR_RING[0], 22.5 + 45 * s), self.spoke(s, SPOKE_LENGTH + 3), None)
            for s in range(count)
        ]

        for i, region in enumerate(self.regions):
            left, right = (i - 1) % count, i
            for kind, (radius, offsets), ends, tag in (
                (Inner, INNER_RING, (spokes[left][3], spokes[right][3]), "inner"),
                (Outer, OUTER_RING, (spokes[left][7], spokes[right][7]), "outer"),
                (Far, FAR_RING, (far[left], far[right]), "far"),
            ):
                prev = ends[0]
                for j, off in enumerate(offsets):
                    n = self.add(f"{region.key}.{tag}.{j}", polar(radius, 45 * i + off), region.travel(), i)
                    self.rings[(region.key, kind, j)] = n
                    self.link(prev, n)
                    prev = n
                self.link(prev, ends[1])

            for cluster in region.all_clusters():
                self.cluster(region, i, cluster)
            exit = self._exit(region.inner)
            nearest = min(
                (self.rings[(region.key, Outer, j)] for j in range(len(OUTER_RING[1]))),
                key=lambda n: self.at(n).distance(self.at(exit)),
            )
            self.road(exit, nearest, f"{self.cluster_ids[region.inner]}.out", region.travel(), i)

            for placed in region.keystones:
                at = polar(placed.radius, 45 * i + placed.angle)
                kid = f"keystone.{slug(placed.keystone.title)}"
                from_ = self.resolve(region, placed.hangs_from, at)
                self.keystone(kid, at, placed.keystone, i)
                self.road(from_, kid, f"{kid}.in", region.travel(), i, KEYSTONE_PAD)

        for s, border in enumerate(borders):
            angle, tip = 22.5 + 45 * s, spokes[s][-1]
            match border:
                case Keystone():
                    kid = f"keystone.{slug(border.title)}"
                    a = self.add(f"{kid}.in.1", polar(432, angle), self.spoke(s, SPOKE_LENGTH + 1), None)
                    b = self.add(f"{kid}.in.2", polar(480, angle), self.spoke(s, SPOKE_LENGTH + 2), None)
                    self.keystone(kid, polar(524, angle), border, None)
                    for x, y in ((tip, a), (a, b), (b, kid)):
                        self.link(x, y)
                case BorderCluster():
                    cid = f"border.{slug(border.name)}"
                    a = self.add(f"{cid}.1", polar(432, angle), Small(border.name, (border.smalls[0],), border.icon), None)
                    b = self.add(f"{cid}.2", polar(480, angle), Small(border.name, (border.smalls[1],), border.icon), None)
                    n = self.add(f"{cid}.{slug(border.notable.title)}", polar(530, angle), border.notable, None)
                    for x, y in ((tip, a), (a, b), (b, n)):
                        self.link(x, y)
                    self.tree.clusters[cid] = [a, b, n]
                    if border.reaches_far:
                        self.spoke_road(n, far[s], s)

        for first, second in roads:
            pairs = ((x, y) for x in self.smalls_of(first) for y in self.smalls_of(second))
            x, y = min(pairs, key=lambda p: self.at(p[0]).distance(self.at(p[1])))
            home = self.cluster_region[first]
            name = f"road.{self.cluster_ids[first].replace('.', '_')}.{self.cluster_ids[second].replace('.', '_')}"
            self.road(x, y, name, self.regions[home].travel(), home)

        return self.tree


def build(regions: tuple[Region, ...], borders: tuple[Border, ...], roads: tuple[tuple[Cluster, Cluster], ...], spokes: tuple[Travel, ...]) -> Tree:
    return _Builder(regions, spokes).build(borders, roads)


def walk(tree: Tree) -> Iterator[tuple[str, int]]:
    """Every node reachable from the start, with how many points it takes to reach."""
    adj = tree.neighbours()
    dist = {"start": 0}
    queue = ["start"]
    for n in queue:
        yield n, dist[n]
        for m in adj[n]:
            if m not in dist:
                dist[m] = dist[n] + 1
                queue.append(m)
