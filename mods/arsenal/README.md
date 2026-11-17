# Feastcraft Arsenal

Weapon-class damage the skill tree can scale, and (later in this mod) the spear and mace as Tinkers
tools.

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
