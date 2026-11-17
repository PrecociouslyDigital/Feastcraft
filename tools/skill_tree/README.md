# Feastcraft skill tree

Generates the pack's [Pufferfish's Skills](https://modrinth.com/mod/skills) tree as a datapack:
about 700 nodes, every one costing a point, with keystones that change a rule and pay for it with
something no other node can buy back. Points come from advancements, through `advancement_points`.

```sh
uv run python -m skilltree                       # writes datapack/ and build/preview.svg
uv run python -m skilltree --install <instance>  # also copies it into the instance's Open Loader folder
uv run python -m unittest
uvx mypy --strict skilltree tests
```

`datapack/` is committed, so a change to the tree shows up as a diff. A test fails if it's out of date.
Open Loader loads it into every world from `config/openloader/data/feastcraft_skill_tree/`.

## Where things are

- `tree.py` lists the regions clockwise from the top, the spur out along each border, and the few
  roads that join outer clusters across a border. A far ring lies past every region's tips, and
  four of the spurs run on out to it.
- `regions/` has one file per region. Each cluster names its shape, its notables, the stats its small
  nodes hand out in turn, and where it hangs: a radius, an angle off the region's centre line, and a
  ring road node or an earlier cluster.
- `powers/` has the keystones that need custom Origins powers, each next to the text that describes it.
- `origins.py` has the nine origins. Taking one locks the other eight.
- `stats.py` is the catalogue of attributes. A node gives `Bonus(Stat.BLADE_DAMAGE, 4)` and the
  catalogue decides the modifier and the "+4% Blade Damage" tooltip.

## Things that bite

- Pufferfish stores what a player owns by skill id, so ids come from names: `blade.duelist.3`,
  `keystone.acrobat`. Renaming a cluster or notable takes it away from players who had it.
- Pufferfish reruns a command reward's unlock command on every server start and reload, and Curios
  can only be told a slot's whole count. So a slot notable tags its owner and calls a function that
  sets the count from the tags held, which is what lets the three ring notables stack. The counts
  assume the pack's current slots: two rings, one charm and one head. The Mana Tablet slot is the
  datapack's own: nobody has one until Mana Well sets its count.
- Wellspring's mana trickle is a reward type from `mods/skill_rewards`, since no command or power
  touches Botania mana.
- Clear Mind and Initiate's Notes run Thaumcraft's `/thaumcraft` command, and Thaumcraft says so in
  chat every time. That's why they run every 10 and 30 minutes, not more often.
- Origins doesn't stack a power granted twice, so each resistance node gets its own power named
  after the node.
- The datapack replaces Origins' origin layer with one that offers only Human and picks it.
