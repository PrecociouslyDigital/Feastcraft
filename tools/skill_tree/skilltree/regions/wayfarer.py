from ..layout import Cluster, Inner, KeystoneAt, Outer, Region
from ..model import Bonus as B, Notable as N
from ..origins import ELYTRIAN, FELINE
from ..powers.feast_or_famine import FEAST_OR_FAMINE
from ..shapes import Ring6, Spine, Wheel6, Wheel8
from ..stats import Stat as S

STRIDE = Cluster(
    "Stride",
    Ring6(N("Long Road", (B(S.SPRINTING_SPEED, 12),), "minecraft:leather_boots")),
    smalls=(B(S.MOVEMENT_SPEED, 2), B(S.SPRINTING_SPEED, 4)),
    icon="minecraft:sugar",
    radius=288, angle=0, hangs_from=Inner(1),
)
LEAP = Cluster(
    "Leap",
    Spine(
        N("Spring Heels", (B(S.JUMP, 15),), "minecraft:slime_block"),
        N("Soft Landing", (B(S.FALL_REDUCTION, 3),), "minecraft:hay_block"),
    ),
    smalls=(B(S.JUMP, 5), B(S.FALL_REDUCTION, 0.5)),
    icon="minecraft:rabbit_hide",
    radius=494, angle=-11, hangs_from=Outer(1),
)
TRAILBLAZER = Cluster(
    "Trailblazer",
    Wheel6(
        N("Sure-footed", (B(S.STEP_HEIGHT, 0.5),), "minecraft:oak_stairs"),
        N("Second Wind", (B(S.JUMP_COUNT, 1),), "minecraft:firework_rocket"),
    ),
    smalls=(B(S.STEP_HEIGHT, 0.1), B(S.SAFE_FALL_DISTANCE, 0.5)),
    icon="minecraft:compass",
    radius=640, angle=-12, hangs_from=LEAP,
)
SHADOW = Cluster(
    "Shadow",
    Wheel8(
        N("Unseen", (B(S.STEALTH, 3),), "minecraft:black_wool"),
        N("Light Step", (B(S.STEALTH, 1), B(S.MOVEMENT_SPEED, 3)), "minecraft:white_wool"),
    ),
    smalls=(B(S.STEALTH, 0.5),),
    icon="minecraft:ink_sac",
    radius=480, angle=11.5, hangs_from=Outer(3),
)
WARDING = Cluster(
    "Warding",
    Wheel6(
        N("Arrow Turn", (B(S.RANGED_RESISTANCE, 10),), "minecraft:arrow"),
        N("Spectral Ward", (B(S.GHOST_HEALTH, 4),), "minecraft:totem_of_undying"),
    ),
    smalls=(B(S.RANGED_RESISTANCE, 4), B(S.MAGIC_RESISTANCE, 4), B(S.GHOST_HEALTH, 1)),
    icon="minecraft:amethyst_shard",
    radius=622, angle=12, hangs_from=SHADOW,
)

WAYFARER = Region(
    "wayfarer", "Wayfarer",
    road=B(S.MOVEMENT_SPEED, 1), road_icon="minecraft:dirt_path",
    inner=STRIDE,
    clusters=(LEAP, TRAILBLAZER, SHADOW, WARDING),
    keystones=(
        KeystoneAt(FELINE, 732, -14, TRAILBLAZER),
        KeystoneAt(FEAST_OR_FAMINE, 708, 15, WARDING),
        KeystoneAt(ELYTRIAN, 792, 2, WARDING),
    ),
)
