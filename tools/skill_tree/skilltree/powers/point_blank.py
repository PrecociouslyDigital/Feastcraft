from ..model import Keystone
from .ranged_distance import by_distance

# (blocks, percent): each applies within that distance, and they add up.
STEPS = ((12, 10), (8, 10), (5, 15), (3, 15))
FAR = 20

POINT_BLANK = Keystone(
    "Point Blank",
    gain=(
        f"Your projectiles deal more the closer the target: +{STEPS[0][1]}% within {STEPS[0][0]} blocks, "
        f"up to +{sum(p for _, p in STEPS)}% within {STEPS[-1][0]}."
    ),
    cost=f"Past {FAR} blocks they deal half.",
    rewards=(
        *(by_distance("point_blank", "<=", blocks, percent) for blocks, percent in STEPS),
        by_distance("point_blank", ">", FAR, -50, multiplicative=True),
    ),
    icon="minecraft:crossbow",
)
