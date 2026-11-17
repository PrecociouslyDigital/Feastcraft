"""Writes the aspect entries the feastcraft_thaumcraft_aspects mod reads, and a report of every subject."""

from __future__ import annotations

import json
import shutil
from collections import defaultdict
from collections.abc import Collection, Mapping
from pathlib import Path
from typing import Any

from .budget import block_items
from .derive import Asked, Crafted, Hand, Inherits, Listed, Resolution, Tag, Value, subjects, value
from packexport import Aspects, Export, Subject

ENTRIES = Path("data/feastcraft_thaumcraft_compat/feastcraft_thaumcraft_aspects")


def _entry(kind: str, id: str, value: Value) -> dict[str, Any]:
    if isinstance(value, Inherits):
        return {kind: id, "parent": value.parent}
    return {kind: id, "aspects": value.aspects}


def entries(export: Export, resolved: Mapping[Subject, Resolution], kept: Collection[str]) -> list[dict[str, Any]]:
    """An entry per subject with aspects, and one per block of a block item within the budget."""
    blocks = block_items(export)
    out = []
    for subject, resolution in sorted(resolved.items(), key=lambda pair: (pair[0].kind, pair[0].id)):
        item_value = value(resolution)
        if subject.kind == "entity":
            out.append(_entry("entity", subject.id, item_value))
            continue
        if subject.id in blocks and subject.id not in kept:
            continue
        out.append(_entry("item", subject.id, item_value))
        for block in blocks.get(subject.id, []):
            out.append(_entry("block", block, _block_value(export, item_value)))
    return out


def _block_value(export: Export, item_value: Value) -> Value:
    """A block copies the block form of its item's parent, or lists the parent item's aspects if that has none."""
    if isinstance(item_value, Listed):
        return item_value
    if Subject("block", item_value.parent) in export.known:
        return item_value
    return Listed(export.known[Subject("item", item_value.parent)])


def write(datapack: Path, entries: list[dict[str, Any]]) -> None:
    folder = datapack / ENTRIES
    if folder.exists():
        shutil.rmtree(folder)
    folder.mkdir(parents=True)
    by_namespace: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for entry in entries:
        id = next(entry[kind] for kind in ("item", "block", "entity") if kind in entry)
        by_namespace[id.split(":")[0]].append(entry)
    for namespace, listed in sorted(by_namespace.items()):
        lines = ",\n".join("  " + json.dumps(entry, sort_keys=False) for entry in listed)
        (folder / f"{namespace}.json").write_text(f"[\n{lines}\n]\n")


def _aspects(export: Export, subject: Subject, resolution: Resolution) -> Aspects:
    resolved = value(resolution)
    return resolved.aspects if isinstance(resolved, Listed) else export.known[Subject(subject.kind, resolved.parent)]


def _step(resolution: Resolution) -> tuple[str, str]:
    if isinstance(resolution, Hand):
        return "hand", resolution.value.parent if isinstance(resolution.value, Inherits) else ""
    if isinstance(resolution, Tag):
        return "tag", f"{resolution.tag} -> {resolution.parent}"
    if isinstance(resolution, Crafted):
        return "recipe", resolution.recipe
    assert isinstance(resolution, Asked)
    return resolution.model, ""


def report(
    path: Path,
    export: Export,
    resolved: Mapping[Subject, Resolution],
    to_ask: Collection[Subject],
    kept: Collection[str],
) -> None:
    """One line per subject: kind, id, aspects, the step that gave them and its detail, and a note."""
    blocks = block_items(export)
    lines = ["kind\tid\taspects\tstep\tdetail\tnote"]
    for subject in subjects(export):
        note = "over the block budget" if subject.kind == "item" and subject.id in blocks and subject.id not in kept else ""
        if subject in resolved:
            step, detail = _step(resolved[subject])
            aspects = " ".join(f"{a}:{n}" for a, n in _aspects(export, subject, resolved[subject]).items())
        elif subject in to_ask:
            step, detail, aspects = "raw", "", ""
        else:
            step, detail, aspects = "none", "", ""
        lines.append(f"{subject.kind}\t{subject.id}\t{aspects}\t{step}\t{detail}\t{note}")
    path.write_text("\n".join(lines) + "\n")
