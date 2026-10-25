#!/usr/bin/env bash
# The expeditions loop as a player sees it: joining the base, fuel and stasis, deploying, following
# a beacon's beam, extracting with a mob, dying, and the base's residency rules. Each step takes a
# named screenshot to review.
#
#   scripts/harness up mod=expeditions && tools/harness/scenarios/expeditions.sh
source "$(dirname "$0")/lib.sh"

base=feastcraft_expeditions:base
beacon=feastcraft_expeditions:exfil_beacon
compass=feastcraft_expeditions:exfil_compass

# land_near <x> <z>: puts the player on the surface within a few blocks of that column.
land_near() {
    expect_reply "landed near $1, $2" "/spreadplayers $1 $2 0 8 false @s" "Spread 1 player"
}

# compass_target: {x, z} of where the exfil compass in the hotbar points.
compass_target() {
    call status | jq --arg compass "$compass" \
        'first(.hotbar[] | select(.item == $compass)).nbt
        | capture("pos:\\[I;(?<x>-?[0-9]+),-?[0-9]+,(?<z>-?[0-9]+)\\]")
        | map_values(tonumber)'
}

step "Joining"
status=$(call status)
expect "the player joins in the base" ".dimension == \"$base\"" "$status"
expect "standing on the platform" '.pos[1] == 65' "$status"
run "/difficulty peaceful" >/dev/null
run "/time set noon" >/dev/null
call look yaw=-70 pitch=30 >/dev/null
# The client learns the new time with the server's next time update.
call wait ticks=40 >/dev/null
shot platform

step "Fuel"
run "/give @s minecraft:coal 4" >/dev/null
call select slot="$(hotbar_slot minecraft:coal)" >/dev/null
expect "looking at the hearth" '.lookingAt.block == "feastcraft_expeditions:hearth"' "$(call look x=2.5 y=65.5 z=0.5)"
expect "feeding it shows the fuel left" 'any(.lines[]; .kind == "actionbar" and (.text | startswith("Fuel remaining")))' "$(call use)"

step "Frozen crops"
run "/expeditions fuel set 0" >/dev/null
run "/setblock 0 64 2 minecraft:farmland" >/dev/null
run "/setblock 0 65 2 minecraft:wheat" >/dev/null
# About 200 random ticks for the crop, so a fuelled base can't fail to grow it by chance.
run "/gamerule randomTickSpeed 4096" >/dev/null
call wait ticks=200 >/dev/null
expect_reply "a crop doesn't grow while the base is frozen" "/execute if block 0 65 2 minecraft:wheat[age=0]" "Test passed"
run "/expeditions fuel add 72000" >/dev/null
call wait ticks=200 >/dev/null
expect_reply "and grows once the hearth burns" "/execute unless block 0 65 2 minecraft:wheat[age=0]" "Test passed"
run "/gamerule randomTickSpeed 3" >/dev/null

step "Deploy"
run "/expeditions deploy" >/dev/null
wait_until "the deployment lands in the overworld" '.dimension == "minecraft:overworld" and .screen == null' 1200
expect_reply "the raid is recorded" "/expeditions raid info" "is raiding"
expect "an exfil compass is in the hotbar" "any(.hotbar[]; .item == \"$compass\")" "$(call status)"
call look yaw=0 pitch=-10 >/dev/null
call wait ticks=100 >/dev/null
shot deployed

step "Beam"
read -r near_x near_z < <(run "/expeditions beacon nearest" |
    jq -r 'first(.lines[].text | capture("around (?<x>-?[0-9]+), (?<z>-?[0-9]+)")) | "\(.x) \(.z)"')
land_near "$near_x" "$near_z"
call wait ticks=60 >/dev/null
found=$(call find block="$beacon" radius=32)
expect "the beacon is near where the command said" '.found | length > 0' "$found"
read -r bx by bz < <(jq -r '.found[0] | map(tostring) | join(" ")' <<<"$found")
land_near "$((bx + 60))" "$bz"
call select slot="$(hotbar_slot "$compass")" >/dev/null
call look x="$(jq -n "$bx + 0.5")" y="$((by + 25))" z="$(jq -n "$bz + 0.5")" >/dev/null
# Long enough for the client to draw the chunks, and for the compass to re-target from here.
call wait ticks=120 >/dev/null
shot beam
target=$(compass_target)
expect "the compass points at that beacon" ".x == $near_x and .z == $near_z" "$target"
call look x="$(jq -n "$near_x + 0.5")" y="$(call status | jq '.pos[1] + 1.62')" z="$(jq -n "$near_z + 0.5")" >/dev/null
call wait ticks=20 >/dev/null
shot compass

step "Extraction"
run "/tp @s $((bx + 2)) $by $bz" >/dev/null
run "/summon minecraft:sheep $((bx + 3)) $by $((bz + 2)) {NoAI:1b}" >/dev/null
run "/give @s minecraft:lead" >/dev/null
call select slot="$(hotbar_slot minecraft:lead)" >/dev/null
call wait ticks=10 >/dev/null
expect "looking at the sheep" '.lookingAt.entity == "minecraft:sheep"' \
    "$(call look x="$(jq -n "$bx + 3.5")" y="$(jq -n "$by + 0.6")" z="$(jq -n "$bz + 2.5")")"
call use >/dev/null
expect_reply "the sheep is on the lead" "/execute if entity @e[type=minecraft:sheep,nbt={Leash:{}}]" "Test passed"
expect "looking at the beacon" ".lookingAt.block == \"$beacon\"" \
    "$(call look x="$(jq -n "$bx + 0.5")" y="$(jq -n "$by + 0.5")" z="$(jq -n "$bz + 0.5")")"
expect "using it calls extraction" 'any(.lines[]; .text | startswith("Extraction called"))' "$(call use)"
status=$(call status)
expect "a countdown boss bar shows" 'any(.bossBars[]; .name | startswith("Extraction in"))' "$status"
call look yaw=90 pitch=-30 >/dev/null
call wait ticks=20 >/dev/null
shot countdown
wait_until "the countdown ends in the base" ".dimension == \"$base\"" 400
expect_reply "the raid is over" "/expeditions raid info" "is at the base"
expect_reply "the sheep came too" "/execute if entity @e[type=minecraft:sheep,distance=..8]" "Test passed"

step "Spent beacon"
run "/expeditions deploy" >/dev/null
wait_until "a second deployment lands" '.dimension == "minecraft:overworld" and .screen == null' 1200
run "/tp @s $((bx + 2)) $by $bz" >/dev/null
call wait ticks=60 >/dev/null
status=$(call look x="$(jq -n "$bx + 0.5")" y="$(jq -n "$by + 0.5")" z="$(jq -n "$bz + 0.5")")
expect "the beacon shows as spent" ".lookingAt.block == \"$beacon\" and .lookingAt.state.spent == \"true\"" "$status"
# Far enough back to see that there is no beam above it.
run "/tp @s $((bx + 12)) $by $bz" >/dev/null
call look x="$(jq -n "$bx + 0.5")" y="$((by + 6))" z="$(jq -n "$bz + 0.5")" >/dev/null
call wait ticks=100 >/dev/null
shot spent
expect "the compass re-targets to another beacon" ".x != $near_x or .z != $near_z" "$(compass_target)"

step "Death"
run "/give @s minecraft:diamond 3" >/dev/null
run "/kill @s" >/dev/null
wait_until "the death screen opens" '.screen == "DeathScreen"' 100
expect_reply "the player's items drop where they died" "/execute if entity @e[type=minecraft:item,distance=..4]" "Test passed"
respawn
expect "the player respawns in the base" ".dimension == \"$base\"" "$(call status)"
expect_reply "the raid ended with the death" "/expeditions raid info" "is at the base"

step "Floor"
run "/setblock 0 64 0 minecraft:air" >/dev/null
run "/kill @s" >/dev/null
wait_until "the death screen opens" '.screen == "DeathScreen"' 100
respawn
status=$(call status)
expect "the player respawns on the platform" ".dimension == \"$base\" and .pos[1] == 65" "$status"
expect_reply "the floor under spawn is restored" "/execute if block 0 64 0 minecraft:stone" "Test passed"

step "Portals and teleports"
run "/fill -2 65 -3 1 69 -3 minecraft:obsidian" >/dev/null
run "/fill -1 66 -3 0 68 -3 minecraft:air" >/dev/null
run "/give @s minecraft:flint_and_steel" >/dev/null
call select slot="$(hotbar_slot minecraft:flint_and_steel)" >/dev/null
expect "looking inside the frame" '.lookingAt.block == "minecraft:obsidian" and .lookingAt.pos == [0, 65, -3]' \
    "$(call look x=0.5 y=66 z=-2.5)"
call use >/dev/null
expect_reply "the frame catches fire" "/execute if block 0 66 -3 minecraft:fire" "Test passed"
expect_reply "but no portal opens" "/execute if block -1 66 -3 minecraft:nether_portal" "Test failed"
call look yaw=180 pitch=0 >/dev/null
call wait ticks=10 >/dev/null
shot portal
run "/execute in minecraft:overworld run tp @s 0 100 0" >/dev/null
call wait ticks=20 >/dev/null
expect "a survival player can't teleport out of the base" ".dimension == \"$base\"" "$(call status)"
