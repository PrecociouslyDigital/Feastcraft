import unittest
from collections import Counter

from recipes import bridges
from recipes.__main__ import EXPORT

HONEYED_ALMOND = {
    "type": "create:filling",
    "ingredients": [{"item": "garnished:almond"}, {"amount": 250, "fluidTag": "forge:honey"}],
    "results": [{"item": "garnished:honeyed_almond"}],
}


class CastingTests(unittest.TestCase):
    def test_a_filling_becomes_a_casting_onto_the_item_it_fills(self) -> None:
        made = bridges.tinkers_casting("garnished:filling/honeyed_almond", HONEYED_ALMOND, frozenset())
        assert isinstance(made, bridges.Bridge)
        self.assertEqual(made.recipe["type"], "tconstruct:casting_table")
        self.assertEqual(made.recipe["cast"], {"item": "garnished:almond"})
        self.assertEqual(made.recipe["fluid"], {"amount": 250, "tag": "forge:honey"})
        self.assertEqual(made.recipe["result"], {"item": "garnished:honeyed_almond"})

    def test_filling_one_block_into_another_uses_the_basin(self) -> None:
        dirt = {
            "ingredients": [{"item": "minecraft:dirt"}, {"amount": 500, "fluid": "minecraft:water"}],
            "results": [{"item": "minecraft:grass_block"}],
        }
        made = bridges.tinkers_casting("create:filling/grass_block", dirt, frozenset({"minecraft:dirt", "minecraft:grass_block"}))
        assert isinstance(made, bridges.Bridge)
        self.assertEqual(made.recipe["type"], "tconstruct:casting_basin")


@unittest.skipUnless(EXPORT.exists(), "needs scripts/export-datapack's output in build/datapack-export")
class PackTests(unittest.TestCase):
    made: list[bridges.Bridge]

    @classmethod
    def setUpClass(cls) -> None:
        cls.made, _ = bridges.build(EXPORT)

    def test_no_two_castings_compete(self) -> None:
        slots = Counter(slot for b in self.made if b.path.startswith("casting/") for slot in bridges._slots(b.recipe))
        self.assertEqual([slot for slot, n in slots.items() if n > 1], [])

    def test_every_press_recipe_has_a_hammer_twin(self) -> None:
        twins = {b.path.removeprefix("hammering/") for b in self.made if b.path.startswith("hammering/")}
        self.assertIn("create/pressing/iron_ingot", twins)
        self.assertIn("cookbook_of_babel/pressing/latkes/step_2", twins)


if __name__ == "__main__":
    unittest.main()
