from ..model import Bonus, Keystone, Multiply
from ..stats import Stat
from . import power

UNYIELDING = Keystone(
    "Unyielding",
    gain="Nothing knocks you back, and your armor counts for half again as much.",
    cost="You cannot sprint.",
    rewards=(
        Bonus(Stat.KNOCKBACK_RESISTANCE, 100),
        Multiply(Stat.ARMOR, 50),
        power("unyielding/no_sprint", "Cannot sprint", "prevent_sprinting"),
    ),
    icon="minecraft:anvil",
)
