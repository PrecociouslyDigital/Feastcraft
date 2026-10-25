package feastcraft.expeditions.exfil;

import feastcraft.expeditions.Expeditions;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** The beacons of one level that have been used. This, not the block state, decides whether a beacon is spent. */
final class SpentBeacons extends SavedData {
    private static final String NAME = Expeditions.MOD_ID + "_spent_beacons";

    private final LongSet keys = new LongOpenHashSet();

    static SpentBeacons get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SpentBeacons::load, SpentBeacons::new, NAME);
    }

    boolean contains(long key) {
        return keys.contains(key);
    }

    void add(long key) {
        if (keys.add(key)) {
            setDirty();
        }
    }

    void remove(long key) {
        if (keys.remove(key)) {
            setDirty();
        }
    }

    private static SpentBeacons load(CompoundTag tag) {
        SpentBeacons spent = new SpentBeacons();
        for (long key : tag.getLongArray("keys")) {
            spent.keys.add(key);
        }
        return spent;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("keys", keys.toLongArray());
        return tag;
    }
}
