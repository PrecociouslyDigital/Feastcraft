from ..model import Json, Notable
from ..stats import Stat
from . import all_of, attribute, power

METALS = ("manasteel", "elementium", "terrasteel")
PIECES = (("head", "helmet"), ("chest", "chestplate"), ("legs", "leggings"), ("feet", "boots"))
TOUGHNESS = 2
MAGIC_RESISTANCE = 15


def _wearing(slot: str, piece: str) -> Json:
    items = [{"item": f"botania:{metal}_{piece}"} for metal in METALS]
    return {"type": "origins:equipped_item", "equipment_slot": slot, "item_condition": {"type": "origins:ingredient", "ingredient": items}}


RUNIC_WEAVE = Notable(
    "Runic Weave",
    (
        power(
            "runic_weave",
            f"In a full set of Manasteel, Elementium or Terrasteel: +{TOUGHNESS} Armor Toughness, +{MAGIC_RESISTANCE}% Magic Resistance",
            "conditioned_attribute",
            condition=all_of(*(_wearing(slot, piece) for slot, piece in PIECES)),
            tick_rate=20,
            modifiers=[
                attribute(Stat.ARMOR_TOUGHNESS, "addition", TOUGHNESS),
                attribute(Stat.MAGIC_RESISTANCE, "multiply_base", MAGIC_RESISTANCE / 100),
            ],
        ),
    ),
    "botania:terrasteel_chestplate",
)
