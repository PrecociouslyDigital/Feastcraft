"""Puts the market together from an export."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import packexport

from . import emit, prices, rules, select

HERE = Path(__file__).resolve().parent.parent
# The tag prices.toml and bounties.toml use for every item an entity's loot table drops.
MOB_DROPS = "loot:entities"


@dataclass(frozen=True)
class Market:
    export: packexport.Export
    tags: dict[str, frozenset[str]]
    price_rules: rules.Prices
    rules: rules.Bounties
    prices: dict[str, prices.Price]
    objectives: list[select.Entry]
    rewards: list[select.Entry]
    left_out: list[select.LeftOut]
    # Other packs' decrees and pools, to turn off.
    stock: list[str]


def build(export_dir: Path) -> Market:
    export = packexport.load(export_dir)
    tags = export.item_tags | {MOB_DROPS: packexport.mob_drops(export_dir)}
    price_rules = rules.prices(HERE / "prices.toml")
    bounty_rules = rules.bounties(HERE / "bounties.toml")
    priced = prices.resolve(export.items, tags, export.recipes, price_rules)
    objectives, left_out = select.objectives(export, tags, priced, bounty_rules)
    rewards = select.rewards(export, tags, priced, price_rules, bounty_rules)
    stock = emit.stock_files(export_dir / "sources.tsv")
    return Market(export, tags, price_rules, bounty_rules, priced, objectives, rewards, left_out, stock)


def write(market: Market, datapack: Path, report: Path) -> None:
    emit.write(datapack, market.rules, market.objectives, market.rewards, market.stock)
    emit.report(report, market.export.names, market.objectives, market.rewards, market.left_out)
