package feastcraft.expeditions.base;

import feastcraft.expeditions.Expeditions;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The base's one shared fuel tank, burning one tick of fuel per game tick. */
@Mod.EventBusSubscriber(modid = Expeditions.MOD_ID)
public final class BaseFuel extends SavedData {
    private static final String NAME = Expeditions.MOD_ID + "_fuel";

    private FuelTank tank = FuelTank.EMPTY;

    public static BaseFuel get(ServerLevel base) {
        return base.getDataStorage().computeIfAbsent(BaseFuel::load, BaseFuel::new, NAME);
    }

    public static BaseFuel get(MinecraftServer server) {
        return get(BaseDimension.level(server));
    }

    @SubscribeEvent
    static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level && BaseDimension.isBase(level)) {
            get(level).update(level, tank -> tank.drain(1));
        }
    }

    public FuelTank tank() {
        return tank;
    }

    public void update(ServerLevel base, UnaryOperator<FuelTank> change) {
        boolean wasRunning = tank.running();
        tank = change.apply(tank);
        setDirty();
        if (tank.running() != wasRunning) {
            BaseChunkLoading.sync(base, tank.running());
            Component notice = tank.running()
                    ? Component.translatable("feastcraft_expeditions.base.running").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("feastcraft_expeditions.base.frozen").withStyle(ChatFormatting.AQUA);
            base.players().forEach(player -> player.sendSystemMessage(notice));
        }
    }

    private static BaseFuel load(CompoundTag tag) {
        BaseFuel fuel = new BaseFuel();
        fuel.tank = new FuelTank(Math.max(0, tag.getLong("ticksRemaining")));
        return fuel;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("ticksRemaining", tank.ticksRemaining());
        return tag;
    }
}
