# Recipes

Feastcraft's own recipe changes, as a datapack. Most of it is generated from other mods' recipes so
that a player has more than one way to make things:

- Each Create filling recipe is also a Tinkers casting. The item being filled is the cast, used up,
  and blocks go in the basin. A bowl takes 125 mB of apple jello to make apple jello on the table,
  as it does under a spout.
- Each Create pressing recipe is also a Farmer's Delight cutting board recipe that takes a hammer
  from `forge:tools/hammers`: an iron ingot hammered on the board is an iron sheet.

```sh
uv run python -m recipes                       # writes datapack/ from build/datapack-export
uv run python -m recipes install --instance <instance>
uv run python -m unittest
uvx mypy --strict recipes tests
```

`datapack/` is committed, so a change shows up as a diff. Run `scripts/export-datapack` first.
Open Loader loads the datapack from `config/openloader/data/feastcraft_recipes/`.

## Fillings Tinkers can't copy

Most of Cookbook of Babel's fillings pour its bulk fluid, which says what it is in NBT. Tinkers'
fluid ingredients can't match NBT, so those stay spout-only, as do Create's potion fillings.

Buckets are left to Tinkers, which already fills any bucket with any fluid. Where several mods add
the same filling, or Tinkers already casts that fluid onto that item, only the first is kept.
