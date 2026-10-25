package feastcraft.expeditions.exfil;

import feastcraft.expeditions.Expeditions;
import feastcraft.expeditions.ExpeditionsConfig;
import feastcraft.expeditions.expedition.Expedition;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraftforge.common.MinecraftForge;

/**
 * Exfil beacons: generated with the world, each usable once. A beacon is identified by the chunk its
 * structure started in, or by its own chunk if it was placed by hand.
 */
public final class ExfilBeacons {
    public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Expeditions.id("exfil_beacon"));
    public static final ResourceKey<StructureSet> STRUCTURE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Expeditions.id("exfil_beacons"));

    public enum CallResult { STARTED, NOT_RAIDING, SPENT, REFUSED }

    private ExfilBeacons() {
    }

    public static long key(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, STRUCTURE);
        return start.isValid() ? start.getChunkPos().toLong() : ChunkPos.asLong(pos);
    }

    public static boolean isSpent(ServerLevel level, BlockPos pos) {
        return SpentBeacons.get(level).contains(key(level, pos));
    }

    public static Optional<ExfilCountdown> countdownAt(ServerLevel level, BlockPos pos) {
        return ActiveExfils.get(level).countdownAt(key(level, pos));
    }

    /** A player asking a beacon for extraction. Only a raiding player in survival or adventure can call one. */
    public static CallResult call(ServerLevel level, BlockPos pos, ServerPlayer caller) {
        boolean spent = isSpent(level, pos);
        showSpent(level, pos, spent);
        if (!Expedition.isBound(caller) || !Expedition.hasRaid(caller)) {
            return CallResult.NOT_RAIDING;
        }
        if (spent) {
            return CallResult.SPENT;
        }
        if (MinecraftForge.EVENT_BUS.post(new ExfilEvent.Call(level, pos, caller))) {
            return CallResult.REFUSED;
        }
        ignite(level, pos, ExpeditionsConfig.EXFIL_COUNTDOWN_TICKS.get());
        return CallResult.STARTED;
    }

    /** Spends the beacon and starts its countdown. Returns false if it was already spent. */
    public static boolean ignite(ServerLevel level, BlockPos pos, int countdownTicks) {
        long key = key(level, pos);
        SpentBeacons spent = SpentBeacons.get(level);
        if (spent.contains(key)) {
            return false;
        }
        spent.add(key);
        showSpent(level, pos, true);
        ActiveExfils.get(level).start(key, pos, countdownTicks);
        return true;
    }

    /** Makes a beacon usable again, cancelling its countdown if one is running. */
    public static void reset(ServerLevel level, BlockPos pos) {
        long key = key(level, pos);
        SpentBeacons.get(level).remove(key);
        ActiveExfils.get(level).cancel(key);
        showSpent(level, pos, false);
    }

    private static void showSpent(ServerLevel level, BlockPos pos, boolean spent) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ExfilBeaconBlock && state.getValue(ExfilBeaconBlock.SPENT) != spent) {
            level.setBlockAndUpdate(pos, state.setValue(ExfilBeaconBlock.SPENT, spent));
        }
    }

    /**
     * The closest unspent beacon within {@code maxDistance} blocks, horizontally. This walks the
     * structure set's placement grid outward the way vanilla's {@code /locate} does, skipping spent
     * beacons and without touching explorer-map references.
     */
    public static Optional<BlockPos> nearestUnspent(ServerLevel level, BlockPos origin, int maxDistance) {
        var generatorState = level.getChunkSource().getGeneratorState();
        Optional<Holder.Reference<Structure>> structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(STRUCTURE);
        Optional<Holder<StructureSet>> structureSet = generatorState.possibleStructureSets().stream()
                .filter(set -> set.is(STRUCTURE_SET))
                .findFirst();
        if (structure.isEmpty() || structureSet.isEmpty()
                || !(structureSet.get().value().placement() instanceof RandomSpreadStructurePlacement placement)) {
            return Optional.empty();
        }

        SpentBeacons spent = SpentBeacons.get(level);
        long seed = generatorState.getLevelSeed();
        int originChunkX = SectionPos.blockToSectionCoord(origin.getX());
        int originChunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        int maxRing = maxDistance / (placement.spacing() * 16) + 1;
        Comparator<BlockPos> byDistance = Comparator.comparingDouble(pos -> horizontalDistance(origin, pos));

        // A cell in the next ring can still hold a closer beacon than the far corners of this one.
        Optional<BlockPos> nearest = Optional.empty();
        int foundInRing = -1;
        for (int ring = 0; ring <= maxRing && (foundInRing < 0 || ring <= foundInRing + 1); ring++) {
            for (SpreadRings.Cell cell : SpreadRings.ring(ring)) {
                ChunkPos chunk = placement.getPotentialStructureChunk(seed,
                        originChunkX + cell.dx() * placement.spacing(), originChunkZ + cell.dz() * placement.spacing());
                BlockPos at = chunk.getMiddleBlockPosition(origin.getY());
                if (spent.contains(chunk.toLong())
                        || horizontalDistance(origin, at) > maxDistance
                        || !hasStarted(level, structure.get().value(), chunk)) {
                    continue;
                }
                if (nearest.isEmpty() || byDistance.compare(at, nearest.get()) < 0) {
                    nearest = Optional.of(at);
                }
                if (foundInRing < 0) {
                    foundInRing = ring;
                }
            }
        }
        return nearest;
    }

    private static boolean hasStarted(ServerLevel level, Structure structure, ChunkPos chunk) {
        StructureCheckResult check = level.structureManager().checkStructurePresence(chunk, structure, false);
        if (check != StructureCheckResult.CHUNK_LOAD_NEEDED) {
            return check == StructureCheckResult.START_PRESENT;
        }
        ChunkAccess access = level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS);
        StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(access), structure, access);
        return start != null && start.isValid();
    }

    public static double horizontalDistance(BlockPos a, BlockPos b) {
        return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
    }
}
