"""Copies recipes from one machine to another, so a player can take either route."""

from __future__ import annotations

import json
from collections.abc import Iterator
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import packexport

Json = dict[str, Any]

HAMMERS = "forge:tools/hammers"
# A Create spout takes about this long to fill one item.
COOLING_TIME = 20


@dataclass(frozen=True)
class Bridge:
    # Where the copy goes under data/feastcraft/recipes/, from the id of the recipe it copies.
    path: str
    recipe: Json


@dataclass(frozen=True)
class Skipped:
    source: str
    reason: str


def _path(source_id: str) -> str:
    namespace, _, path = source_id.partition(":")
    return f"{namespace}/{path}"


def tinkers_casting(source_id: str, filling: Json, block_items: frozenset[str]) -> Bridge | Skipped:
    """A Create filling recipe as a Tinkers casting, with the item being filled as a consumed cast."""
    fluids = [i for i in filling["ingredients"] if "fluid" in i or "fluidTag" in i]
    items = [i for i in filling["ingredients"] if i not in fluids]
    if len(fluids) != 1 or len(items) != 1 or len(filling["results"]) != 1:
        return Skipped(source_id, "not one item, one fluid and one result")
    fluid, cast, result = fluids[0], items[0], filling["results"][0]
    if fluid.get("nbt"):
        # Mantle's fluid ingredients can't match NBT, so Cookbook of Babel's bulk fluid and potions are out.
        return Skipped(source_id, "fluid needs NBT")
    if result.get("chance", 1) < 1:
        return Skipped(source_id, "result is a chance")
    if cast == {"item": "minecraft:bucket"}:
        return Skipped(source_id, "Tinkers already fills buckets")

    cast_items = {i.get("item") for i in (cast if isinstance(cast, list) else [cast])}
    is_block = cast_items <= block_items and result["item"] in block_items
    return Bridge(
        f"casting/{_path(source_id)}",
        {
            "type": "tconstruct:casting_basin" if is_block else "tconstruct:casting_table",
            "cast": cast,
            "cast_consumed": True,
            "cooling_time": COOLING_TIME,
            "fluid": (
                {"amount": fluid["amount"], "tag": fluid["fluidTag"]}
                if "fluidTag" in fluid
                else {"amount": fluid["amount"], "fluid": fluid["fluid"]}
            ),
            "result": result,
        },
    )


def cutting_board(source_id: str, pressing: Json) -> Bridge:
    """A Create pressing recipe as a Farmer's Delight cutting board recipe that takes a hammer."""
    return Bridge(
        f"hammering/{_path(source_id)}",
        {
            "type": "farmersdelight:cutting",
            "ingredients": pressing["ingredients"],
            "tool": {"tag": HAMMERS},
            "result": pressing["results"],
        },
    )


def _sources(export: Path, recipes: tuple[packexport.Recipe, ...], type: str) -> Iterator[tuple[str, Json]]:
    for recipe in recipes:
        if recipe.type == type:
            namespace, _, path = recipe.id.partition(":")
            yield recipe.id, json.loads((export / "data" / namespace / "recipes" / f"{path}.json").read_text())


def _slots(casting: Json) -> set[str]:
    """The cast and fluid pairs Tinkers matches a casting on. Castings that share one compete."""
    cast = json.dumps(casting.get("cast"), sort_keys=True)
    fluids = casting["fluid"] if isinstance(casting["fluid"], list) else [casting["fluid"]]
    return {f"{cast} {fluid.get('fluid', fluid.get('tag'))}" for fluid in fluids}


def build(export_dir: Path) -> tuple[list[Bridge], list[Skipped]]:
    export = packexport.load(export_dir)
    block_items = frozenset(item for item in export.blocks.values() if item)
    taken = {
        slot
        for type in ("tconstruct:casting_table", "tconstruct:casting_basin")
        for _, casting in _sources(export_dir, export.recipes, type)
        if "cast" in casting
        for slot in _slots(casting)
    }

    bridges: list[Bridge] = []
    skipped: list[Skipped] = []
    for id, filling in _sources(export_dir, export.recipes, "create:filling"):
        made = tinkers_casting(id, filling, block_items)
        if isinstance(made, Skipped):
            skipped.append(made)
        elif _slots(made.recipe) & taken:
            # Several mods add the same filling, and Tinkers casts some of them already.
            skipped.append(Skipped(id, "something already casts that fluid onto that item"))
        else:
            taken |= _slots(made.recipe)
            bridges.append(made)

    bridges += [cutting_board(id, pressing) for id, pressing in _sources(export_dir, export.recipes, "create:pressing")]
    return bridges, skipped
