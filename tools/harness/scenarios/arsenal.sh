#!/usr/bin/env bash
# The arsenal mod in play: weapon-class damage on a Tinkers sword, the spear's charge (a target
# running onto it, and riding onto a target), Lunge, the mace's smash, casting a mace head over a
# heavy core, and heavy cores in loot.
# Ends on the Tinker Station, whose screenshot should show the spear and mace layouts.
#
#   scripts/harness up mod=arsenal && tools/harness/scenarios/arsenal.sh
source "$(dirname "$0")/lib.sh"

# A sky platform keeps terrain, mobs and daylight-burning zombies out of the numbers.
y=151
iron_spear='feastcraft_arsenal:spear{tic_materials:["tconstruct:iron","tconstruct:wood","tconstruct:wood"]}'
iron_mace='feastcraft_arsenal:mace{tic_materials:["tconstruct:iron","tconstruct:wood"]}'
# A stone button on the head keeps a zombie from burning; 100 health leaves room to measure big hits.
target='{NoAI:1b,PersistenceRequired:1b,Health:100f,Attributes:[{Name:"generic.max_health",Base:100d},{Name:"generic.armor",Base:0d}],ArmorItems:[{},{},{},{id:"minecraft:stone_button",Count:1b}]}'

# number <command>: the number in the reply of a data query.
number() {
    run "$1" | jq -r '.lines[-1].text' | grep -oE -- '-?[0-9]+(\.[0-9]+)?' | tail -1
}

zombie_health() {
    number "/data get entity @e[type=zombie,limit=1] Health"
}

# fresh_target <x> <z>: one 100-health zombie, and nothing else.
fresh_target() {
    run "/kill @e[type=!player]" >/dev/null
    run "/summon zombie $1 $y $2 $target" >/dev/null
}

# holding <item>: clears the inventory and puts the item in the first slot.
holding() {
    run "/clear @s" >/dev/null
    run "/give @s $1" >/dev/null
    call select slot=0 >/dev/null
}

# between <description> <value> <low> <high>
between() {
    expect "$1 ($2)" ". >= $3 and . <= $4" "$2"
}

step "Platform"
for c in "/gamerule doMobSpawning false" "/gamerule doDaylightCycle false" "/time set noon" "/difficulty easy" \
    "/fill 0 $((y - 1)) 0 40 $((y - 1)) 40 minecraft:stone" "/fill 0 $y 0 40 $((y + 15)) 40 minecraft:air"; do
    run "$c" >/dev/null
done

step "Weapon-class damage"
holding 'tconstruct:sword{tic_materials:["tconstruct:iron","tconstruct:wood","tconstruct:wood"]}'
run "/tp @s 20.5 $y 18.5 0 0" >/dev/null
fresh_target 20.5 20.5
call wait ticks=40 >/dev/null
call attack >/dev/null
plain=$(echo "100 - $(zombie_health)" | bc -l)
run "/attribute @s feastcraft_arsenal:blade_damage base set 1.5" >/dev/null
fresh_target 20.5 20.5
call wait ticks=40 >/dev/null
call attack >/dev/null
boosted=$(echo "100 - $(zombie_health)" | bc -l)
run "/attribute @s feastcraft_arsenal:blade_damage base set 1" >/dev/null
between "blade damage 1.5 makes a Tinkers sword hit half again as hard" "$(echo "$boosted / $plain" | bc -l)" 1.45 1.55

step "Spear: a target running onto it"
holding "$iron_spear"
run "/tp @s 20.5 $y 5.5 0 0" >/dev/null
fresh_target 20.5 30
call wait ticks=10 >/dev/null
shot spear_held
call use hold=60 >/dev/null &
call wait ticks=20 >/dev/null
shot spear_charging
run "/kill @e[type=zombie]" >/dev/null
# NoAI mobs ignore Motion, so this one has AI and flies at the player without gravity.
run "/summon zombie 20.5 $((y + 0)).5 12.5 {NoGravity:1b,Motion:[0.0,0.0,-0.8],PersistenceRequired:1b,ArmorItems:[{},{},{},{id:\"minecraft:stone_button\",Count:1b}]}" >/dev/null
call wait ticks=15 >/dev/null
wait
between "the charge hurts the zombie" "$(echo "20 - $(zombie_health)" | bc -l)" 4 20

step "Spear: riding onto a standing target"
holding "$iron_spear"
run "/kill @e[type=!player]" >/dev/null
run "/fill 20 $((y - 1)) 4 20 $((y - 1)) 36 minecraft:redstone_block" >/dev/null
run "/fill 20 $y 4 20 $y 36 minecraft:powered_rail" >/dev/null
run "/summon minecart 20.5 $y.1 5.5" >/dev/null
run "/ride @s mount @e[type=minecart,limit=1]" >/dev/null
run "/summon zombie 20.5 $y 33.5 $target" >/dev/null
call look yaw=0 pitch=10 >/dev/null
call use hold=120 >/dev/null &
call wait ticks=15 >/dev/null
# A powered rail holds a minecart at 8 blocks a second; an iron spear deals 0.95 damage per block a second.
run "/data merge entity @e[type=minecart,limit=1] {Motion:[0.0,0.0,0.4]}" >/dev/null
call wait ticks=90 >/dev/null
wait
between "the charge deals about 0.95 x 8" "$(echo "100 - $(zombie_health)" | bc -l)" 7 8
run "/ride @s dismount" >/dev/null
run "/fill 20 $y 4 20 $y 36 minecraft:air" >/dev/null
run "/fill 20 $((y - 1)) 4 20 $((y - 1)) 36 minecraft:stone" >/dev/null

step "Lunge"
holding 'feastcraft_arsenal:spear{tic_materials:["tconstruct:iron","tconstruct:wood","tconstruct:wood"],tic_upgrades:[{name:"feastcraft_arsenal:lunge",level:3}]}'
run "/kill @e[type=!player]" >/dev/null
run "/tp @s 20.5 $y 5.5 0 0" >/dev/null
run "/effect give @s minecraft:saturation 1 20 true" >/dev/null
call wait ticks=40 >/dev/null
saturation=$(number "/data get entity @s foodSaturationLevel")
call attack >/dev/null
status=$(call wait ticks=20)
between "Lunge III carries the player forward" "$(jq '.pos[2] - 5.5' <<<"$status")" 2.5 4
# Every 4 exhaustion costs a point of saturation.
between "Lunge III costs 12 exhaustion" "$(echo "$saturation - $(number "/data get entity @s foodSaturationLevel")" | bc -l)" 2.9 3.1

step "Mace"
holding "$iron_mace"
fresh_target 20.5 20.5
run "/tp @s 20.5 $((y + 14)) 20.5 0 90" >/dev/null
until call status | jq -e ".pos[1] < $((y + 4))" >/dev/null; do :; done
call attack >/dev/null
status=$(call wait ticks=20)
expect "the smash cancels the fall" '.health == 20' "$status"
# 6 damage, 1.5x for the falling crit, then 4/2/1 a block for the 8 to 14 blocks fallen by the hit.
between "a ten-odd block smash" "$(echo "100 - $(zombie_health)" | bc -l)" 30 40

step "Casting a mace head"
run "/kill @e[type=!player]" >/dev/null
holding feastcraft_arsenal:heavy_core
run "/tp @s 20.5 $y 21.5 180 30" >/dev/null
run "/setblock 20 $y 25 tconstruct:seared_table" >/dev/null
run "/setblock 20 $((y + 1)) 26 tconstruct:seared_fuel_tank{tank:{FluidName:\"tconstruct:molten_iron\",Amount:4000}}" >/dev/null
run "/setblock 20 $((y + 1)) 25 tconstruct:seared_faucet[facing=north]" >/dev/null
call look x=20.5 y=$y.9 z=25.5 >/dev/null
call use >/dev/null
run "/setblock 21 $((y + 1)) 25 minecraft:redstone_block" >/dev/null
call wait ticks=4 >/dev/null
run "/setblock 21 $((y + 1)) 25 minecraft:air" >/dev/null
call wait ticks=300 >/dev/null
table=$(run "/data get block 20 $y 25 Items")
expect "the table holds an iron mace head" 'any(.lines[]; .text | contains("feastcraft_arsenal:mace_head") and contains("tconstruct:iron"))' "$table"
expect "the heavy core is used up" 'all(.lines[]; .text | contains("heavy_core") | not)' "$table"
shot cast_mace_head

step "Heavy cores in loot"
run "/kill @e[type=item]" >/dev/null
for _ in $(seq 1 500); do
    run "/loot spawn 10 $((y + 1)) 10 loot minecraft:chests/ancient_city" >/dev/null
done
expect_reply "ancient cities hold heavy cores" '/execute if entity @e[type=item,nbt={Item:{id:"feastcraft_arsenal:heavy_core"}}]' "Test passed"
run "/kill @e[type=item]" >/dev/null

step "Tinker Station"
run "/setblock 20 $y 8 tconstruct:tinker_station" >/dev/null
run "/tp @s 20.5 $y 5.5 0 30" >/dev/null
holding minecraft:air
call look x=20.5 y=$y.9 z=8.5 >/dev/null
call use >/dev/null
call wait ticks=10 >/dev/null
shot tinker_station
