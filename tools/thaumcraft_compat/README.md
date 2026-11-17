# Thaumcraft compatibility

A datapack that fits Thaumcraft 4, running through MFCL, into the rest of the pack. Open Loader
loads it from `config/openloader/data/feastcraft_thaumcraft_compat/`, and `scripts/modpack` puts it
there.

```sh
uv run python -m thaumcompat biomes --instance <instance>   # rewrites the overworld from Terralith's
uv run python -m thaumcompat aspects                         # works out aspects from build/datapack-export
uv run python -m thaumcompat install --instance <instance>  # copies datapack/ into the instance
uv run python -m unittest
uvx mypy --strict thaumcompat tests
```

`datapack/` and `subjects.tsv` are committed, so a change shows up as a diff. `aspects` reads
`scripts/export-datapack`'s output, so run that first.

## What's in it

- Thaumcraft's swords, picks, axes, shovels and hoes join `minecraft:swords` and the rest, and
  `forge:tools/*`.
- Terralith's overworld with the Magical Forest and Tainted Land added. Terralith covers every
  climate with its own biomes, so an added entry would only tie with whichever was already there.
  Instead each Thaumcraft biome gets a climate box in `biomes.py`, and the Terralith entries it
  overlaps are cut around it. Rerun `biomes` when Terralith updates.

## Aspects

Every item and mob outside vanilla and Thaumcraft is a subject. The `feastcraft_thaumcraft_aspects`
mod reads the entries `aspects` writes. A subject takes its aspects from the first of these that
gives any:

1. `aspects.toml`, written by hand. It always wins, so a bad result from a later step is corrected
   there.
2. `tags.toml`, which maps a tag to a vanilla item to copy: modded planks copy oak planks. These
   follow the vanilla value if it changes.
3. Recipes, by Thaumcraft's own rule: three quarters of the ingredients' aspects, divided among the
   results, at most 64 of each, from the cheapest recipe. It runs in rounds from what is known, and
   a recipe only counts once every ingredient has aspects, so a block made from ingots made from the
   block never feeds itself.
4. Answers about raw materials: what no recipe makes, and one item from each loop of recipes
   nothing else feeds.

`subjects.tsv` lists every subject with its aspects and the step that gave them, so it can be read
through. Spawn eggs get no aspects.

### Asking about raw materials

Each raw material is asked about through OpenRouter's Decisions API: one yes/no question per aspect,
with the subject's id, English name, tags and food value as the state. A request holds up to four
subjects, which keeps it under 32,000 bytes. The most probable aspects over even odds are kept, at
most four, at 1 to 4 by probability. That mapping is crude, so a raw material that matters belongs
in `aspects.toml`.

Jev answers first. Where Jev is unsure, mammoth-1 is asked too and its answer wins. mammoth-1 knows
more about the world, but it's paid, so it only gets the tenth of subjects where no aspect is
clearly there.

`aspects` writes the requests it has no answer for to `build/pending`, and prints how to send them:

```sh
# from the repo root, where secretspec.toml is
secretspec run --reason "Thaumcraft aspects" -- tools/thaumcraft_compat/decide.sh \
    tools/thaumcraft_compat/decisions tools/thaumcraft_compat/build/pending
```

`decide.sh` comes from Cookbook of Babel, and resumes if stopped. Answers are kept in `decisions/`,
named by the hash of the request, and committed, so a rerun only asks about new or reworded
subjects. mammoth-1's requests only exist once Jev has answered, so it takes two rounds. Once
Jev has nothing pending, answers nothing refers to any more are deleted.

### The block budget

A block with aspects takes one of MFCL's legacy block ids. There are 4,095, and MFCL also hands
them out whenever Thaumcraft looks at a modded block in the world, and fails when they run out.
So only `BLOCK_BUDGET` block items in `budget.py` get aspects. Raw-material blocks come first, then
crafted blocks by how many recipes use them, then blocks that copy a tag's parent, and stairs,
slabs, walls and the like last. A block item outside the budget gets no aspects as an item either.
If the log says "No free read-only host identity", the budget is too high.
