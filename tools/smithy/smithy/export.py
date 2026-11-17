"""Reads `build/datapack-export`, which `scripts/export-datapack` wrote.

The tag walker collects every item the export tags as a tool, weapon or armor
from the vanilla, c and forge tags. Recipes and advancements are read as raw
JSON so a rewrite can swap an item id without understanding its structure.
"""

from __future__ import annotations

import json
from collections.abc import Iterator
from pathlib import Path
from typing import Any

import packexport

Json = dict[str, Any]

# Mods whose items are Tinkers tools already, either built-in or from an add-on.
TINKERS_MODS = frozenset({
    "tconstruct",
    "tcintegrations",
    "tinkersdelight",
    "tinkers_reforged",
    "tinkers_katanas",
    "tinkers_things",
    "tinkersdiving",
    "tinkersjewelry",
    "constructs_casting",
})

# The vanilla, c and forge tags that name a plain tool or weapon. Anything tagged here
# from outside Tinkers must be curated in smithy.toml.
TOOL_TAGS = (
    "minecraft:swords",
    "minecraft:pickaxes",
    "minecraft:axes",
    "minecraft:shovels",
    "minecraft:hoes",
    "minecraft:weapons",
    "minecraft:bows",
    "minecraft:crossbows",
    "c:bows",
    "forge:bows",
    "c:crossbows",
    "forge:crossbows",
    "forge:tools/knives",
    "forge:tools/hatchets",
)

ARMOR_TAGS = (
    "forge:armors/helmets",
    "forge:armors/chestplates",
    "forge:armors/leggings",
    "forge:armors/boots",
)

# Shields only cost a durability bar, but a plain one is still a plain tool.
SHIELD_TAGS = ("minecraft:shields", "c:shields", "forge:shields")

TAGS = TOOL_TAGS + ARMOR_TAGS + SHIELD_TAGS


def tagged_items(export: packexport.Export) -> dict[str, set[str]]:
    """Every item in a tool, weapon or armor tag, with the tags it carries."""
    tagged: dict[str, set[str]] = {}
    for tag in TAGS:
        for item in export.item_tags.get(tag, frozenset()):
            namespace = item.partition(":")[0]
            if namespace not in TINKERS_MODS:
                tagged.setdefault(item, set()).add(tag)
    return tagged


def recipes(export_dir: Path) -> Iterator[tuple[str, Json]]:
    """Every recipe file the export has, with its id."""
    for file in sorted((export_dir / "data").glob("*/recipes/**/*.json")):
        namespace = file.relative_to(export_dir / "data").parts[0]
        path = file.relative_to(export_dir / "data" / namespace / "recipes").with_suffix("").as_posix()
        yield f"{namespace}:{path}", json.loads(file.read_text())


def advancements(export_dir: Path) -> Iterator[tuple[str, Json]]:
    """Every advancement file the export has, with its id."""
    for file in sorted((export_dir / "data").glob("*/advancements/**/*.json")):
        namespace = file.relative_to(export_dir / "data").parts[0]
        path = file.relative_to(export_dir / "data" / namespace / "advancements").with_suffix("").as_posix()
        yield f"{namespace}:{path}", json.loads(file.read_text())


def _item_containers(node: Any) -> Iterator[tuple[Any, str]]:
    if isinstance(node, dict):
        item = node.get("item")
        if isinstance(item, str) and ":" in item:
            yield node, item
        item_list = node.get("items")
        if isinstance(item_list, list):
            for id in item_list:
                if isinstance(id, str) and ":" in id:
                    yield node, id
        for value in node.values():
            yield from _item_containers(value)
    elif isinstance(node, list):
        for value in node:
            yield from _item_containers(value)


def recipe_items(recipe: Json) -> Iterator[tuple[Any, str]]:
    """Every place in a recipe that names an item, as (container, id).

    A container is the dict or list holding the id, so a rewrite can set it
    back. Ingredients given as a tag are left alone: the runtime conversion
    turns whatever they craft into a Tinkers tool anyway.
    """
    yield from _item_containers(recipe)


def advancement_items(advancement: Json) -> Iterator[tuple[Any, str]]:
    """Every item an advancement names: criteria's item predicates and icons."""
    yield from _item_containers(advancement)
