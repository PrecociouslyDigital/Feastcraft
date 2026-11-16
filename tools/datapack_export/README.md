# Feastcraft datapack export

Writes out the data a fresh world in the real pack would load, merged across every mod, Open Loader
datapack and generated pack, so there is one place to grep for a recipe, tag, loot table or
advancement and see what the game ends up with.

```sh
scripts/export-datapack                        # Feastcraft3 into build/datapack-export
scripts/export-datapack instance=Feastcraft2 out=/tmp/fc2
```

The script builds a dev-only mod (`feastcraft_datapack_export`), drops it into the ATLauncher
instance, launches the instance, and removes the mod again when the game exits. The mod asks the
game for its data packs the way the Create World screen does, writes them out when the title screen
opens, and quits. A run takes about as long as the pack takes to start. The game window does open.

## What comes out

- `data/<namespace>/...` holds each file as the game resolves it. Where several packs have the same
  file the highest one wins, as in game. Tags and Forge's `global_loot_modifiers.json` are merged
  instead, following `replace` and `remove`.
- `sources.tsv` has a line per file naming the packs that supply it, lowest first:
  `origins:origin_layers/origin.json	origins-forge-1.20.1-1.10.0.9-all.jar < data/feastcraft_skill_tree`.
- `packs.txt` lists the packs, lowest first.

The export is the files, not what the game makes of them. A recipe behind an unmet
`forge:conditional` is still there, and a recipe a KubeJS script removes at load is too.

## Things that bite

- The mod finds directories to export by walking mods, folder packs and zip packs, so a
  directory that only a runtime-generated pack uses is missed.
- The script can run while the ATLauncher window is open, but the
  instance itself must not be running.
