#!/usr/bin/env bash
# The smithy in play: a zombie's plain gear arrives as Tinkers tools, dungeon loot holds no
# vanilla tools, a given diamond pickaxe becomes a Tinkers pickaxe with the Diamond modifier,
# and an ancient will works on a full Terrestrial plate set.
#
#   scripts/harness up mod=smithy && tools/harness/scenarios/smithy.sh
source "$(dirname "$0")/lib.sh"

# A sky platform keeps terrain, mobs and daylight out of the numbers.
y=151
step "Platform"
for c in "/gamerule doMobSpawning false" "/gamerule doDaylightCycle false" "/time set noon" \
    "/difficulty easy" "/gamerule keepInventory true" "/fill 0 $((y - 1)) 0 40 $((y - 1)) 40 minecraft:stone" "/fill 0 $y 0 40 $((y + 15)) 40 minecraft:air"; do
    run "$c" >/dev/null
done
run "/tp @s 20.5 $y 18.5 0 0" >/dev/null

zombie_gear='{NoAI:1b,PersistenceRequired:1b,Health:100f,HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],ArmorItems:[{id:"minecraft:iron_boots",Count:1b},{id:"minecraft:iron_leggings",Count:1b},{id:"minecraft:iron_chestplate",Count:1b},{id:"minecraft:stone_button",Count:1b}]}'
skeleton_gear='{NoAI:1b,PersistenceRequired:1b,Health:100f,HandItems:[{id:"minecraft:bow",Count:1b},{}]}'

step "Given a diamond pickaxe"
run "/clear @s" >/dev/null
run "/give @s minecraft:diamond_pickaxe" >/dev/null
call wait ticks=30 >/dev/null
give=$(run "/data get entity @s Inventory")
expect "the pickaxe is a Tinkers pickaxe" '.lines[-1].text | contains("tconstruct:pickaxe")' "$give"
expect "it left vanilla behind" '(.lines[-1].text | contains("diamond_pickaxe")) | not' "$give"
expect "it carries the Diamond modifier" '.lines[-1].text | contains("tconstruct:diamond")' "$give"
shot smithy_given_pickaxe

step "A zombie's plain gear"
run "/kill @e[type=!player]" >/dev/null
run "/summon zombie 0 $y 0 $zombie_gear" >/dev/null
    run "/data merge entity @e[type=zombie,limit=1] {HandDropChances:[0.0f,0.0f],ArmorDropChances:[0.0f,0.0f,0.0f,0.0f]}" >/dev/null
call wait ticks=20 >/dev/null
gear=$(run "/data get entity @e[type=zombie,limit=1] HandItems[0]")
expect "the zombie holds a Tinkers sword" '.lines[-1].text | contains("tconstruct:sword")' "$gear"
expect "not a vanilla one" '(.lines[-1].text | contains("iron_sword")) | not' "$gear"
armor=$(run "/data get entity @e[type=zombie,limit=1] ArmorItems[2]")
expect "the zombie wears plate armor" '.lines[-1].text | contains("tconstruct:plate_chestplate")' "$armor"
shot smithy_zombie_gear

step "A skeleton keeps its bow"
run "/kill @e[type=!player]" >/dev/null
run "/summon skeleton 0 $y 0 $skeleton_gear" >/dev/null
call wait ticks=20 >/dev/null
gear=$(run "/data get entity @e[type=skeleton,limit=1] HandItems[0]")
expect "the skeleton holds a vanilla bow" '.lines[-1].text | contains("minecraft:bow")' "$gear"

step "Dungeon loot"
run "/kill @e[type=!player]" >/dev/null
run "/clear @s" >/dev/null
# Ten rolls: a plain tool or armor piece in any of them fails the scenario.
vanilla_tools='minecraft:(wooden|stone|iron|golden|diamond|netherite)_(sword|pickaxe|axe|shovel|hoe)|minecraft:(leather|chainmail|iron|golden|diamond|netherite|turtle)_(helmet|chestplate|leggings|boots)|minecraft:bow|minecraft:crossbow|minecraft:shield'
plain=""
for roll in 1 2 3 4 5 6 7 8 9 10; do
    run "/loot give @s loot minecraft:chests/simple_dungeon" >/dev/null
    found=$(run "/data get entity @s Inventory" | jq -r '.lines[-1].text')
    if echo "$found" | grep -qE "id: \"$vanilla_tools"; then
        plain=$found
        break
    fi
    run "/clear @s" >/dev/null
done
if [[ -z $plain ]]; then
    echo "ok   ten dungeon rolls hold no plain tool or armor"
else
    echo "FAIL ten dungeon rolls hold no plain tool or armor" >&2
    echo "     found: $plain" >&2
    exit 1
fi
shot smithy_dungeon_loot

step "A full Terrestrial set"
run "/kill @e[type=!player]" >/dev/null
run "/clear @s" >/dev/null
terrestrial='tic_modifiers:[{name:"tcintegrations:terrestrial",level:1}]'
for piece in helmet chestplate leggings boots; do
    run "/item replace entity @s armor.$piece with tconstruct:plate_$piece{tic_materials:[\"tcintegrations:manasteel\",\"tconstruct:chain\"],$terrestrial}" >/dev/null
done
call wait ticks=10 >/dev/null
expect "the player is in survival with the set on" '.gameMode == "survival"' "$(call status)"

# number <command>: the last number in the reply.
number() {
    run "$1" | jq -r '.lines[-1].text' | grep -oE -- '-?[0-9]+(\.[0-9]+)?' | tail -1
}

step "Guthan's will heals the wielder"
run "/summon zombie 3 $y 3 {NoAI:1b,PersistenceRequired:1b,Health:100f,Attributes:[{Name:\"generic.max_health\",Base:100d},{Name:\"generic.armor\",Base:0d}]}" >/dev/null
run "/give @s tconstruct:sword{tic_materials:[\"tconstruct:iron\",\"tconstruct:wood\",\"tconstruct:wood\"]}" >/dev/null
call select slot="$(hotbar_slot 'tconstruct:sword')" >/dev/null

# helmet_with <will?>: wears the Terrestrial helmet, with the will modifier when given.
# Modifiers go in tic_upgrades: Tinkers rebuilds a hand-made tool and keeps upgrades.
helmet_with() {
    local mods='[{name:"tcintegrations:terrestrial",level:1}'
    [[ -n $1 ]] && mods+='[{name:"feastcraft_smithy:will_'"$1"'",level:1}]'
    mods+=']'
    run "/item replace entity @s armor.head with tconstruct:plate_helmet{tic_materials:[\"tcintegrations:manasteel\",\"tconstruct:chain\"],tic_upgrades:$mods}" >/dev/null
}

# health_after <will?>: damage down to half health, hit the zombie once, read health back.
health_after() {
    helmet_with "${1:-}"
    run "/effect clear @s" >/dev/null
    run "/damage @s 10 minecraft:generic" >/dev/null
    run "/tp @s 1.5 $y 1.5" >/dev/null
    call look yaw=135 pitch=20 >/dev/null
    call attack >/dev/null
    call wait ticks=10 >/dev/null
    number "/data get entity @s Health"
}

with_will=$(health_after guthan)
without_will=$(health_after)
echo "     health after one hit: with will $with_will, without $without_will"
awk -v a="$with_will" -v b="$without_will" 'BEGIN { exit !(a > b) }' &&
    echo "ok   the will healed the wielder" ||
    { echo "FAIL the will healed the wielder" >&2; exit 1; }
shot smithy_guthan_will
