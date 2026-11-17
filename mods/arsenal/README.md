# Feastcraft Arsenal

Weapon-class damage the skill tree can scale, and newer Minecraft's spear and mace as Tinkers tools.

## Weapon-class damage

Pufferfish's sword, axe and trident damage check `instanceof SwordItem` and friends, which no
Tinkers tool is. This mod gives players one attribute per weapon class instead:
`feastcraft_arsenal:blade_damage`, `axe_damage`, `spear_damage`, `mace_damage`, `bow_damage`,
`crossbow_damage` and `thrown_damage`.

Each attribute is a damage multiplier with a base of 1, so a tree node's `multiply_base` 0.1 is
+10%, and nodes add up rather than compound.

- Melee classes are item tags, `feastcraft_arsenal:weapon_class/<class>`, matched against the
  main-hand item.
- A hit from an entity in `feastcraft_arsenal:thrown_weapons` is thrown damage, such as a thrown
  trident or javelin.
- Any other arrow is crossbow damage when it was shot from a crossbow, and bow damage otherwise.

## Spear

A Tinkers tool from a small blade and two tool handles. It jabs like a weak sword with 1.5 more
blocks of reach, and its Charge trait copies vanilla's spear charge:

- Hold right-click to lower it. After a short delay, anything in reach takes damage from how fast the
  two of you are closing: 0.7 per block a second for wood up to 1.2 for netherite, from 4.6 blocks a
  second.
- The tool's harvest tier picks vanilla's row, so an iron head charges like an iron spear.
- A fresh charge also knocks back and dismounts; it tires into knockback only, then damage only, then
  ends. Each hit costs 1 durability, and a target can't be hit again for half a second.

Players move on their own client, so the server never knows their velocity. `Motion` records how far
each player, or whatever they ride, moved in the last tick.

### Lunge

An upgrade for spears only, up to level 3, from a rabbit's foot and a piston. Every jab, hit or miss,
throws the player forward 0.458 blocks a tick per level, for 4 exhaustion per level and 1 durability.
It doesn't fire while riding, gliding, in water, or below 7 food in survival.

## Heavy core and mace

The heavy core is a rare chest find: a 1.5% chance in the chests listed in
`loot_modifiers/heavy_core.json`, such as ancient cities, strongholds and bastions. Pouring any castable metal over one on a casting table makes a mace
head and uses up the core. Nothing else makes a mace head.

The mace is a mace head on a tool handle, about vanilla's 6 damage at 0.6 speed with an iron head.
Its Smash trait copies vanilla's mace:

- Hitting a target while falling at least 1.5 blocks adds 4 damage a block for the first 3 blocks,
  2 a block for the next 5, and 1 a block after that. Elytra flight and Slow Falling stop it.
- The hit cancels the fall, so the wielder takes no fall damage.
- Living things within 2.5 blocks of the target are thrown back, twice as hard after a fall of more
  than 5 blocks. The wielder's pets and allies are spared.

`art/draw.py` draws the mace and heavy core sprites from pixel maps.
