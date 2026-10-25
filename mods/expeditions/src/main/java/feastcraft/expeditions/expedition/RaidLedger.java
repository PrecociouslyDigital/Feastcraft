package feastcraft.expeditions.expedition;

import feastcraft.expeditions.Expeditions;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Every active raid. A player with no entry here is at base; there is no other record of where they are. */
public final class RaidLedger extends SavedData {
    private static final String NAME = Expeditions.MOD_ID + "_raids";

    private final Map<UUID, Raid> raids = new HashMap<>();

    public static RaidLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(RaidLedger::load, RaidLedger::new, NAME);
    }

    public Optional<Raid> raidOf(UUID player) {
        return Optional.ofNullable(raids.get(player));
    }

    public boolean hasRaid(UUID player) {
        return raids.containsKey(player);
    }

    void begin(UUID player, Raid raid) {
        raids.put(player, raid);
        setDirty();
    }

    void end(UUID player) {
        if (raids.remove(player) != null) {
            setDirty();
        }
    }

    private static RaidLedger load(CompoundTag tag) {
        RaidLedger ledger = new RaidLedger();
        for (String key : tag.getAllKeys()) {
            ledger.raids.put(UUID.fromString(key), Raid.load(tag.getCompound(key)));
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        raids.forEach((player, raid) -> tag.put(player.toString(), raid.save()));
        return tag;
    }
}
