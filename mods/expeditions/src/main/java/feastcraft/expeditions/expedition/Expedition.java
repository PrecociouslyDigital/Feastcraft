package feastcraft.expeditions.expedition;

import feastcraft.expeditions.ExpeditionsRegistry;
import feastcraft.expeditions.base.BaseDimension;
import feastcraft.expeditions.exfil.ExfilCompassItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The raid lifecycle. Steps that change the ledger do so before teleporting, so the residency
 * invariant already holds when the teleport's travel check runs.
 */
public final class Expedition {
    private Expedition() {
    }

    public static boolean isBound(ServerPlayer player) {
        return player.gameMode.getGameModeForPlayer().isSurvival();
    }

    public static boolean hasRaid(ServerPlayer player) {
        return RaidLedger.get(player.server).hasRaid(player.getUUID());
    }

    public static void deploy(ServerPlayer player, BlockPos drop) {
        ServerLevel overworld = player.server.overworld();
        RaidLedger ledger = RaidLedger.get(player.server);
        ledger.begin(player.getUUID(), new Raid(drop, overworld.getGameTime()));
        teleportToDrop(player, drop);
        if (player.level() == overworld) {
            ItemStack compass = ExfilCompassItem.pointingFrom(new ItemStack(ExpeditionsRegistry.EXFIL_COMPASS.get()), overworld, drop);
            if (!player.getInventory().add(compass)) {
                player.drop(compass, false);
            }
        } else {
            // Roll back the raid if the travel check vetoed the teleport.
            ledger.end(player.getUUID());
        }
    }

    /** Ends the raid and brings the player home with everything they carry. */
    public static void extract(ServerPlayer player) {
        end(player);
        BaseDimension.arrive(player);
    }

    /** Ends the raid where the player stands; used when they die. */
    public static void end(ServerPlayer player) {
        RaidLedger.get(player.server).end(player.getUUID());
    }

    /** Returns a player who is still on a raid to the point they were dropped at. */
    public static void resume(ServerPlayer player) {
        RaidLedger.get(player.server).raidOf(player.getUUID())
                .ifPresent(raid -> teleportToDrop(player, raid.drop()));
    }

    private static void teleportToDrop(ServerPlayer player, BlockPos drop) {
        Vec3 at = Vec3.atBottomCenterOf(drop);
        player.teleportTo(player.server.overworld(), at.x, at.y, at.z, player.getYRot(), player.getXRot());
    }
}
