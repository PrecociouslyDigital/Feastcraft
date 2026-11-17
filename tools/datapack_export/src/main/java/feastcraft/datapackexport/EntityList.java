package feastcraft.datapackexport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * {@code entities.tsv}: every registered entity type, its spawn category, and whether it has
 * attributes. Mobs have attributes; arrows, boats and item frames don't.
 */
final class EntityList {
    private EntityList() {
    }

    static void write(Path output) {
        try {
            Files.createDirectories(output);
            Files.write(output.resolve("entities.tsv"), ForgeRegistries.ENTITY_TYPES.getEntries().stream()
                    .map(entry -> entry.getKey().location() + "\t" + entry.getValue().getCategory().getName()
                            + "\t" + (DefaultAttributes.hasSupplier(entry.getValue()) ? "living" : ""))
                    .sorted()
                    .toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
