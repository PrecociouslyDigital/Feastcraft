from ..model import Keystone
from ..stats import Stat
from . import all_of, attribute, inverted, power, scale

SKY = {"type": "origins:exposed_to_sky"}
UNDER_A_ROOF = inverted(SKY)
SUNLIT = all_of(SKY, {"type": "origins:daytime"})

TROGLODYTE = Keystone(
    "Troglodyte",
    gain="Under a roof you mine twice as fast and see in the dark.",
    cost="Under open sky in daylight you are 25% slower and deal 30% less melee damage.",
    rewards=(
        power("troglodyte/delving", "Under a roof: mine twice as fast", "modify_break_speed", condition=UNDER_A_ROOF, modifier=scale(100)),
        power("troglodyte/dark_sight", "Under a roof: see in the dark", "night_vision", condition=UNDER_A_ROOF, strength=1.0),
        power(
            "troglodyte/sunblind",
            "In daylight under open sky: slow and weak",
            "conditioned_attribute",
            condition=SUNLIT,
            tick_rate=10,
            modifiers=[
                attribute(Stat.MOVEMENT_SPEED, "multiply_base", -0.25),
                attribute(Stat.MELEE_DAMAGE, "multiply_base", -0.30),
            ],
        ),
    ),
    icon="minecraft:lantern",
)
