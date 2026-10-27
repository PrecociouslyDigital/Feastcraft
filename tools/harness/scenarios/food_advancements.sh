#!/usr/bin/env bash
# Food advancements as a player sees them: eating writes foods into Spice of Life: Carrot Edition's
# list, the Spice of Life tab appears, a hidden goal is announced, and Carrot's book opens.
#
#   scripts/harness up mod=food_advancements && tools/harness/scenarios/food_advancements.sh
source "$(dirname "$0")/lib.sh"

# eat <item>: puts the food in the first hotbar slot and eats it the way a player does.
eat() {
    run "/item replace entity @s hotbar.0 with $1" >/dev/null
    call select slot=0 >/dev/null
    call use hold=40 >/dev/null
}

step "Joining"
run "/difficulty peaceful" >/dev/null
run "/time set noon" >/dev/null
# Creative players can always eat, and Carrot still counts what they eat.
run "/gamemode creative" >/dev/null
expect_reply "Carrot's list starts empty" "/foodlist size" "eaten 0 unique foods"

step "First food"
eat minecraft:bread
expect_reply "Carrot wrote the bread down" "/foodlist size" "eaten one unique food"
expect_reply "and it earned the tab" "/execute if entity @s[advancements={feastcraft_food_advancements:root=true}]" "Test passed"

step "Every fish"
for fish in cod salmon tropical_fish pufferfish cooked_cod cooked_salmon; do
    eat "minecraft:$fish"
done
expect "eating every fish announces the hidden goal" \
    'any(.lines[]; .text | contains("Catch of the Day"))' "$(call chat since=0)"
shot every_fish

step "Carrot's book"
run "/item replace entity @s hotbar.0 with solcarrot:food_book" >/dev/null
call select slot=0 >/dev/null
call look yaw=0 pitch=0 >/dev/null
call use >/dev/null
wait_until "Carrot's food book opens" '.screenClass == "com.cazsius.solcarrot.client.gui.FoodBookScreen"' 40
shot carrot_book hud=false
