# Feastcraft Smithy

Tinkers as the only smith. The smithy datapack (`tools/smithy`) holds the ledger
of what each plain tool, weapon and armor piece becomes; this mod does the
converting, the hiding, the new Tinkers materials, and the ancient wills.

## Conversion

`Conversions` is a reload listener over the datapack's
`smithy/conversions/<ns>/<item>.json`. `Smithy.convert(ItemStack)` builds the
tool with `ToolBuildHandler.buildItemFromMaterials`, adds the modifiers, and
keeps the damage fraction and a custom name. It is the only conversion
function, and every entry point funnels through it:

- A global loot modifier over every loot table, which covers chests, fishing
  and archaeology.
- `EntityJoinLevelEvent` for `ItemEntity`, which covers mob drops, block drops
  and dispensers.
- `EntityJoinLevelEvent` for mob equipment, both hands and armor. Ranged
  weapons stay vanilla on mobs, because skeleton and pillager AI checks
  `Items.BOW` and `Items.CROSSBOW` exactly. Their drops are converted by the
  item entity hook instead.
- A server player inventory sweep every 20 ticks, which covers trades, items
  already in inventories and other mods' rewards.
- `VillagerTradesEvent` and `WandererTradesEvent`, wrapping each listing so the
  trade screen shows the Tinkers item.

## Hiding

Everything in `#feastcraft_smithy:disabled` is hidden: a JEI plugin removes it
at runtime, an EMI plugin removes the stacks, and
`BuildCreativeModeTabContentsEvent` keeps it out of the creative menu and its
search.

## New Tinkers materials

Thaumium, Void Metal, Infused Iron, Skies, Depths, Necromium, Resonarium and
Warden carapace, in `tinkering/materials/`, for the metals that would otherwise
be left without tools. Each definition is `craftable: true`, so the part
builder takes the ingot and nothing needs a molten fluid. The traits echo the
originals from Tinkers' own modifiers: Void Metal repairs itself (Preserved),
Necromium leeches (Lifesteal), Resonarium swings on its vibration (Kinetic),
Warden carapace ignores knockback. Warp isn't recreated.

Part sprites come from `/tconstruct generate part_textures all` in the harness
— see `breadcrumbs/mods/smithy/`.

## Ancient wills

Botania's Gaia wills check for a Botania Terrasteel helmet, but the Terrestrial
set is Tinkers plate armor with `tcintegrations:terrestrial`. Six modifiers
(`feastcraft_smithy:will_ahrim` … `will_verac`) are applied in the tinker
station with that will's item, only to `plate_helmet`, and only with
Terrestrial already on the tool. A mixin on Botania's `TerrasteelHelmItem`
accepts a full Terrestrial Tinkers set as the terra armor set, accepts a
Tinkers helmet wearing `will_<type>` where Botania checks its own helm, and
leaves Botania's will effects to run unchanged.

## Tests

`ConversionProperties` (jqwik) checks the ledger the mod will read: every
disabled item is a conversion source, converting twice equals converting once
(a Tinkers tool is never itself a conversion source), and converting a
non-disabled stack returns it unchanged. The runtime behaviors — the tag, the
tool build, the hooks — are harness scenarios.
