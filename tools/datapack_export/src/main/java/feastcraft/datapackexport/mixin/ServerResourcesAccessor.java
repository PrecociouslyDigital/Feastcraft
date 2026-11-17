package feastcraft.datapackexport.mixin;

import net.minecraft.server.ReloadableServerResources;
import net.minecraft.tags.TagManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the tags a load found, before anything binds them. */
@Mixin(ReloadableServerResources.class)
public interface ServerResourcesAccessor {
    @Accessor("tagManager")
    TagManager feastcraft_datapack_export$tagManager();
}
