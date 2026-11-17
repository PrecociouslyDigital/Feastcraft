# Thaumcraft compatibility

A datapack that fits Thaumcraft 4, running through MFCL, into the rest of the pack. Open Loader
loads it from `config/openloader/data/feastcraft_thaumcraft_compat/`, and `scripts/modpack` puts it
there.

```sh
uv run python -m thaumcompat biomes --instance <instance>   # rewrites the overworld from Terralith's
uv run python -m thaumcompat install --instance <instance>  # copies datapack/ into the instance
uv run python -m unittest
uvx mypy --strict thaumcompat tests
```

`datapack/` is committed, so a change shows up as a diff.

## What's in it

- Thaumcraft's swords, picks, axes, shovels and hoes join `minecraft:swords` and the rest, and
  `forge:tools/*`.
- Terralith's overworld with the Magical Forest and Tainted Land added. Terralith covers every
  climate with its own biomes, so an added entry would only tie with whichever was already there.
  Instead each Thaumcraft biome gets a climate box in `biomes.py`, and the Terralith entries it
  overlaps are cut around it. Rerun `biomes` when Terralith updates.
