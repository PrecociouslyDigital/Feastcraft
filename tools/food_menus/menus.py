#!/usr/bin/env python3
"""Generates the pack's food menus as a datapack: extended type menus and a menu per food mod.

The food_advancements mod ships the menu advancements and the menus behind them, but its tags hold
only vanilla foods. This script reads the datapack export's items.tsv, classifies the pack's
edibles into the mod's six menus (soups, cooked meats, sweets, golden foods, risky foods), and adds
one menu per food mod with enough dishes of its own. Tag files merge with the mod's, so the six
existing advancements go pack-wide untouched, and each new menu gets an advancement of its own.

    python3 menus.py                  # writes datapack/ from build/datapack-export/items.tsv
    python3 menus.py --install <dir>  # also copies the datapack into this Open Loader folder

Both live in the repo: datapack/ is committed, so a change to the menus shows up as a diff.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
DATAPACK = HERE / "datapack"
EXPORT = HERE.parent.parent / "build" / "datapack-export"
NAMESPACE = "feastcraft_food_advancements"
PACK_FORMAT = 15
ROOT = f"{NAMESPACE}:root"

# Mods with at least this many edibles get their own menu.
MOD_MENU_MINIMUM = 45

# Menus already in the mod, by tag path. These only gain values here; the advancement exists.
EXTENDED = ("soups", "cooked_meats", "sweets", "golden_foods", "risky_foods")

# New menus, by mod namespace: (key, title, description). The icon comes from the menu's own foods.
MOD_MENUS = {
    "cookbook_of_babel": ("whole_library", "The Whole Library", "Eat every dish in the Cookbook of Babel"),
    "croptopia": ("croptopia", "Taste of Croptopia", "Eat every Croptopia crop and dish"),
    "garnished": ("garnished", "Garnished to Perfection", "Eat every Create: Garnished delicacy"),
    "pamhc2crops": ("pams_crops", "Pam's Harvest", "Eat every crop from Pam's HarvestCraft"),
    "pamhc2foodcore": ("pams_pantry", "Pam's Pantry", "Eat everything in Pam's food core"),
    "pamhc2trees": ("pams_orchard", "The Orchard", "Eat every fruit from Pam's trees"),
    "alexscaves": ("cave_cuisine", "Cave Cuisine", "Eat everything Alex's Caves has to offer"),
    "tide": ("tide_catch", "Angler's Banquet", "Eat every catch of the Tide"),
    "fruitsdelight": ("fruit_basket", "Fruit Basket", "Eat every Fruit Delight treat"),
    "twilightdelight": ("forest_feast", "Forest Feast", "Eat every Twilight Delight dish"),
    "farmersdelight": ("farmers_spread", "Farmer's Spread", "Eat every Farmer's Delight dish"),
    "veggiesdelight": ("garden_plate", "Garden Plate", "Eat every Veggie Delight dish"),
    "miners_delight": ("spelunkers_snacks", "Spelunker's Snacks", "Eat every Miner's Delight morsel"),
    "vintagedelight": ("pantry_of_the_past", "Pantry of the Past", "Eat every Vintage Delight preserve"),
    "netherdepthsupgrade": ("lava_angler", "Lava Angler", "Eat every catch from the Nether depths"),
}

# A food whose English name matches goes on the menu. Word-boundary \b...\b unless the phrase is
# meant to match inside a word, and a match on the id as well as the name.
NAME_WORDS = {
    "soups": [r"\bsoup", r"\bstew\b", r"\sstew\b", r"\bbroth\b", r"\bchowder\b", r"\bbisque\b"],
    "cooked_meats": [
        r"\bcooked\b", r"\bgrilled\b", r"\broasted\b", r"\bsmoked\b", r"\bbacon\b", r"\bsausage",
        r"\bmeatball", r"\bmeatloaf\b", r"\bjerky\b", r"\bribs?\b", r"\bwings\b", r"\bsteak\b",
        r"\bham\b", r"\broast\b", r"\bpulled\b",
    ],
    "sweets": [
        r"\bcake\b", r"\bcookie", r"\bpie\b", r"\bcandy\b", r"\bsweet\b", r"\bchocolate\b",
        r"\bcaramel", r"\bice cream\b", r"\bjelly\b", r"\bdonut", r"\bsundae\b", r"\bpudding\b",
        r"\bfudge\b", r"\btoffee\b", r"\bmarshmallow", r"\bcandied\b", r"\btart\b",
    ],
    "golden_foods": [r"\bgolden\b"],
    "risky_foods": [
        r"\brotten\b", r"\bpoison", r"\bspider eye\b", r"\bpufferfish\b", r"\btoxic\b",
        r"\bdeadly\b", r"\bvenom", r"\braw chicken\b",
    ],
}

# Names that match a menu's words without belonging on it. Checked after the match.
NAME_EXCLUSIONS = {
    "sweets": [r"sweet potato", r"sweetberry"],
    "cooked_meats": [r"meat tenderiser"],
}


def words_match(words: list[str], *texts: str) -> bool:
    """One of the regexes matches any of the texts, lowercase, and none of the exclusions apply."""
    joined = " \n ".join(text.lower() for text in texts)
    return any(re.search(pattern, joined) for pattern in words)


def load_edibles() -> tuple[dict[str, tuple[str, str]], set[str]]:
    """Every registered item by id, and the ids of the ones with hunger, from the export."""
    items: dict[str, tuple[str, str]] = {}
    edible: set[str] = set()
    with (EXPORT / "items.tsv").open() as file:
        for line in file:
            id, hunger, _, name, *rest = line.rstrip("\n").split("\t")
            items[id] = (name, hunger)
            try:
                if float(hunger) > 0:
                    edible.add(id)
            except ValueError:
                pass
    return items, edible


def menus(edible: set[str], items: dict[str, tuple[str, str]]) -> dict[str, list[str]]:
    """Every menu's tag path, with the pack's edibles on it."""
    menus: dict[str, list[str]] = {tag: [] for tag in EXTENDED}
    mod_foods: dict[str, list[str]] = defaultdict(list)
    for id in sorted(edible):
        namespace, _, path = id.partition(":")
        name = items[id][0]
        for tag, words in NAME_WORDS.items():
            exclusions = NAME_EXCLUSIONS.get(tag, [])
            if words_match(words, name, path) and not words_match(exclusions, name, path):
                menus[tag].append(id)
                break
        if namespace in MOD_MENUS:
            mod_foods[namespace].append(id)
    for namespace, (key, _, _) in MOD_MENUS.items():
        menus[f"menus/{key}"] = mod_foods[namespace]
    return menus


def menu_advancement(id: str, tag: str, title: str, description: str, frame: str, icon: str) -> dict:
    return {
        "parent": ROOT,
        "criteria": {
            "eaten": {
                "conditions": {"count": "all", "foods": {"tag": tag}},
                "trigger": f"{NAMESPACE}:foods_eaten",
            }
        },
        "display": {
            "announce_to_chat": True,
            "description": {"text": description},
            "frame": frame,
            "hidden": True,
            "icon": {"item": icon},
            "show_toast": True,
            "title": {"text": title},
        },
        "requirements": [["eaten"]],
        "sends_telemetry_event": True,
    }


def files(items: dict[str, tuple[str, str]], edible: set[str]) -> dict[str, dict | str]:
    """Every file of the datapack, by path inside it."""
    out: dict[str, dict | str] = {
        "pack.mcmeta": {"pack": {"description": "Feastcraft food menus", "pack_format": PACK_FORMAT}},
    }
    for tag, values in menus(edible, items).items():
        if not values:
            sys.exit(f"menu {tag} came out empty")
        out[f"data/{NAMESPACE}/tags/items/{tag}.json"] = {"replace": False, "values": values}
        if tag not in EXTENDED:
            key = tag.removeprefix("menus/")
            _, title, description = next(m for m in MOD_MENUS.values() if m[0] == key)
            out[f"data/{NAMESPACE}/advancements/every_{key}.json"] = menu_advancement(
                f"{NAMESPACE}:every_{key}", f"{NAMESPACE}:{tag}", title, description, "goal", values[0])
    return out


def write(files_out: dict[str, dict | str], install: Path | None) -> None:
    for name, content in files_out.items():
        path = DATAPACK / name
        path.parent.mkdir(parents=True, exist_ok=True)
        if isinstance(content, str):
            path.write_text(content)
        else:
            path.write_text(json.dumps(content, indent=2) + "\n")
    # A tag the pack no longer fills must not linger from an earlier run.
    for old in DATAPACK.rglob("*"):
        if old.is_file() and old.relative_to(DATAPACK).as_posix() not in files_out and old.name != "pack.mcmeta":
            old.unlink()
    if install is not None:
        target = install / "feastcraft_food_menus"
        if target.exists():
            shutil.rmtree(target)
        shutil.copytree(DATAPACK, target)
        print(f"installed to {target}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--install", type=Path, metavar="DIR", help="copy the datapack into this Open Loader folder")
    args = parser.parse_args()
    items, edible = load_edibles()
    out = files(items, edible)
    menus_out = menus(edible, items)
    write(out, args.install)
    print(f"{len(out) - 1 - len(menus_out)} advancements, {len(menus_out)} menus -> {DATAPACK}")
    for tag in sorted(menus_out):
        print(f"  {tag}: {len(menus_out[tag])}")
