"""Chooses the market's objectives and rewards."""

from __future__ import annotations

import math
from collections.abc import Mapping
from dataclasses import dataclass

from packexport import Export

from .prices import Crafted, Made, Price, Raw
from .rules import Bounties, Prices, Rarity, Selector

# Where a reward's rarity comes from its value; Ember Charcoal has one offer at each.
REWARD_RARITIES = (Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE)


@dataclass(frozen=True)
class Entry:
    category: str
    item: str
    rarity: Rarity
    # Bountiful's unitWorth: what one of the item is worth.
    worth: float
    amount: tuple[int, int]
    # Bountiful's weightMult, which scales the rarity's weight.
    weight: float
    # How the worth was worked out. Ember Charcoal's is set in bounties.toml.
    price: Price | None


@dataclass(frozen=True)
class LeftOut:
    category: str
    item: str
    reason: str


def objectives(export: Export, tags: Mapping[str, frozenset[str]], prices: Mapping[str, Price], rules: Bounties) -> tuple[list[Entry], list[LeftOut]]:
    """Made things to hand in. An item goes in the first category that selects it."""
    chosen: list[Entry] = []
    left_out: list[LeftOut] = []
    taken: set[str] = set()
    for name, category in rules.objectives.items():
        selected = category.selector.select(export.items, tags) - taken
        if category.food:
            selected = {item for item in selected if export.items.get(item) is not None}
        taken |= selected
        priced = []
        for item in sorted(selected):
            price = prices.get(item)
            if isinstance(price, Crafted | Made):
                priced.append((item, price))
            elif not category.food:
                left_out.append(LeftOut(name, item, "raw" if price else "no price"))
        for item, price in priced:
            chosen.append(Entry(name, item, category.rarity, price.value, (1, category.most), category.weight / len(priced), price))
    return chosen, left_out


def reward_rarity(value: float, rules: Bounties) -> Rarity:
    uncommon, rare = rules.rarer_from
    return Rarity.RARE if value >= rare else Rarity.UNCOMMON if value >= uncommon else Rarity.COMMON


def reward_amount(worth: float, rules: Bounties) -> tuple[int, int]:
    """Enough of an item to come to about rules.reward_worth."""
    low, high = rules.reward_worth
    most = max(1, min(64, math.floor(high / worth)))
    return min(most, max(1, math.ceil(low / worth))), most


def rewards(export: Export, tags: Mapping[str, frozenset[str]], prices: Mapping[str, Price], price_rules: Prices, rules: Bounties) -> list[Entry]:
    """Raw things to pay with, then Ember Charcoal to match them weight for weight at each rarity."""
    chosen: list[Entry] = []
    taken: set[str] = set()
    for name, category in rules.rewards.items():
        selected: set[str] = set()
        for group in category.groups:
            selected |= price_rules.raw[group].selector.select(export.items, tags)
        selected = Selector(ids=("*",), deny=category.deny).select(selected - taken, tags)
        taken |= selected
        raw = sorted((item, price) for item in selected if isinstance(price := prices.get(item), Raw))
        for item, price in raw:
            rarity = reward_rarity(price.value, rules)
            chosen.append(Entry(name, item, rarity, price.value, reward_amount(price.value, rules), category.weight / len(raw), price))
    return chosen + ember(chosen, rules)


def ember(others: list[Entry], rules: Bounties) -> list[Entry]:
    """One Ember Charcoal offer per rarity, together as heavy as every other reward on a new board.

    They share one weightMult, so Bountiful's rarity weights make the common offer twice as likely
    as the uncommon and four times the rare. A board's reputation shifts weight toward rarer
    rewards, so the share drifts as it grows; the report shows by how much.
    """
    others_weight = sum(entry.weight * entry.rarity.value for entry in others)
    weight = others_weight / sum(rarity.value for rarity in REWARD_RARITIES)
    prices = rules.ember.price
    return [Entry("ember", rules.ember.item, r, prices[r], reward_amount(prices[r], rules), weight, None) for r in REWARD_RARITIES]


def ember_share(rewards: list[Entry], reputation: Rarity) -> float:
    """Ember Charcoal's share of reward weight on a board whose reputation unlocks the given rarity."""
    rank = list(Rarity)

    def weight(entry: Entry) -> float:
        # Each rarity the board's reputation has passed divides an entry's weight by 2.25.
        behind = max(rank.index(reputation) - rank.index(entry.rarity), 0)
        return entry.weight * entry.rarity.value / 2.25**behind

    total = sum(weight(entry) for entry in rewards)
    return sum(weight(entry) for entry in rewards if entry.category == "ember") / total
