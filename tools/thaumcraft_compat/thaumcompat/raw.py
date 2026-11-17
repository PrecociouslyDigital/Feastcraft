"""Which unresolved subjects are raw materials: made by no recipe, or only by a loop of recipes."""

from __future__ import annotations

import re
from collections import Counter, defaultdict
from collections.abc import Iterable, Mapping

from .derive import Resolution, subjects
from .export import Export, Recipe, Subject

# Items that should have no aspects at all.
NO_ASPECTS = re.compile(r"_spawn_egg$")


def uses(export: Export) -> Counter[str]:
    """How many recipes take each item."""
    return Counter(item for recipe in export.recipes for slot in recipe.slots for item in {i for i, _ in slot})


def _reachable(start: set[str], recipes: Mapping[str, list[Recipe]], targets: Iterable[str]) -> set[str]:
    have = set(start)
    pending = {t for t in targets if t not in have and t in recipes}
    while True:
        new = {t for t in pending if any(all(any(i in have for i, _ in slot) for slot in r.slots) for r in recipes[t])}
        if not new:
            return have
        have |= new
        pending -= new


def _cycles(nodes: set[str], edges: Mapping[str, set[str]]) -> list[set[str]]:
    """Strongly connected components that contain a cycle and depend on no other component."""
    index: dict[str, int] = {}
    low: dict[str, int] = {}
    stack: list[str] = []
    on_stack: set[str] = set()
    components: list[set[str]] = []
    for root in sorted(nodes):
        if root in index:
            continue
        work = [(root, iter(sorted(edges[root])))]
        index[root] = low[root] = len(index)
        stack.append(root)
        on_stack.add(root)
        while work:
            node, children = work[-1]
            child = next(children, None)
            if child is None:
                work.pop()
                if work:
                    low[work[-1][0]] = min(low[work[-1][0]], low[node])
                if low[node] == index[node]:
                    component = set()
                    while True:
                        member = stack.pop()
                        on_stack.discard(member)
                        component.add(member)
                        if member == node:
                            break
                    components.append(component)
            elif child not in index:
                index[child] = low[child] = len(index)
                stack.append(child)
                on_stack.add(child)
                work.append((child, iter(sorted(edges[child]))))
            elif child in on_stack:
                low[node] = min(low[node], index[child])
    out = []
    for component in components:
        cyclic = len(component) > 1 or any(m in edges[m] for m in component)
        leaves_it = any(edges[m] - component for m in component)
        if cyclic and not leaves_it:
            out.append(component)
    return out


def raw_materials(export: Export, resolved: Mapping[Subject, Resolution]) -> list[Subject]:
    """Mobs without aspects, items no recipe makes, and one item from each loop nothing else feeds."""
    mobs = [s for s in subjects(export) if s.kind == "entity" and s not in resolved]
    items = {s.id for s in subjects(export) if s.kind == "item" and s not in resolved and not NO_ASPECTS.search(s.id)}
    making: dict[str, list[Recipe]] = defaultdict(list)
    for recipe in export.recipes:
        making[recipe.result].append(recipe)
    counts = uses(export)

    raw = {item for item in items if item not in making}
    have = {s.id for s in export.known if s.kind == "item"} | {s.id for s in resolved if s.kind == "item"} | raw
    while True:
        have = _reachable(have, making, items)
        left = items - have
        edges = {item: {i for r in making[item] for slot in r.slots for i, _ in slot if i in left} for item in left}
        loops = _cycles(left, edges)
        if not loops:
            break
        for loop in loops:
            pick = min(loop, key=lambda item: (-counts[item], item))
            raw.add(pick)
            have.add(pick)
    return mobs + [Subject("item", item) for item in sorted(raw)]
