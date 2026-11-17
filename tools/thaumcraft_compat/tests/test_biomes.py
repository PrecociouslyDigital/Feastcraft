import random
import unittest

from thaumcompat.biomes import PARAMETERS, THAUMCRAFT_BIOMES, Box, with_thaumcraft


def random_box(rng: random.Random) -> Box:
    return Box({p: tuple(sorted((rng.uniform(-1, 1), rng.uniform(-1, 1)))) for p in PARAMETERS})  # type: ignore[misc]


def contains(box: Box, point: dict[str, float]) -> bool:
    return all(box.ranges[p][0] <= point[p] <= box.ranges[p][1] for p in PARAMETERS)


def strictly_inside(box: Box, point: dict[str, float]) -> bool:
    return all(box.ranges[p][0] < point[p] < box.ranges[p][1] for p in PARAMETERS)


class CuttingTests(unittest.TestCase):
    def test_pieces_cover_the_box_except_the_hole(self) -> None:
        rng = random.Random(0)
        for _ in range(300):
            box, hole = random_box(rng), random_box(rng)
            pieces = box.without(hole)
            for _ in range(50):
                point = {p: rng.uniform(*box.ranges[p]) for p in PARAMETERS}
                in_a_piece = any(contains(piece, point) for piece in pieces)
                self.assertEqual(not strictly_inside(hole, point), in_a_piece)

    def test_pieces_never_overlap_the_hole(self) -> None:
        rng = random.Random(1)
        for _ in range(300):
            hole = random_box(rng)
            self.assertFalse(any(piece.overlaps(hole) for piece in random_box(rng).without(hole)))

    def test_cutting_again_changes_nothing(self) -> None:
        rng = random.Random(2)
        entries = [
            {"biome": f"test:{i}", "parameters": {**{p: list(random_box(rng).ranges[p]) for p in PARAMETERS}, "offset": 0}}
            for i in range(40)
        ]
        overworld = {"type": "minecraft:overworld", "generator": {"biome_source": {"type": "minecraft:multi_noise", "biomes": entries}}}
        once = with_thaumcraft(overworld)
        self.assertEqual(once, with_thaumcraft(once))
        added = [e["biome"] for e in once["generator"]["biome_source"]["biomes"] if e["biome"] in THAUMCRAFT_BIOMES]
        self.assertEqual(sorted(THAUMCRAFT_BIOMES), sorted(added))


if __name__ == "__main__":
    unittest.main()
