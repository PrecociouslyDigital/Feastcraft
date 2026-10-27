#!/usr/bin/env bash
# Carrot's and Onion's food books as one: turning past Carrot's last page lands on Onion's first,
# and turning back returns to Carrot's last.
#
#   scripts/harness up mod=food_books && tools/harness/scenarios/food_books.sh
source "$(dirname "$0")/lib.sh"

carrot=com.cazsius.solcarrot.client.gui.FoodBookScreen
onion=team.creative.solonion.client.gui.screen.FoodBookScreen

step "Carrot's book"
run "/gamemode creative" >/dev/null
run "/item replace entity @s hotbar.0 with solcarrot:food_book" >/dev/null
call select slot=0 >/dev/null
call look yaw=0 pitch=0 >/dev/null
call use >/dev/null
wait_until "Carrot's food book opens" ".screenClass == \"$carrot\"" 40
expect "its page buttons are labelled" 'any(.buttons[]; . == "Next Page")' "$(call status)"

step "Into Onion's"
turns=0
until call status | jq -e ".screenClass == \"$onion\"" >/dev/null; do
    ((++turns <= 20)) || expect "Onion's book follows Carrot's" false "$(call status)"
    call click label="Next Page" >/dev/null
done
echo "ok   turned into Onion's book after $turns pages"
shot onion_book hud=false

step "Back to Carrot's"
status=$(call click label="Previous Page")
expect "turning back returns to Carrot's book" ".screenClass == \"$carrot\"" "$status"
shot carrot_last_page hud=false
