"""Thaumcraft's overworld biomes, cut into Terralith's climate map."""

from __future__ import annotations

import json
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any

PARAMETERS = ("temperature", "humidity", "continentalness", "erosion", "weirdness", "depth")
SURFACE = (-0.005, 0.0)

Range = tuple[float, float]


@dataclass(frozen=True)
class Box:
    ranges: dict[str, Range]

    def overlaps(self, other: Box) -> bool:
        return all(self.ranges[p][0] < other.ranges[p][1] and other.ranges[p][0] < self.ranges[p][1] for p in PARAMETERS)

    def without(self, hole: Box) -> list[Box]:
        """What's left of this box after cutting out the hole, as boxes that don't overlap it."""
        if not self.overlaps(hole):
            return [self]
        pieces = []
        rest = dict(self.ranges)
        for parameter in PARAMETERS:
            low, high = rest[parameter]
            hole_low, hole_high = hole.ranges[parameter]
            if low < hole_low:
                pieces.append(Box({**rest, parameter: (low, hole_low)}))
            if hole_high < high:
                pieces.append(Box({**rest, parameter: (hole_high, high)}))
            rest[parameter] = (max(low, hole_low), min(high, hole_high))
        return pieces


# Where each biome grows. Tune these by flying around a new world.
THAUMCRAFT_BIOMES = {
    "thaumcraft:magical_forest": Box({
        "temperature": (-0.15, 0.2),
        "humidity": (0.3, 1.0),
        "continentalness": (0.03, 1.0),
        "erosion": (-0.375, 0.45),
        "weirdness": (0.25, 0.45),
        "depth": SURFACE,
    }),
    "thaumcraft:tainted_land": Box({
        "temperature": (0.2, 0.55),
        "humidity": (-0.35, 0.1),
        "continentalness": (0.3, 1.0),
        "erosion": (-0.375, 0.45),
        "weirdness": (-0.45, -0.3),
        "depth": SURFACE,
    }),
}


def _range(value: Any) -> Range:
    return (float(value[0]), float(value[1])) if isinstance(value, list) else (float(value), float(value))


def _box(parameters: dict[str, Any]) -> Box:
    return Box({p: _range(parameters[p]) for p in PARAMETERS})


def _entry(biome: str, box: Box, offset: Any) -> dict[str, Any]:
    parameters: dict[str, Any] = {p: list(box.ranges[p]) for p in PARAMETERS}
    parameters["offset"] = offset
    return {"biome": biome, "parameters": dict(sorted(parameters.items()))}


def with_thaumcraft(overworld: dict[str, Any]) -> dict[str, Any]:
    """Terralith's overworld with each Thaumcraft biome owning its box outright."""
    entries = []
    for entry in overworld["generator"]["biome_source"]["biomes"]:
        box = _box(entry["parameters"])
        pieces = [box]
        for hole in THAUMCRAFT_BIOMES.values():
            pieces = [left for piece in pieces for left in piece.without(hole)]
        if pieces == [box]:
            entries.append(entry)
        else:
            entries += [_entry(entry["biome"], piece, entry["parameters"]["offset"]) for piece in pieces]
    entries += [_entry(biome, box, 0) for biome, box in THAUMCRAFT_BIOMES.items()]
    source = {**overworld["generator"]["biome_source"], "biomes": entries}
    return {**overworld, "generator": {**overworld["generator"], "biome_source": source}}


def terralith_overworld(instance: Path) -> dict[str, Any]:
    """Read from Terralith's jar, because the merged data has this pack's own copy once it's installed."""
    [jar] = (instance / "mods").glob("Terralith_*.jar")
    with zipfile.ZipFile(jar) as terralith:
        overworld: dict[str, Any] = json.loads(terralith.read("data/minecraft/dimension/overworld.json"))
    return overworld


def write(instance: Path, datapack: Path) -> int:
    """Writes the overworld into the datapack and returns how many biome entries it has."""
    out = with_thaumcraft(terralith_overworld(instance))
    target = datapack / "data/minecraft/dimension/overworld.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(out, indent=2) + "\n")
    return len(out["generator"]["biome_source"]["biomes"])
