import dataclasses
import random
import unittest

from packexport import Recipe

from bounties import prices
from bounties.prices import Crafted, Raw
from bounties.rules import Prices, RawGroup, Selector


def rules(raw: dict[str, float], ignored: frozenset[str] = frozenset(), free: tuple[str, ...] = ()) -> Prices:
    return Prices(
        markup=1.6,
        step_bonus=5,
        cap=1_000_000,
        ignored_recipe_types=ignored,
        unpriced=frozenset({"babel:substance"}),
        free=Selector(ids=free),
        raw={},
        items=raw,
        made={},
    )


def recipe(id: str, result: str, count: int, *slots: str, type: str = "minecraft:crafting_shapeless") -> Recipe:
    return Recipe(id, type, result, count, tuple(tuple((item, 1) for item in slot.split("|")) for slot in slots))


def random_pack(seed: int) -> tuple[list[str], list[Recipe], dict[str, float]]:
    """Raw items and crafted ones, where a crafted item can be made from anything, loops included."""
    rng = random.Random(seed)
    raw = {f"raw:{n}": float(rng.randint(1, 100)) for n in range(rng.randint(1, 8))}
    crafted = [f"made:{n}" for n in range(rng.randint(1, 15))]
    items = list(raw) + crafted
    recipes = []
    for n in range(rng.randint(1, 30)):
        slots = ["|".join(rng.sample(items, rng.randint(1, min(3, len(items))))) for _ in range(rng.randint(1, 4))]
        recipes.append(recipe(f"r:{n}", rng.choice(crafted), rng.randint(1, 4), *slots))
    return items, recipes, raw


class PropertyTests(unittest.TestCase):
    def test_every_price_is_positive(self) -> None:
        for seed in range(300):
            items, recipes, raw = random_pack(seed)
            for item, price in prices.resolve(items, {}, recipes, rules(raw)).items():
                self.assertGreater(price.value, 0, (seed, item))

    def test_crafting_adds_value(self) -> None:
        for seed in range(300):
            items, recipes, raw = random_pack(seed)
            for item, price in prices.resolve(items, {}, recipes, rules(raw)).items():
                if isinstance(price, Crafted):
                    self.assertGreater(price.value, price.inputs, (seed, item))

    def test_an_item_is_worth_more_than_any_one_ingredient_of_a_single_result(self) -> None:
        for seed in range(300):
            items, recipes, raw = random_pack(seed)
            priced = prices.resolve(items, {}, recipes, rules(raw))
            by_id = {r.id: r for r in recipes}
            for item, price in priced.items():
                if not isinstance(price, Crafted) or by_id[price.recipe].count != 1:
                    continue
                for slot in by_id[price.recipe].slots:
                    cheapest = min(priced[i].value for i, _ in slot if i in priced)
                    self.assertGreater(price.value, cheapest, (seed, item))

    def test_no_recipe_gives_a_cheaper_price_than_the_one_chosen(self) -> None:
        for seed in range(300):
            items, recipes, raw = random_pack(seed)
            priced = prices.resolve(items, {}, recipes, rules(raw))
            for r in recipes:
                if r.result in raw or not all(any(i in priced for i, _ in slot) for slot in r.slots):
                    continue
                cost = sum(min(priced[i].value * n for i, n in slot if i in priced) for slot in r.slots) / r.count
                self.assertLessEqual(priced[r.result].value, cost * 1.6 + 5 + 1e-9, (seed, r.id))


class PricingTests(unittest.TestCase):
    def test_takes_the_cheapest_recipe(self) -> None:
        priced = prices.resolve(
            ["a", "b", "x"], {}, [recipe("cheap", "x", 1, "a"), recipe("dear", "x", 1, "b")], rules({"a": 10, "b": 1000})
        )
        self.assertEqual(Crafted("cheap", 10, 21, 1), priced["x"])

    def test_a_raw_item_keeps_its_value_when_a_recipe_makes_it(self) -> None:
        priced = prices.resolve(["wheat", "hay"], {}, [recipe("unpack", "wheat", 9, "hay")], rules({"wheat": 30, "hay": 1}))
        self.assertEqual(Raw("items", 30), priced["wheat"])

    def test_ignored_recipe_types_price_nothing(self) -> None:
        priced = prices.resolve(
            ["iron", "sulfur"], {}, [recipe("swap", "sulfur", 1, "iron", type="theurgy:reformation")], rules({"iron": 60}, frozenset({"theurgy:reformation"}))
        )
        self.assertNotIn("sulfur", priced)

    def test_a_slot_of_stand_ins_leaves_the_recipe_unpriced(self) -> None:
        priced = prices.resolve(["babel:substance", "juice"], {}, [recipe("press", "juice", 1, "babel:substance")], rules({}))
        self.assertNotIn("juice", priced)

    def test_utensils_cost_nothing(self) -> None:
        priced = prices.resolve(
            ["apple", "bakeware", "pie"], {}, [recipe("bake", "pie", 1, "apple", "bakeware")], rules({"apple": 30, "bakeware": 500}, free=("bakeware",))
        )
        self.assertEqual(30 * 1.6 + 5, priced["pie"].value)

    def test_a_loop_of_recipes_settles(self) -> None:
        priced = prices.resolve(
            ["ore", "ingot", "block"], {},
            [recipe("smelt", "ingot", 1, "ore"), recipe("pack", "block", 1, *["ingot"] * 9), recipe("unpack", "ingot", 9, "block")],
            rules({"ore": 60}),
        )
        self.assertEqual("smelt", priced["ingot"].recipe if isinstance(priced["ingot"], Crafted) else None)

    def test_the_first_raw_group_wins(self) -> None:
        groups = {"other_dimensions": RawGroup(Selector(ids=("minecraft:nether_wart",)), 100), "crops": RawGroup(Selector(tags=("forge:crops",)), 30)}
        tags = {"forge:crops": frozenset({"minecraft:nether_wart", "minecraft:wheat"})}
        raw = prices.raw(["minecraft:nether_wart", "minecraft:wheat"], tags, dataclasses.replace(rules({}), raw=groups))
        self.assertEqual({"minecraft:nether_wart": Raw("other_dimensions", 100), "minecraft:wheat": Raw("crops", 30)}, raw)


if __name__ == "__main__":
    unittest.main()
