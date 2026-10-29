package feastcraft.expeditions.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import feastcraft.expeditions.ExpeditionsConfig;
import feastcraft.expeditions.ExpeditionsRegistry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Puts ember charcoal into the world's loot without editing any vanilla tables. Chests get it at
 * roughly the rate iron shows up, and monsters drop it at wither-skull rarity.
 */
public final class EmberCharcoalLoot {
    /** Per-chest chance, in the band where iron ingots land across vanilla chest tables. */
    private static final LootItemCondition CHEST_CHANCE =
            LootItemRandomChanceCondition.randomChance(0.15F).build();
    /** Wither-skeleton skull rarity, plus the same per-Looting bonus skulls get. */
    private static final LootItemCondition HOSTILE_CHANCE =
            LootItemRandomChanceWithLootingCondition.randomChanceAndLootingBoost(0.025F, 0.01F).build();
    private static final String CHEST_FOLDER = "chests/";

    private EmberCharcoalLoot() {
    }

    private static ItemStack emberCharcoal() {
        return new ItemStack(ExpeditionsRegistry.EMBER_CHARCOAL.get());
    }

    /** Adds ember charcoal to every vanilla chest loot table. */
    public static final class Chests extends LootModifier {
        public static final Codec<Chests> CODEC = RecordCodecBuilder.create(
                instance -> codecStart(instance).apply(instance, Chests::new));

        public Chests(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        public Codec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            if (!ExpeditionsConfig.EMBER_ENABLED.get()) {
                return loot;
            }
            var table = context.getQueriedLootTableId();
            if (table.getNamespace().equals("minecraft") && table.getPath().startsWith(CHEST_FOLDER)
                    && CHEST_CHANCE.test(context)) {
                loot.add(emberCharcoal());
            }
            return loot;
        }
    }

    /** Gives every hostile monster a wither-skull-rare chance to drop ember charcoal. */
    public static final class Hostiles extends LootModifier {
        public static final Codec<Hostiles> CODEC = RecordCodecBuilder.create(
                instance -> codecStart(instance).apply(instance, Hostiles::new));

        public Hostiles(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        public Codec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            if (!ExpeditionsConfig.EMBER_ENABLED.get()) {
                return loot;
            }
            if (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Enemy
                    && HOSTILE_CHANCE.test(context)) {
                loot.add(emberCharcoal());
            }
            return loot;
        }
    }
}
