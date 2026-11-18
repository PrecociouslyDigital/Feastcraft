package feastcraft.smithy;

import com.mojang.logging.LogUtils;
import feastcraft.smithy.convert.Conversions;
import feastcraft.smithy.convert.HideFromCreative;
import feastcraft.smithy.convert.InventorySweep;
import feastcraft.smithy.convert.JoinConversions;
import feastcraft.smithy.convert.LootConversions;
import feastcraft.smithy.convert.TradeConversions;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Tinkers as the only smith: every plain tool, weapon and armor piece is a Tinkers tool. */
@Mod(Smithy.MOD_ID)
public final class Smithy {
    public static final String MOD_ID = "feastcraft_smithy";
    public static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    public Smithy(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        LootConversions.register(modBus);
        HideFromCreative.register(modBus);
        // Conversions reloads with the datapacks, which is a game-bus event.
        Conversions.register(MinecraftForge.EVENT_BUS);
        MinecraftForge.EVENT_BUS.addListener(JoinConversions::onItemJoinLevel);
        MinecraftForge.EVENT_BUS.addListener(JoinConversions::onMobJoinLevel);
        MinecraftForge.EVENT_BUS.addListener(InventorySweep::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(TradeConversions::onVillagerTrades);
        MinecraftForge.EVENT_BUS.addListener(TradeConversions::onWandererTrades);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /** The one conversion function; every entry point funnels through here. */
    public static net.minecraft.world.item.ItemStack convert(net.minecraft.world.item.ItemStack stack) {
        return Conversions.convert(stack);
    }
}
