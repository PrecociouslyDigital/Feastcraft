"""Every attribute the tree grants."""

from dataclasses import dataclass
from enum import Enum


class Grant(Enum):
    """How a node's amount turns into an attribute modifier, and how its tooltip shows it.

    A node states its amount the way the tooltip shows it: 4 for "+4% Sword Damage", 1 for "+1 Armor".
    """

    FLAT = ("addition", 1, "")
    POINTS = ("addition", 0.01, "%")
    """Stats the game holds as fractions, such as crit chance at 0.05 for 5%."""
    SCALE = ("multiply_base", 0.01, "%")
    """Pufferfish's dynamic stats add these up rather than compounding them."""
    SECONDS = ("addition", 20, "s")
    """Stats the game counts in ticks."""

    @property
    def operation(self) -> str:
        operation: str = self.value[0]
        return operation

    def modifier_value(self, amount: float) -> float:
        factor: float = self.value[1]
        return amount * factor

    @property
    def suffix(self) -> str:
        suffix: str = self.value[2]
        return suffix


class Mod(Enum):
    MINECRAFT = "minecraft"
    FORGE = "forge"
    PUFFERFISH = "puffish_attributes"
    APOTHIC = "attributeslib"
    ADDITIONAL = "additionalentityattributes"
    TINKERS = "tconstruct"


@dataclass(frozen=True)
class StatInfo:
    mod: Mod
    path: str
    name: str
    grant: Grant

    @property
    def id(self) -> str:
        return f"{self.mod.value}:{self.path}"


def _s(mod: Mod, path: str, name: str, grant: Grant) -> StatInfo:
    return StatInfo(mod, path, name, grant)


M, F, P, A, X, T = Mod.MINECRAFT, Mod.FORGE, Mod.PUFFERFISH, Mod.APOTHIC, Mod.ADDITIONAL, Mod.TINKERS
FLAT, POINTS, SCALE, SECONDS = Grant.FLAT, Grant.POINTS, Grant.SCALE, Grant.SECONDS


class Stat(Enum):
    MAX_HEALTH = _s(M, "generic.max_health", "Max Health", FLAT)
    ARMOR = _s(M, "generic.armor", "Armor", FLAT)
    ARMOR_TOUGHNESS = _s(M, "generic.armor_toughness", "Armor Toughness", FLAT)
    ATTACK_DAMAGE = _s(M, "generic.attack_damage", "Attack Damage", FLAT)
    ATTACK_SPEED = _s(M, "generic.attack_speed", "Attack Speed", SCALE)
    MOVEMENT_SPEED = _s(M, "generic.movement_speed", "Movement Speed", SCALE)
    KNOCKBACK_RESISTANCE = _s(M, "generic.knockback_resistance", "Knockback Resistance", POINTS)
    LUCK = _s(M, "generic.luck", "Luck", FLAT)

    SWIM_SPEED = _s(F, "swim_speed", "Swim Speed", SCALE)
    BLOCK_REACH = _s(F, "block_reach", "Block Reach", FLAT)
    ATTACK_REACH = _s(F, "entity_reach", "Attack Reach", FLAT)
    STEP_HEIGHT = _s(F, "step_height_addition", "Step Height", FLAT)

    STAMINA = _s(P, "stamina", "Stamina", FLAT)
    MAGIC_DAMAGE = _s(P, "magic_damage", "Magic Damage", SCALE)
    MELEE_DAMAGE = _s(P, "melee_damage", "Melee Damage", SCALE)
    RANGED_DAMAGE = _s(P, "ranged_damage", "Ranged Damage", SCALE)
    TAMED_DAMAGE = _s(P, "tamed_damage", "Tamed Animal Damage", SCALE)
    SWORD_DAMAGE = _s(P, "sword_damage", "Sword Damage", SCALE)
    AXE_DAMAGE = _s(P, "axe_damage", "Axe Damage", SCALE)
    TRIDENT_DAMAGE = _s(P, "trident_damage", "Trident Damage", SCALE)
    FORTUNE = _s(P, "fortune", "Fortune", FLAT)
    HEALING = _s(P, "healing", "Healing", SCALE)
    JUMP = _s(P, "jump", "Jump Height", SCALE)
    MAGIC_RESISTANCE = _s(P, "magic_resistance", "Magic Resistance", SCALE)
    MELEE_RESISTANCE = _s(P, "melee_resistance", "Melee Resistance", SCALE)
    RANGED_RESISTANCE = _s(P, "ranged_resistance", "Ranged Resistance", SCALE)
    MINING_SPEED = _s(P, "mining_speed", "Mining Speed", SCALE)
    BREAKING_SPEED = _s(P, "breaking_speed", "Breaking Speed", SCALE)
    PICKAXE_SPEED = _s(P, "pickaxe_speed", "Pickaxe Speed", SCALE)
    AXE_SPEED = _s(P, "axe_speed", "Axe Speed", SCALE)
    SHOVEL_SPEED = _s(P, "shovel_speed", "Shovel Speed", SCALE)
    SPRINTING_SPEED = _s(P, "sprinting_speed", "Sprinting Speed", SCALE)
    MOUNT_SPEED = _s(P, "mount_speed", "Mount Speed", SCALE)
    EATING_SPEED = _s(P, "consuming_speed", "Eating Speed", SCALE)
    KNOCKBACK = _s(P, "knockback", "Knockback", SCALE)
    REPAIR_COST = _s(P, "repair_cost", "Anvil Repair Cost", SCALE)
    MAGIC_RESISTANCE_SHRED = _s(P, "magic_resistance_shred", "Magic Resistance Shred", SCALE)
    TAMED_RESISTANCE = _s(P, "tamed_resistance", "Tamed Animal Resistance", SCALE)
    NATURAL_REGENERATION = _s(P, "natural_regeneration", "Natural Regeneration", SCALE)
    DAMAGE_REFLECTION = _s(P, "damage_reflection", "Damage Reflected", SCALE)
    STEALTH = _s(P, "stealth", "Stealth", FLAT)
    FALL_REDUCTION = _s(P, "fall_reduction", "Fall Distance Ignored", FLAT)
    BOW_PROJECTILE_SPEED = _s(P, "bow_projectile_speed", "Bow Projectile Speed", SCALE)
    CROSSBOW_PROJECTILE_SPEED = _s(P, "crossbow_projectile_speed", "Crossbow Projectile Speed", SCALE)

    DRAW_SPEED = _s(A, "draw_speed", "Draw Speed", POINTS)
    CRIT_CHANCE = _s(A, "crit_chance", "Crit Chance", POINTS)
    CRIT_DAMAGE = _s(A, "crit_damage", "Crit Damage", POINTS)
    COLD_DAMAGE = _s(A, "cold_damage", "Cold Damage", FLAT)
    FIRE_DAMAGE = _s(A, "fire_damage", "Fire Damage", FLAT)
    LIFE_STEAL = _s(A, "life_steal", "Life Steal", POINTS)
    CURRENT_HP_DAMAGE = _s(A, "current_hp_damage", "Damage from Target's Current Health", POINTS)
    OVERHEAL = _s(A, "overheal", "Overheal", POINTS)
    GHOST_HEALTH = _s(A, "ghost_health", "Ghost Health", FLAT)
    ARROW_DAMAGE = _s(A, "arrow_damage", "Arrow Damage", POINTS)
    ARROW_VELOCITY = _s(A, "arrow_velocity", "Arrow Velocity", POINTS)
    EXPERIENCE_GAINED = _s(A, "experience_gained", "Experience Gained", POINTS)
    HEALING_RECEIVED = _s(A, "healing_received", "Healing Received", POINTS)
    ARMOR_PIERCE = _s(A, "armor_pierce", "Armor Pierce", FLAT)
    ARMOR_SHRED = _s(A, "armor_shred", "Armor Shred", POINTS)
    PROTECTION_PIERCE = _s(A, "prot_pierce", "Protection Pierce", FLAT)
    PROTECTION_SHRED = _s(A, "prot_shred", "Protection Shred", POINTS)
    DODGE_CHANCE = _s(A, "dodge_chance", "Dodge Chance", POINTS)

    WATER_SPEED = _s(X, "water_speed", "Water Speed", SCALE)
    LAVA_SPEED = _s(X, "lava_speed", "Lava Speed", SCALE)
    BREATH = _s(X, "lung_capacity", "Breath", SECONDS)
    WATER_VISION = _s(X, "water_visibility", "Underwater Vision", SCALE)
    LAVA_VISION = _s(X, "lava_visibility", "Vision in Lava", SCALE)
    RARE_LOOT_ROLLS = _s(X, "bonus_rare_loot_rolls", "Rare Loot Rolls", FLAT)
    LOOT_COUNT_ROLLS = _s(X, "bonus_loot_count_rolls", "Loot Count Rolls", FLAT)

    USE_ITEM_SPEED = _s(T, "player.use_item_speed", "Item Use Speed", POINTS)
    JUMP_COUNT = _s(T, "player.jump_count", "Mid-air Jumps", FLAT)
    SAFE_FALL_DISTANCE = _s(T, "generic.safe_fall_distance", "Safe Fall Distance", FLAT)
    GOOD_EFFECT_DURATION = _s(T, "generic.good_effect_duration_multiplier", "Good Effect Duration", POINTS)
    BAD_EFFECT_DURATION = _s(T, "generic.bad_effect_duration_multiplier", "Bad Effect Duration", POINTS)

    @property
    def info(self) -> StatInfo:
        info: StatInfo = self.value
        return info
