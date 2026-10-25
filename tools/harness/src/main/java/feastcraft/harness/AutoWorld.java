package feastcraft.harness;

import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Starts every harness run in the same fresh world: when the title screen first opens, the
 * previous run's world is deleted and a new one is created from a fixed seed.
 */
final class AutoWorld {
    private static final String LEVEL = "harness";
    private static final long SEED = 7_031_996L;

    private static boolean started;

    private AutoWorld() {
    }

    @SubscribeEvent
    static void onScreenOpening(ScreenEvent.Opening event) {
        if (started || !(event.getNewScreen() instanceof TitleScreen)) {
            return;
        }
        started = true;
        Minecraft minecraft = Minecraft.getInstance();
        // Loading a world opens screens of its own, so it can't happen while this one is opening.
        minecraft.tell(() -> {
            deleteLevel(minecraft.getLevelSource());
            minecraft.createWorldOpenFlows().createFreshLevel(LEVEL, settings(), new WorldOptions(SEED, true, false),
                    WorldPresets::createNormalWorldDimensions);
        });
    }

    // Survival, so the player joins as a real player would, with cheats so scenarios can set up
    // anything; a frozen clock and sky, so screenshots of the same scene look the same.
    private static LevelSettings settings() {
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        return new LevelSettings(LEVEL, GameType.SURVIVAL, false, Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT);
    }

    private static void deleteLevel(LevelStorageSource source) {
        if (!source.levelExists(LEVEL)) {
            return;
        }
        try (LevelStorageSource.LevelStorageAccess access = source.createAccess(LEVEL)) {
            access.deleteLevel();
        } catch (IOException e) {
            throw new UncheckedIOException("could not delete the previous harness world", e);
        }
    }
}
