"""Which block items get aspects. Each takes one of MFCL's 4,095 legacy block ids."""

from __future__ import annotations

from collections import Counter
from collections.abc import Iterable, Mapping

from .derive import Crafted, Hand, Resolution, Tag
from .export import Export, Subject

BLOCK_BUDGET = 1500
# Shapes of a block that only repeat what the full block offers.
SHAPE_TAGS = frozenset({
    "minecraft:stairs", "minecraft:slabs", "minecraft:walls", "minecraft:fences", "minecraft:fence_gates",
    "minecraft:buttons", "minecraft:pressure_plates", "minecraft:doors", "minecraft:trapdoors",
    "minecraft:signs", "minecraft:hanging_signs", "minecraft:wool_carpets", "quark:vertical_slabs",
})


def block_items(export: Export) -> dict[str, list[str]]:
    out: dict[str, list[str]] = {}
    for block, item in sorted(export.blocks.items()):
        if item:
            out.setdefault(item, []).append(block)
    return out


def kept(
    export: Export,
    resolved: Mapping[Subject, Resolution],
    raw: Iterable[Subject],
    uses: Counter[str],
) -> set[str]:
    """The block items within the budget: raw materials first, then crafted blocks, then tagged ones, shapes last."""
    blocks = block_items(export)

    def tier(subject: Subject) -> int:
        if SHAPE_TAGS & set(export.tags_of(subject)):
            return 3
        resolution = resolved.get(subject)
        if resolution is None or isinstance(resolution, Hand):
            return 0
        if isinstance(resolution, Crafted):
            return 1
        if isinstance(resolution, Tag):
            return 2
        return 0

    candidates = [s for s in list(resolved) + list(raw) if s.kind == "item" and s.id in blocks]
    ranked = sorted(set(candidates), key=lambda s: (tier(s), -uses[s.id], s.id))
    return {s.id for s in ranked[:BLOCK_BUDGET]}
