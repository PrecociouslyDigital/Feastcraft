from ..model import Keystone
from . import DIGGING_TOOLS, EMPTY_HANDED, NOTHING, holding, power, scale

# A bare hand mines at 1; diamond tools at 8.
HAND_SPEED_PERCENT = 700

STONEFIST = Keystone(
    "Stonefist",
    gain="Your empty hand mines like a diamond pickaxe, axe and shovel at once.",
    cost="Tools break nothing in your hands.",
    rewards=(
        power("stonefist/harvest", "Empty hand harvests anything", "modify_harvest", condition=EMPTY_HANDED, allow=True),
        power("stonefist/speed", "Empty hand mines at diamond speed", "modify_break_speed", condition=EMPTY_HANDED, modifier=scale(HAND_SPEED_PERCENT)),
        power("stonefist/no_tools", "Tools break nothing", "modify_break_speed", condition=holding(DIGGING_TOOLS), modifier=NOTHING),
    ),
    icon="minecraft:cobblestone",
)
