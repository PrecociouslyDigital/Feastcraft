package feastcraft.datapackexport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/** {@code blocks.tsv}: every registered block, and the item that places it if there is one. */
final class BlockList {
    private BlockList() {
    }

    static void write(Path output) {
        try {
            Files.createDirectories(output);
            Files.write(output.resolve("blocks.tsv"), ForgeRegistries.BLOCKS.getEntries().stream()
                    .map(entry -> entry.getKey().location() + "\t" + item(entry.getValue()))
                    .sorted()
                    .toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String item(Block block) {
        return block.asItem() == Items.AIR ? "" : String.valueOf(ForgeRegistries.ITEMS.getKey(block.asItem()));
    }
}
