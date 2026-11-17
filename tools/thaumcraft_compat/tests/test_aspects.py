import unittest
from collections import Counter

from thaumcompat import budget, derive, emit, raw
from thaumcompat.derive import Crafted, Hand, Inherits, Listed, Tag
from thaumcompat.export import Aspects, Export, Recipe, Subject

VANILLA: dict[str, Aspects] = {
    "minecraft:iron_ingot": {"metallum": 4},
    "minecraft:coal": {"ignis": 2, "potentia": 2},
    "minecraft:brick": {"ignis": 1, "terra": 1},
    "minecraft:bricks": {"ignis": 3, "terra": 3},
    "minecraft:oak_planks": {"arbor": 1},
    "minecraft:stone": {"terra": 2},
}


def recipe(id: str, result: str, count: int, *slots: str) -> Recipe:
    return Recipe(id, result, count, tuple(((item, 1),) for item in slots))


def make_export(items: list[str], recipes: list[Recipe] = [], tags: dict[str, frozenset[str]] = {},
                blocks: dict[str, str | None] = {}, mobs: frozenset[str] = frozenset()) -> Export:
    known = {Subject("item", id): aspects for id, aspects in VANILLA.items()}
    return Export(
        items={id: None for id in list(VANILLA) + items},
        blocks=blocks,
        mobs=mobs,
        recipes=tuple(recipes),
        known=known,
        item_tags=tags,
        entity_tags={},
    )


class CraftingTests(unittest.TestCase):
    def test_reproduces_values_thaumcraft_derives_itself(self) -> None:
        known = dict(VANILLA)
        self.assertEqual({"metallum": 21}, derive.crafted(recipe("cauldron", "x", 1, *["minecraft:iron_ingot"] * 7), known))
        self.assertEqual({"ignis": 13, "potentia": 13}, derive.crafted(recipe("coal_block", "x", 1, *["minecraft:coal"] * 9), known))
        self.assertEqual({"ignis": 3, "terra": 3}, derive.crafted(recipe("brick_stairs", "x", 4, *["minecraft:bricks"] * 6), known))

    def test_caps_each_aspect(self) -> None:
        self.assertEqual({"metallum": 64}, derive.crafted(recipe("big", "x", 1, *["minecraft:iron_ingot"] * 30), dict(VANILLA)))

    def test_takes_the_cheapest_recipe(self) -> None:
        export = make_export(["mod:thing"], [
            recipe("dear", "mod:thing", 1, *["minecraft:iron_ingot"] * 4),
            recipe("cheap", "mod:thing", 1, *["minecraft:stone"] * 4),
        ])
        self.assertEqual(Crafted("cheap", {"terra": 6}), derive.resolve(export, {}, {}, {})[Subject("item", "mod:thing")])

    def test_later_rounds_build_on_earlier_ones(self) -> None:
        export = make_export(["mod:plate", "mod:gear"], [
            recipe("plate", "mod:plate", 1, *["minecraft:iron_ingot"] * 2),
            recipe("gear", "mod:gear", 1, *["mod:plate"] * 4),
        ])
        self.assertEqual(Crafted("gear", {"metallum": 18}), derive.resolve(export, {}, {}, {})[Subject("item", "mod:gear")])

    def test_a_loop_resolves_nothing_on_its_own(self) -> None:
        export = make_export(["mod:ingot", "mod:block"], [
            recipe("block", "mod:block", 1, *["mod:ingot"] * 9),
            recipe("ingot", "mod:ingot", 9, "mod:block"),
        ])
        resolved = derive.resolve(export, {}, {}, {})
        self.assertNotIn(Subject("item", "mod:ingot"), resolved)
        self.assertNotIn(Subject("item", "mod:block"), resolved)
        self.assertEqual([Subject("item", "mod:ingot")], raw.raw_materials(export, resolved))


class PrecedenceTests(unittest.TestCase):
    export = make_export(["mod:planks"], [recipe("planks", "mod:planks", 1, *["minecraft:stone"] * 4)],
                         tags={"minecraft:planks": frozenset({"mod:planks"})})
    planks = Subject("item", "mod:planks")
    tags = {"item": {"minecraft:planks": "minecraft:oak_planks"}}

    def test_a_tag_beats_a_recipe(self) -> None:
        self.assertEqual(Tag("minecraft:planks", "minecraft:oak_planks"), derive.resolve(self.export, {}, self.tags, {})[self.planks])

    def test_a_hand_entry_beats_a_tag(self) -> None:
        hand = {self.planks: Listed({"arbor": 2})}
        self.assertEqual(Hand(Listed({"arbor": 2})), derive.resolve(self.export, hand, self.tags, {})[self.planks])

    def test_an_answer_only_counts_where_no_recipe_does(self) -> None:
        answer = derive.Asked("jev", {"aer": 1})
        resolved = derive.resolve(self.export, {}, {}, {self.planks: answer})
        self.assertIsInstance(resolved[self.planks], Crafted)


class BudgetTests(unittest.TestCase):
    def test_the_budget_holds_and_raw_blocks_come_first(self) -> None:
        crafted = [f"mod:crafted_{i}" for i in range(budget.BLOCK_BUDGET)]
        raws = [f"mod:raw_{i}" for i in range(10)]
        items = crafted + raws
        export = make_export(items, [recipe(id, id, 1, "minecraft:stone") for id in crafted],
                             blocks={id: id for id in items})
        resolved = derive.resolve(export, {}, {}, {})
        raw_materials = raw.raw_materials(export, resolved)
        kept = budget.kept(export, resolved, raw_materials, Counter())
        self.assertEqual(budget.BLOCK_BUDGET, len(kept))
        self.assertTrue(set(raws) <= kept)


class EmitTests(unittest.TestCase):
    def test_every_emitted_id_and_parent_exists(self) -> None:
        export = make_export(["mod:planks", "mod:brick_thing"],
                             [recipe("thing", "mod:brick_thing", 1, "minecraft:brick", "minecraft:brick")],
                             tags={"minecraft:planks": frozenset({"mod:planks"})},
                             blocks={"mod:planks": "mod:planks", "minecraft:oak_planks": "minecraft:oak_planks"})
        resolved = derive.resolve(export, {}, {"item": {"minecraft:planks": "minecraft:oak_planks"}}, {})
        entries = emit.entries(export, resolved, {"mod:planks"})
        self.assertEqual(3, len(entries))
        for entry in entries:
            kind = next(k for k in ("item", "block", "entity") if k in entry)
            ids = [entry[kind]] + ([entry["parent"]] if "parent" in entry else [])
            existing = export.blocks if kind == "block" else export.items
            self.assertTrue(all(id in existing for id in ids), entry)

    def test_a_block_outside_the_budget_gets_nothing(self) -> None:
        export = make_export(["mod:planks"], tags={"minecraft:planks": frozenset({"mod:planks"})},
                             blocks={"mod:planks": "mod:planks"})
        resolved = derive.resolve(export, {}, {"item": {"minecraft:planks": "minecraft:oak_planks"}}, {})
        self.assertEqual([], emit.entries(export, resolved, set()))
        self.assertEqual(Inherits("minecraft:oak_planks"), derive.value(resolved[Subject("item", "mod:planks")]))


if __name__ == "__main__":
    unittest.main()
