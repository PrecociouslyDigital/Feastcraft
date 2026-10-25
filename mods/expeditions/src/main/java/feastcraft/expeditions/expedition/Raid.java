package feastcraft.expeditions.expedition;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

/**
 * One player's trip out of the base.
 *
 * @param drop where in the overworld the player was deployed
 * @param startedAt overworld game time at deployment
 */
public record Raid(BlockPos drop, long startedAt) {
    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("drop", NbtUtils.writeBlockPos(drop));
        tag.putLong("startedAt", startedAt);
        return tag;
    }

    static Raid load(CompoundTag tag) {
        return new Raid(NbtUtils.readBlockPos(tag.getCompound("drop")), tag.getLong("startedAt"));
    }
}
