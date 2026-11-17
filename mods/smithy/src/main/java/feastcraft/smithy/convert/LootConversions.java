package feastcraft.smithy.convert;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import feastcraft.smithy.Smithy;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Converts every item loot produces: chests, fishing, archaeology and mob drops
 * that go straight through a table. No conditions, so it applies to every table.
 */
public final class LootConversions {
    private LootConversions() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(LootConversions::onRegisterSerializers);
    }

    private static void onRegisterSerializers(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, (Consumer<RegisterEvent.RegisterHelper<Codec<? extends IGlobalLootModifier>>>) helper ->
                helper.register(Smithy.id("convert_all"), ConvertAll.CODEC));
    }

    /** The same shape as arsenal's HeavyCoreLoot, minus the fields: every stack, every table. */
    public static final class ConvertAll extends LootModifier {
        public static final Codec<ConvertAll> CODEC = RecordCodecBuilder.create(instance ->
                codecStart(instance).apply(instance, ConvertAll::new));

        public ConvertAll(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        public Codec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            loot.replaceAll(Conversions::convert);
            return loot;
        }
    }
}
