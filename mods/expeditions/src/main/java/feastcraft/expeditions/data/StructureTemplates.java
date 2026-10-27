package feastcraft.expeditions.data;

import com.google.common.hash.Hashing;
import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Writes structure templates from layouts declared in code, so no hand-built NBT is committed. */
final class StructureTemplates implements DataProvider {
    record Layout(String name, Vec3i size, Map<BlockPos, BlockState> blocks) {
    }

    /** A dark pad with the beacon at its center and cleared air above, so terrain never buries it. */
    static final Layout EXFIL_BEACON = layout("exfil_beacon", new Vec3i(5, 4, 5), (x, y, z) -> {
        if (y == 0) {
            return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        return x == 2 && y == 1 && z == 2
                ? ExpeditionsRegistry.EXFIL_BEACON.get().defaultBlockState()
                : Blocks.AIR.defaultBlockState();
    });

    /** Open floor for GameTests that need room around a beacon. */
    static final Layout ARENA = layout("arena", new Vec3i(24, 4, 24), (x, y, z) ->
            y == 0 ? Blocks.STONE.defaultBlockState() : null);

    @FunctionalInterface
    interface BlockAt {
        @Nullable
        BlockState at(int x, int y, int z);
    }

    private static Layout layout(String name, Vec3i size, BlockAt blockAt) {
        var blocks = new HashMap<BlockPos, BlockState>();
        BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(size).offset(-1, -1, -1)).forEach(pos -> {
            BlockState state = blockAt.at(pos.getX(), pos.getY(), pos.getZ());
            if (state != null) {
                blocks.put(pos.immutable(), state);
            }
        });
        return new Layout(name, size, Map.copyOf(blocks));
    }

    private final PackOutput output;
    private final List<Layout> layouts;

    StructureTemplates(PackOutput output, List<Layout> layouts) {
        this.output = output;
        this.layouts = layouts;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.runAsync(() -> layouts.forEach(layout -> write(cache, layout)));
    }

    private void write(CachedOutput cache, Layout layout) {
        var bytes = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(encode(layout), bytes);
            cache.writeIfNeeded(
                    output.getOutputFolder(PackOutput.Target.DATA_PACK)
                            .resolve(Expeditions.MOD_ID).resolve("structures").resolve(layout.name() + ".nbt"),
                    bytes.toByteArray(),
                    Hashing.sha1().hashBytes(bytes.toByteArray()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static CompoundTag encode(Layout layout) {
        var palette = new ArrayList<BlockState>();
        var blocks = new ListTag();
        layout.blocks().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (!palette.contains(entry.getValue())) {
                        palette.add(entry.getValue());
                    }
                    var block = new CompoundTag();
                    block.put("pos", ints(entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()));
                    block.putInt("state", palette.indexOf(entry.getValue()));
                    blocks.add(block);
                });

        var paletteTag = new ListTag();
        palette.forEach(state -> paletteTag.add(NbtUtils.writeBlockState(state)));

        var tag = new CompoundTag();
        tag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        tag.put("size", ints(layout.size().getX(), layout.size().getY(), layout.size().getZ()));
        tag.put("palette", paletteTag);
        tag.put("blocks", blocks);
        tag.put("entities", new ListTag());
        return tag;
    }

    private static ListTag ints(int... values) {
        var list = new ListTag();
        for (int value : values) {
            list.add(IntTag.valueOf(value));
        }
        return list;
    }

    @Override
    public String getName() {
        return "Structure templates";
    }
}
