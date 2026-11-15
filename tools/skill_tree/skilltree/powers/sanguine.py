from ..model import Bonus, Keystone
from ..stats import Stat
from . import power

SANGUINE = Keystone(
    "Sanguine",
    gain="You heal for a fifth of every melee hit you deal.",
    cost="Nothing else heals you over time: food fills you but does not mend you, and Regeneration does nothing.",
    rewards=(
        Bonus(Stat.LIFE_STEAL, 20),
        power("sanguine/no_regen", "No natural regeneration", "disable_regen"),
        power("sanguine/no_regeneration_effect", "Immune to Regeneration", "effect_immunity", effects=["minecraft:regeneration"]),
    ),
    icon="minecraft:redstone",
)
