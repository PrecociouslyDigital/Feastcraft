from ..model import Keystone
from ..stats import Stat
from . import attribute, compare, power

# Food level, in half shanks, at or below which each step applies.
STEPS = (15, 12, 9, 6, 3)
DAMAGE_PER_STEP = 8
SPEED_PER_STEP = 4

FEAST_OR_FAMINE = Keystone(
    "Feast or Famine",
    gain=(
        f"Each 1.5 shanks you are below 9 gives +{DAMAGE_PER_STEP}% melee damage and "
        f"+{SPEED_PER_STEP}% movement speed, up to +{DAMAGE_PER_STEP * len(STEPS)}% and "
        f"+{SPEED_PER_STEP * len(STEPS)}% at 1.5 shanks."
    ),
    cost="Staying that hungry means no natural regeneration, and eating switches the bonus off.",
    rewards=tuple(
        power(
            f"feast_or_famine/hunger_{level}",
            f"At {level / 2:g} shanks or fewer: +{DAMAGE_PER_STEP}% melee damage, +{SPEED_PER_STEP}% speed",
            "conditioned_attribute",
            condition=compare("food_level", "<=", level),
            tick_rate=10,
            modifiers=[
                attribute(Stat.MELEE_DAMAGE, "multiply_base", DAMAGE_PER_STEP / 100),
                attribute(Stat.MOVEMENT_SPEED, "multiply_base", SPEED_PER_STEP / 100),
            ],
        )
        for level in STEPS
    ),
    icon="minecraft:rotten_flesh",
)
