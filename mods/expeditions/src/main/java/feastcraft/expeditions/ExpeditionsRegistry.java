package feastcraft.expeditions;

import com.mojang.serialization.Codec;
import feastcraft.expeditions.base.HearthBlock;
import feastcraft.expeditions.base.HearthBlockEntity;
import feastcraft.expeditions.expedition.DepartureGateBlock;
import feastcraft.expeditions.exfil.ExfilBeaconBlock;
import feastcraft.expeditions.exfil.ExfilBeaconBlockEntity;
import feastcraft.expeditions.exfil.ExfilCompassItem;
import feastcraft.expeditions.loot.EmberCharcoalLoot;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ExpeditionsRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Expeditions.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Expeditions.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Expeditions.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Expeditions.MOD_ID);
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Expeditions.MOD_ID);

    public static final RegistryObject<HearthBlock> HEARTH = block("hearth", HearthBlock::new);
    public static final RegistryObject<DepartureGateBlock> DEPARTURE_GATE = block("departure_gate", DepartureGateBlock::new);
    public static final RegistryObject<ExfilBeaconBlock> EXFIL_BEACON = block("exfil_beacon", ExfilBeaconBlock::new);
    public static final RegistryObject<BlockEntityType<ExfilBeaconBlockEntity>> EXFIL_BEACON_ENTITY = BLOCK_ENTITIES.register("exfil_beacon",
            () -> BlockEntityType.Builder.of(ExfilBeaconBlockEntity::new, EXFIL_BEACON.get()).build(null));
    public static final RegistryObject<ExfilCompassItem> EXFIL_COMPASS = ITEMS.register("exfil_compass", ExfilCompassItem::new);
    public static final RegistryObject<Item> EMBER_CHARCOAL = ITEMS.register("ember_charcoal", () -> new Item(new Item.Properties()));
    public static final RegistryObject<BlockEntityType<HearthBlockEntity>> HEARTH_ENTITY = BLOCK_ENTITIES.register("hearth",
            () -> BlockEntityType.Builder.of(HearthBlockEntity::new, HEARTH.get()).build(null));
    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> CHEST_FUEL = LOOT_MODIFIERS.register("ember_charcoal_chests",
            () -> EmberCharcoalLoot.Chests.CODEC);
    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> HOSTILE_FUEL = LOOT_MODIFIERS.register("ember_charcoal_hostiles",
            () -> EmberCharcoalLoot.Hostiles.CODEC);

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.feastcraft_expeditions"))
                .icon(() -> new ItemStack(HEARTH.get()))
                .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                .build());
    }

    private ExpeditionsRegistry() {
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TABS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
    }

    private static <B extends Block> RegistryObject<B> block(String name, Supplier<B> block) {
        RegistryObject<B> registered = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }
}
