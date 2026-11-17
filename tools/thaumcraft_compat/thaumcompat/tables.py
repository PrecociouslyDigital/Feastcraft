"""The hand-written aspects and the tag table, checked against the export."""

from __future__ import annotations

import tomllib
from pathlib import Path
from typing import Any

from .aspects import ASPECTS
from .derive import Inherits, Listed, Value
from .export import Aspects, Export, Kind, Subject


class TableError(Exception):
    pass


def _exists(export: Export, subject: Subject) -> bool:
    return subject.id in (export.mobs if subject.kind == "entity" else export.items)


def _aspects_or_parent(where: str, raw: Any) -> Value:
    if isinstance(raw, str):
        return Inherits(raw)
    if not raw or not isinstance(raw, dict):
        raise TableError(f"{where} needs aspects or a parent")
    unknown = sorted(set(raw) - set(ASPECTS))
    if unknown:
        raise TableError(f"{where} has aspects Thaumcraft doesn't: {', '.join(unknown)}")
    if any(not isinstance(amount, int) or amount < 1 for amount in raw.values()):
        raise TableError(f"{where} needs whole amounts of at least 1")
    return Listed(dict(sorted(raw.items())))


def _kind(path: Path, table: str) -> Kind:
    if table == "item":
        return "item"
    if table == "entity":
        return "entity"
    raise TableError(f"{path.name} has a [{table}] table; it takes [item] and [entity]")


def hand(path: Path, export: Export) -> dict[Subject, Value]:
    out: dict[Subject, Value] = {}
    for table, entries in tomllib.loads(path.read_text()).items():
        if table == "known":
            continue
        kind = _kind(path, table)
        for id, raw in entries.items():
            subject = Subject(kind, id)
            if not _exists(export, subject):
                raise TableError(f"{path.name}: the game has no {subject}")
            value = _aspects_or_parent(f"{path.name}: {subject}", raw)
            if isinstance(value, Inherits) and Subject(kind, value.parent) not in export.known:
                raise TableError(f"{path.name}: {subject} copies {value.parent}, which Thaumcraft gives no aspects")
            out[subject] = value
    return out


def tag_parents(path: Path, export: Export) -> dict[str, dict[str, str]]:
    out: dict[str, dict[str, str]] = {}
    for table, entries in tomllib.loads(path.read_text()).items():
        kind = _kind(path, table)
        for tag, parent in entries.items():
            if Subject(kind, parent) not in export.known:
                raise TableError(f"{path.name}: {tag} copies {parent}, which Thaumcraft gives no aspects")
        out[kind] = dict(entries)
    return out


def known(path: Path, export: Export) -> dict[Subject, Aspects]:
    """Vanilla items' aspects for the recipe step, where the export has none."""
    out: dict[Subject, Aspects] = {}
    for id, raw in tomllib.loads(path.read_text()).get("known", {}).items():
        subject = Subject("item", id)
        if id not in export.items:
            raise TableError(f"{path.name}: the game has no {subject}")
        if subject in export.known:
            raise TableError(f"{path.name}: [known] has {id}, which Thaumcraft already gives aspects")
        value = _aspects_or_parent(f"{path.name}: {subject}", raw)
        if not isinstance(value, Listed):
            raise TableError(f"{path.name}: [known] lists aspects, not parents")
        out[subject] = value.aspects
    return out
