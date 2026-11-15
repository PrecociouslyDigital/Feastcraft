"""Origins JSON building blocks shared by the custom powers."""

from ..model import CustomPower, Json
from ..stats import Stat


def power(name: str, summary: str, type_: str, **fields: object) -> CustomPower:
    return CustomPower(name, summary, {"type": f"origins:{type_}", "hidden": True, **fields})


def value(operation: str, amount: float) -> Json:
    """A value modifier, as damage and break speed powers take."""
    return {"operation": f"apoli:{operation}", "value": amount}


def scale(percent: float) -> Json:
    return value("multiply_base_additive", percent / 100)


NOTHING = value("multiply_total_multiplicative", -1)


def attribute(stat: Stat, operation: str, amount: float) -> Json:
    return {"attribute": stat.info.id, "operation": operation, "value": amount}


def compare(type_: str, comparison: str, compare_to: float) -> Json:
    return {"type": f"origins:{type_}", "comparison": comparison, "compare_to": compare_to}


def inverted(condition: Json) -> Json:
    return {**condition, "inverted": True}


def all_of(*conditions: Json) -> Json:
    return {"type": "origins:and", "conditions": list(conditions)}


def holding(item_condition: Json) -> Json:
    return {"type": "origins:equipped_item", "equipment_slot": "mainhand", "item_condition": item_condition}


EMPTY = {"type": "origins:empty"}
EMPTY_HANDED = holding(EMPTY)


def tagged(*tags: str) -> Json:
    return {"type": "origins:ingredient", "ingredient": [{"tag": t} for t in tags]}


MELEE_WEAPONS = tagged(
    "minecraft:swords", "minecraft:axes", "forge:tools/swords", "forge:tools/axes",
    "forge:tools/tridents", "tconstruct:modifiable/melee/weapon",
)
DIGGING_TOOLS = tagged(
    "minecraft:pickaxes", "minecraft:axes", "minecraft:shovels", "minecraft:hoes",
    "forge:tools/pickaxes", "forge:tools/axes", "forge:tools/shovels", "forge:tools/hoes",
    "tconstruct:modifiable/harvest",
)
