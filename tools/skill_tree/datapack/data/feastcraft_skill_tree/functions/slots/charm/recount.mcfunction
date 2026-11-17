scoreboard objectives add feastcraft_slots dummy
scoreboard players set @s feastcraft_slots 0
execute if entity @s[tag=feastcraft_skill_tree.arcane.trinkets.charmed] run scoreboard players add @s feastcraft_slots 1
execute if score @s feastcraft_slots matches 0 run curios set charm @s 1
execute if score @s feastcraft_slots matches 1 run curios set charm @s 2
