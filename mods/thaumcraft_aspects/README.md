# Feastcraft Thaumcraft Aspects

Gives other mods' items, blocks and mobs Thaumcraft 4 aspects, from datapacks, through
[TC Modern Lib](https://www.curseforge.com/minecraft/mc-mods/tc-modern-lib). Without TC Modern Lib it
loads the entries and does nothing with them.

## Entries

Each file at `data/<namespace>/feastcraft_thaumcraft_aspects/<name>.json` is a list of entries:

```json
[
  {"item": "biomesoplenty:fir_planks", "parent": "minecraft:oak_planks"},
  {"block": "create:zinc_ore", "aspects": {"terra": 1, "metallum": 2}},
  {"entity": "alexsmobs:grizzly_bear", "aspects": {"bestia": 3, "corpus": 1}}
]
```

- An entry names one `item`, `block` or `entity`.
- It either copies a `parent` of the same kind, which follows the parent if the parent's aspects
  change, or lists its `aspects` by Thaumcraft's lowercase names.
- An entry naming an id the game doesn't have, as subject or parent, is skipped with a log line. A
  datapack written for a mod that has since been removed still loads.

Every block with aspects takes one of MFCL's 4,095 legacy block ids, so a pack should give aspects
to a budget of blocks rather than all of them. `tools/thaumcraft_compat` keeps that budget.

TC Modern Lib never forgets an entry, so removing one takes a restart rather than `/reload`.
