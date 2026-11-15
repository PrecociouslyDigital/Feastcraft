"""Projectile damage that depends on how far the target is from the shooter."""

from ..model import CustomPower
from . import compare, power, value


def by_distance(name: str, comparison: str, blocks: float, percent: float, *, multiplicative: bool = False) -> CustomPower:
    operation = "multiply_total_multiplicative" if multiplicative else "multiply_base_additive"
    nearer = "within" if comparison == "<=" else "from"
    sign = "+" if percent >= 0 else ""
    return power(
        f"{name}/{'within' if comparison == '<=' else 'beyond'}_{blocks:g}",
        f"Projectiles {nearer} {blocks:g} blocks: {sign}{percent:g}% damage",
        "modify_projectile_damage",
        bientity_condition=compare("distance", comparison, blocks),
        modifier=value(operation, percent / 100),
    )
