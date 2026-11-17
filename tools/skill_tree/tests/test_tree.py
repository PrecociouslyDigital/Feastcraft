import dataclasses
import itertools
import json
import unittest
from pathlib import Path

from skilltree import emit, layout
from skilltree.layout import Cluster, Hang, KeystoneAt, Region, Tree
from skilltree.model import Json, Notable, Origin
from skilltree.preview import radius
from skilltree.shapes import Point
from skilltree.tree import BORDERS, REGIONS, ROADS, SPOKES, tree

DATAPACK = Path(__file__).resolve().parent.parent / "datapack"
ROAD_MARKERS = (".in.", ".out.", ".join.")


def _cross(o: Point, a: Point, b: Point) -> float:
    return (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)


def _segments_cross(p1: Point, p2: Point, q1: Point, q2: Point) -> bool:
    """Strictly: segments on one line, like opposite spokes, don't count."""

    def side(o: Point, a: Point, b: Point) -> int:
        c = _cross(o, a, b)
        return 0 if abs(c) < 1e-6 else (1 if c > 0 else -1)

    return side(q1, q2, p1) * side(q1, q2, p2) < 0 and side(p1, p2, q1) * side(p1, p2, q2) < 0


def _distance_to_segment(p: Point, a: Point, b: Point) -> float:
    ab = b - a
    t = max(0.0, min(1.0, ((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / (ab.x**2 + ab.y**2)))
    return p.distance(a + ab * t)


def _nudged(regions: tuple[Region, ...], roads: tuple[tuple[Cluster, Cluster], ...], by: float) -> Tree:
    """The same tree with every cluster and keystone moved outward."""
    moved: dict[Cluster, Cluster] = {}

    def hang(h: Hang) -> Hang:
        return moved[h] if isinstance(h, Cluster) else h

    def move(c: Cluster) -> Cluster:
        moved[c] = dataclasses.replace(
            c, radius=c.radius + by, hangs_from=hang(c.hangs_from), joins=None if c.joins is None else hang(c.joins)
        )
        return moved[c]

    new_regions = []
    for r in regions:
        inner = move(r.inner)
        clusters = tuple(move(c) for c in r.clusters)
        keystones = tuple(KeystoneAt(k.keystone, k.radius + by, k.angle, hang(k.hangs_from)) for k in r.keystones)
        new_regions.append(dataclasses.replace(r, inner=inner, clusters=clusters, keystones=keystones))
    new_roads = tuple((moved[a], moved[b]) for a, b in roads)
    return layout.build(tuple(new_regions), BORDERS, new_roads, SPOKES)


class TreeTest(unittest.TestCase):
    tree: Tree

    @classmethod
    def setUpClass(cls) -> None:
        cls.tree = tree()

    def test_every_node_is_reachable_from_the_start(self) -> None:
        reached = {n for n, _ in layout.walk(self.tree)}
        self.assertEqual(set(self.tree.nodes) - reached, set())

    def test_no_two_nodes_overlap(self) -> None:
        for a, b in itertools.combinations(self.tree.nodes.values(), 2):
            gap = a.at.distance(b.at) - radius(a.content) - radius(b.content)
            self.assertGreater(gap, 0, f"{a.id} overlaps {b.id}")

    def test_no_connection_crosses_another(self) -> None:
        at = {n: node.at for n, node in self.tree.nodes.items()}
        for (a, b), (c, d) in itertools.combinations(self.tree.edges, 2):
            if {a, b} & {c, d}:
                continue
            self.assertFalse(_segments_cross(at[a], at[b], at[c], at[d]), f"{a}-{b} crosses {c}-{d}")

    def test_no_connection_passes_over_a_node(self) -> None:
        for a, b in self.tree.edges:
            pa, pb = self.tree.nodes[a].at, self.tree.nodes[b].at
            for node in self.tree.nodes.values():
                if node.id in (a, b):
                    continue
                clearance = _distance_to_segment(node.at, pa, pb) - radius(node.content)
                self.assertGreater(clearance, 0, f"{a}-{b} passes over {node.id}")

    def test_every_cluster_has_a_notable(self) -> None:
        for cluster, ids in self.tree.clusters.items():
            notables = [n for n in ids if isinstance(self.tree.nodes[n].content, Notable)]
            self.assertTrue(notables, cluster)

    def test_origins_exclude_each_other_pairwise(self) -> None:
        origins = {n for n, node in self.tree.nodes.items() if isinstance(node.content, Origin)}
        self.assertEqual(len(origins), 9)
        pairs = {frozenset(e) for e in self.tree.exclusive}
        self.assertEqual(pairs, {frozenset(p) for p in itertools.combinations(origins, 2)})

    def test_skill_ids_survive_moving_placements(self) -> None:
        def named(t: Tree) -> set[str]:
            return {n for n in t.nodes if not n.startswith("road.") and not any(m in n for m in ROAD_MARKERS)}

        self.assertEqual(named(_nudged(REGIONS, ROADS, 6)), named(self.tree))


class DatapackTest(unittest.TestCase):
    files: dict[str, Json]

    @classmethod
    def setUpClass(cls) -> None:
        cls.files = emit.files(tree())

    def category(self, name: str) -> Json:
        return self.files[f"data/feastcraft_skill_tree/puffish_skills/categories/{emit.CATEGORY}/{name}.json"]

    def test_connections_name_skills_that_exist(self) -> None:
        skills = set(self.category("skills"))
        connections = self.category("connections")
        for group in ("normal", "exclusive"):
            for kind in ("bidirectional", "unidirectional"):
                for pair in connections[group][kind]:
                    self.assertLessEqual(set(pair), skills, pair)

    def test_every_skill_has_its_definition(self) -> None:
        definitions = self.category("definitions")
        for skill, body in self.category("skills").items():
            self.assertIn(body["definition"], definitions, skill)

    def test_every_power_reward_names_a_power_that_exists(self) -> None:
        ours = {
            "feastcraft_skill_tree:" + path.removeprefix("data/feastcraft_skill_tree/powers/").removesuffix(".json")
            for path in self.files
            if path.startswith("data/feastcraft_skill_tree/powers/")
        }
        for skill, definition in self.category("definitions").items():
            for reward in definition["rewards"]:
                if reward["type"] == "puffish_skills_origins:power":
                    power = reward["data"]["power"]
                    self.assertTrue(power.startswith("origins:") or power in ours, f"{skill}: {power}")

    def test_committed_datapack_is_up_to_date(self) -> None:
        on_disk = {str(p.relative_to(DATAPACK)) for p in DATAPACK.rglob("*") if p.is_file()}
        self.assertEqual(on_disk, set(self.files), "run `uv run python -m skilltree`")
        for path, body in self.files.items():
            self.assertEqual(json.loads((DATAPACK / path).read_text()), body, path)


if __name__ == "__main__":
    unittest.main()
