package feastcraft.smithy.convert;

import feastcraft.smithy.Smithy;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;

/**
 * Wraps every trade listing so the trade screen shows the Tinkers item the
 * conversion makes, not a plain sword nobody can take twice.
 */
public final class TradeConversions {
    private TradeConversions() {
    }

    public static void onVillagerTrades(VillagerTradesEvent event) {
        for (var listings : event.getTrades().values()) {
            listings.replaceAll(TradeConversions::wrap);
        }
    }

    public static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().replaceAll(TradeConversions::wrap);
        event.getRareTrades().replaceAll(TradeConversions::wrap);
    }

    /** A listing that makes each offer fresh, then converts its cost and result. */
    private static VillagerTrades.ItemListing wrap(VillagerTrades.ItemListing listing) {
        return (Entity trader, RandomSource random) -> converted(listing.getOffer(trader, random));
    }

    /** A converted copy of an offer: same terms, Tinkers item. */
    static MerchantOffer converted(MerchantOffer offer) {
        ItemStack selling = Smithy.convert(offer.getResult());
        ItemStack costA = Smithy.convert(offer.getCostA());
        ItemStack costB = Smithy.convert(offer.getCostB());
        MerchantOffer wrapped = new MerchantOffer(costA, costB, selling, offer.getUses(), offer.getMaxUses(),
                offer.getXp(), offer.getPriceMultiplier(), offer.getDemand());
        wrapped.setSpecialPriceDiff(offer.getSpecialPriceDiff());
        return wrapped;
    }
}
