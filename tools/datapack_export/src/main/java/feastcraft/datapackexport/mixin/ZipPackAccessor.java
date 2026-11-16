package feastcraft.datapackexport.mixin;

import java.io.File;
import net.minecraft.server.packs.FilePackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the zip file a zip pack serves. */
@Mixin(FilePackResources.class)
public interface ZipPackAccessor {
    @Accessor("file")
    File feastcraft_datapack_export$file();
}
