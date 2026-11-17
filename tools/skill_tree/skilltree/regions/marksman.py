from ..layout import Cluster, Far, Inner, KeystoneAt, Outer, Region
from ..model import Bonus as B, DamageKind, Notable as N, Resist
from ..origins import PHANTOM
from ..powers.far_shot import FAR_SHOT
from ..shapes import Kite, Spine, Wheel6, Wheel8
from ..stats import Stat as S

FIRE_AND_LAVA = (DamageKind.FIRE,)

FLETCHING = Cluster(
    "Fletching",
    Kite(N("Broadhead", (B(S.ARROW_DAMAGE, 12),), "minecraft:flint")),
    smalls=(B(S.ARROW_DAMAGE, 4), B(S.ARROW_VELOCITY, 4)),
    icon="minecraft:arrow",
    radius=286, angle=-3, hangs_from=Inner(1),
)
QUICK_DRAW = Cluster(
    "Quick Draw",
    Wheel8(
        N("Snap Shot", (B(S.DRAW_SPEED, 15),), "minecraft:bow"),
        N("Taut String", (B(S.BOW_PROJECTILE_SPEED, 15),), "minecraft:string"),
    ),
    smalls=(B(S.DRAW_SPEED, 5), B(S.BOW_PROJECTILE_SPEED, 5)),
    icon="minecraft:bow",
    radius=500, angle=-11, hangs_from=Outer(1),
)
SHARPSHOOTER = Cluster(
    "Sharpshooter",
    Spine(
        N("Steady Aim", (B(S.CRIT_CHANCE, 8),), "minecraft:target"),
        N("Heartseeker", (B(S.CRIT_DAMAGE, 20),), "minecraft:spectral_arrow"),
    ),
    smalls=(B(S.CRIT_CHANCE, 2), B(S.CRIT_DAMAGE, 5)),
    icon="minecraft:tipped_arrow",
    radius=654, angle=-11, hangs_from=QUICK_DRAW,
)
ARBALEST = Cluster(
    "Arbalest",
    Wheel6(
        N("Heavy Bolt", (B(S.CROSSBOW_PROJECTILE_SPEED, 15), B(S.KNOCKBACK, 25)), "minecraft:crossbow"),
        N("Punch Through", (B(S.ARMOR_PIERCE, 2),), "minecraft:iron_nugget"),
    ),
    smalls=(B(S.CROSSBOW_PROJECTILE_SPEED, 5), B(S.ARMOR_PIERCE, 0.5)),
    icon="minecraft:crossbow",
    radius=466, angle=12, hangs_from=Outer(3),
)
SKIRMISHER = Cluster(
    "Skirmisher",
    Wheel8(
        N("Hit and Run", (B(S.SPRINTING_SPEED, 8), B(S.RANGED_RESISTANCE, 5)), "minecraft:leather_leggings"),
        N("Low Profile", (B(S.RANGED_RESISTANCE, 12),), "minecraft:tall_grass"),
    ),
    smalls=(B(S.RANGED_RESISTANCE, 4), B(S.SPRINTING_SPEED, 3)),
    icon="minecraft:leather_helmet",
    radius=606, angle=12, hangs_from=ARBALEST,
)
EMBERHIDE = Cluster(
    "Emberhide",
    Kite(N("Fireproof", (Resist(FIRE_AND_LAVA, 20),), "minecraft:magma_cream")),
    smalls=(Resist(FIRE_AND_LAVA, 8),),
    icon="minecraft:nether_brick",
    radius=574, angle=0, hangs_from=ARBALEST, joins=SHARPSHOOTER,
)

LONGBOW = Cluster(
    "Longbow",
    Wheel8(
        N("Eagle Eye", (B(S.ARROW_VELOCITY, 15),), "minecraft:spyglass"),
        N("Piercing Shot", (B(S.ARMOR_PIERCE, 2),), "minecraft:spectral_arrow"),
    ),
    smalls=(B(S.RANGED_DAMAGE, 3), B(S.ARROW_DAMAGE, 3)),
    icon="minecraft:bow",
    radius=970, angle=0, hangs_from=Far(4),
)

MARKSMAN = Region(
    "marksman", "Marksman",
    road=B(S.RANGED_DAMAGE, 2), road_icon="minecraft:stick",
    inner=FLETCHING,
    clusters=(QUICK_DRAW, SHARPSHOOTER, ARBALEST, SKIRMISHER, EMBERHIDE, LONGBOW),
    keystones=(
        KeystoneAt(FAR_SHOT, 792, -11, SHARPSHOOTER),
        KeystoneAt(PHANTOM, 710, 10, SKIRMISHER),
    ),
)
