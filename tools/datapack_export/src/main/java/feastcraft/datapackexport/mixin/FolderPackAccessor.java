package feastcraft.datapackexport.mixin;

import java.nio.file.Path;
import net.minecraft.server.packs.PathPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the directory a folder pack serves. */
@Mixin(PathPackResources.class)
public interface FolderPackAccessor {
    @Accessor("root")
    Path feastcraft_datapack_export$root();
}
