"""Tier mapping rules, from the source item to the Tinkers tool it becomes.

Metals map to the Tinkers material of the same name; diamond and netherite
become iron plus a modifier. New Tinkers materials live in mods/smithy.
"""

from __future__ import annotations

from typing import Any

from . import export

Json = dict[str, Any]

SMITHY = "feastcraft_smithy"

# The head material a source tier is made of. A key is the source item's id
# with the tool or armor word removed, so `minecraft:iron_` matches
# iron_sword and iron_helmet alike.
TIER_MATERIALS: dict[str, str] = {
    "wooden_": "tconstruct:wood",
    "leather_": "tconstruct:leather",
    "chainmail_": "tconstruct:chain",
    "stone_": "tconstruct:rock",
    "flint_": "tconstruct:flint",
    "copper_": "tconstruct:copper",
    "waxed_exposed_copper_": "tconstruct:copper",
    "waxed_weathered_copper_": "tconstruct:copper",
    "waxed_oxidized_copper_": "tconstruct:copper",
    "waxed_copper_": "tconstruct:copper",
    "exposed_copper_": "tconstruct:copper",
    "weathered_copper_": "tconstruct:copper",
    "oxidized_copper_": "tconstruct:copper",
    "iron_": "tconstruct:iron",
    "golden_": "tconstruct:gold",
    "gold_": "tconstruct:gold",
    "diamond_": "tconstruct:iron",
    "netherite_": "tconstruct:iron",
    "gilded_netherite_": "tconstruct:iron",
    "silver_": "tconstruct:silver",
    "armor_copper_metal_": "tconstruct:copper",
    "armor_silver_metal_": "tconstruct:silver",
    "armor_bronze_": "tconstruct:bronze",
    "dragonbone_": "tconstruct:blazing_bone",
    "dragonsteel_fire_": "tcintegrations:dragonsteel_fire",
    "dragonsteel_ice_": "tcintegrations:dragonsteel_ice",
    "dragonsteel_lightning_": "tcintegrations:dragonsteel_lightning",
    "myrmex_desert_": "tconstruct:iron",
    "myrmex_jungle_": "tconstruct:iron",
    "neptunium_": "tcintegrations:neptunium",
    "ironwood_": "tconstruct:ironwood",
    "steeleaf_": "tconstruct:steeleaf",
    "knightmetal_": "tconstruct:knightmetal",
    "fiery_": "tconstruct:fiery",
    "manasteel_": "tcintegrations:manasteel",
    "manaweave_": "tconstruct:leather",
    "elementium_": "tcintegrations:manasteel",
    "terrasteel_": "tcintegrations:manasteel",
    "infused_iron_": f"{SMITHY}:infused_iron",
    "depth_": f"{SMITHY}:depths",
    "sky_": f"{SMITHY}:skies",
    "resonarium_": f"{SMITHY}:resonarium",
    "warden_": f"{SMITHY}:warden_carapace",
    "necromium_": f"{SMITHY}:necromium",
    "bronze_": "tconstruct:bronze",
    "certus_quartz_": "tconstruct:quartz",
    "nether_quartz_": "tconstruct:quartz",
    "rose_quartz_": "tconstruct:quartz",
    "fluix_": "tconstruct:amethyst",
    "rose_gold_": "tconstruct:rose_gold",
    "brass_": "tcintegrations:brass",
    "zinc_": "tconstruct:lead",
    "experience_": "tconstruct:gold",
    "bow": "tconstruct:wood",
    "crossbow": "tconstruct:wood",
    "shield": "tconstruct:wood",
}

# A modifier a source tier is upgraded with, after its head material.
TIER_MODIFIERS: dict[str, tuple[str, ...]] = {
    "diamond_": ("tconstruct:diamond",),
    "netherite_": ("tconstruct:netherite",),
    "gilded_netherite_": ("tconstruct:netherite",),
    "elementium_": ("tcintegrations:elemental",),
    "terrasteel_": ("tcintegrations:terrestrial",),
}

# The material of a tool's handle, binding, grip and maille parts.
HANDLE = "tconstruct:wood"
MAILLE = "tconstruct:chain"
BOWSTRING = "tconstruct:string"

# The Tinkers tool a source tool kind becomes.
TOOL_TARGETS: dict[str, str] = {
    "swords": "tconstruct:sword",
    "pickaxes": "tconstruct:pickaxe",
    "axes": "tconstruct:hand_axe",
    "hatchets": "tconstruct:hand_axe",
    "shovels": "tconstruct:pickadze",
    "hoes": "tconstruct:kama",
    "knives": "tconstruct:dagger",
    "bows": "tconstruct:longbow",
    "crossbows": "tconstruct:crossbow",
    "shields": "tconstruct:plate_shield",
}

ARMOR_TARGETS: dict[str, str] = {
    "helmets": "tconstruct:plate_helmet",
    "chestplates": "tconstruct:plate_chestplate",
    "leggings": "tconstruct:plate_leggings",
    "boots": "tconstruct:plate_boots",
}

TRAVELERS_TARGETS: dict[str, str] = {
    "helmets": "tconstruct:travelers_helmet",
    "chestplates": "tconstruct:travelers_chestplate",
    "leggings": "tconstruct:travelers_leggings",
    "boots": "tconstruct:travelers_boots",
}

# Armor that is cloth or chain wears the travelers set. Everything metal is plated.
CLOTH_ARMOR = frozenset({
    "minecraft:leather_",
    "minecraft:chainmail_",
    "botania:manaweave_",
})

# Tool and armor words, longest first, so dragonsteel_fire_ wins over dragonsteel_.
WORDS = (
    "waxed_exposed_copper_", "waxed_weathered_copper_", "waxed_oxidized_copper_",
    "waxed_copper_", "exposed_copper_", "weathered_copper_", "oxidized_copper_",
    "armor_copper_metal_", "armor_silver_metal_", "armor_bronze_", "armor_",
    "certus_quartz_", "nether_quartz_", "rose_quartz_", "gilded_netherite_",
    "dragonsteel_fire_", "dragonsteel_ice_", "dragonsteel_lightning_", "dragonsteel_",
    "myrmex_desert_", "myrmex_jungle_", "infused_iron_", "resonarium_", "necromium_",
    "dragonbone_", "chainmail_", "manaweave_", "terrasteel_", "elementium_",
    "manasteel_", "knightmetal_", "ironwood_", "steeleaf_", "neptunium_",
    "rose_gold_", "golden_", "gold_", "netherite_", "diamond_", "bronze_", "brass_",
    "zinc_", "experience_", "fluix_", "fiery_", "warden_", "flint_", "copper_",
    "silver_", "stone_", "wooden_", "leather_", "iron_", "depth_", "sky_", "wood_",
    "bow", "crossbow", "shield",
)

HEAD_PARTS = ("head", "blade", "plating", "limb")


def _prefixes(item: str) -> list[str]:
    """The item id with one tool or armor word removed, longest first."""
    namespace, _, name = item.partition(":")
    prefixes = [f"{namespace}:{word}" for word in WORDS if name.startswith(word)]
    return sorted({*prefixes, f"{namespace}:"}, key=len, reverse=True)


# Thaumcraft names its tools item_<tool>_<material>.
SUFFIX_MATERIALS: dict[str, str] = {
    "thaumium": f"{SMITHY}:thaumium",
    "void": f"{SMITHY}:void_metal",
}


def source_material(item: str) -> str | None:
    """The Tinkers material that stands in for the source tier, if there is one."""
    name = item.partition(":")[2]
    for word in [p.partition(":")[2] for p in _prefixes(item)]:
        if word in TIER_MATERIALS and name.startswith(word):
            return TIER_MATERIALS[word]
    for suffix, material in SUFFIX_MATERIALS.items():
        if name.endswith(suffix):
            return material
    return None


def _modifier(item: str) -> tuple[str, ...]:
    name = item.partition(":")[2]
    for word in [p.partition(":")[2] for p in _prefixes(item)]:
        if word in TIER_MODIFIERS and name.startswith(word):
            return TIER_MODIFIERS[word]
    return ()


# Most specific tool kind first, so a crossbow is not read as a bow.
TOOL_KIND_ORDER = (
    "crossbows", "bows", "shields", "swords", "pickaxes", "axes", "hatchets",
    "shovels", "hoes", "knives", "weapons",
)


def _kind_tags(item: str, tags: set[str]) -> list[str]:
    """The tool or armor words in the item's tags, armor before tools."""
    kinds: list[str] = []
    for tag in sorted(tags):
        kind = tag.split(":")[-1].split("/")[-1]
        if tag in export.ARMOR_TAGS:
            kinds.append(kind)
    tools = set()
    for tag in tags:
        kind = tag.split(":")[-1].split("/")[-1]
        if tag in export.TOOL_TAGS:
            tools.add(kind)
        if tag in export.SHIELD_TAGS:
            tools.add("shields")
    kinds += [kind for kind in TOOL_KIND_ORDER if kind in tools]
    return kinds


def _parts(tool: str, tool_definitions: dict[str, Any]) -> list[tuple[str, bool]]:
    """The part ids a Tinkers tool is built from, in recipe order, with whether each is head.

    Armor and shields built from `material_stats` take one material for the
    whole piece, which their part list spells as the tool itself.
    """
    definition = tool_definitions[tool]
    for module in definition["modules"]:
        if module["type"] == "tconstruct:part_stats":
            return [(part if isinstance(part, str) else part["item"], False) for part in module["parts"]]
        if module["type"] == "tconstruct:material_stats":
            return [(tool, True)]
    raise LookupError(f"no parts in {tool}")


def _material_of_part(part: str, head: str, whole: bool) -> str:
    """The material of one part: the head material for the piece itself and
    head parts, chain for maille, string for a bowstring, wood for the rest."""
    if whole:
        return head
    if "maille" in part:
        return MAILLE
    if "bowstring" in part:
        return BOWSTRING
    if any(word in part for word in HEAD_PARTS):
        return head
    return HANDLE


def _materials(tool: str, head: str, tool_definitions: dict[str, Any]) -> list[str]:
    return [_material_of_part(part, head, whole) for part, whole in _parts(tool, tool_definitions)]


def conversion(item: str, tags: set[str], tool_definitions: dict[str, Any]) -> Json | None:
    """The conversion of one source item, or None if its tier has no material."""
    head = source_material(item)
    if head is None:
        return None
    for kind in _kind_tags(item, tags):
        if kind in ("helmets", "chestplates", "leggings", "boots"):
            cloth = any(prefix in CLOTH_ARMOR for prefix in _prefixes(item))
            tool = (TRAVELERS_TARGETS if cloth else ARMOR_TARGETS)[kind]
        elif kind in TOOL_TARGETS:
            tool = TOOL_TARGETS[kind]
        else:
            continue
        materials = _materials(tool, head, tool_definitions)
        return {"tool": tool, "materials": materials, "modifiers": list(_modifier(item))}
    return None
