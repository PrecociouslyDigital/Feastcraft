# Feastcraft Skill Rewards

[Pufferfish's Skills](https://modrinth.com/mod/skills) reward types for the skill tree in
`tools/skill_tree`, for things no command or Origins power can do.

## `feastcraft_skill_rewards:mana_trickle`

Each second, sends mana into the Botania mana items a player carries, in their inventory or Curios
slots. Each unlocked skill with the reward adds its `mana_per_second`.

```json
{"type": "feastcraft_skill_rewards:mana_trickle", "data": {"mana_per_second": 20}}
```
