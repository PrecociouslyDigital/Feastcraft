# Feastcraft datapack export

Writes out the data a fresh world in the real pack would load, merged across every mod, Open Loader
datapack and generated pack, so there is one place to grep for a recipe, tag, loot table or
advancement and see what the game ends up with.

```sh
scripts/export-datapack                        # Feastcraft3 into build/datapack-export
scripts/export-datapack instance=Feastcraft2 out=/tmp/fc2
```

The script builds a dev-only mod (`feastcraft_datapack_export`), drops it into the ATLauncher
instance, launches the instance, and removes the mod again when the game exits. When the title
screen opens, the mod loads data the way the Create World screen does, writes it out, and quits. A
run takes about as long as the pack takes to start. The game window does open.

## What comes out

- `data/<namespace>/...` holds each file as the game resolves it. Where several packs have the same
  file the highest one wins, as in game. Tags and Forge's `global_loot_modifiers.json` are merged
  instead, following `replace` and `remove`.
- `sources.tsv` has a line per file naming the packs that supply it, lowest first:
  `origins:origin_layers/origin.json	origins-forge-1.20.1-1.10.0.9-all.jar < data/feastcraft_skill_tree`.
- `packs.txt` lists the packs, lowest first.
- `items.tsv` lists every registered item, with hunger points and saturation modifier for foods, so
  a recipe audit can tell which results are food and which ingredients don't exist, and its English
  name.
- `blocks.tsv` lists every registered block and the item that places it.
- `entities.tsv` lists every entity type, its spawn category, `living` if it has attributes, which
  picks out the mobs, and its English name.
- `aspects.tsv` lists the Thaumcraft aspects TC Modern Lib gives items, blocks and mobs:
  `item	minecraft:iron_ingot	metallum:4`. Aspects that datapacks add through it are left out.
- `recipes.tsv` lists the recipes the game is left with once it has loaded those files: id,
  serializer, result item and count, then the ingredients. Each ingredient lists the items that fit
  it with `|` between them, tags already expanded, and ingredients are space separated.
  Conditions have been applied, and so has anything a mod does to the loaded recipes, such as
  Cookbook of Babel withdrawing other mods' recipes for food it makes. A recipe file in `data/` can
  therefore have no line here.

## Things that bite

- The mod finds directories to export by walking mods, folder packs and zip packs, so a
  directory that only a runtime-generated pack uses is missed.
- The script can run while the ATLauncher window is open, but the
  instance itself must not be running.
