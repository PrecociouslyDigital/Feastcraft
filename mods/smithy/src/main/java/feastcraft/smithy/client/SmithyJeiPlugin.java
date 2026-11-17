package feastcraft.smithy.client;

import feastcraft.smithy.convert.HideFromCreative;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

/** Retired items don't show in JEI, not even by searching their old name. */
@JeiPlugin
public final class SmithyJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return feastcraft.smithy.Smithy.id("jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> hidden = new ArrayList<>();
        net.minecraft.core.registries.BuiltInRegistries.ITEM.holders()
                .filter(holder -> holder.is(HideFromCreative.DISABLED))
                .forEach(holder -> hidden.add(new ItemStack(holder.value())));
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
    }
}
