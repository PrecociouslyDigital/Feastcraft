from ..layout import Cluster, Far, Inner, KeystoneAt, Outer, Region
from ..model import Bonus as B, ExtraSlot, Notable as N, Slot
from ..origins import BLAZEBORN, ENDERIAN
from ..powers.pack_leader import PACK_LEADER
from ..powers.thaumaturgy import CLEAR_MIND, INITIATES_NOTES
from ..shapes import Ring6, Spine, Wheel6, Wheel8
from ..stats import Stat as S

ATTUNEMENT = Cluster(
    "Attunement",
    Ring6(N("Focus", (B(S.MAGIC_DAMAGE, 10), B(S.MAGIC_RESISTANCE, 10)), "minecraft:amethyst_cluster")),
    smalls=(B(S.MAGIC_DAMAGE, 4), B(S.MAGIC_RESISTANCE, 4)),
    icon="minecraft:lapis_lazuli",
    radius=290, angle=2, hangs_from=Inner(1),
)
HEXCRAFT = Cluster(
    "Hexcraft",
    Wheel6(
        N("Frailty", (B(S.MAGIC_RESISTANCE_SHRED, 10),), "minecraft:fermented_spider_eye"),
        N("Unravel", (B(S.PROTECTION_SHRED, 10), B(S.PROTECTION_PIERCE, 1)), "minecraft:shears"),
    ),
    smalls=(B(S.MAGIC_RESISTANCE_SHRED, 3), B(S.PROTECTION_SHRED, 3)),
    icon="minecraft:soul_lantern",
    radius=472, angle=-12, hangs_from=Outer(1),
)
TRINKETS = Cluster(
    "Trinkets",
    Wheel8(
        N("Crowned", (ExtraSlot(Slot.HEAD),), "minecraft:golden_helmet"),
        N("Charmed", (ExtraSlot(Slot.CHARM),), "minecraft:rabbit_foot"),
    ),
    smalls=(B(S.EXPERIENCE_GAINED, 3),),
    icon="minecraft:gold_ingot",
    radius=614, angle=-11.5, hangs_from=HEXCRAFT,
)
FROST = Cluster(
    "Frost",
    Spine(
        N("Frostbite", (B(S.COLD_DAMAGE, 1.5),), "minecraft:powder_snow_bucket"),
        N("Deep Freeze", (B(S.COLD_DAMAGE, 3),), "minecraft:blue_ice"),
    ),
    smalls=(B(S.COLD_DAMAGE, 0.5), B(S.MAGIC_DAMAGE, 4)),
    icon="minecraft:snowball",
    radius=498, angle=11, hangs_from=Outer(3),
)
RUNESMITH = Cluster(
    "Runesmith",
    Wheel6(
        N("Thrifty Smith", (B(S.REPAIR_COST, -20),), "minecraft:anvil"),
        N("Scribe's Notes", (B(S.EXPERIENCE_GAINED, 15),), "minecraft:writable_book"),
    ),
    smalls=(B(S.REPAIR_COST, -5), B(S.EXPERIENCE_GAINED, 3)),
    icon="minecraft:experience_bottle",
    radius=646, angle=11, hangs_from=FROST,
)

MANA_FONT = Cluster(
    "Mana Font",
    Wheel8(
        N("Mana Well", (ExtraSlot(Slot.MANA_TABLET),), "botania:mana_tablet"),
        N("Arcane Reserve", (B(S.MAGIC_DAMAGE, 10),), "minecraft:amethyst_shard"),
    ),
    smalls=(B(S.MAGIC_DAMAGE, 4), B(S.EXPERIENCE_GAINED, 3)),
    icon="botania:mana_pearl",
    radius=970, angle=-9, hangs_from=Far(2),
)
THAUMATURGY = Cluster(
    "Thaumaturgy",
    Wheel8(CLEAR_MIND, INITIATES_NOTES),
    smalls=(B(S.MAGIC_DAMAGE, 4), B(S.MAGIC_RESISTANCE, 4)),
    icon="thaumcraft:item_thaumonomicon",
    radius=970, angle=9, hangs_from=Far(6),
)

ARCANE = Region(
    "arcane", "Arcane",
    road=B(S.EXPERIENCE_GAINED, 2), road_icon="minecraft:glass_bottle",
    inner=ATTUNEMENT,
    clusters=(HEXCRAFT, TRINKETS, FROST, RUNESMITH, MANA_FONT, THAUMATURGY),
    keystones=(
        KeystoneAt(PACK_LEADER, 568, 0, HEXCRAFT),
        KeystoneAt(ENDERIAN, 720, -13, TRINKETS),
        KeystoneAt(BLAZEBORN, 780, 12, RUNESMITH),
    ),
)
