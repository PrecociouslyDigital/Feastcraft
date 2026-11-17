"""Works out what items are worth: raw ones by hand, crafted ones from their cheapest recipe."""

from __future__ import annotations

from collections import defaultdict, deque
from collections.abc import Collection, Iterable, Mapping, Set as AbstractSet
from dataclasses import dataclass

from packexport import Recipe

from .rules import Prices


@dataclass(frozen=True)
class Raw:
    # The raw group in prices.toml, or "items" for one listed by itself.
    group: str
    value: float


@dataclass(frozen=True)
class Made:
    """Made in a way the export can't see, such as Thaumcraft's arcane crafting."""

    value: float


@dataclass(frozen=True)
class Crafted:
    recipe: str
    # What the ingredients for one of the results cost.
    inputs: float
    value: float
    # How many recipes deep the item is, counting this one.
    steps: int


Price = Raw | Made | Crafted


def steps(price: Price) -> int:
    return price.steps if isinstance(price, Crafted) else 0


def raw(items: Iterable[str], item_tags: Mapping[str, frozenset[str]], rules: Prices) -> dict[str, Raw]:
    """Each raw item's value: its own entry in [items], or else the first raw group that has it."""
    items = set(items)
    out: dict[str, Raw] = {}
    for name, group in rules.raw.items():
        for item in group.selector.select(items, item_tags):
            out.setdefault(item, Raw(name, group.value))
    out |= {item: Raw("items", value) for item, value in rules.items.items()}
    return {item: price for item, price in out.items() if item not in rules.unpriced}


def _cost(recipe: Recipe, known: Mapping[str, Price], free: AbstractSet[str]) -> tuple[float, int] | None:
    """What one result's ingredients cost from the cheapest item in each slot, and the deepest of them."""
    total = 0.0
    deepest = 0
    for slot in recipe.slots:
        options = [(0.0, 0) for item, _ in slot if item in free]
        options += [(known[item].value * count, steps(known[item])) for item, count in slot if item in known]
        if not options:
            return None
        cost, depth = min(options)
        total += cost
        deepest = max(deepest, depth)
    return total / recipe.count, deepest


def resolve(items: Collection[str], item_tags: Mapping[str, frozenset[str]], recipes: Iterable[Recipe], rules: Prices) -> dict[str, Price]:
    """Every item that has a price.

    A crafted item is priced from its cheapest recipe, so an expensive alternate recipe can't make it
    look valuable. Prices only ever fall as cheaper ingredients are found, which settles loops of
    recipes (a block from ingots from the block) the same way shortest paths settle.
    """
    free = rules.free.select(items, item_tags)
    fixed: dict[str, Price] = {**raw(items, item_tags, rules), **{item: Made(value) for item, value in rules.made.items()}}
    usable = [
        recipe
        for recipe in recipes
        if recipe.type not in rules.ignored_recipe_types and recipe.result not in fixed and recipe.result not in rules.unpriced
    ]
    making: dict[str, list[Recipe]] = defaultdict(list)
    using: dict[str, set[str]] = defaultdict(set)
    for recipe in usable:
        making[recipe.result].append(recipe)
        for slot in recipe.slots:
            for item, _ in slot:
                using[item].add(recipe.result)

    known: dict[str, Price] = dict(fixed)
    # A queue in a fixed order, so ties between equally cheap recipes always go the same way.
    queue = deque(sorted(making))
    waiting = set(queue)
    while queue:
        item = queue.popleft()
        waiting.remove(item)
        options = []
        for recipe in making[item]:
            cost = _cost(recipe, known, free)
            if cost is not None:
                inputs, depth = cost
                value = min(rules.cap, inputs * rules.markup + rules.step_bonus)
                options.append((value, recipe.id, inputs, depth + 1))
        if not options:
            continue
        value, recipe_id, inputs, depth = min(options)
        current = known.get(item)
        if current is None or value < current.value:
            known[item] = Crafted(recipe_id, inputs, value, depth)
            for user in sorted(using[item] - waiting):
                queue.append(user)
                waiting.add(user)
    return known
