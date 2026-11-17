"""Writes the datapack and report.tsv."""

from __future__ import annotations

import json
import shutil
from collections.abc import Iterable
from pathlib import Path
from typing import Any

from .prices import Crafted, Made, Price, Raw
from .rules import Bounties, Rarity
from .select import Entry, LeftOut, ember_share

NAMESPACE = "feastcraft_bounties"
OBJECTIVES = "feastcraft_objs"
REWARDS = "feastcraft_rews"
# Bountiful only loads a decree or pool when every mod it requires is loaded.
NEVER_LOADED = "feastcraft_bounties_turned_off"


def _write(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def _pool(entries: Iterable[Entry], kind: str) -> dict[str, Any]:
    content = {}
    for entry in entries:
        key = f"{entry.category}_{kind}_{entry.item.replace(':', '_').replace('/', '_')}"
        content[key] = {
            "type": "item",
            "content": entry.item,
            "rarity": entry.rarity.name,
            "amount": {"min": entry.amount[0], "max": entry.amount[1]},
            "unitWorth": round(entry.worth, 1),
            "weightMult": round(entry.weight, 6),
        }
    return {"content": content}


def stock_files(sources: Path) -> list[str]:
    """Decrees and pools other packs ship, as namespace:path, from the export's sources.tsv."""
    out = []
    for line in sources.read_text().splitlines():
        location = line.split("\t")[0]
        namespace, _, path = location.partition(":")
        if namespace != NAMESPACE and path.startswith(("bounty_decrees/", "bounty_pools/")) and path.endswith(".json"):
            out.append(location)
    return sorted(out)


def write(datapack: Path, rules: Bounties, objectives: list[Entry], rewards: list[Entry], stock: list[str]) -> None:
    if datapack.exists():
        shutil.rmtree(datapack)
    _write(datapack / "pack.mcmeta", {"pack": {"description": "Feastcraft bounty market", "pack_format": 15}})
    data = datapack / "data"
    decree = {"name": rules.name, "objectives": [OBJECTIVES], "rewards": [REWARDS]}
    _write(data / NAMESPACE / "bounty_decrees" / NAMESPACE / "market.json", decree)
    _write(data / NAMESPACE / "bounty_pools" / NAMESPACE / f"{OBJECTIVES}.json", _pool(objectives, "obj"))
    _write(data / NAMESPACE / "bounty_pools" / NAMESPACE / f"{REWARDS}.json", _pool(rewards, "rew"))
    for location in stock:
        namespace, _, path = location.partition(":")
        turned_off: dict[str, Any] = {"requires": [NEVER_LOADED]}
        turned_off |= {"objectives": [], "rewards": []} if path.startswith("bounty_decrees/") else {"content": {}}
        _write(data / namespace / path, turned_off)


def _how(price: Price | None) -> tuple[str, str, str]:
    """How a worth was worked out: the method, what it came from, and how many recipes deep."""
    if isinstance(price, Raw):
        return "raw", price.group, "0"
    if isinstance(price, Made):
        return "made", "", "0"
    if isinstance(price, Crafted):
        return "crafted", f"{price.recipe} ({price.inputs:.1f})", str(price.steps)
    return "set", "bounties.toml", "0"


def report(path: Path, names: dict[str, str], objectives: list[Entry], rewards: list[Entry], left_out: list[LeftOut]) -> None:
    shares = ", ".join(f"{rarity.name.lower()} {ember_share(rewards, rarity):.1%}" for rarity in Rarity)
    lines = [
        f"# Ember Charcoal's share of reward weight, by board reputation: {shares}",
        "\t".join(["pool", "category", "item", "name", "rarity", "worth", "amount", "weight", "priced", "from", "steps"]),
    ]
    for pool, entries in (("objective", objectives), ("reward", rewards)):
        for entry in entries:
            how, source, steps = _how(entry.price)
            amount = f"{entry.amount[0]}-{entry.amount[1]}"
            lines.append("\t".join([pool, entry.category, entry.item, names.get(entry.item, ""), entry.rarity.name.lower(),
                                    f"{entry.worth:.1f}", amount, f"{entry.weight:.6f}", how, source, steps]))
    for left in left_out:
        lines.append("\t".join(["left out", left.category, left.item, names.get(left.item, ""), "", "", "", "", left.reason, "", ""]))
    path.write_text("\n".join(lines) + "\n")
