"""The two Hearth and Home notables that need more than a stat."""

from . import compare, power

FOOD = {"type": "origins:food"}


def _effect(effect: str, seconds: int) -> dict[str, object]:
    return {
        "type": "origins:apply_effect",
        "effect": {"effect": effect, "duration": seconds * 20, "amplifier": 0, "show_particles": False},
    }


COMFORT_FOOD = power(
    "hearth/comfort_food",
    "Regeneration for 5 seconds after eating",
    "action_on_item_use",
    item_condition=FOOD,
    entity_action=_effect("minecraft:regeneration", 5),
)

BOTTOMLESS = power(
    "hearth/bottomless",
    "Eat while full; eating while full gives a minute of Absorption",
    "modify_food",
    item_condition=FOOD,
    always_edible=True,
    entity_action={
        "type": "origins:if_else",
        "condition": compare("food_level", ">=", 20),
        "if_action": _effect("minecraft:absorption", 60),
    },
)
