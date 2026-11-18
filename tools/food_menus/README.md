# Feastcraft food menus

Fills the food_advancements mod's menus with the pack's foods, as a datapack: the six menus the mod
ships (soups, cooked meats, sweets, golden foods, risky foods) gain every edible that fits, and each
food mod with enough dishes of its own gets a menu of its own, with an advancement.

```sh
python3 tools/food_menus/menus.py                  # writes datapack/ from build/datapack-export/items.tsv
python3 tools/food_menus/menus.py --install <dir>  # also copies it into this Open Loader folder
```

`datapack/` is committed, so a change to the menus shows up as a diff. Open Loader loads it into
every world from `config/openloader/data/feastcraft_food_menus/`. `scripts/modpack build` ships it
in the pack.

## How foods land on a menu

The datapack export (`scripts/export-datapack`) writes `items.tsv`, listing every registered item
with hunger points and its English name. Classification is by English name and item id against
word lists in `menus.py`, first match wins in the order soups, cooked meats, sweets, golden foods,
risky foods. A food that matches nothing still counts toward the milestone ladder; it only misses
the type menus. `every_fish` needs nothing here: the vanilla `fishes` tag already merges the pack's
fish.

The per-mod menus are explicit: `MOD_MENUS` in `menus.py` names each menu's key, title and
description for mods with at least 45 edibles. Their tags and advancements live under
`feastcraft_food_advancements:menus/<key>` and `every_<key>`, alongside the mod's own.

## Things that bite

- The export must be current: it lists what the pack offers, so after adding a food mod, re-run
  `scripts/export-datapack` before regenerating the menus.
- Tag files merge with the mod's (no `replace`), so the six vanilla menus stay on their vanilla
  foods no matter what lands here.
- The menus pay like any other advancement: the skill tree's rule pays their frame. With `goals`
  at 10, each new menu is worth a small node.
