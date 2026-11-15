from ..model import Keystone
from .ranged_distance import by_distance

# Each step adds this much from that distance on.
STEPS = (16, 24, 32, 40)
PERCENT_PER_STEP = 25
NEAR = 8

FAR_SHOT = Keystone(
    "Far Shot",
    gain=(
        f"Your projectiles deal +{PERCENT_PER_STEP}% from {STEPS[0]} blocks, and another "
        f"{PERCENT_PER_STEP}% every {STEPS[1] - STEPS[0]} blocks after, up to double at {STEPS[-1]}."
    ),
    cost=f"Within {NEAR} blocks they deal half.",
    rewards=(
        *(by_distance("far_shot", ">=", blocks, PERCENT_PER_STEP) for blocks in STEPS),
        by_distance("far_shot", "<=", NEAR, -50, multiplicative=True),
    ),
    icon="minecraft:spyglass",
)
