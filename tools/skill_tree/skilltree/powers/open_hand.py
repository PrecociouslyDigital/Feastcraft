from ..model import Keystone
from ..stats import Stat
from . import EMPTY_HANDED, MELEE_WEAPONS, NOTHING, attribute, holding, power

# A fist does 1; a diamond sword does 7.
FIST_BONUS = 6

OPEN_HAND = Keystone(
    "Open Hand",
    gain="Your empty hand hits as hard as a diamond sword, at a fist's speed.",
    cost="Swords, axes and tridents deal no damage in your hands.",
    rewards=(
        power(
            "open_hand/fist",
            f"Empty hand: +{FIST_BONUS} attack damage",
            "conditioned_attribute",
            condition=EMPTY_HANDED,
            tick_rate=5,
            modifiers=[attribute(Stat.ATTACK_DAMAGE, "addition", FIST_BONUS)],
        ),
        power("open_hand/no_weapons", "Weapons deal no damage", "modify_damage_dealt", condition=holding(MELEE_WEAPONS), modifier=NOTHING),
    ),
    icon="minecraft:bone_meal",
)
