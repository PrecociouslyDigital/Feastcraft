# Smithy

Tinkers as the pack's tool and armor system: the plain tools and armor from other
mods are retired, and everything that named them now names the Tinkers tool that
stands in for it.

```sh
uv run python -m smithy                       # writes datapack/ from build/datapack-export
uv run python -m smithy draft                 # emits a smithy.toml draft from the export
uv run python -m smithy install --instance <instance>
uv run python -m unittest
uvx mypy --strict smithy tests
```

`datapack/` is committed, so a change shows up as a diff. Run `scripts/export-datapack`
first. Open Loader loads the datapack from `config/openloader/data/feastcraft_smithy/`.

## smithy.toml

The single source of truth. Every item the export tags as a tool, weapon or armor
that isn't from a Tinkers mod must appear in it, either as a conversion or as a
keep — the generator fails when it finds an unlisted item, so a mod added later
can't slip a plain sword in unnoticed.

```toml
[convert."minecraft:diamond_sword"]
tool = "tconstruct:sword"
materials = ["tconstruct:iron", "tconstruct:wood", "tconstruct:wood"]
modifiers = ["tconstruct:diamond"]

[keep]
"botania:terra_pick" = "the Terra Shatterer, ranked by mana"
```

The tier mapping is mechanical (`smithy/tiers.py`): wood is `wood`, stone is
`rock`, and a metal maps to the Tinkers material of the same name. Diamond and
netherite become iron plus the `tconstruct:diamond` or `tconstruct:netherite`
modifier. Sword to `sword`, pickaxe to `pickaxe`, axe to `hand_axe`, shovel to
`pickadze` (Tinkers has no plain shovel), hoe to `kama`, knife to `dagger`, bow
to `longbow`, crossbow to `crossbow`, shield to `plate_shield`. Metal armor is
`plate_*` with the metal as plating and chain as maille; leather or chain armor
is `travelers_*`.

`draft` emits a `smithy.toml` from those rules. A human should read it before
committing: the keeps are curation, not generation.

## What the datapack does

- `data/feastcraft_smithy/tags/items/disabled.json` lists every converted item.
  The mod hides and converts it; the tag is the handoff between the two.
- `data/feastcraft_smithy/smithy/conversions/<ns>/<item>.json` holds one
  conversion each, read by the mod.
- A `forge:false` override for every live recipe whose result is disabled,
  including smithing upgrades, Forestry kits and Twilight Forest's fiery tools.
- A rewrite of every live recipe that consumes a disabled item, with
  `{"item": X}` replaced by the conversion's Tinkers item. The dispenser's bow
  takes a longbow, the cooking pot takes a pickadze. Tinkers melting recipes for
  disabled items are left alone: they are how a retired item becomes ingots.
- The same id rewrite for advancement criteria, so "Isn't It Iron Pick", "Suit
  Up" and "Serious Dedication" stay obtainable — they pay skill points.

## Checks

`tests/test_smithy.py` checks the ledger and the output against the export:

- Every tool, armor or weapon tag member is either kept or converted.
- No recipe left in the output both is live and produces or consumes a
  disabled item.
- Every conversion names a tool definition and materials that exist — the new
  materials count, which is why they live in `mods/smithy`.

Thaumcraft's MFCL recipes have hash ids (`thaumcraft:legacy_crafting/…`). If an
export shows the overrides not taking effect on them, the runtime conversion
still turns the crafted item into Tinkers, and we leave it at that.
