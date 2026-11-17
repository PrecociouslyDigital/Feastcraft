scoreboard objectives add feastcraft_slots dummy
scoreboard players set @s feastcraft_slots 0
execute if entity @s[tag=feastcraft_skill_tree.arcane.mana_font.mana_well] run scoreboard players add @s feastcraft_slots 1
execute if score @s feastcraft_slots matches 0 run curios set mana_tablet @s 0
execute if score @s feastcraft_slots matches 1 run curios set mana_tablet @s 1
