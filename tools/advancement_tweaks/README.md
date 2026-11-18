# Feastcraft advancement tweaks

Overrides advancements that hand out starter items on first spawn, as a datapack. A fresh player
opens the game to a clean inventory; the advancements themselves stay, so the tabs and point rules
keep working.

```sh
# install into an instance's Open Loader folder
cp -r tools/advancement_tweaks/datapack <instance>/config/openloader/data/feastcraft_advancement_tweaks
```

`scripts/modpack build` ships it in the pack. Books with a config toggle are turned off in the
instance's configs instead: Alex's Mobs (`giveBookOnStartup`), Tinkers
(`shouldSpawnWithTinkersBook`), Tide (`giveJournal`), Apotheosis (`Give Book on First Join`).

## What's here

- `data/forestry/advancements/root.json`: Forestry's root auto-grants on the player's first tick
  (a location criterion with an empty predicate) and its rewards loot table `forestry:grant_guide`
  gives the Forester's Manual. Forestry has no config for it, so this file is the same advancement
  without the `rewards` block.
