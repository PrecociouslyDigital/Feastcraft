import filecmp
import json
import tempfile
import unittest
from pathlib import Path

from bounties import emit, market
from bounties.__main__ import DATAPACK, EXPORT
from bounties.prices import Crafted
from bounties.rules import Rarity
from bounties.select import ember_share


@unittest.skipUnless(EXPORT.exists(), "needs scripts/export-datapack's output in build/datapack-export")
class MarketTests(unittest.TestCase):
    built: market.Market

    @classmethod
    def setUpClass(cls) -> None:
        cls.built = market.build(EXPORT)

    def test_every_pool_item_exists(self) -> None:
        known = self.built.export.items.keys() | self.built.rules.known_extras
        for entry in self.built.objectives + self.built.rewards:
            self.assertIn(entry.item, known)

    def test_every_crafted_objective_is_worth_more_than_its_ingredients(self) -> None:
        for entry in self.built.objectives:
            if isinstance(entry.price, Crafted):
                self.assertGreater(entry.worth, entry.price.inputs, entry.item)

    def test_ember_charcoal_is_about_half_of_a_new_boards_rewards(self) -> None:
        self.assertTrue(0.45 <= ember_share(self.built.rewards, Rarity.COMMON) <= 0.55)

    def test_ember_charcoal_is_dearest_where_it_is_commonest(self) -> None:
        ember = sorted((e for e in self.built.rewards if e.category == "ember"), key=lambda e: -e.rarity.value)
        self.assertEqual([e.worth for e in ember], sorted((e.worth for e in ember), reverse=True))

    def test_squid_ink_is_a_reward(self) -> None:
        rewards = {entry.item for entry in self.built.rewards}
        self.assertLessEqual({"minecraft:ink_sac", "minecraft:glow_ink_sac"}, rewards)

    def test_rewards_are_raw_and_objectives_are_not(self) -> None:
        self.assertFalse({e.item for e in self.built.rewards if e.category != "ember"} & {e.item for e in self.built.objectives})

    def test_every_stock_decree_and_pool_is_turned_off(self) -> None:
        for location in self.built.stock:
            namespace, _, path = location.partition(":")
            data = json.loads((DATAPACK / "data" / namespace / path).read_text())
            self.assertEqual([emit.NEVER_LOADED], data["requires"])

    def test_datapack_is_up_to_date(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            fresh = Path(tmp) / "datapack"
            market.write(self.built, fresh, Path(tmp) / "report.tsv")
            self.assertEqual([], _differences(filecmp.dircmp(DATAPACK, fresh)), "run: uv run python -m bounties")


def _differences(comparison: filecmp.dircmp[str]) -> list[str]:
    out = comparison.left_only + comparison.right_only + comparison.diff_files
    for sub in comparison.subdirs.values():
        out += _differences(sub)
    return out


if __name__ == "__main__":
    unittest.main()
