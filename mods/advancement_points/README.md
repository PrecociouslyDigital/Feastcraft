# Feastcraft Advancement Points

Pays [Pufferfish's Skills](https://modrinth.com/mod/skills) points for advancements. Data pack
rules decide which advancements pay, how much, and into which skill tree. The mod ships no rules
and no skill tree.

Requires Pufferfish's Skills 0.19 or later.

## Rules

Each rule is a JSON file at `data/<namespace>/feastcraft_advancement_points/rules/<name>.json`:

```json
{
  "category": "mypack:gourmet",
  "advancements": ["feastcraft_food_advancements:*"],
  "points": {"task": 1, "goal": 2, "challenge": 3}
}
```

- `category` is a Pufferfish's Skills category id: the namespace of the data pack that defines the
  tree, and the category's name in that pack's `puffish_skills/config.json`.
- `advancements` lists advancement ids as `namespace:path`. `*` stands for any run of characters:
  `mymod:*` is all of a mod's advancements, and `minecraft:story/*` is one tab of vanilla's.
- `points` is what each matching advancement pays: a number for every frame, or an object by frame
  (`task`, `goal`, `challenge`) where a missing frame pays nothing.

## How points are counted

A player's points from advancements always equal what their completed advancements are worth under
the current rules. The mod recomputes them whenever an advancement is granted or revoked, at login,
on respawn, and on `/reload`, rather than adding them up over time. So:

- Revoking an advancement takes its points back.
- Changing or adding a rule applies to advancements already earned.
- An advancement that matches several rules pays for each of them, so rules from different packs
  add up. To change a rule another pack ships, override its file.
- Advancements without a display, such as recipe unlocks, never pay.

The points go to their own source, `feastcraft_advancement_points:advancements`, so they never mix
with points from Pufferfish's experience or its `points` commands. A rule naming a category no tree
defines is logged once per reload. A rule file that fails to parse is logged and skipped.
