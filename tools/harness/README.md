# Feastcraft harness

A real Forge client, running headless in Docker, that an agent can play through an HTTP API: run
commands, look around, click, read chat and boss bars, and take screenshots. It is for checking what
a player *sees*, which GameTests can't: models, beams, boss bars, HUD messages, respawn screens.

The harness is a dev-only mod (`feastcraft_harness`). It is never part of the pack. Every pack mod
gets a `harness` run that loads it alongside that mod, and the mod does nothing unless that run's
`feastcraft.harness.port` system property is set.

## Running it

```sh
scripts/harness up                     # or: up mod=<name under mods/>
scripts/harness call status
scripts/harness call command c='/time set noon'
scripts/harness call screenshot name=noon hud=false
tools/harness/scenarios/expeditions.sh
scripts/harness down
```

`up` starts the client and waits until it is standing in a fresh world. The first run downloads and
decompiles Minecraft into a Docker volume, which takes several minutes; later runs only take as long as
the client needs to start. `logs` follows the client's output.

Each run starts in a fresh world named `harness`:
- It is created from a fixed seed with real terrain and structures.
- The player is in survival, with cheats on.
- The daylight and weather cycles are off.
- `run-template/` is copied into the run's game directory, `mods/<mod>/run-harness/`. Its options
  keep the client from pausing or showing first-launch screens. Its `defaultconfigs/` become each
  new world's server configs; for example, they shorten the exfil countdown to 10 seconds.

Screenshots land in `mods/<mod>/run-harness/screenshots/` on the host.

### How the container runs

The container builds from its own copy of the repo, which is re-synced on every `up`, so the host's
`build/` directories are never touched. Xvfb provides the display, and Mesa's llvmpipe renders
OpenGL in software. Audio is disabled. The API is published only on `127.0.0.1:25580`.

Mojang ships no linux-arm64 LWJGL natives for 1.20.1. On arm64 Linux, such as Docker on Apple
silicon, the `harness` run adds them from Maven Central.

## API

Every endpoint takes its arguments as query parameters and accepts GET or POST. It answers with JSON,
or with `{"error": ...}` and one of these statuses:
- 400: a bad argument.
- 409: the game can't do it right now, for example because there is no world or no screen open.
- 504: the action timed out.

Any endpoint also takes `timeout`, in seconds; the default is 120. Keyed inputs (`use`, `attack`,
`sneak`, `select`) go through the client's own key mappings, so vanilla handles them as it would a
real press.

| Endpoint | Arguments | Does |
| --- | --- | --- |
| `status` | | Reports the open screen (`screen`, and its full class name as `screenClass`) and its buttons, and whether the client is in a world. In a world, it also reports the dimension, position, rotation, game mode, health, the block or entity under the crosshair (with block state), the hotbar (with NBT), and boss bar names and progress. |
| `command` | `c` | Runs a command as the player. Returns the chat and action-bar lines that come back. |
| `use` | `hold` | Clicks the use key and returns the chat and action-bar lines that come back. With `hold`, keeps it down for that many ticks, as eating needs, and returns the status. |
| `attack` | | Clicks the attack key. Returns the chat and action-bar lines that come back. |
| `look` | `yaw`, `pitch` or `x`, `y`, `z` | Turns to that rotation in degrees, or to face that point. Returns the status. |
| `find` | `block`, `radius` | Returns the nearest blocks of that id, at most 8, within `radius` (64 at most) horizontally, as far as the client has them loaded. |
| `sneak` | `down` | Holds (`true`) or releases (`false`) the sneak key. Returns the status. |
| `select` | `slot` | Presses the hotbar key for slot 0–8. Returns the status. |
| `wait` | `ticks` | Returns the status after that many client ticks. |
| `click` | `label` | Presses the button with that label on the open screen, such as `Respawn`. Returns the status. |
| `screenshot` | `name`, `hud` | Saves the next frame as `screenshots/<name>.png`, with the HUD hidden when `hud=false`. Returns the host `path`. |
| `chat` | `since` | Returns the chat and action-bar lines numbered `since` or later, and `next`, the number the next line will get. |
| `quit` | | Shuts the client down cleanly. |

## Scenarios

A scenario is a bash script in `scenarios/` that calls the API through `scenarios/lib.sh`:
- `expect` asserts a jq filter against a response.
- `expect_reply` runs a command and checks its reply.
- `wait_until` polls the status until a condition holds.
- `shot` takes a named screenshot.

A scenario stops at the first failed expectation and prints the response it failed on. Its
screenshots are for whoever runs it to look at.
