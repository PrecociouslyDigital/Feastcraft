"""Works out each subject's aspects from the first step that gives any: hand, tag, recipe, then asked."""

from __future__ import annotations

import math
from collections import defaultdict
from collections.abc import Iterable, Mapping
from dataclasses import dataclass

from packexport import Aspects, Export, Recipe, Subject

# Namespaces whose aspects Thaumcraft already settles.
SETTLED = ("minecraft", "thaumcraft")
CRAFTING_KEEPS = 0.75
MOST_OF_AN_ASPECT = 64


@dataclass(frozen=True)
class Inherits:
    parent: str


@dataclass(frozen=True)
class Listed:
    aspects: Aspects


Value = Inherits | Listed


@dataclass(frozen=True)
class Hand:
    value: Value


@dataclass(frozen=True)
class Tag:
    tag: str
    parent: str


@dataclass(frozen=True)
class Crafted:
    recipe: str
    aspects: Aspects


@dataclass(frozen=True)
class Asked:
    model: str
    aspects: Aspects


Resolution = Hand | Tag | Crafted | Asked


def value(resolution: Resolution) -> Value:
    if isinstance(resolution, Hand):
        return resolution.value
    if isinstance(resolution, Tag):
        return Inherits(resolution.parent)
    return Listed(resolution.aspects)


def subjects(export: Export) -> list[Subject]:
    """Everything outside vanilla and Thaumcraft that could have aspects."""
    items = [Subject("item", id) for id in export.items if id.split(":")[0] not in SETTLED]
    mobs = [Subject("entity", id) for id in export.mobs if id.split(":")[0] not in SETTLED]
    return sorted(items + mobs, key=lambda s: (s.kind, s.id))


def crafted(recipe: Recipe, known: Mapping[str, Aspects]) -> Aspects | None:
    """Thaumcraft's rule: three quarters of the ingredients' aspects, shared among the results."""
    total: dict[str, int] = defaultdict(int)
    for slot in recipe.slots:
        options = [(known[item], count) for item, count in slot if item in known]
        if not options:
            return None
        aspects, count = min(options, key=lambda option: sum(option[0].values()) * option[1])
        for aspect, amount in aspects.items():
            total[aspect] += amount * count
    out = {aspect: min(MOST_OF_AN_ASPECT, math.floor(amount * CRAFTING_KEEPS / recipe.count)) for aspect, amount in total.items()}
    out = {aspect: amount for aspect, amount in sorted(out.items()) if amount > 0}
    return out or None


def by_recipe(targets: Iterable[str], recipes: Iterable[Recipe], known: dict[str, Aspects]) -> dict[str, Crafted]:
    """Resolves items from recipes in rounds, each round using what earlier rounds found."""
    making: dict[str, list[Recipe]] = defaultdict(list)
    for recipe in recipes:
        making[recipe.result].append(recipe)
    pending = {item for item in targets if item in making and item not in known}
    known = dict(known)
    found: dict[str, Crafted] = {}
    while True:
        this_round: dict[str, Crafted] = {}
        for item in sorted(pending):
            options = [(recipe.id, aspects) for recipe in making[item] if (aspects := crafted(recipe, known)) is not None]
            if options:
                recipe_id, aspects = min(options, key=lambda option: (sum(option[1].values()), option[0]))
                this_round[item] = Crafted(recipe_id, aspects)
        if not this_round:
            return found
        found |= this_round
        pending -= this_round.keys()
        known |= {item: crafted.aspects for item, crafted in this_round.items()}


def resolve(
    export: Export,
    hand: Mapping[Subject, Value],
    tag_parents: Mapping[str, Mapping[str, str]],
    asked: Mapping[Subject, Asked],
) -> dict[Subject, Resolution]:
    """Each subject's resolution, if any step gives one. Subjects left out are raw materials still to ask about."""
    out: dict[Subject, Resolution] = {}
    for subject in subjects(export):
        if subject in hand:
            out[subject] = Hand(hand[subject])
            continue
        tags = set(export.tags_of(subject))
        for tag, parent in tag_parents.get(subject.kind, {}).items():
            if tag in tags and parent != subject.id:
                out[subject] = Tag(tag, parent)
                break

    def aspects_of(subject: Subject, resolution: Resolution) -> Aspects | None:
        resolved = value(resolution)
        if isinstance(resolved, Listed):
            return resolved.aspects
        return export.known.get(Subject(subject.kind, resolved.parent))

    known = {s.id: a for s, a in export.known.items() if s.kind == "item"}

    def craft_the_rest() -> None:
        for subject, resolution in out.items():
            aspects = aspects_of(subject, resolution)
            if subject.kind == "item" and aspects:
                known[subject.id] = aspects
        items = [s.id for s in subjects(export) if s.kind == "item" and s not in out]
        for id, crafting in by_recipe(items, export.recipes, known).items():
            out[Subject("item", id)] = crafting

    craft_the_rest()
    # What no recipe makes is a raw material, and only those take answers.
    everything = set(subjects(export))
    out |= {subject: answer for subject, answer in asked.items() if subject not in out and subject in everything}
    craft_the_rest()
    return out
