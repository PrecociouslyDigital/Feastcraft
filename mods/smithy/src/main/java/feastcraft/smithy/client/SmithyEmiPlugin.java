package feastcraft.smithy.client;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import feastcraft.smithy.convert.HideFromCreative;
import net.minecraft.world.item.Item;

/** Retired items don't show in EMI, not even by searching their old name. */
@dev.emi.emi.api.EmiEntrypoint
public final class SmithyEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.removeEmiStacks(stack -> stack.getKey() instanceof Item item && HideFromCreative.isDisabled(item));
    }
}
