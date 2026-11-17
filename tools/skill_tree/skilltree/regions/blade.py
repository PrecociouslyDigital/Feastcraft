from ..layout import Cluster, Far, Inner, KeystoneAt, Outer, Region
from ..model import Bonus as B, ExtraSlot, Notable as N, Slot
from ..origins import ARACHNID
from ..powers.acrobat import ACROBAT
from ..powers.sanguine import SANGUINE
from ..shapes import Kite, Ring6, Spine, Wheel6, Wheel8
from ..stats import Stat as S

EDGEWORK = Cluster(
    "Edgework",
    Ring6(N("Whetstone", (B(S.BLADE_DAMAGE, 10), B(S.AXE_DAMAGE, 10)), "minecraft:grindstone")),
    smalls=(B(S.BLADE_DAMAGE, 4), B(S.AXE_DAMAGE, 4)),
    icon="minecraft:iron_sword",
    radius=288, angle=0, hangs_from=Inner(1),
)
DUELIST = Cluster(
    "Duelist",
    Wheel8(
        N("Riposte", (B(S.ATTACK_SPEED, 5), B(S.MELEE_RESISTANCE, 6)), "minecraft:shield"),
        N("Flurry", (B(S.ATTACK_SPEED, 10),), "minecraft:sugar"),
    ),
    smalls=(B(S.BLADE_DAMAGE, 4), B(S.ATTACK_SPEED, 3)),
    icon="minecraft:golden_sword",
    radius=476, angle=-11.5, hangs_from=Outer(1),
)
EXECUTIONER = Cluster(
    "Executioner",
    Spine(
        N("Lethal Precision", (B(S.CRIT_CHANCE, 10),), "minecraft:target"),
        N("Coup de Grâce", (B(S.CRIT_DAMAGE, 20), B(S.CURRENT_HP_DAMAGE, 3)), "minecraft:wither_skeleton_skull"),
    ),
    smalls=(B(S.CRIT_CHANCE, 2), B(S.CRIT_DAMAGE, 5)),
    icon="minecraft:netherite_sword",
    radius=628, angle=-12.5, hangs_from=DUELIST,
)
WOODSMANS_WRATH = Cluster(
    "Woodsman's Wrath",
    Spine(
        N("Cleave", (B(S.AXE_DAMAGE, 10), B(S.KNOCKBACK, 25)), "minecraft:iron_axe"),
        N("Sunder", (B(S.ARMOR_SHRED, 15),), "minecraft:chainmail_chestplate"),
    ),
    smalls=(B(S.AXE_DAMAGE, 4), B(S.ARMOR_SHRED, 3)),
    icon="minecraft:stone_axe",
    radius=502, angle=11, hangs_from=Outer(3),
)
POLEARM = Cluster(
    "Polearm",
    Wheel6(
        N("Barbed Prongs", (B(S.SPEAR_DAMAGE, 15),), "minecraft:prismarine_shard"),
        N("Long Reach", (B(S.ATTACK_REACH, 1),), "minecraft:end_rod"),
    ),
    smalls=(B(S.SPEAR_DAMAGE, 5), B(S.ATTACK_REACH, 0.2)),
    icon="minecraft:trident",
    radius=644, angle=11, hangs_from=WOODSMANS_WRATH,
)
SKULLCRUSHER = Cluster(
    "Skullcrusher",
    Kite(N("Meteor Strike", (B(S.MACE_DAMAGE, 15), B(S.SAFE_FALL_DISTANCE, 3)), "minecraft:anvil")),
    smalls=(B(S.MACE_DAMAGE, 4), B(S.SAFE_FALL_DISTANCE, 0.5)),
    icon="minecraft:skeleton_skull",
    radius=800, angle=24, hangs_from=POLEARM,
)

SIGNET = Cluster(
    "Signet",
    Kite(N("Signet Ring", (ExtraSlot(Slot.RING),), "minecraft:gold_nugget")),
    smalls=(B(S.MELEE_DAMAGE, 3),),
    icon="minecraft:gold_ingot",
    radius=700, angle=1, hangs_from=EXECUTIONER,
)

BLADESONG = Cluster(
    "Bladesong",
    Wheel8(
        N("Keen Edge", (B(S.CRIT_CHANCE, 5),), "minecraft:flint"),
        N("Deep Cuts", (B(S.CRIT_DAMAGE, 15),), "minecraft:redstone"),
    ),
    smalls=(B(S.CRIT_CHANCE, 1.5), B(S.MELEE_DAMAGE, 3)),
    icon="minecraft:netherite_sword",
    radius=970, angle=0, hangs_from=Far(4),
)

BLADE = Region(
    "blade", "Blade",
    road=B(S.MELEE_DAMAGE, 2), road_icon="minecraft:wooden_sword",
    inner=EDGEWORK,
    clusters=(DUELIST, EXECUTIONER, WOODSMANS_WRATH, POLEARM, SKULLCRUSHER, SIGNET, BLADESONG),
    keystones=(
        KeystoneAt(ACROBAT, 574, 0, DUELIST),
        KeystoneAt(ARACHNID, 790, -10, EXECUTIONER),
        KeystoneAt(SANGUINE, 738, 14, POLEARM),
    ),
)
