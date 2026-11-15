from ..model import Bonus, Keystone
from ..stats import Stat
from . import EMPTY, inverted, power

WORN = inverted(EMPTY)

ACROBAT = Keystone(
    "Acrobat",
    gain="A 30% chance to dodge any hit.",
    cost="You cannot wear anything on your head or chest, elytra included.",
    rewards=(
        Bonus(Stat.DODGE_CHANCE, 30),
        power("acrobat/bare_chest", "Nothing on head or chest", "conditioned_restrict_armor", head=WORN, chest=WORN, tick_rate=20),
    ),
    icon="minecraft:rabbit_foot",
)
