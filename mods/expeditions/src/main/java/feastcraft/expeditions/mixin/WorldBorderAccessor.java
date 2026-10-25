package feastcraft.expeditions.mixin;

import java.util.List;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldBorder.class)
public interface WorldBorderAccessor {
    @Invoker("getListeners")
    List<BorderChangeListener> feastcraft$listeners();
}
