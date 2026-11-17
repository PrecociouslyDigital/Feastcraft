"""Reads prices.toml and bounties.toml."""

from __future__ import annotations

import enum
import fnmatch
import tomllib
from collections.abc import Iterable, Mapping
from dataclasses import dataclass
from pathlib import Path
from typing import Any


class Rarity(enum.Enum):
    """Bountiful's rarities, with the weight it picks each by."""

    COMMON = 1024
    UNCOMMON = 512
    RARE = 256
    EPIC = 128
    LEGENDARY = 6


@dataclass(frozen=True)
class Selector:
    """Items in any of the tags or namespaces, or matching an id glob, less the denied ones.

    A deny entry is an id glob, or a tag when it starts with #.
    """

    tags: tuple[str, ...] = ()
    namespaces: tuple[str, ...] = ()
    ids: tuple[str, ...] = ()
    deny: tuple[str, ...] = ()

    def select(self, items: Iterable[str], item_tags: Mapping[str, frozenset[str]]) -> set[str]:
        items = set(items)
        chosen = {item for tag in self.tags for item in item_tags.get(tag, frozenset()) if item in items}
        chosen |= {item for item in items if item.split(":")[0] in self.namespaces}
        chosen |= {item for item in items if any(fnmatch.fnmatchcase(item, glob) for glob in self.ids)}
        denied_tags = [entry[1:] for entry in self.deny if entry.startswith("#")]
        denied = {item for tag in denied_tags for item in item_tags.get(tag, frozenset())}
        globs = [entry for entry in self.deny if not entry.startswith("#")]
        return {item for item in chosen if item not in denied and not any(fnmatch.fnmatchcase(item, glob) for glob in globs)}


def _selector(table: Mapping[str, Any]) -> Selector:
    return Selector(
        tags=tuple(table.get("tags", ())),
        namespaces=tuple(table.get("namespaces", ())),
        ids=tuple(table.get("ids", ())),
        deny=tuple(table.get("deny", ())),
    )


@dataclass(frozen=True)
class RawGroup:
    selector: Selector
    value: float


@dataclass(frozen=True)
class Prices:
    markup: float
    step_bonus: float
    cap: float
    ignored_recipe_types: frozenset[str]
    # Items that stand for many things through NBT, so no single price fits them.
    unpriced: frozenset[str]
    # Utensils a recipe gives back.
    free: Selector
    # In order: an item is worth the first group that has it.
    raw: dict[str, RawGroup]
    # Raw values that beat every group.
    items: dict[str, float]
    # What things made in ways the export can't see are worth.
    made: dict[str, float]


def prices(path: Path) -> Prices:
    data = tomllib.loads(path.read_text())
    return Prices(
        markup=data["markup"],
        step_bonus=data["step_bonus"],
        cap=data["cap"],
        ignored_recipe_types=frozenset(data["ignored_recipe_types"]),
        unpriced=frozenset(data["unpriced"]),
        free=_selector(data["free"]),
        raw={name: RawGroup(_selector(table), table["value"]) for name, table in data["raw"].items()},
        items=dict(data["items"]),
        made=dict(data["made"]),
    )


@dataclass(frozen=True)
class Objectives:
    selector: Selector
    rarity: Rarity
    # Only food: things with a hunger value.
    food: bool
    # The most of one item a bounty asks for.
    most: int
    # Shared among the category's items, so a big category isn't picked more for being big.
    weight: float


@dataclass(frozen=True)
class Rewards:
    # Names of raw groups in prices.toml.
    groups: tuple[str, ...]
    deny: tuple[str, ...]
    weight: float


@dataclass(frozen=True)
class Ember:
    item: str
    price: dict[Rarity, float]


@dataclass(frozen=True)
class Bounties:
    name: str
    objectives: dict[str, Objectives]
    rewards: dict[str, Rewards]
    # A reward is uncommon from the first value here, and rare from the second.
    rarer_from: tuple[float, float]
    # Each reward hands out between these worths.
    reward_worth: tuple[float, float]
    ember: Ember
    # Ids the export doesn't have, because the instance it came from lacks the repo's mods.
    known_extras: frozenset[str]


def bounties(path: Path) -> Bounties:
    data = tomllib.loads(path.read_text())
    ember = data["ember"]
    return Bounties(
        name=data["name"],
        objectives={
            name: Objectives(
                _selector(table), Rarity[table.get("rarity", "COMMON")], table.get("food", False), table.get("most", 64), table["weight"]
            )
            for name, table in data["objectives"].items()
        },
        rewards={
            name: Rewards(tuple(table["groups"]), tuple(table.get("deny", ())), table["weight"]) for name, table in data["rewards"].items()
        },
        rarer_from=tuple(data["rarer_from"]),
        reward_worth=tuple(data["reward_worth"]),
        ember=Ember(ember["item"], {Rarity[rarity]: price for rarity, price in ember["price"].items()}),
        known_extras=frozenset(data["known_extras"]),
    )
