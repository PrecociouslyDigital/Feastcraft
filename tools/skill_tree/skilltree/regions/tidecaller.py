from ..layout import Cluster, Far, Inner, KeystoneAt, Outer, Region
from ..model import Bonus as B, DamageKind, ManaTrickle, Notable as N, Resist
from ..origins import MERLING
from ..shapes import Fork, Kite, Wheel10, Wheel6, Wheel8
from ..stats import Stat as S

POISON_AND_WITHER = (DamageKind.POISON, DamageKind.WITHER)

SWIMMER = Cluster(
    "Swimmer",
    Kite(N("Strong Stroke", (B(S.SWIM_SPEED, 20),), "minecraft:heart_of_the_sea")),
    smalls=(B(S.SWIM_SPEED, 5), B(S.WATER_SPEED, 5)),
    icon="minecraft:tropical_fish",
    radius=286, angle=4, hangs_from=Inner(1),
)
ANGLER = Cluster(
    "Angler",
    Wheel8(
        N("Patient Hand", (B(S.LUCK, 2),), "minecraft:fishing_rod"),
        N("Big Catch", (B(S.RARE_LOOT_ROLLS, 1),), "minecraft:nautilus_shell"),
    ),
    smalls=(B(S.LUCK, 0.5),),
    icon="minecraft:salmon",
    radius=480, angle=-11.5, hangs_from=Outer(1),
)
LEECHCRAFT = Cluster(
    "Leechcraft",
    Fork(
        N("Leech", (B(S.LIFE_STEAL, 6),), "minecraft:ghast_tear"),
        N("Second Skin", (B(S.OVERHEAL, 10),), "minecraft:slime_ball"),
    ),
    smalls=(B(S.LIFE_STEAL, 2), B(S.OVERHEAL, 3)),
    icon="minecraft:spider_eye",
    radius=626, angle=-12, hangs_from=ANGLER,
)
DEEP_LUNGS = Cluster(
    "Deep Lungs",
    Wheel6(
        N("Pearl Diver", (B(S.BREATH, 8),), "minecraft:turtle_helmet"),
        N("Clear Eyes", (B(S.WATER_VISION, 50),), "minecraft:sea_lantern"),
    ),
    smalls=(B(S.BREATH, 2), B(S.WATER_VISION, 10)),
    icon="minecraft:pufferfish",
    radius=476, angle=11.5, hangs_from=Outer(3),
)
DEPTHWALKER = Cluster(
    "Depthwalker",
    Wheel10(
        N("Magma Wader", (B(S.LAVA_SPEED, 30), B(S.LAVA_VISION, 100)), "minecraft:magma_block"),
        N("Undertow", (B(S.SWIM_SPEED, 10), B(S.WATER_SPEED, 10), B(S.LAVA_SPEED, 10)), "minecraft:kelp"),
    ),
    smalls=(B(S.LAVA_SPEED, 8), B(S.LAVA_VISION, 20), B(S.SWIM_SPEED, 4)),
    icon="minecraft:lava_bucket",
    radius=632, angle=10.5, hangs_from=DEEP_LUNGS,
)
ANTIVENOM = Cluster(
    "Antivenom",
    Kite(N("Iron Blood", (Resist(POISON_AND_WITHER, 20),), "minecraft:milk_bucket")),
    smalls=(Resist(POISON_AND_WITHER, 8),),
    icon="minecraft:fermented_spider_eye",
    radius=566, angle=0, hangs_from=DEEP_LUNGS, joins=LEECHCRAFT,
)

FLOODTIDE = Cluster(
    "Floodtide",
    Wheel8(
        N("Wellspring", (ManaTrickle(20),), "botania:mana_tablet"),
        N("Tidal Pull", (B(S.SWIM_SPEED, 15), B(S.BREATH, 4)), "minecraft:conduit"),
    ),
    smalls=(B(S.SWIM_SPEED, 4), B(S.BREATH, 2)),
    icon="botania:mana_pearl",
    radius=970, angle=0, hangs_from=Far(4),
)

TIDECALLER = Region(
    "tidecaller", "Tidecaller",
    road=B(S.SWIM_SPEED, 3), road_icon="minecraft:kelp",
    inner=SWIMMER,
    clusters=(ANGLER, LEECHCRAFT, DEEP_LUNGS, DEPTHWALKER, ANTIVENOM, FLOODTIDE),
    keystones=(KeystoneAt(MERLING, 778, 6, DEPTHWALKER),),
)
