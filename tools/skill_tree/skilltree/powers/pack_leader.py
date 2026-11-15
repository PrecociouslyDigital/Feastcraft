from ..model import Bonus, Keystone
from ..stats import Stat
from . import NOTHING, power

PACK_LEADER = Keystone(
    "Pack Leader",
    gain="Your tamed animals deal triple damage and take a third.",
    cost="You deal no damage yourself.",
    rewards=(
        Bonus(Stat.TAMED_DAMAGE, 200),
        Bonus(Stat.TAMED_RESISTANCE, 67),
        power("pack_leader/harmless", "You deal no damage", "modify_damage_dealt", modifier=NOTHING),
        power("pack_leader/harmless_projectiles", "Your projectiles deal no damage", "modify_projectile_damage", modifier=NOTHING),
    ),
    icon="minecraft:bone",
)
