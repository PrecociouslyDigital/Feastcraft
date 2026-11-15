"""Writes the tree as a datapack: Pufferfish's category, the custom powers, the origin layer and the point rule."""

import json
import shutil
from collections.abc import Iterator
from pathlib import Path

from .layout import Tree
from .model import (
    NAMESPACE, Bonus, Command, Content, CustomPower, Json, Keystone, Multiply, Notable, Origin,
    OriginPower, Resist, Reward, Small, Start, Travel,
)
from .powers import scale

CATEGORY = "feastcraft"
PACK_FORMAT = 15
GAIN, COST, NOTE = "green", "red", "gray"


def _frame(content: Content) -> tuple[str, float]:
    match content:
        case Start():
            return "goal", 1.25
        case Travel():
            return "task", 0.75
        case Small():
            return "task", 1.0
        case Notable():
            return "goal", 1.25
        case Keystone() | Origin():
            return "challenge", 1.6


def _resist_powers(skill: str, resist: Resist) -> Iterator[CustomPower]:
    for kind in resist.kinds:
        body: Json = {
            "type": "origins:modify_damage_taken",
            "hidden": True,
            "damage_condition": kind.damage_condition,
            "modifier": scale(-resist.percent),
        }
        if kind.holder_condition is not None:
            body["condition"] = kind.holder_condition
        yield CustomPower(f"resist/{skill}/{kind.key}", resist.text(), body)


def _powers_of(skill: str, reward: Reward) -> Iterator[CustomPower]:
    match reward:
        case CustomPower():
            yield reward
        case Resist():
            yield from _resist_powers(skill, reward)
        case _:
            pass


def _power_reward(power_id: str) -> Json:
    return {"type": "puffish_skills_origins:power", "data": {"power": power_id}}


def _rewards(skill: str, reward: Reward) -> Iterator[Json]:
    match reward:
        case Bonus(stat, amount):
            grant = stat.info.grant
            yield {
                "type": "puffish_skills:attribute",
                "data": {"attribute": stat.info.id, "value": round(grant.modifier_value(amount), 6), "operation": grant.operation},
            }
        case Multiply(stat, percent):
            yield {
                "type": "puffish_skills:attribute",
                "data": {"attribute": stat.info.id, "value": round(percent / 100, 6), "operation": "multiply_total"},
            }
        case OriginPower() | CustomPower():
            yield _power_reward(reward.id)
        case Resist():
            yield from (_power_reward(p.id) for p in _resist_powers(skill, reward))
        case Command(unlock, lock, _):
            yield {"type": "puffish_skills:command", "data": {"unlock_command": unlock, "lock_command": lock}}


def _reward_line(reward: Reward) -> Json:
    match reward:
        case OriginPower():
            return {"translate": f"power.origins.{reward.path}.name"}
        case _:
            return {"text": reward.text()}


def _lines(lines: list[Json]) -> Json:
    out: list[Json | str] = [""]
    for i, line in enumerate(lines):
        if i:
            out.append("\n")
        out.append(line)
    return {"text": "", "extra": out}


def _text(content: Content) -> tuple[Json, Json | None]:
    """The description, and the extra description shown on demand."""
    match content:
        case Start():
            return _lines([{"text": "Everyone begins here."}]), None
        case Travel(_, rewards, _) | Small(_, rewards, _) | Notable(_, rewards, _):
            return _lines([_reward_line(r) for r in rewards]), None
        case Keystone(_, gain, cost, rewards, _):
            description = _lines([{"text": gain, "color": GAIN}, {"text": cost, "color": COST}])
            return description, _lines([_reward_line(r) for r in rewards])
        case Origin(_, gain, cost, powers, _):
            description = _lines([
                {"text": gain, "color": GAIN},
                {"text": cost, "color": COST},
                {"text": "Locks the other origins.", "color": NOTE},
            ])
            return description, _lines([_reward_line(p) for p in powers])


def _rewards_of(content: Content) -> tuple[Reward, ...]:
    match content:
        case Start():
            return ()
        case Origin():
            return content.powers
        case _:
            return content.rewards


def _definition(skill: str, content: Content) -> Json:
    frame, size = _frame(content)
    description, extra = _text(content)
    definition: Json = {
        "title": content.title,
        "description": description,
        "icon": {"type": "item", "data": {"item": content.icon}},
        "frame": {"type": "advancement", "data": {"frame": frame}},
        "size": size,
        "rewards": [r for reward in _rewards_of(content) for r in _rewards(skill, reward)],
    }
    if extra is not None:
        definition["extra_description"] = extra
    return definition


def _custom_powers(tree: Tree) -> dict[str, CustomPower]:
    found: dict[str, CustomPower] = {}
    for skill, node in tree.nodes.items():
        for reward in _rewards_of(node.content):
            for power in _powers_of(skill, reward):
                if power.name in found and found[power.name].body != power.body:
                    raise ValueError(f"two different powers are called {power.name}")
                found[power.name] = power
    return found


def files(tree: Tree, scale_to_pixels: float = 1.0) -> dict[str, Json]:
    """Every file of the datapack, by path inside it."""
    data = f"data/{NAMESPACE}"
    category = f"{data}/puffish_skills/categories/{CATEGORY}"
    out: dict[str, Json] = {
        "pack.mcmeta": {"pack": {"description": "Feastcraft skill tree", "pack_format": PACK_FORMAT}},
        f"{data}/puffish_skills/config.json": {"version": 3, "categories": [CATEGORY]},
        f"{category}/category.json": {
            "title": "Feastcraft",
            "icon": {"type": "item", "data": {"item": "minecraft:cake"}},
            "background": {"texture": "minecraft:textures/block/dark_oak_planks.png", "width": 16, "height": 16, "position": "tile"},
            "unlocked_by_default": True,
            "starting_points": 0,
            "colors": {"connections": {"excluded": {"fill": "#00000000", "stroke": "#00000000"}}},
        },
        f"{category}/definitions.json": {skill: _definition(skill, node.content) for skill, node in tree.nodes.items()},
        f"{category}/skills.json": {
            skill: {
                "x": round(node.at.x * scale_to_pixels),
                "y": round(node.at.y * scale_to_pixels),
                "definition": skill,
                **({"root": True} if isinstance(node.content, Start) else {}),
            }
            for skill, node in tree.nodes.items()
        },
        f"{category}/connections.json": {
            "normal": {"bidirectional": [list(e) for e in tree.edges], "unidirectional": []},
            "exclusive": {"bidirectional": [list(e) for e in tree.exclusive], "unidirectional": []},
        },
        "data/origins/origin_layers/origin.json": {
            "replace": True,
            "enabled": True,
            "origins": ["origins:human"],
            "auto_choose": True,
            "hidden": True,
        },
        f"{data}/feastcraft_advancement_points/rules/advancements.json": {
            "category": f"{NAMESPACE}:{CATEGORY}",
            "advancements": ["*:*"],
            "points": 1,
        },
    }
    for name, power in sorted(_custom_powers(tree).items()):
        out[f"{data}/powers/{name}.json"] = power.body
    return out


def write(tree: Tree, root: Path) -> None:
    if root.exists():
        shutil.rmtree(root)
    for path, body in files(tree).items():
        target = root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(body, indent=2, ensure_ascii=False) + "\n")
