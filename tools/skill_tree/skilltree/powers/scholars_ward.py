from ..model import Keystone
from . import compare, power, scale

# Experience level at which each step of protection starts.
STEPS = (5, 10, 20, 40, 80)
PERCENT_PER_STEP = 6
POINTS_BURNED_PER_HIT = 10

SCHOLARS_WARD = Keystone(
    "Scholar's Ward",
    gain=(
        f"You take {PERCENT_PER_STEP}% less damage at level {STEPS[0]}, and another {PERCENT_PER_STEP}% "
        f"each time your level doubles, up to {PERCENT_PER_STEP * len(STEPS)}% at level {STEPS[-1]}."
    ),
    cost=(
        f"Each hit you take burns {POINTS_BURNED_PER_HIT} experience points, and levels spent on "
        "enchanting stop protecting you."
    ),
    rewards=(
        *(
            power(
                f"scholars_ward/level_{level}",
                f"At level {level}: -{PERCENT_PER_STEP}% damage taken",
                "modify_damage_taken",
                condition=compare("xp_levels", ">=", level),
                modifier=scale(-PERCENT_PER_STEP),
            )
            for level in STEPS
        ),
        power(
            "scholars_ward/burn",
            f"Each hit burns {POINTS_BURNED_PER_HIT} experience points",
            "action_when_damage_taken",
            cooldown=1,
            entity_action={"type": "origins:execute_command", "command": f"xp add @s -{POINTS_BURNED_PER_HIT} points"},
        ),
    ),
    icon="minecraft:enchanted_book",
)
