from ..layout import Cluster, Inner, KeystoneAt, Outer, Region
from ..model import Bonus as B, Notable as N
from ..origins import AVIAN
from ..powers.hearth import BOTTOMLESS, COMFORT_FOOD
from ..powers.open_hand import OPEN_HAND
from ..shapes import Ring6, Spine, Wheel6, Wheel8
from ..stats import Stat as S

APPETITE = Cluster(
    "Appetite",
    Ring6(N("Second Helping", (B(S.STAMINA, 1),), "minecraft:cooked_beef")),
    smalls=(B(S.STAMINA, 0.2), B(S.EATING_SPEED, 8)),
    icon="minecraft:bread",
    radius=288, angle=-3, hangs_from=Inner(1),
)
FORAGER = Cluster(
    "Forager",
    Wheel6(
        N("Green Thumb", (B(S.LUCK, 2),), "minecraft:bone_meal"),
        N("Full Basket", (B(S.LOOT_COUNT_ROLLS, 1),), "minecraft:composter"),
    ),
    smalls=(B(S.LUCK, 0.5),),
    icon="minecraft:sweet_berries",
    radius=468, angle=-12, hangs_from=Outer(1),
)
SHEPHERD = Cluster(
    "Shepherd",
    Wheel8(
        N("Loyal Hound", (B(S.TAMED_DAMAGE, 15), B(S.TAMED_RESISTANCE, 10)), "minecraft:lead"),
        N("Saddle-born", (B(S.MOUNT_SPEED, 20),), "minecraft:saddle"),
    ),
    smalls=(B(S.TAMED_DAMAGE, 6), B(S.TAMED_RESISTANCE, 5), B(S.MOUNT_SPEED, 5)),
    icon="minecraft:wheat",
    radius=608, angle=-12.5, hangs_from=FORAGER,
)
HEARTH_AND_HOME = Cluster(
    "Hearth and Home",
    Wheel8(
        N("Comfort Food", (COMFORT_FOOD,), "minecraft:mushroom_stew"),
        N("Bottomless", (BOTTOMLESS,), "minecraft:cake"),
    ),
    smalls=(B(S.NATURAL_REGENERATION, 6), B(S.HEALING, 4)),
    icon="minecraft:campfire",
    radius=488, angle=11, hangs_from=Outer(3),
)
EPICURE = Cluster(
    "Epicure",
    Spine(
        N("Savor", (B(S.GOOD_EFFECT_DURATION, 20),), "minecraft:honey_bottle"),
        N("Strong Stomach", (B(S.BAD_EFFECT_DURATION, -20),), "minecraft:milk_bucket"),
    ),
    smalls=(B(S.GOOD_EFFECT_DURATION, 5), B(S.BAD_EFFECT_DURATION, -5)),
    icon="minecraft:brewing_stand",
    radius=642, angle=10, hangs_from=HEARTH_AND_HOME,
)

GOURMAND = Region(
    "gourmand", "Gourmand",
    road=B(S.NATURAL_REGENERATION, 4), road_icon="minecraft:carrot",
    inner=APPETITE,
    clusters=(FORAGER, SHEPHERD, HEARTH_AND_HOME, EPICURE),
    keystones=(
        KeystoneAt(OPEN_HAND, 562, -1, HEARTH_AND_HOME),
        KeystoneAt(AVIAN, 786, 10, EPICURE),
    ),
)
