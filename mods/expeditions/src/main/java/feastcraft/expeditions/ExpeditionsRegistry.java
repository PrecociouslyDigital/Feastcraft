package feastcraft.expeditions;

import feastcraft.expeditions.base.HearthBlock;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ExpeditionsRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Expeditions.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Expeditions.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Expeditions.MOD_ID);

    public static final RegistryObject<HearthBlock> HEARTH = block("hearth", HearthBlock::new);

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
        TABS.register(modBus);
    }

    private static <B extends Block> RegistryObject<B> block(String name, Supplier<B> block) {
        RegistryObject<B> registered = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }
}
