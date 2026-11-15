from ..layout import Cluster, Inner, KeystoneAt, Outer, Region
from ..model import Bonus as B, Notable as N
from ..origins import SHULK
from ..powers.scholars_ward import SCHOLARS_WARD
from ..powers.unyielding import UNYIELDING
from ..shapes import Kite, Spine, Wheel6, Wheel8, Wheel10
from ..stats import Stat as S

PLATING = Cluster(
    "Plating",
    Kite(N("Tempered", (B(S.ARMOR, 2), B(S.ARMOR_TOUGHNESS, 1)), "minecraft:iron_block")),
    smalls=(B(S.ARMOR, 1), B(S.ARMOR_TOUGHNESS, 0.5)),
    icon="minecraft:iron_chestplate",
    radius=286, angle=-4, hangs_from=Inner(1),
)
SHIELDWALL = Cluster(
    "Shieldwall",
    Wheel6(
        N("Unmoved", (B(S.KNOCKBACK_RESISTANCE, 25),), "minecraft:obsidian"),
        N("Bastion", (B(S.MELEE_RESISTANCE, 10),), "minecraft:stone_bricks"),
    ),
    smalls=(B(S.MELEE_RESISTANCE, 4), B(S.KNOCKBACK_RESISTANCE, 5)),
    icon="minecraft:shield",
    radius=470, angle=-12, hangs_from=Outer(1),
)
THORNMAIL = Cluster(
    "Thornmail",
    Wheel10(
        N("Barbs", (B(S.DAMAGE_REFLECTION, 10),), "minecraft:sweet_berries"),
        N("Retribution", (B(S.DAMAGE_REFLECTION, 10), B(S.ARMOR, 2)), "minecraft:pointed_dripstone"),
    ),
    smalls=(B(S.DAMAGE_REFLECTION, 3), B(S.ARMOR, 1)),
    icon="minecraft:cactus",
    radius=628, angle=-11, hangs_from=SHIELDWALL,
)
CONSTITUTION = Cluster(
    "Constitution",
    Wheel8(
        N("Hearty", (B(S.MAX_HEALTH, 4),), "minecraft:golden_apple"),
        N("Mend", (B(S.HEALING_RECEIVED, 15),), "minecraft:glistering_melon_slice"),
    ),
    smalls=(B(S.MAX_HEALTH, 2), B(S.HEALING_RECEIVED, 4)),
    icon="minecraft:apple",
    radius=500, angle=11.5, hangs_from=Outer(3),
)
EVASION = Cluster(
    "Evasion",
    Spine(
        N("Sidestep", (B(S.DODGE_CHANCE, 5),), "minecraft:leather_boots"),
        N("Slip Away", (B(S.DODGE_CHANCE, 5), B(S.MOVEMENT_SPEED, 3)), "minecraft:phantom_membrane"),
    ),
    smalls=(B(S.DODGE_CHANCE, 2), B(S.MOVEMENT_SPEED, 2)),
    icon="minecraft:feather",
    radius=654, angle=12, hangs_from=CONSTITUTION,
)

BULWARK = Region(
    "bulwark", "Bulwark",
    road=B(S.ARMOR, 0.5), road_icon="minecraft:leather_chestplate",
    inner=PLATING,
    clusters=(SHIELDWALL, THORNMAIL, CONSTITUTION, EVASION),
    keystones=(
        KeystoneAt(SCHOLARS_WARD, 572, 0, CONSTITUTION),
        KeystoneAt(UNYIELDING, 742, -12, THORNMAIL),
        KeystoneAt(SHULK, 792, 12, EVASION),
    ),
)
