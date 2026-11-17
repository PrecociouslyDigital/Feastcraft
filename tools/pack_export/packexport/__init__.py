"""Reads what scripts/export-datapack wrote."""

from __future__ import annotations

import json
from collections.abc import Iterator
from dataclasses import dataclass
from pathlib import Path
from typing import Literal

Kind = Literal["item", "block", "entity"]
Aspects = dict[str, int]


@dataclass(frozen=True)
class Subject:
    kind: Kind
    id: str

    def __str__(self) -> str:
        return f"{self.kind} {self.id}"


@dataclass(frozen=True)
class Food:
    hunger: int
    saturation: float


@dataclass(frozen=True)
class Recipe:
    id: str
    # The recipe serializer, such as minecraft:smelting.
    type: str
    result: str
    count: int
    # One entry per slot: each item that fits it, with how many it takes.
    slots: tuple[tuple[tuple[str, int], ...], ...]


@dataclass(frozen=True)
class Export:
    items: dict[str, Food | None]
    # English names of items and mobs.
    names: dict[str, str]
    # Block id to the item that places it.
    blocks: dict[str, str | None]
    # Entity ids that have attributes, which is to say mobs.
    mobs: frozenset[str]
    recipes: tuple[Recipe, ...]
    # What Thaumcraft already gives, by subject.
    known: dict[Subject, Aspects]
    item_tags: dict[str, frozenset[str]]
    entity_tags: dict[str, frozenset[str]]

    def tags_of(self, subject: Subject) -> list[str]:
        tags = self.entity_tags if subject.kind == "entity" else self.item_tags
        return sorted(tag for tag, members in tags.items() if subject.id in members)


def _rows(path: Path) -> Iterator[list[str]]:
    for line in path.read_text().splitlines():
        yield line.split("\t")


def parse_aspects(text: str) -> Aspects:
    return {aspect: int(amount) for aspect, amount in (pair.split(":") for pair in text.split())}


def _slot(text: str) -> tuple[tuple[str, int], ...]:
    def item(alternative: str) -> tuple[str, int]:
        id, _, count = alternative.partition("*")
        return id, int(count or 1)

    return tuple(item(alternative) for alternative in text.split("|"))


def _recipes(path: Path) -> Iterator[Recipe]:
    for id, type, result, count, *rest in _rows(path):
        ingredients = rest[0] if rest else ""
        if result and ingredients:
            yield Recipe(id, type, result, int(count), tuple(_slot(slot) for slot in ingredients.split(" ")))


def _tags(data: Path, folder: str) -> dict[str, frozenset[str]]:
    raw: dict[str, list[str]] = {}
    for file in data.glob(f"*/tags/{folder}/**/*.json"):
        namespace = file.relative_to(data).parts[0]
        name = file.relative_to(data / namespace / "tags" / folder).with_suffix("").as_posix()
        values = json.loads(file.read_text()).get("values", [])
        raw[f"{namespace}:{name}"] = [value if isinstance(value, str) else value["id"] for value in values]

    resolved: dict[str, frozenset[str]] = {}

    def resolve(tag: str, seen: frozenset[str]) -> frozenset[str]:
        if tag in resolved:
            return resolved[tag]
        members: set[str] = set()
        for value in raw.get(tag, []):
            if value.startswith("#"):
                if value[1:] not in seen:
                    members |= resolve(value[1:], seen | {tag})
            else:
                members.add(value)
        resolved[tag] = frozenset(members)
        return resolved[tag]

    for tag in raw:
        resolve(tag, frozenset())
    return resolved


def load(export: Path) -> Export:
    item_rows = list(_rows(export / "items.tsv"))
    entity_rows = list(_rows(export / "entities.tsv"))
    items = {id: Food(int(hunger), float(saturation)) if hunger else None for id, hunger, saturation, _ in item_rows}
    names = {row[0]: row[-1] for row in item_rows + entity_rows}
    blocks = {block: item or None for block, item in _rows(export / "blocks.tsv")}
    mobs = frozenset(id for id, _category, living, _ in entity_rows if living)
    known = {Subject(kind, id): parse_aspects(aspects) for kind, id, aspects in _rows(export / "aspects.tsv")}  # type: ignore[arg-type]
    return Export(
        items=items,
        names=names,
        blocks=blocks,
        mobs=mobs,
        recipes=tuple(_recipes(export / "recipes.tsv")),
        known=known,
        item_tags=_tags(export / "data", "items"),
        entity_tags=_tags(export / "data", "entity_types"),
    )


def _loot_items(node: object) -> Iterator[str]:
    if isinstance(node, dict):
        if node.get("type") in ("minecraft:item", "item") and "name" in node:
            yield node["name"]
        for value in node.values():
            yield from _loot_items(value)
    elif isinstance(node, list):
        for value in node:
            yield from _loot_items(value)


def mob_drops(export: Path) -> frozenset[str]:
    """Every item some entity's loot table can drop."""
    tables = (export / "data").glob("*/loot_tables/entities/**/*.json")
    return frozenset(item for table in tables for item in _loot_items(json.loads(table.read_text()))) - {"minecraft:air"}
