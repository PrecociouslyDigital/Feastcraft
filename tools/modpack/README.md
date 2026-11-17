# Feastcraft modpack

Packs the ATLauncher instance and this repo's mods into a Modrinth pack (`.mrpack`), and turns a
pack into a dedicated server directory without the client-only mods.

```sh
scripts/modpack build                   # Feastcraft3 into build/modpack/Feastcraft-<version>.mrpack
scripts/modpack server                  # the newest built pack into build/modpack/server
scripts/modpack server pack=some.mrpack out=/srv/feastcraft
```

The pack's name and version are in `pack.toml`.

## What goes into the pack

- Every jar and zip in the instance's `mods/`, `resourcepacks/` and `shaderpacks/`. Each is looked
  up on Modrinth by its hash. The pack links to the ones Modrinth has and carries the rest itself,
  such as Twilight Forest and Cookbook of Babel. `build` lists the ones it carries.
- The instance's `config/` and the 1.7.10 Thaumcraft jars in `mods/1.7.10/`. `pack.toml` lists
  these folders and the files in them to leave out.
- The mods under `mods/` in this repo, built fresh. A `feastcraft_*` jar in the instance is ignored.
- The repo's datapacks, in Open Loader's folder: the skill tree from `tools/skill_tree/datapack`,
  the Thaumcraft compatibility pack from `tools/thaumcraft_compat/datapack`, the bounty
  market from `tools/bounties/datapack`, the recipe changes from `tools/recipes/datapack`,
  and the smithy from `tools/smithy/datapack`.

`options.txt` is left out, so the pack doesn't replace a player's key bindings and video settings.

## Client-only mods

Resource packs, shader packs and the mods Modrinth lists as unsupported on servers are marked
client-only in the pack. Modrinth's listing is sometimes wrong, and it says nothing about the jars
the pack carries itself, so `pack.toml` corrects it by jar name:

- `client_only` adds mods, such as Controlling.
- `server_too` keeps mods on the server, such as Radiant Gear, which Curious Lanterns requires.

A mod wrongly left off the server shows up at startup as a missing dependency.

## The server

`server` works from the pack alone, so it also installs a pack built elsewhere. It copies each mod
from the instance when the instance has the same file and downloads it otherwise, then installs
Forge. Running it again on the same directory updates the server: `mods/` is replaced, the pack's
configs overwrite the server's, and the world is left alone.

It needs `java` on the path. The server wants about 8 GB (`-Xmx8G` in `user_jvm_args.txt`) and takes
about four minutes to create a world.
