scoreboard objectives add feastcraft_slots dummy
scoreboard players set @s feastcraft_slots 0
execute if entity @s[tag=feastcraft_skill_tree.blade.signet.signet_ring] run scoreboard players add @s feastcraft_slots 1
execute if entity @s[tag=feastcraft_skill_tree.gourmand.heirloom.family_ring] run scoreboard players add @s feastcraft_slots 1
execute if entity @s[tag=feastcraft_skill_tree.wayfarer.keepsake.travelers_ring] run scoreboard players add @s feastcraft_slots 1
execute if score @s feastcraft_slots matches 0 run curios set ring @s 2
execute if score @s feastcraft_slots matches 1 run curios set ring @s 3
execute if score @s feastcraft_slots matches 2 run curios set ring @s 4
execute if score @s feastcraft_slots matches 3 run curios set ring @s 5
