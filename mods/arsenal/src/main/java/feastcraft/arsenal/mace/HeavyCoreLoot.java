package feastcraft.arsenal.mace;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import feastcraft.arsenal.ArsenalRegistry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Puts a heavy core, rarely, into the chests of the listed loot tables. */
public final class HeavyCoreLoot extends LootModifier {
    public static final Codec<HeavyCoreLoot> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(ResourceLocation.CODEC.listOf().fieldOf("tables").forGetter(loot -> List.copyOf(loot.tables)))
            .and(Codec.FLOAT.optionalFieldOf("chance", 0.015F).forGetter(loot -> loot.chance))
            .apply(instance, HeavyCoreLoot::new));

    private final Set<ResourceLocation> tables;
    private final float chance;

    public HeavyCoreLoot(LootItemCondition[] conditions, List<ResourceLocation> tables, float chance) {
        super(conditions);
        this.tables = Set.copyOf(tables);
        this.chance = chance;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (tables.contains(context.getQueriedLootTableId()) && context.getRandom().nextFloat() < chance) {
            loot.add(new ItemStack(ArsenalRegistry.HEAVY_CORE.get()));
        }
        return loot;
    }
}
