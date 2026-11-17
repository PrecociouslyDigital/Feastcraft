# Bounty market

Replaces Bountiful's villager-profession bounties with one market. Players hand in things they made
(meals, preserves, Botania runes and flowers, Thaumcraft and Theurgy goods, Hexerei herbs) and are
paid in Ember Charcoal or raw things: seeds, saplings, crops, mob drops, ores. Crafting is what adds
value, so a few made things buy a pile of raw ones.

```sh
uv run python -m bounties                       # writes datapack/ and report.tsv from build/datapack-export
uv run python -m bounties install --instance <instance>
uv run python -m unittest
uvx mypy --strict bounties tests
```

`datapack/` and `report.tsv` are committed, so a change shows up as a diff. Run
`scripts/export-datapack` first. Open Loader loads the datapack from
`config/openloader/data/feastcraft_bounties/`.

## Prices

`prices.toml` sets what raw things are worth, by tag and by id. Everything else is priced from
recipes: the cheapest recipe's ingredients, times `markup`, plus `step_bonus`. A cooked beef is
worth 85 against raw beef's 50, and a three-step meal several times what went into it.

Some recipes would give silly prices, so they're ignored: Theurgy turning sulfur back into ores,
Tinkers' casting (the fluid isn't in the export), and Create's washing (its outputs come by chance).
Utensils that recipes give back, like Pam's bakeware, cost nothing.

Cookbook of Babel replaces many recipes with chains through its `substance` and `portion` items,
which stand for hundreds of things through NBT. Those have no price, so a dish only made that way
is left out of the market. `report.tsv` lists what was left out and why.

## The board

There is one decree, so every bounty board offers the market. The datapack turns off every other
decree and pool by overriding the file with one that requires a mod that doesn't exist.

Ember Charcoal comes in three offers. The common one is the dearest per charcoal, the rare one the
cheapest. Together they're half of reward weight on a new board. Bountiful favours rarer rewards as
a board's reputation grows, so the share rises to about 63%. The first line of `report.tsv` shows it.
