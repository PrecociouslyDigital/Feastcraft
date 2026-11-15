from ..layout import Cluster, Inner, KeystoneAt, Outer, Region
from ..model import Bonus as B, DamageKind, Notable as N, Resist
from ..powers.troglodyte import TROGLODYTE
from ..shapes import Fork, Kite, Ring6, Spine, Wheel6, Wheel8
from ..stats import Stat as S

PICKWORK = Cluster(
    "Pickwork",
    Ring6(N("Tunneler", (B(S.PICKAXE_SPEED, 25),), "minecraft:diamond_pickaxe")),
    smalls=(B(S.PICKAXE_SPEED, 6), B(S.MINING_SPEED, 4)),
    icon="minecraft:iron_pickaxe",
    radius=290, angle=3, hangs_from=Inner(1),
)
LUMBER_AND_LOAM = Cluster(
    "Lumber and Loam",
    Spine(
        N("Timberjack", (B(S.AXE_SPEED, 30),), "minecraft:diamond_axe"),
        N("Earthmover", (B(S.SHOVEL_SPEED, 30),), "minecraft:diamond_shovel"),
    ),
    smalls=(B(S.AXE_SPEED, 8), B(S.SHOVEL_SPEED, 8)),
    icon="minecraft:oak_log",
    radius=496, angle=-11, hangs_from=Outer(1),
)
TINKERS_REACH = Cluster(
    "Tinker's Reach",
    Wheel6(
        N("Long Arms", (B(S.BLOCK_REACH, 1),), "minecraft:scaffolding"),
        N("Deft Hands", (B(S.USE_ITEM_SPEED, 25),), "minecraft:clock"),
    ),
    smalls=(B(S.BLOCK_REACH, 0.25), B(S.BREAKING_SPEED, 4)),
    icon="minecraft:crafting_table",
    radius=642, angle=-9, hangs_from=LUMBER_AND_LOAM,
)
PROSPECTING = Cluster(
    "Prospecting",
    Wheel8(
        N("Rich Seam", (B(S.FORTUNE, 0.5),), "minecraft:raw_gold"),
        N("Lucky Strike", (B(S.RARE_LOOT_ROLLS, 1),), "minecraft:emerald"),
    ),
    smalls=(B(S.FORTUNE, 0.15),),
    icon="minecraft:raw_iron",
    radius=474, angle=11, hangs_from=Outer(3),
)
FORGEFIRE = Cluster(
    "Forgefire",
    Fork(
        N("Kindle", (B(S.FIRE_DAMAGE, 1.5),), "minecraft:flint_and_steel"),
        N("Conflagration", (B(S.FIRE_DAMAGE, 3),), "minecraft:fire_charge"),
    ),
    smalls=(B(S.FIRE_DAMAGE, 0.5), B(S.MAGIC_DAMAGE, 4)),
    icon="minecraft:blaze_powder",
    radius=618, angle=12, hangs_from=PROSPECTING,
)
BLAST_PADDING = Cluster(
    "Blast Padding",
    Kite(N("Bombproof", (Resist((DamageKind.EXPLOSION,), 20),), "minecraft:tnt")),
    smalls=(Resist((DamageKind.EXPLOSION,), 8),),
    icon="minecraft:gunpowder",
    radius=572, angle=1, hangs_from=PROSPECTING, joins=TINKERS_REACH,
)

DELVER = Region(
    "delver", "Delver",
    road=B(S.MINING_SPEED, 3), road_icon="minecraft:wooden_pickaxe",
    inner=PICKWORK,
    clusters=(LUMBER_AND_LOAM, TINKERS_REACH, PROSPECTING, FORGEFIRE, BLAST_PADDING),
    keystones=(KeystoneAt(TROGLODYTE, 772, -8, TINKERS_REACH),),
)
