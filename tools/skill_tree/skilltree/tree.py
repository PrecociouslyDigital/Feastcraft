"""The whole tree: the regions in clockwise order from the top, the border spurs and the extra roads."""

from .layout import BorderCluster, Tree, build
from .model import Bonus as B, Notable as N, Travel
from .powers.point_blank import POINT_BLANK
from .powers.stonefist import STONEFIST
from .regions import arcane, blade, bulwark, delver, gourmand, marksman, tidecaller, wayfarer
from .stats import Stat as S

REGIONS = (
    blade.BLADE,
    bulwark.BULWARK,
    delver.DELVER,
    gourmand.GOURMAND,
    tidecaller.TIDECALLER,
    wayfarer.WAYFARER,
    marksman.MARKSMAN,
    arcane.ARCANE,
)


def _mix(name: str, first: B, second: B, icon: str) -> BorderCluster:
    notable = N(name, (B(first.stat, first.amount * 2), B(second.stat, second.amount * 2)), icon)
    return BorderCluster(name, (first, second), notable, icon)


# BORDERS[s] runs out between REGIONS[s] and the region after it.
BORDERS = (
    _mix("Vanguard", B(S.MELEE_DAMAGE, 3), B(S.ARMOR, 1), "minecraft:iron_helmet"),
    _mix("Sapper", B(S.ARMOR, 1), B(S.MINING_SPEED, 5), "minecraft:iron_shovel"),
    _mix("Harvester", B(S.FORTUNE, 0.15), B(S.LUCK, 0.5), "minecraft:golden_hoe"),
    POINT_BLANK,
    STONEFIST,
    _mix("Scout", B(S.MOVEMENT_SPEED, 2), B(S.RANGED_DAMAGE, 3), "minecraft:map"),
    _mix("Spellshot", B(S.RANGED_DAMAGE, 3), B(S.MAGIC_DAMAGE, 4), "minecraft:prismarine_crystals"),
    _mix("Spellblade", B(S.MELEE_DAMAGE, 3), B(S.MAGIC_DAMAGE, 4), "minecraft:diamond_sword"),
)

# Roads between outer clusters across a border, so the outer tree is not only dead ends.
ROADS = (
    (blade.POLEARM, bulwark.THORNMAIL),
    (gourmand.EPICURE, tidecaller.LEECHCRAFT),
    (marksman.SKIRMISHER, arcane.TRINKETS),
    (wayfarer.TRAILBLAZER, tidecaller.DEPTHWALKER),
)

SPOKES = tuple(
    Travel("Spoke", (bonus,), icon)
    for bonus, icon in (
        (B(S.MAX_HEALTH, 0.5), "minecraft:poppy"),
        (B(S.ARMOR, 0.25), "minecraft:leather"),
        (B(S.MOVEMENT_SPEED, 0.5), "minecraft:feather"),
        (B(S.JUMP, 1), "minecraft:rabbit_foot"),
        (B(S.ARMOR_TOUGHNESS, 0.125), "minecraft:scute"),
        (B(S.KNOCKBACK_RESISTANCE, 1), "minecraft:cobblestone"),
    )
)


def tree() -> Tree:
    return build(REGIONS, BORDERS, ROADS, SPOKES)
