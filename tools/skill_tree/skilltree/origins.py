"""The non-human origins as keystones: each grants its origin's powers, drawbacks included."""

from .model import Origin, powers

ARACHNID = Origin(
    "Arachnid",
    gain="Climb walls. Melee hits wrap the target in cobweb.",
    cost="Meat only. Three fewer hearts.",
    powers=powers("climbing", "master_of_webs", "carnivore", "fragile", "arthropod"),
    icon="minecraft:cobweb",
)
SHULK = Origin(
    "Shulk",
    gain="Nine extra inventory slots, kept on death. Natural armor. Break stone bare-handed.",
    cost="No shields. Hunger drains faster.",
    powers=powers("shulker_inventory", "natural_armor", "strong_arms", "strong_arms_break_speed", "no_shield", "more_exhaustion"),
    icon="minecraft:shulker_shell",
)
AVIAN = Origin(
    "Avian",
    gain="Fall slowly. Walk faster. Lay an egg each morning.",
    cost="No meat. Can only sleep high up.",
    powers=powers("slow_falling", "fresh_air", "like_air", "tailwind", "lay_eggs", "vegetarian"),
    icon="minecraft:feather",
)
MERLING = Origin(
    "Merling",
    gain="Breathe, see and mine freely underwater. Swim fast.",
    cost="Cannot breathe air.",
    powers=powers(
        "water_breathing", "water_vision", "aqua_affinity", "swim_speed", "like_water", "aquatic",
        "conduit_power_on_land", "air_from_potions",
    ),
    icon="minecraft:cod",
)
FELINE = Origin(
    "Feline",
    gain="No fall damage. Higher sprint jumps. Creepers keep away. See in the dark.",
    cost="One fewer heart. Cannot mine stone that has stone beside it.",
    powers=powers("fall_immunity", "sprint_jump", "velvet_paws", "nine_lives", "weak_arms", "scare_creepers", "cat_vision"),
    icon="minecraft:orange_wool",
)
ELYTRIAN = Origin(
    "Elytrian",
    gain="Built-in wings. Launch into the air. Hit harder while flying.",
    cost="Light armor only. Weak under low ceilings. Crashing hurts more.",
    powers=powers("elytra", "launch_into_air", "aerial_combatant", "light_armor", "claustrophobia", "more_kinetic_damage"),
    icon="minecraft:elytra",
)
PHANTOM = Origin(
    "Phantom",
    gain="Walk through walls. Turn invisible.",
    cost="Burns in daylight. Always hungry. Three fewer hearts.",
    powers=powers(
        "phantomize", "translucent", "phasing", "invisibility", "burn_in_daylight", "hunger_over_time",
        "fragile", "phantomize_overlay",
    ),
    icon="minecraft:phantom_membrane",
)
ENDERIAN = Origin(
    "Enderian",
    gain="Throw ender pearls at will. Longer reach.",
    cost="Water hurts. Afraid of pumpkins.",
    powers=powers("throw_ender_pearl", "water_vulnerability", "pumpkin_hate", "extra_reach", "ender_particles", "damage_from_potions"),
    icon="minecraft:ender_pearl",
)
# nether_spawn is left out: it makes no sense taken mid-game.
BLAZEBORN = Origin(
    "Blazeborn",
    gain="Immune to fire, poison and hunger effects. Hit harder while burning.",
    cost="Water hurts.",
    powers=powers(
        "fire_immunity", "burning_wrath", "hotblooded", "water_vulnerability", "flame_particles",
        "damage_from_snowballs", "damage_from_potions",
    ),
    icon="minecraft:blaze_powder",
)
